package com.floently.read

/**
 * Thin application handoff into the service-owned progressive playback owner.
 *
 * The MediaSessionService owns synthesis, cache refill and background lifetime.
 * UI/browser surfaces only provide a canonical manifest and playback intent.
 */
class ReadDocumentPlaybackLoader(
    private val playback: ReadPlaybackController
) {
    suspend fun load(
        manifest: ReadingManifestV1,
        voiceId: String,
        autoplay: Boolean = false,
        startingAt: Int = 0
    ) {
        playback.loadManifest(
            manifest = manifest,
            voiceId = voiceId,
            autoplay = autoplay,
            startingAt = startingAt
        )
    }
}
