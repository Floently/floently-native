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

    fun release() {
        val value = controller
        if (value != null) {
            value.removeListener(listener)
            value.release()
            controller = null
        } else {
            MediaController.releaseFuture(future)
        }
    }

    private fun publish(player: Player) {
        val duration = resolvedDuration(player)
        val buffered = player.bufferedPosition
            .takeIf { it != C.TIME_UNSET }
            ?.coerceIn(0L, duration.coerceAtLeast(0L))
            ?: 0L

        val status = when {
            player.playbackState == Player.STATE_BUFFERING ->
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
            positionMs = player.currentPosition
                .coerceIn(0L, duration.coerceAtLeast(0L)),
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
