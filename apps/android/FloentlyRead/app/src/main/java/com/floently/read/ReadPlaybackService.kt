package com.floently.read

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.floently.shared.auth.FloentlySecureSessionStore
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class ReadServiceRefillTelemetry(
    var refillAttempts: Int = 0,
    var refillSuccesses: Int = 0,
    var refillFailures: Int = 0,
    var underruns: Int = 0,
    var lastFailure: String? = null
)

/**
 * Application-level owner for Floently Read media playback.
 *
 * Physical playback, progressive synthesis/cache refill, logical document
 * virtualization and durable resume all live outside Activity/Compose
 * lifecycles so background reading does not depend on a screen staying alive.
 */
@OptIn(UnstableApi::class)
class ReadPlaybackService : MediaSessionService() {
    private var physicalPlayer: ExoPlayer? = null
    private var documentPlayer: ReadDocumentTimelinePlayer? = null
    private var mediaSession: MediaSession? = null
    private var resumeStore: ReadPlaybackResumeStore? = null
    private var sessionStore: FloentlySecureSessionStore? = null
    private var coordinator: ReadProgressiveAudioCoordinator? = null

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var manifestLoadJob: Job? = null
    private var manifestLoadFuture:
        SettableFuture<SessionResult>? = null
    private var refillJob: Job? = null
    private var seekLoadJob: Job? = null
    private var voiceChangeJob: Job? = null

    private var activeManifest: ReadingManifestV1? = null
    private var activeVoiceId: String? = null
    private val preparedSegments =
        linkedMapOf<Int, ReadPlaybackSegment>()

    private var refillTelemetry =
        ReadServiceRefillTelemetry()
    private var lastUnderrunBoundaryIndex: Int? = null
    private var nextRefillAllowedAtMs = 0L

    private val refillLowWatermarkMs = 45_000L
    private val mainHandler = Handler(Looper.getMainLooper())

