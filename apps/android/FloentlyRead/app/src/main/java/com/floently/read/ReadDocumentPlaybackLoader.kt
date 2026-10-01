package com.floently.read

import android.content.Context
import com.floently.shared.auth.FloentlySecureSessionStore

class ReadDocumentPlaybackLoader(
    context: Context,
    private val playback: ReadPlaybackController,
    private val sessionStore: FloentlySecureSessionStore =
        FloentlySecureSessionStore(context),
    private val coordinator: ReadProgressiveAudioCoordinator =
        ReadProgressiveAudioCoordinator(context),
    private val resumeStore: ReadPlaybackResumeStore =
        ReadPlaybackResumeStore(context)
) {
    suspend fun load(
        manifest: ReadingManifestV1,
        voiceId: String,
        autoplay: Boolean = false,
        startingAt: Int = 0
    ) {
        val accessToken = sessionStore.session?.token
        val effectiveStartingAt = resolvedStartingIndex(
            manifest = manifest,
            requestedIndex = startingAt
        )

        val segments = coordinator.prepare(
            manifest = manifest,
            startingAt = effectiveStartingAt,
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

    private fun resolvedStartingIndex(
        manifest: ReadingManifestV1,
        requestedIndex: Int
    ): Int {
        if (requestedIndex != 0 || manifest.segments.isEmpty()) {
            return requestedIndex
        }

        val snapshot = resumeStore.load(
            documentId = manifest.documentId,
            revisionId = manifest.revisionId
        ) ?: return requestedIndex

        return manifest.segments.indexOfFirst {
            snapshot.logicalTimeMs < it.logicalEndMs
        }.takeIf { it >= 0 }
            ?: manifest.segments.lastIndex
    }
}
