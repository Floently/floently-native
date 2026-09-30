package com.floently.read

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
@OptIn(UnstableApi::class)
class ReadPlaybackService : MediaSessionService() {
    private var physicalPlayer: ExoPlayer? = null
    private var documentPlayer: ReadDocumentTimelinePlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val virtualPlayer = ReadDocumentTimelinePlayer(exoPlayer)

        physicalPlayer = exoPlayer
        documentPlayer = virtualPlayer
        mediaSession = MediaSession.Builder(this, virtualPlayer).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null

        documentPlayer?.release()
        documentPlayer = null

        physicalPlayer?.release()
        physicalPlayer = null

        super.onDestroy()
    }
}