    private val persistResumeRunnable = object : Runnable {
        override fun run() {
            persistResume()
            mainHandler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()

        resumeStore = ReadPlaybackResumeStore(this)
        sessionStore = FloentlySecureSessionStore(this)
        coordinator = ReadProgressiveAudioCoordinator(this)

        val speechAudioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                speechAudioAttributes,
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val virtualPlayer = ReadDocumentTimelinePlayer(
            physicalPlayer = exoPlayer,
            onUnpreparedSeek = { logicalTimeMs ->
                handleProgressiveSeek(logicalTimeMs)
            }
        )

        physicalPlayer = exoPlayer
        documentPlayer = virtualPlayer
        mediaSession = MediaSession.Builder(this, virtualPlayer)
            .setCallback(
                object : MediaSession.Callback {
                    override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): MediaSession.ConnectionResult {
                        val defaults =
                            MediaSession.ConnectionResult.AcceptedResultBuilder(
                                session,
                                controller
                            ).build()

                        val commandsBuilder = defaults
                            .availableSessionCommands
                            .buildUpon()

                        if (controller.packageName == packageName) {
                            commandsBuilder.add(
                                ReadPlaybackCommandContract
                                    .loadDocumentCommand
                            )
                            commandsBuilder.add(
                                ReadPlaybackCommandContract
                                    .loadManifestCommand
                            )
                            commandsBuilder.add(
                                ReadPlaybackCommandContract
                                    .changeVoiceCommand
                            )
                        }

                        return MediaSession.ConnectionResult
                            .AcceptedResultBuilder(
                                session,
                                controller
                            )
                            .setAvailableSessionCommands(
                                commandsBuilder.build()
                            )
                            .build()
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: SessionCommand,
                        args: Bundle
                    ): ListenableFuture<SessionResult> {
                        if (controller.packageName != packageName) {
                            return Futures.immediateFuture(
                                SessionResult(
                                    SessionResult
                                        .RESULT_ERROR_PERMISSION_DENIED
                                )
                            )
                        }

                        return when (customCommand.customAction) {
                            ReadPlaybackCommandContract
                                .ACTION_LOAD_MANIFEST -> {
                                val request =
                                    ReadPlaybackCommandContract
                                        .decodeLoadManifest(args)
                                        ?: return Futures.immediateFuture(
                                            SessionResult(
                                                SessionResult
                                                    .RESULT_ERROR_BAD_VALUE
                                            )
                                        )

                                startManifestLoad(request)
                            }

                            ReadPlaybackCommandContract
                                .ACTION_LOAD_DOCUMENT -> {
                                val decoded =
                                    ReadPlaybackCommandContract
                                        .decodeLoadDocument(args)
                                        ?: return Futures.immediateFuture(
                                            SessionResult(
                                                SessionResult
                                                    .RESULT_ERROR_BAD_VALUE
                                            )
                                        )

                                loadPlayableDocument(decoded)
                            }

                            ReadPlaybackCommandContract
                                .ACTION_CHANGE_VOICE -> {
                                val voiceId =
                                    ReadPlaybackCommandContract
                                        .decodeChangeVoice(args)
                                        ?: return Futures.immediateFuture(
                                            SessionResult(
                                                SessionResult
                                                    .RESULT_ERROR_BAD_VALUE
                                            )
                                        )

                                startVoiceChange(voiceId)
                            }

                            else -> Futures.immediateFuture(
                                SessionResult(
                                    SessionResult
                                        .RESULT_ERROR_NOT_SUPPORTED
                                )
                            )
                        }
                    }
                }
            )
            .build()

        mainHandler.postDelayed(
            persistResumeRunnable,
            5_000L
        )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        persistResume()
        mainHandler.removeCallbacks(persistResumeRunnable)

        cancelProgressiveWork(
            completePendingLoad = true,
            clearActiveManifest = true
        )
        serviceScope.cancel()

        mediaSession?.release()
        mediaSession = null

        documentPlayer?.release()
        documentPlayer = null

        physicalPlayer?.release()
        physicalPlayer = null
        resumeStore = null
        sessionStore = null
        coordinator = null

        super.onDestroy()
    }

