package com.floently.read

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
    private val physicalPlayer: ExoPlayer
) : ForwardingSimpleBasePlayer(physicalPlayer) {

    private var document: ReadPlayableDocument? = null

    fun loadDocument(
        value: ReadPlayableDocument,
        autoplay: Boolean = false
    ) {
        document = value

        val items = value.segments.mapNotNull { segment ->
            val uri = segment.audioUri?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            MediaItem.Builder()
                .setMediaId(segment.id)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(value.title)
                        .setArtist(value.author ?: "Floently Read")
                        .build()
                )
                .build()
        }

        physicalPlayer.setMediaItems(items, true)
        physicalPlayer.prepare()
        physicalPlayer.playWhenReady = autoplay
        invalidateState()
    }

    fun clearDocument() {
        document = null
        physicalPlayer.stop()
        physicalPlayer.clearMediaItems()
        invalidateState()
    }

    override fun getState(): SimpleBasePlayer.State {
        val state = super.getState()
        val value = document ?: return state

        val logicalItem = SimpleBasePlayer.MediaItemData.Builder(
            "read-document:${value.id}:${value.revisionId}"
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

        val activeIndex = physicalPlayer.currentMediaItemIndex
        val active = value.segments.getOrNull(activeIndex)

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
            0L
        }

        val logicalBuffered = if (active != null) {
            val currentBufferedLogical = ReadPlaybackTimeline.logicalTime(
                segment = active,
                physicalOffsetMs = physicalPlayer.bufferedPosition,
                documentDurationMs = value.logicalDurationMs,
                resolvedPhysicalDurationMs = resolvedPhysicalDuration
            )

            val futureReady = value.segments
                .drop(active.index + 1)
                .sumOf { it.logicalDurationMs }

            (currentBufferedLogical + futureReady)
                .coerceIn(logicalPosition, value.logicalDurationMs)
        } else {
            0L
        }

        return state.buildUpon()
            .setPlaylist(listOf(logicalItem))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(logicalPosition)
            .setContentBufferedPositionMs(logicalBuffered)
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

        val requested = if (positionMs == C.TIME_UNSET) 0L else positionMs
        val target = ReadPlaybackTimeline.segmentAndOffset(
            document = value,
            logicalTimeMs = requested
        ) ?: return Futures.immediateVoidFuture()

        val physicalDuration = target.segment.actualDurationMs
        val physicalOffset = ReadPlaybackTimeline.physicalOffset(
            segment = target.segment,
            logicalOffsetMs = target.localOffsetMs,
            resolvedPhysicalDurationMs = physicalDuration
        )

        physicalPlayer.seekTo(
            target.segment.index,
            physicalOffset
        )
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
