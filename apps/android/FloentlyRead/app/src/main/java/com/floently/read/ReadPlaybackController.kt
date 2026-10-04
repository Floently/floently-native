package com.floently.read

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

data class ReadPlayerUiSnapshot(
    val connected: Boolean = false,
    val visible: Boolean = false,
    val title: String = "Floently Read",
    val status: String = "Idle",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val speed: Float = 1f
)

class ReadPlaybackController(
    context: Context
) {
    var snapshot by mutableStateOf(ReadPlayerUiSnapshot())
        private set

    var activeLanguage by mutableStateOf("auto")
        private set

    var activeVoiceId by mutableStateOf<String?>(null)
        private set

    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command ->
        mainHandler.post(command)
    }
    private val token = SessionToken(
        applicationContext,
        ComponentName(
            applicationContext,
            ReadPlaybackService::class.java
        )
    )
    private val future: ListenableFuture<MediaController> =
        MediaController.Builder(applicationContext, token)
            .buildAsync()

    private var controller: MediaController? = null
    private var pendingDocumentLoad:
        Pair<ReadPlayableDocument, Boolean>? = null
    private var pendingManifestLoad: ReadManifestLoadRequest? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            controller?.let(::publish)
            mainHandler.postDelayed(this, 500L)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
            publish(player)
        }
    }

    init {
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { value ->
                        controller = value
                        value.addListener(listener)
                        publish(value)
                        mainHandler.removeCallbacks(refreshRunnable)
                        mainHandler.post(refreshRunnable)

                        pendingManifestLoad?.let { request ->
                            pendingManifestLoad = null
                            sendManifestLoad(
                                value,
                                request
                            )
                        }

                        pendingDocumentLoad?.let { (document, autoplay) ->
                            pendingDocumentLoad = null
                            sendDocumentLoad(
                                value,
                                document,
                                autoplay
                            )
                        }
                    }
                    .onFailure {
                        snapshot = ReadPlayerUiSnapshot(
                            status = "Player unavailable"
                        )
                    }
            },
            mainExecutor
        )
    }

    fun loadManifest(
        manifest: ReadingManifestV1,
        voiceId: String,
        autoplay: Boolean = false,
        startingAt: Int = 0
    ) {
        activeLanguage = manifest.language
        activeVoiceId = voiceId

        val request = ReadManifestLoadRequest(
            manifest = manifest,
            voiceId = voiceId,
            autoplay = autoplay,
            startingAt = startingAt.coerceAtLeast(0)
        )
        pendingDocumentLoad = null

        val player = controller
        if (player == null) {
            pendingManifestLoad = request
            snapshot = snapshot.copy(
                status = "Connecting player"
            )
            return
        }

        sendManifestLoad(
            player,
            request
        )
    }

    fun loadDocument(
        document: ReadPlayableDocument,
        autoplay: Boolean = false
    ) {
        pendingManifestLoad = null

        val player = controller
        if (player == null) {
            pendingDocumentLoad = document to autoplay
            snapshot = snapshot.copy(
                status = "Connecting player"
            )
            return
        }

        sendDocumentLoad(
            player,
            document,
            autoplay
        )
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying || player.playWhenReady) {
            player.pause()
        } else {
            player.play()
        }
        publish(player)
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val duration = resolvedDuration(player)
        player.seekTo(
            positionMs.coerceIn(
                0L,
                duration.coerceAtLeast(0L)
            )
        )
    }

    fun setSpeed(speed: Float) {
        val player = controller ?: return
        val bounded = speed.coerceIn(0.5f, 3f)
        player.playbackParameters = PlaybackParameters(bounded)
        publish(player)
    }

    fun changeVoice(voiceId: String) {
        val value = voiceId.trim()
        if (value.isBlank() || value == activeVoiceId) {
            return
        }

        val player = controller ?: return
        snapshot = snapshot.copy(
            status = "Changing voice"
        )

        val request = player.sendCustomCommand(
            ReadPlaybackCommandContract.changeVoiceCommand,
            ReadPlaybackCommandContract.encodeChangeVoice(value)
        )

        request.addListener(
            {
                runCatching { request.get() }
                    .onSuccess { result ->
                        if (
                            result.resultCode
                            == SessionResult.RESULT_SUCCESS
                        ) {
                            activeVoiceId = value
                            publish(player)
                        } else {
                            snapshot = snapshot.copy(
                                status = "Voice change failed"
                            )
                        }
                    }
                    .onFailure {
                        snapshot = snapshot.copy(
                            status = "Voice change failed"
                        )
                    }
            },
            mainExecutor
        )
    }

    fun clear() {
        pendingDocumentLoad = null
        pendingManifestLoad = null
        activeVoiceId = null
        activeLanguage = "auto"

        val player = controller
        if (player == null) {
            snapshot = ReadPlayerUiSnapshot()
            return
        }

        snapshot = snapshot.copy(
            status = "Stopping"
        )

        val request = player.sendCustomCommand(
            ReadPlaybackCommandContract.clearPlaybackCommand,
            android.os.Bundle.EMPTY
        )

        request.addListener(
            {
                runCatching { request.get() }
                    .onSuccess { result ->
                        if (
                            result.resultCode
                            == SessionResult.RESULT_SUCCESS
                        ) {
                            publish(player)
                        } else {
                            snapshot = ReadPlayerUiSnapshot(
                                connected = true
                            )
                        }
                    }
                    .onFailure {
                        snapshot = ReadPlayerUiSnapshot(
                            connected = true
                        )
                    }
            },
            mainExecutor
        )
    }

    fun release() {
        pendingDocumentLoad = null
        pendingManifestLoad = null
        activeVoiceId = null
        activeLanguage = "auto"
        mainHandler.removeCallbacks(refreshRunnable)

        val value = controller
        if (value != null) {
            value.removeListener(listener)
            value.release()
            controller = null
        } else {
            MediaController.releaseFuture(future)
        }
    }

    private fun sendManifestLoad(
        player: MediaController,
        request: ReadManifestLoadRequest
    ) {
        snapshot = snapshot.copy(
            status = "Loading document"
        )

        val handoffId =
            ReadManifestHandoffRegistry.register(request)

        val resultFuture = player.sendCustomCommand(
            ReadPlaybackCommandContract.loadManifestCommand,
            ReadPlaybackCommandContract
                .encodeLoadManifestHandoff(handoffId)
        )

        resultFuture.addListener(
            {
                runCatching { resultFuture.get() }
                    .onSuccess { result ->
                        ReadManifestHandoffRegistry.discard(
                            handoffId
                        )

                        if (
                            result.resultCode
                            != SessionResult.RESULT_SUCCESS
                        ) {
                            snapshot = snapshot.copy(
                                status = "Document load failed"
                            )
                        } else {
                            publish(player)
                        }
                    }
                    .onFailure {
                        ReadManifestHandoffRegistry.discard(
                            handoffId
                        )
                        snapshot = snapshot.copy(
                            status = "Document load failed"
                        )
                    }
            },
            mainExecutor
        )
    }

    private fun sendDocumentLoad(
        player: MediaController,
        document: ReadPlayableDocument,
        autoplay: Boolean
    ) {
        snapshot = snapshot.copy(
            status = "Loading document"
        )

        val request = player.sendCustomCommand(
            ReadPlaybackCommandContract.loadDocumentCommand,
            ReadPlaybackCommandContract.encodeLoadDocument(
                document = document,
                autoplay = autoplay
            )
        )

        request.addListener(
            {
                runCatching { request.get() }
                    .onSuccess { result ->
                        if (
                            result.resultCode
                            != SessionResult.RESULT_SUCCESS
                        ) {
                            snapshot = snapshot.copy(
                                status = "Document load failed"
                            )
                        } else {
                            publish(player)
                        }
                    }
                    .onFailure {
                        snapshot = snapshot.copy(
                            status = "Document load failed"
                        )
                    }
            },
            mainExecutor
        )
    }

    private fun publish(player: Player) {
        val duration = resolvedDuration(player)
        val position = player.currentPosition
            .coerceIn(
                0L,
                duration.coerceAtLeast(0L)
            )
        val buffered = player.bufferedPosition
            .takeIf { it != C.TIME_UNSET }
            ?.coerceIn(0L, duration.coerceAtLeast(0L))
            ?: position

        val prefixExhausted =
            player.playbackState == Player.STATE_ENDED
                && position + 1L < duration

        val status = when {
            player.playbackState == Player.STATE_BUFFERING ->
                "Preparing audio"
            prefixExhausted ->
                "Preparing audio"
            player.playbackState == Player.STATE_ENDED ->
                "Finished"
            player.isPlaying ->
                "Playing"
            player.playWhenReady ->
                "Preparing"
            player.mediaItemCount > 0 ->
                "Paused"
            else ->
                "Idle"
        }

        snapshot = ReadPlayerUiSnapshot(
            connected = true,
            visible = player.mediaItemCount > 0,
            title = player.mediaMetadata.title
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: "Floently Read",
            status = status,
            isPlaying = player.isPlaying || player.playWhenReady,
            positionMs = position,
            durationMs = duration,
            bufferedPositionMs = buffered,
            speed = player.playbackParameters.speed
        )
    }

    private fun resolvedDuration(player: Player): Long =
        player.duration
            .takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: 0L
}
