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
    val actualDurationMs: Long? = null,
    val renditionId: String? = null,
    val timingMapId: String? = null
) {
    val logicalDurationMs: Long
        get() = (logicalEndMs - logicalStartMs).coerceAtLeast(0L)

    fun logicalOffsetForPhysical(
        physicalOffsetMs: Long,
        resolvedPhysicalDurationMs: Long? = actualDurationMs
    ): Long {
        val physicalDuration = resolvedPhysicalDurationMs
            ?.takeIf { it > 0L }

        if (physicalDuration == null || logicalDurationMs <= 0L) {
            return physicalOffsetMs
                .coerceIn(0L, logicalDurationMs)
        }

        val fraction = physicalOffsetMs
            .coerceIn(0L, physicalDuration)
            .toDouble() / physicalDuration.toDouble()

        return (fraction * logicalDurationMs)
            .toLong()
            .coerceIn(0L, logicalDurationMs)
    }

    fun physicalOffsetForLogical(
        logicalOffsetMs: Long,
        resolvedPhysicalDurationMs: Long? = actualDurationMs
    ): Long {
        val physicalDuration = resolvedPhysicalDurationMs
            ?.takeIf { it > 0L }

        if (physicalDuration == null || logicalDurationMs <= 0L) {
            return logicalOffsetMs
                .coerceIn(0L, logicalDurationMs)
        }

        val fraction = logicalOffsetMs
            .coerceIn(0L, logicalDurationMs)
            .toDouble() / logicalDurationMs.toDouble()

        return (fraction * physicalDuration)
            .toLong()
            .coerceIn(0L, physicalDuration)
    }
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
        physicalOffsetMs: Long,
        documentDurationMs: Long,
        resolvedPhysicalDurationMs: Long? = segment.actualDurationMs
    ): Long {
        val logicalOffset = segment.logicalOffsetForPhysical(
            physicalOffsetMs = physicalOffsetMs,
            resolvedPhysicalDurationMs = resolvedPhysicalDurationMs
        )

        return (segment.logicalStartMs + logicalOffset)
            .coerceIn(0L, documentDurationMs.coerceAtLeast(0L))
    }

    fun physicalOffset(
        segment: ReadPlaybackSegment,
        logicalOffsetMs: Long,
        resolvedPhysicalDurationMs: Long? = segment.actualDurationMs
    ): Long = segment.physicalOffsetForLogical(
        logicalOffsetMs = logicalOffsetMs,
        resolvedPhysicalDurationMs = resolvedPhysicalDurationMs
    )

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
