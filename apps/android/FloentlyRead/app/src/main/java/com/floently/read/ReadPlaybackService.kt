package com.floently.read

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Application-level owner for Floently Read media playback.
 *
 * This service intentionally lives outside Activity/Compose lifecycles so
 * navigation cannot destroy the player. The next phase will place a
 * document-timeline adapter in front of ExoPlayer so MediaSession exposes one
 * logical document duration/position while hidden TTS segments remain private.
 */
class ReadPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
