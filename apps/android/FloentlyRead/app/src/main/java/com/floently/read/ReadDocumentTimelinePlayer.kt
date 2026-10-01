package com.floently.read

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * System-facing player that virtualizes hidden TTS segments as one document.
 *
 * ExoPlayer owns physical clip playback. MediaSession sees one logical item,
 * with document-wide duration/position/buffering and document-time seeking.
 */
@OptIn(UnstableApi::class)
class ReadDocumentTimelinePlayer(
    private val physicalPlayer: ExoPlayer,
    private val onUnpreparedSeek: ((Long) -> Unit)? = null
) : ForwardingSimpleBasePlayer(physicalPlayer) {

    private var document: ReadPlayableDocument? = null
    private var resumeAfterProgressiveSeek = false

    fun currentDocument(): ReadPlayableDocument? = document

    fun currentLogicalPositionMs(): Long {
        val value = document ?: return 0L
        return logicalPosition(value)
    }

    fun currentPlaybackSpeed(): Float =
        physicalPlayer.playbackParameters.speed
            .coerceIn(0.5f, 3f)

    fun isDocumentEnded(): Boolean {
        val value = document ?: return false
        return physicalPlayer.playbackState == Player.STATE_ENDED
            && currentLogicalPositionMs() >= value.logicalDurationMs
    }

    fun hasExhaustedPreparedWindowBeforeDocumentEnd(): Boolean {
        val value = document ?: return false
        return physicalPlayer.playbackState == Player.STATE_ENDED
            && currentLogicalPositionMs() < value.logicalDurationMs
    }

    fun loadDocument(
        value: ReadPlayableDocument,
        autoplay: Boolean = false,
        resumePositionMs: Long? = null,
        resumeSpeed: Float? = null,
        overridePositionMs: Long? = null
    ) {
        val previous = document
        val sameDocument = previous?.id == value.id
            && previous.revisionId == value.revisionId
        val previousPosition = if (sameDocument && previous != null) {
            logicalPosition(previous)
        } else {
            null
        }
        val previousPlayWhenReady =
            sameDocument && physicalPlayer.playWhenReady
        val progressiveSeekShouldResume =
            sameDocument
                && overridePositionMs != null
                && resumeAfterProgressiveSeek
        val previousSpeed = physicalPlayer.playbackParameters.speed

        if (
            sameDocument
            && previous != null
            && overridePositionMs == null
            && isAppendOnlyExpansion(
                previous = previous,
                updated = value
            )
            && physicalPlayer.mediaItemCount
                == previous.segments.size
        ) {
            val appended = value.segments.drop(
                previous.segments.size
            )
            val wasEnded =
                physicalPlayer.playbackState
                    == Player.STATE_ENDED
            val shouldResume =
                physicalPlayer.playWhenReady

            document = value

            val appendedItems = appended.mapNotNull {
                mediaItemForSegment(
                    segment = it,
                    document = value
                )
            }

            if (appendedItems.isNotEmpty()) {
                physicalPlayer.addMediaItems(
                    appendedItems
                )

                if (wasEnded) {
                    val nextIndex =
                        previous.segments.size
                    physicalPlayer.seekTo(
                        nextIndex,
                        0L
                    )
                    physicalPlayer.prepare()
                    physicalPlayer.playWhenReady =
                        shouldResume
                }
            }

            invalidateState()
            return
        }

        document = value

        val items = value.segments.mapNotNull { segment ->
            mediaItemForSegment(
                segment = segment,
                document = value
            )
        }

        if (items.isEmpty()) {
            physicalPlayer.stop()
            physicalPlayer.clearMediaItems()
            physicalPlayer.playWhenReady = false
            invalidateState()
            return
        }

        val targetLogical = (
            overridePositionMs
                ?: if (sameDocument) {
                    previousPosition
                } else {
                    resumePositionMs
                }
                ?: 0L
        ).coerceIn(
            0L,
            value.logicalDurationMs.coerceAtLeast(0L)
        )

        val target = availableSegmentAndOffset(
            document = value,
            logicalTimeMs = targetLogical
        ) ?: ReadSegmentPosition(
            segment = value.segments.first(),
            localOffsetMs = 0L
        )

        val targetQueueIndex = value.segments.indexOfFirst {
            it.index == target.segment.index
                && it.id == target.segment.id
        }.takeIf { it >= 0 } ?: 0

        val physicalOffset = ReadPlaybackTimeline.physicalOffset(
            segment = target.segment,
            logicalOffsetMs = target.localOffsetMs,
            resolvedPhysicalDurationMs =
                target.segment.actualDurationMs
        )

        physicalPlayer.setMediaItems(
            items,
            targetQueueIndex.coerceIn(0, items.lastIndex),
            physicalOffset
        )

        val speed = if (sameDocument) {
            previousSpeed
        } else {
            resumeSpeed ?: 1f
        }.coerceIn(0.5f, 3f)
        physicalPlayer.playbackParameters =
            PlaybackParameters(speed)

        physicalPlayer.prepare()
        physicalPlayer.playWhenReady =
            if (sameDocument) {
                previousPlayWhenReady || progressiveSeekShouldResume
            } else {
                autoplay
            }
        resumeAfterProgressiveSeek = false
        invalidateState()
    }

    fun prepareForAudioReplacement() {
        resumeAfterProgressiveSeek =
            physicalPlayer.playWhenReady
        physicalPlayer.pause()
        physicalPlayer.clearMediaItems()
        invalidateState()
    }

    fun clearDocument() {
        document = null
        resumeAfterProgressiveSeek = false
        physicalPlayer.stop()
        physicalPlayer.clearMediaItems()
        invalidateState()
    }

    override fun getState(): SimpleBasePlayer.State {
        val state = super.getState()
        val value = document ?: return state

        val logicalItem = SimpleBasePlayer.MediaItemData.Builder(
            "read-document:" + value.id + ":" + value.revisionId
        )
            .setMediaItem(
                MediaItem.Builder()
                    .setMediaId(value.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(value.title)
                            .setArtist(value.author ?: "Floently Read")
                            .build()
                    )
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(value.title)
                    .setArtist(value.author ?: "Floently Read")
                    .build()
            )
            .setDurationUs(value.logicalDurationMs * 1_000L)
            .setIsSeekable(true)
            .build()

        val activeQueueIndex = physicalPlayer.currentMediaItemIndex
        val active = value.segments.getOrNull(activeQueueIndex)

        val resolvedPhysicalDuration = physicalPlayer.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: active?.actualDurationMs

        val logicalPosition = if (active != null) {
            ReadPlaybackTimeline.logicalTime(
                segment = active,
                physicalOffsetMs = physicalPlayer.currentPosition,
                documentDurationMs = value.logicalDurationMs,
                resolvedPhysicalDurationMs = resolvedPhysicalDuration
            )
        } else {
            value.segments.firstOrNull()?.logicalStartMs ?: 0L
        }

        val logicalBuffered = if (active != null) {
            val currentBufferedLogical =
                ReadPlaybackTimeline.logicalTime(
                    segment = active,
                    physicalOffsetMs = physicalPlayer.bufferedPosition,
                    documentDurationMs = value.logicalDurationMs,
                    resolvedPhysicalDurationMs =
                        resolvedPhysicalDuration
                )

            val futureReady = value.segments
                .drop(activeQueueIndex + 1)
                .sumOf { it.logicalDurationMs }

            (currentBufferedLogical + futureReady)
                .coerceIn(
                    logicalPosition,
                    value.logicalDurationMs
                )
        } else {
            logicalPosition
        }

        return state.buildUpon()
            .setPlaylist(listOf(logicalItem))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(logicalPosition)
            .setContentBufferedPositionMs(
                SimpleBasePlayer.PositionSupplier.getConstant(
                    logicalBuffered
                )
            )
            .build()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        @Player.Command seekCommand: Int
    ): ListenableFuture<*> {
        val value = document
            ?: return super.handleSeek(
                mediaItemIndex,
                positionMs,
                seekCommand
            )

        val requested = (
            if (positionMs == C.TIME_UNSET) 0L else positionMs
        ).coerceIn(
            0L,
            value.logicalDurationMs.coerceAtLeast(0L)
        )

        val target = availableSegmentAndOffset(
            document = value,
            logicalTimeMs = requested
        )

        if (target == null) {
            resumeAfterProgressiveSeek =
                physicalPlayer.playWhenReady
            physicalPlayer.pause()
            invalidateState()
            onUnpreparedSeek?.invoke(requested)
            return Futures.immediateVoidFuture()
        }

        val queueIndex = value.segments.indexOfFirst {
            it.index == target.segment.index
                && it.id == target.segment.id
        }
        if (queueIndex < 0) {
            return Futures.immediateVoidFuture()
        }

        val physicalOffset = ReadPlaybackTimeline.physicalOffset(
            segment = target.segment,
            logicalOffsetMs = target.localOffsetMs,
            resolvedPhysicalDurationMs =
                target.segment.actualDurationMs
        )

        physicalPlayer.seekTo(
            queueIndex,
            physicalOffset
        )
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    private fun isAppendOnlyExpansion(
        previous: ReadPlayableDocument,
        updated: ReadPlayableDocument
    ): Boolean {
        if (
            updated.segments.size
                < previous.segments.size
            || updated.segments.isEmpty()
        ) {
            return false
        }

        return updated.segments
            .take(previous.segments.size)
            == previous.segments
    }

    private fun mediaItemForSegment(
        segment: ReadPlaybackSegment,
        document: ReadPlayableDocument
    ): MediaItem? {
        val uri = segment.audioUri
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return MediaItem.Builder()
            .setMediaId(segment.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(document.title)
                    .setArtist(
                        document.author
                            ?: "Floently Read"
                    )
                    .build()
            )
            .build()
    }

    private fun availableSegmentAndOffset(
        document: ReadPlayableDocument,
        logicalTimeMs: Long
    ): ReadSegmentPosition? {
        if (document.segments.isEmpty()) return null

        val bounded = logicalTimeMs.coerceIn(
            0L,
            document.logicalDurationMs.coerceAtLeast(0L)
        )

        val segment = document.segments.firstOrNull {
            bounded >= it.logicalStartMs
                && bounded < it.logicalEndMs
        } ?: document.segments.lastOrNull()?.takeIf {
            bounded == document.logicalDurationMs
                && it.logicalEndMs >= document.logicalDurationMs
        } ?: return null

        return ReadSegmentPosition(
            segment = segment,
            localOffsetMs = (
                bounded - segment.logicalStartMs
            ).coerceIn(
                0L,
                segment.logicalDurationMs
            )
        )
    }

    private fun logicalPosition(
        value: ReadPlayableDocument
    ): Long {
        val activeQueueIndex = physicalPlayer.currentMediaItemIndex
        val active = value.segments.getOrNull(activeQueueIndex)
            ?: return value.segments.firstOrNull()
                ?.logicalStartMs
                ?.coerceIn(
                    0L,
                    value.logicalDurationMs.coerceAtLeast(0L)
                )
                ?: 0L

        val resolvedPhysicalDuration = physicalPlayer.duration
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: active.actualDurationMs

        return ReadPlaybackTimeline.logicalTime(
            segment = active,
            physicalOffsetMs = physicalPlayer.currentPosition,
            documentDurationMs = value.logicalDurationMs,
            resolvedPhysicalDurationMs =
                resolvedPhysicalDuration
        )
    }
}
