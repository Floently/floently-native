package com.floently.read

import android.content.Context

class ReadProgressiveAudioCoordinator(
    context: Context,
    private val tts: ReadNativeTtsClient = ReadNativeTtsClient(),
    private val cache: ReadNativeAudioCache = ReadNativeAudioCache(context)
) {
    suspend fun prepare(
        manifest: ReadingManifestV1,
        startingAt: Int,
        voiceId: String,
        accessToken: String? = null,
        horizonMs: Long = 120_000L,
        maxSegments: Int = 4
    ): List<ReadPlaybackSegment> {
        if (manifest.segments.isEmpty()) return emptyList()

        val start = startingAt.coerceIn(
            0,
            manifest.segments.lastIndex
        )
        val horizon = horizonMs.coerceAtLeast(30_000L)
        val limit = maxSegments.coerceAtLeast(1)

        val selected = mutableListOf<ReadingManifestSegmentV1>()
        var buffered = 0L

        for (segment in manifest.segments.drop(start)) {
            if (selected.size >= limit) break
            selected += segment
            buffered += (
                segment.logicalEndMs - segment.logicalStartMs
            ).coerceAtLeast(0L)

            if (buffered >= horizon) break
        }

        return selected.map { segment ->
            val asset = tts.synthesize(
                text = segment.text,
                language = manifest.language,
                voiceId = voiceId,
                accessToken = accessToken
            )
            val local = cache.localFile(asset)

            ReadPlaybackSegment(
                id = segment.id,
                index = segment.index,
                logicalStartMs = segment.logicalStartMs,
                logicalEndMs = segment.logicalEndMs,
                audioUri = local.toURI().toString(),
                actualDurationMs = asset.durationMs
            )
        }
    }

    suspend fun replaceProtectedSegments(
        segments: Collection<ReadPlaybackSegment>
    ) {
        cache.replaceProtectedUris(
            segments.mapNotNull { it.audioUri }
        )
    }

    fun playableDocument(
        manifest: ReadingManifestV1,
        segments: List<ReadPlaybackSegment>,
        author: String? = null
    ): ReadPlayableDocument = ReadPlayableDocument(
        id = manifest.documentId,
        revisionId = manifest.revisionId,
        title = manifest.title,
        author = author,
        estimatedDurationMs = manifest.estimatedSourceDurationMs,
        segments = segments.sortedBy { it.index }
    )
}
