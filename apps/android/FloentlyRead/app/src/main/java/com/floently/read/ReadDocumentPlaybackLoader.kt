package com.floently.read

import android.content.Context
import com.floently.shared.auth.FloentlySecureSessionStore

class ReadDocumentPlaybackLoader(
    context: Context,
    private val playback: ReadPlaybackController,
    private val sessionStore: FloentlySecureSessionStore =
        FloentlySecureSessionStore(context),
    private val coordinator: ReadProgressiveAudioCoordinator =
        ReadProgressiveAudioCoordinator(context)
) {
    suspend fun load(
        manifest: ReadingManifestV1,
        voiceId: String,
        autoplay: Boolean = false,
        startingAt: Int = 0
    ) {
        val accessToken = sessionStore.session?.token

        val segments = coordinator.prepare(
            manifest = manifest,
            startingAt = startingAt,
            voiceId = voiceId,
            accessToken = accessToken
        )

        val document = coordinator.playableDocument(
            manifest = manifest,
            segments = segments
        )

        playback.loadDocument(
            document = document,
            autoplay = autoplay
        )
    }
}
