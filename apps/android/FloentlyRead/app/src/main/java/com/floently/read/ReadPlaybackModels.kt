package com.floently.read

/**
 * Platform model for ReadingManifest v1.
 *
 * Segment media is an implementation detail. Public playback state is always
 * expressed in document time.
 */
data class ReadPlaybackSegment(
    val id: String,
    val index: Int,
    val logicalStartMs: Long,
    val logicalEndMs: Long,
    val audioUri: String? = null,
    val actualDurationMs: Long? = null
) {
    val logicalDurationMs: Long
        get() = (logicalEndMs - logicalStartMs).coerceAtLeast(0L)
}

data class ReadPlayableDocument(
    val id: String,
    val revisionId: String,
    val title: String,
    val author: String? = null,
    val estimatedDurationMs: Long,
    val segments: List<ReadPlaybackSegment>
) {
    val logicalDurationMs: Long
        get() = maxOf(
            estimatedDurationMs,
            segments.lastOrNull()?.logicalEndMs ?: 0L
        )
}

data class ReadSegmentPosition(
    val segment: ReadPlaybackSegment,
    val localOffsetMs: Long
)

object ReadPlaybackTimeline {
    fun segmentAndOffset(
        document: ReadPlayableDocument,
        logicalTimeMs: Long
    ): ReadSegmentPosition? {
        if (document.segments.isEmpty()) return null

        val bounded = logicalTimeMs.coerceIn(0L, document.logicalDurationMs)
        val segment = document.segments.firstOrNull {
            bounded < it.logicalEndMs
        } ?: document.segments.last()

        return ReadSegmentPosition(
            segment = segment,
            localOffsetMs = (bounded - segment.logicalStartMs)
                .coerceIn(0L, segment.logicalDurationMs)
        )
    }

    fun logicalTime(
        segment: ReadPlaybackSegment,
        localOffsetMs: Long,
        documentDurationMs: Long
    ): Long {
        return (segment.logicalStartMs + localOffsetMs.coerceAtLeast(0L))
            .coerceIn(0L, documentDurationMs.coerceAtLeast(0L))
    }

    fun progress(document: ReadPlayableDocument, logicalTimeMs: Long): Double {
        val duration = document.logicalDurationMs
        if (duration <= 0L) return 0.0
        return logicalTimeMs.coerceIn(0L, duration).toDouble() / duration.toDouble()
    }

    fun prefetchIndexes(
        document: ReadPlayableDocument,
        activeIndex: Int,
        horizonMs: Long = 120_000L,
        maxSegments: Int = 4
    ): List<Int> {
        val result = mutableListOf<Int>()
        var bufferedMs = 0L
        val target = horizonMs.coerceAtLeast(30_000L)
        val limit = maxSegments.coerceAtLeast(1)

        for (index in (activeIndex + 1).coerceAtLeast(0) until document.segments.size) {
            if (result.size >= limit) break
            result += index
            bufferedMs += document.segments[index].logicalDurationMs
            if (bufferedMs >= target) break
        }
        return result
    }
}