    private fun startManifestLoad(
        request: ReadManifestLoadRequest
    ): ListenableFuture<SessionResult> {
        val player = documentPlayer
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )
        val audioCoordinator = coordinator
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )

        cancelProgressiveWork(
            completePendingLoad = true,
            clearActiveManifest = false
        )

        activeManifest = request.manifest
        activeVoiceId = request.voiceId
        preparedSegments.clear()
        refillTelemetry = ReadServiceRefillTelemetry()
        lastUnderrunBoundaryIndex = null
        nextRefillAllowedAtMs = 0L

        val future = SettableFuture.create<SessionResult>()
        manifestLoadFuture = future

        manifestLoadJob = serviceScope.launch {
            try {
                val startingAt = resolvedStartingIndex(
                    manifest = request.manifest,
                    requestedIndex = request.startingAt
                )
                val segments = audioCoordinator.prepare(
                    manifest = request.manifest,
                    startingAt = startingAt,
                    voiceId = request.voiceId,
                    accessToken = currentAccessToken(),
                    maxSegments = 1
                )

                preparedSegments.clear()
                segments.forEach {
                    preparedSegments[it.index] = it
                }

                val document = audioCoordinator.playableDocument(
                    manifest = request.manifest,
                    segments = preparedSegments.values.toList()
                )
                val saved = resumeStore?.load(
                    documentId = document.id,
                    revisionId = document.revisionId
                )

                player.loadDocument(
                    value = document,
                    autoplay = request.autoplay,
                    resumePositionMs = saved?.logicalTimeMs,
                    resumeSpeed = saved?.playbackSpeed
                )
                persistResume()

                future.set(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS
                    )
                )
                startRefillLoop(
                    manifest = request.manifest,
                    voiceId = request.voiceId
                )
            } catch (error: CancellationException) {
                if (!future.isDone) {
                    future.set(
                        SessionResult(
                            SessionResult
                                .RESULT_ERROR_INVALID_STATE
                        )
                    )
                }
                throw error
            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "Initial progressive document load failed",
                    error
                )
                if (!future.isDone) {
                    future.set(
                        SessionResult(
                            SessionResult
                                .RESULT_ERROR_INVALID_STATE
                        )
                    )
                }
            } finally {
                if (manifestLoadFuture === future) {
                    manifestLoadFuture = null
                }
            }
        }

        return future
    }

    private fun loadPlayableDocument(
        decoded: Pair<ReadPlayableDocument, Boolean>
    ): ListenableFuture<SessionResult> {
        val player = documentPlayer
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )

        cancelProgressiveWork(
            completePendingLoad = true,
            clearActiveManifest = true
        )

        val (document, autoplay) = decoded
        val saved = resumeStore?.load(
            documentId = document.id,
            revisionId = document.revisionId
        )

        player.loadDocument(
            value = document,
            autoplay = autoplay,
            resumePositionMs = saved?.logicalTimeMs,
            resumeSpeed = saved?.playbackSpeed
        )
        persistResume()

        return Futures.immediateFuture(
            SessionResult(
                SessionResult.RESULT_SUCCESS
            )
        )
    }

    private fun startRefillLoop(
        manifest: ReadingManifestV1,
        voiceId: String
    ) {
        refillJob?.cancel()

        refillJob = serviceScope.launch {
            while (isActive) {
                // Keep refill on the existing cadence until ordinary queue
                // growth is append-only and acoustically qualified.
                delay(1_000L)

                val player = documentPlayer ?: return@launch
                val current = player.currentDocument()
                    ?: return@launch

                if (
                    current.id != manifest.documentId
                    || current.revisionId != manifest.revisionId
                    || activeManifest?.documentId
                        != manifest.documentId
                    || activeManifest?.revisionId
                        != manifest.revisionId
                ) {
                    return@launch
                }

                val highestPrepared =
                    preparedSegments.keys.maxOrNull()
                        ?: continue
                val nextIndex = highestPrepared + 1

                if (nextIndex >= manifest.segments.size) {
                    return@launch
                }

                val logicalPosition =
                    player.currentLogicalPositionMs()
                val readyEnd = preparedSegments[highestPrepared]
                    ?.logicalEndMs
                    ?: continue
                val exhausted =
                    player
                        .hasExhaustedPreparedWindowBeforeDocumentEnd()

                if (
                    exhausted
                    && lastUnderrunBoundaryIndex
                        != highestPrepared
                ) {
                    lastUnderrunBoundaryIndex = highestPrepared
                    refillTelemetry.underruns += 1
                    logTelemetry("prepared-window underrun")
                }

                val bufferedAhead =
                    (readyEnd - logicalPosition)
                        .coerceAtLeast(0L)

                if (
                    !exhausted
                    && bufferedAhead > refillLowWatermarkMs
                ) {
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                if (now < nextRefillAllowedAtMs) {
                    continue
                }

                refillTelemetry.refillAttempts += 1

                try {
                    val more = coordinator?.prepare(
                        manifest = manifest,
                        startingAt = nextIndex,
                        voiceId = voiceId,
                        accessToken = currentAccessToken()
                    ) ?: return@launch

                    more.forEach {
                        preparedSegments[it.index] = it
                    }

                    val updated = coordinator
                        ?.playableDocument(
                            manifest = manifest,
                            segments =
                                preparedSegments.values.toList()
                        )
                        ?: return@launch

                    player.loadDocument(value = updated)
                    refillTelemetry.refillSuccesses += 1
                    refillTelemetry.lastFailure = null
                    nextRefillAllowedAtMs = 0L
                    logTelemetry("refill success")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    refillTelemetry.refillFailures += 1
                    refillTelemetry.lastFailure =
                        error.localizedMessage
                    nextRefillAllowedAtMs =
                        SystemClock.elapsedRealtime() + 5_000L
                    Log.w(
                        TAG,
                        "Progressive refill failed; playable audio retained",
                        error
                    )
                    logTelemetry("refill failure")
                }
            }
        }
    }

    private fun handleProgressiveSeek(
        logicalTimeMs: Long
    ) {
        val manifest = activeManifest ?: return
        val voiceId = activeVoiceId ?: return
        val player = documentPlayer ?: return
        val audioCoordinator = coordinator ?: return

        seekLoadJob?.cancel()
        refillJob?.cancel()

        val bounded = logicalTimeMs.coerceIn(
            0L,
            manifest.estimatedSourceDurationMs
                .coerceAtLeast(0L)
        )
        val targetIndex = manifest.segments.indexOfFirst {
            bounded < it.logicalEndMs
        }.takeIf { it >= 0 }
            ?: manifest.segments.lastIndex

        if (targetIndex < 0) return

        refillTelemetry.refillAttempts += 1

        seekLoadJob = serviceScope.launch {
            try {
                val segments = audioCoordinator.prepare(
                    manifest = manifest,
                    startingAt = targetIndex,
                    voiceId = voiceId,
                    accessToken = currentAccessToken(),
                    maxSegments = 1
                )

                preparedSegments.clear()
                segments.forEach {
                    preparedSegments[it.index] = it
                }

                val updated = audioCoordinator.playableDocument(
                    manifest = manifest,
                    segments = preparedSegments.values.toList()
                )

                player.loadDocument(
                    value = updated,
                    overridePositionMs = bounded
                )

                refillTelemetry.refillSuccesses += 1
                refillTelemetry.lastFailure = null
                lastUnderrunBoundaryIndex = null
                nextRefillAllowedAtMs = 0L
                logTelemetry("progressive seek loaded")

                startRefillLoop(
                    manifest = manifest,
                    voiceId = voiceId
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                refillTelemetry.refillFailures += 1
                refillTelemetry.lastFailure =
                    error.localizedMessage
                Log.e(
                    TAG,
                    "Progressive seek synthesis failed",
                    error
                )
                logTelemetry("progressive seek failure")
            }
        }
    }

    private fun startVoiceChange(
        voiceId: String
    ): ListenableFuture<SessionResult> {
        val manifest = activeManifest
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )
        val player = documentPlayer
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )
        val audioCoordinator = coordinator
            ?: return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )

        val newVoice = voiceId.trim()
        if (newVoice.isBlank()) {
            return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_ERROR_BAD_VALUE
                )
            )
        }

        if (newVoice == activeVoiceId) {
            return Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS
                )
            )
        }

        val cursor = player.currentLogicalPositionMs()
        val previousVoice = activeVoiceId
        activeVoiceId = newVoice

        refillJob?.cancel()
        refillJob = null
        seekLoadJob?.cancel()
        seekLoadJob = null
        voiceChangeJob?.cancel()
        preparedSegments.clear()
        lastUnderrunBoundaryIndex = null
        nextRefillAllowedAtMs = 0L

        player.prepareForAudioReplacement()

        val future = SettableFuture.create<SessionResult>()

        voiceChangeJob = serviceScope.launch {
            try {
                val targetIndex = manifest.segments.indexOfFirst {
                    cursor < it.logicalEndMs
                }.takeIf { it >= 0 }
                    ?: manifest.segments.lastIndex

                if (targetIndex < 0) {
                    future.set(
                        SessionResult(
                            SessionResult.RESULT_ERROR_INVALID_STATE
                        )
                    )
                    return@launch
                }

                val segments = audioCoordinator.prepare(
                    manifest = manifest,
                    startingAt = targetIndex,
                    voiceId = newVoice,
                    accessToken = currentAccessToken(),
                    maxSegments = 1
                )

                segments.forEach {
                    preparedSegments[it.index] = it
                }

                val updated = audioCoordinator.playableDocument(
                    manifest = manifest,
                    segments = preparedSegments.values.toList()
                )

                player.loadDocument(
                    value = updated,
                    overridePositionMs = cursor
                )

                startRefillLoop(
                    manifest = manifest,
                    voiceId = newVoice
                )
                persistResume()

                future.set(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS
                    )
                )
            } catch (error: CancellationException) {
                if (!future.isDone) {
                    future.set(
                        SessionResult(
                            SessionResult.RESULT_ERROR_INVALID_STATE
                        )
                    )
                }
                throw error
            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "Voice change synthesis failed",
                    error
                )
                activeVoiceId = previousVoice

                if (
                    previousVoice != null
                    && activeManifest === manifest
                ) {
                    handleProgressiveSeek(cursor)
                }

                if (!future.isDone) {
                    future.set(
                        SessionResult(
                            SessionResult.RESULT_ERROR_INVALID_STATE
                        )
                    )
                }
            } finally {
                voiceChangeJob = null
            }
        }

        return future
    }

    private fun resolvedStartingIndex(
        manifest: ReadingManifestV1,
        requestedIndex: Int
    ): Int {
        if (manifest.segments.isEmpty()) return 0

        val boundedRequested = requestedIndex.coerceIn(
            0,
            manifest.segments.lastIndex
        )
        if (requestedIndex != 0) {
            return boundedRequested
        }

        val saved = resumeStore?.load(
            documentId = manifest.documentId,
            revisionId = manifest.revisionId
        ) ?: return boundedRequested

        return manifest.segments.indexOfFirst {
            saved.logicalTimeMs < it.logicalEndMs
        }.takeIf { it >= 0 }
            ?: manifest.segments.lastIndex
    }

    private fun currentAccessToken(): String? =
        sessionStore?.session?.token

    private fun cancelProgressiveWork(
        completePendingLoad: Boolean,
        clearActiveManifest: Boolean
    ) {
        if (
            completePendingLoad
            && manifestLoadFuture?.isDone == false
        ) {
            manifestLoadFuture?.set(
                SessionResult(
                    SessionResult.RESULT_ERROR_INVALID_STATE
                )
            )
        }

        manifestLoadJob?.cancel()
        manifestLoadJob = null
        manifestLoadFuture = null

        refillJob?.cancel()
        refillJob = null

        seekLoadJob?.cancel()
        seekLoadJob = null

        voiceChangeJob?.cancel()
        voiceChangeJob = null

        preparedSegments.clear()
        lastUnderrunBoundaryIndex = null
        nextRefillAllowedAtMs = 0L

        if (clearActiveManifest) {
            activeManifest = null
            activeVoiceId = null
        }
    }

    private fun persistResume() {
        val player = documentPlayer ?: return
        val document = player.currentDocument() ?: return
        val store = resumeStore ?: return

        if (player.isDocumentEnded()) {
            store.remove(
                documentId = document.id,
                revisionId = document.revisionId
            )
            return
        }

        store.save(
            ReadPlaybackResumeSnapshot(
                documentId = document.id,
                revisionId = document.revisionId,
                logicalTimeMs =
                    player.currentLogicalPositionMs()
                        .coerceIn(
                            0L,
                            document.logicalDurationMs
                                .coerceAtLeast(0L)
                        ),
                playbackSpeed =
                    player.currentPlaybackSpeed(),
                updatedAtMs =
                    System.currentTimeMillis()
            )
        )
    }

    private fun logTelemetry(reason: String) {
        Log.d(
            TAG,
            reason
                + " attempts=" + refillTelemetry.refillAttempts
                + " successes=" + refillTelemetry.refillSuccesses
                + " failures=" + refillTelemetry.refillFailures
                + " underruns=" + refillTelemetry.underruns
                + " lastFailure="
                + (refillTelemetry.lastFailure ?: "none")
        )
    }

    companion object {
        private const val TAG = "FloentlyReadPlayback"
    }
}
