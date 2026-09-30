package com.floently.read

import android.os.Bundle
import androidx.annotation.OptIn
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

                        val commands = defaults
                            .availableSessionCommands
                            .buildUpon()
                            .add(
                                ReadPlaybackCommandContract.loadDocumentCommand
                            )
                            .build()

                        return MediaSession.ConnectionResult
                            .AcceptedResultBuilder(
                                session,
                                controller
                            )
                            .setAvailableSessionCommands(commands)
                            .build()
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: SessionCommand,
                        args: Bundle
                    ): ListenableFuture<SessionResult> {
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
                        player.loadDocument(
                            value = document,
                            autoplay = autoplay
                        )

                        return Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
                    }
                }
            )
            .build()
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
