package com.floently.read

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Application-level owner for Floently Read media playback.
 *
 * The service owns physical playback, logical document virtualization and
 * durable resume state outside Activity/Compose lifecycles.
 */
@OptIn(UnstableApi::class)
class ReadPlaybackService : MediaSessionService() {
    private var physicalPlayer: ExoPlayer? = null
    private var documentPlayer: ReadDocumentTimelinePlayer? = null
    private var mediaSession: MediaSession? = null
    private var resumeStore: ReadPlaybackResumeStore? = null

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

        val virtualPlayer = ReadDocumentTimelinePlayer(exoPlayer)

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
                                ReadPlaybackCommandContract.loadDocumentCommand
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
                                    SessionResult.RESULT_ERROR_PERMISSION_DENIED
                                )
                            )
                        }

                        if (
                            customCommand.customAction
                            != ReadPlaybackCommandContract.ACTION_LOAD_DOCUMENT
                        ) {
                            return Futures.immediateFuture(
                                SessionResult(
                                    SessionResult.RESULT_ERROR_NOT_SUPPORTED
                                )
                            )
                        }

                        val decoded =
                            ReadPlaybackCommandContract
                                .decodeLoadDocument(args)
                                ?: return Futures.immediateFuture(
                                    SessionResult(
                                        SessionResult.RESULT_ERROR_BAD_VALUE
                                    )
                                )

                        val player = documentPlayer
                            ?: return Futures.immediateFuture(
                                SessionResult(
                                    SessionResult.RESULT_ERROR_INVALID_STATE
                                )
                            )

                        val (document, autoplay) = decoded
                        val saved = resumeStore?.load(
                            documentId = document.id,
                            revisionId = document.revisionId
                        )

                        player.loadDocument(
                            value = document,
                            autoplay = autoplay,
                            resumePositionMs =
                                saved?.logicalTimeMs,
                            resumeSpeed =
                                saved?.playbackSpeed
                        )
                        persistResume()

                        return Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
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

        mediaSession?.release()
        mediaSession = null

        documentPlayer?.release()
        documentPlayer = null

        physicalPlayer?.release()
        physicalPlayer = null
        resumeStore = null

        super.onDestroy()
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
}
