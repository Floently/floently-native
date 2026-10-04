package com.floently.read

import android.content.Context
import kotlin.math.roundToInt
import org.json.JSONObject

data class ReadPlaybackResumeSnapshot(
    val documentId: String,
    val revisionId: String,
    val logicalTimeMs: Long,
    val playbackSpeed: Float,
    val updatedAtMs: Long,
    val sourceScalarOffset: Int? = null,
    val sourceSegmentId: String? = null,
    val sourceSegmentIndex: Int? = null,
    val voiceId: String? = null,
    val renditionId: String? = null,
    val timingMapId: String? = null,
    val sourceAnchorQuote: String? = null,
    val sourceAnchorPrefixContext: String? = null,
    val sourceAnchorSuffixContext: String? = null,
    val sourceAnchorCursorOffset: Int? = null
)

data class ReadPlaybackSourceAnchor(
    val scalarOffset: Int,
    val segmentId: String,
    val segmentIndex: Int,
    val quote: String,
    val prefixContext: String,
    val suffixContext: String,
    val cursorOffset: Int
)

class ReadPlaybackResumeStore(
    context: Context
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(
            "floently_read_playback_resume",
            Context.MODE_PRIVATE
        )

    fun load(
        documentId: String,
        revisionId: String
    ): ReadPlaybackResumeSnapshot? {
        val raw = preferences.getString(
            key(documentId, revisionId),
            null
        ) ?: return null

        return decode(raw)?.takeIf {
            it.documentId == documentId
                && it.revisionId == revisionId
        }
    }

    fun loadLatest(
        documentId: String,
        excludingRevisionId: String
    ): ReadPlaybackResumeSnapshot? {
        val keyPrefix =
            "resume::" + documentId + "::"

        return preferences.all
            .asSequence()
            .filter {
                it.key.startsWith(
                    keyPrefix
                )
            }
            .mapNotNull {
                it.value as? String
            }
            .mapNotNull(::decode)
            .filter {
                it.documentId == documentId
                    && it.revisionId
                        != excludingRevisionId
            }
            .maxByOrNull {
                it.updatedAtMs
            }
    }

    fun loadOrMigrate(
        manifest: ReadingManifestV1
    ): ReadPlaybackResumeSnapshot? {
        load(
            documentId =
                manifest.documentId,
            revisionId =
                manifest.revisionId
        )?.let {
            return it
        }

        val previous =
            loadLatest(
                documentId =
                    manifest.documentId,
                excludingRevisionId =
                    manifest.revisionId
            ) ?: return null
        val quote =
            previous.sourceAnchorQuote
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: return null
        val cursorOffset =
            previous
                .sourceAnchorCursorOffset
                ?.takeIf {
                    it >= 0
                }
                ?: return null
        val prefix =
            previous
                .sourceAnchorPrefixContext
                ?: ""
        val suffix =
            previous
                .sourceAnchorSuffixContext
                ?: ""
        val matches =
            mutableListOf<
                Pair<
                    ReadingManifestSegmentV1,
                    Int
                >
            >()

        manifest.segments.forEach {
            segment ->
            val resolution =
                runCatching {
                    ReadCoreNative
                        .resolveSourceAnchor(
                            sourceText =
                                segment.text,
                            quote = quote,
                            prefixContext =
                                prefix,
                            suffixContext =
                                suffix
                        )
                }
                    .getOrNull()
                    ?: return@forEach

            val localCursor =
                resolution.scalarStart
                    + cursorOffset
            val segmentScalarCount =
                ReadScalarOffsets
                    .scalarCount(
                        segment.text
                    )

            if (
                localCursor !in
                    0..segmentScalarCount
            ) {
                return@forEach
            }

            matches +=
                segment to
                    (
                        segment.scalarStart
                            + localCursor
                    )
                        .coerceAtMost(
                            segment.scalarEnd
                        )

            if (matches.size > 1) {
                return null
            }
        }

        val match =
            matches.singleOrNull()
                ?: return null
        val segment = match.first
        val scalarOffset =
            match.second
        val quoteLength =
            ReadScalarOffsets
                .scalarCount(quote)
        val localQuoteStart =
            (
                scalarOffset
                    - cursorOffset
                    - segment.scalarStart
                )
                .coerceAtLeast(0)
        val localQuoteEnd =
            (
                localQuoteStart
                    + quoteLength
                )
                .coerceAtMost(
                    ReadScalarOffsets
                        .scalarCount(
                            segment.text
                        )
                )
        val migratedPrefix =
            scalarSubstring(
                segment.text,
                (localQuoteStart - 32)
                    .coerceAtLeast(0),
                localQuoteStart
            )
        val migratedSuffix =
            scalarSubstring(
                segment.text,
                localQuoteEnd,
                (localQuoteEnd + 32)
                    .coerceAtMost(
                        ReadScalarOffsets
                            .scalarCount(
                                segment.text
                            )
                    )
            )
        val migrated =
            ReadPlaybackResumeSnapshot(
                documentId =
                    manifest.documentId,
                revisionId =
                    manifest.revisionId,
                logicalTimeMs =
                    logicalTimeMs(
                        scalarOffset =
                            scalarOffset,
                        segment = segment
                    ),
                playbackSpeed =
                    previous.playbackSpeed,
                updatedAtMs =
                    previous.updatedAtMs,
                sourceScalarOffset =
                    scalarOffset,
                sourceSegmentId =
                    segment.id,
                sourceSegmentIndex =
                    segment.index,
                voiceId =
                    previous.voiceId,
                renditionId = null,
                timingMapId = null,
                sourceAnchorQuote =
                    quote,
                sourceAnchorPrefixContext =
                    migratedPrefix,
                sourceAnchorSuffixContext =
                    migratedSuffix,
                sourceAnchorCursorOffset =
                    cursorOffset
            )

        save(migrated)
        remove(
            documentId =
                previous.documentId,
            revisionId =
                previous.revisionId
        )
        return migrated
    }

    fun sourceAnchor(
        manifest: ReadingManifestV1,
        logicalTimeMs: Long
    ): ReadPlaybackSourceAnchor? {
        val segment =
            manifest.segments.firstOrNull {
                logicalTimeMs
                    < it.logicalEndMs
            }
                ?: manifest.segments
                    .lastOrNull()
                ?: return null
        val scalarSpan =
            (segment.scalarEnd
                - segment.scalarStart)
                .coerceAtLeast(0)
        val logicalSpan =
            (segment.logicalEndMs
                - segment.logicalStartMs)
                .coerceAtLeast(1L)
        val localMs =
            (logicalTimeMs
                - segment.logicalStartMs)
                .coerceIn(
                    0L,
                    logicalSpan
                )
        val fraction =
            localMs.toDouble()
                / logicalSpan
                    .toDouble()
        val localScalar =
            (
                scalarSpan
                    .toDouble()
                    * fraction
                )
                .roundToInt()
                .coerceIn(
                    0,
                    scalarSpan
                )
        val scalarOffset =
            (segment.scalarStart
                + localScalar)
                .coerceAtMost(
                    segment.scalarEnd
                )
        val segmentScalarCount =
            ReadScalarOffsets
                .scalarCount(
                    segment.text
                )
        if (segmentScalarCount <= 0) {
            return null
        }

        val localCursor =
            (scalarOffset
                - segment.scalarStart)
                .coerceIn(
                    0,
                    segmentScalarCount
                )
        var quoteStart =
            (localCursor - 24)
                .coerceAtLeast(0)
        var quoteEnd =
            (quoteStart + 64)
                .coerceAtMost(
                    segmentScalarCount
                )
        quoteStart =
            (quoteEnd - 64)
                .coerceAtLeast(0)
        quoteEnd =
            (quoteStart + 64)
                .coerceAtMost(
                    segmentScalarCount
                )
        if (quoteEnd <= quoteStart) {
            return null
        }

        val prefixStart =
            (quoteStart - 32)
                .coerceAtLeast(0)
        val suffixEnd =
            (quoteEnd + 32)
                .coerceAtMost(
                    segmentScalarCount
                )

        return ReadPlaybackSourceAnchor(
            scalarOffset =
                scalarOffset,
            segmentId =
                segment.id,
            segmentIndex =
                segment.index,
            quote =
                scalarSubstring(
                    segment.text,
                    quoteStart,
                    quoteEnd
                ),
            prefixContext =
                scalarSubstring(
                    segment.text,
                    prefixStart,
                    quoteStart
                ),
            suffixContext =
                scalarSubstring(
                    segment.text,
                    quoteEnd,
                    suffixEnd
                ),
            cursorOffset =
                localCursor
                    - quoteStart
        )
    }

    fun save(snapshot: ReadPlaybackResumeSnapshot) {
        val existing =
            load(
                documentId =
                    snapshot.documentId,
                revisionId =
                    snapshot.revisionId
            )
        val resolved =
            snapshot.copy(
                sourceScalarOffset =
                    snapshot.sourceScalarOffset
                        ?: existing
                            ?.sourceScalarOffset,
                sourceSegmentId =
                    snapshot.sourceSegmentId
                        ?: existing
                            ?.sourceSegmentId,
                sourceSegmentIndex =
                    snapshot.sourceSegmentIndex
                        ?: existing
                            ?.sourceSegmentIndex,
                voiceId =
                    snapshot.voiceId
                        ?: existing?.voiceId,
                renditionId =
                    snapshot.renditionId
                        ?: existing
                            ?.renditionId,
                timingMapId =
                    snapshot.timingMapId
                        ?: existing
                            ?.timingMapId,
                sourceAnchorQuote =
                    snapshot.sourceAnchorQuote
                        ?: existing
                            ?.sourceAnchorQuote,
                sourceAnchorPrefixContext =
                    snapshot
                        .sourceAnchorPrefixContext
                        ?: existing
                            ?.sourceAnchorPrefixContext,
                sourceAnchorSuffixContext =
                    snapshot
                        .sourceAnchorSuffixContext
                        ?: existing
                            ?.sourceAnchorSuffixContext,
                sourceAnchorCursorOffset =
                    snapshot
                        .sourceAnchorCursorOffset
                        ?: existing
                            ?.sourceAnchorCursorOffset
            )

        val json = JSONObject()
            .put("document_id", resolved.documentId)
            .put("revision_id", resolved.revisionId)
            .put(
                "logical_time_ms",
                resolved.logicalTimeMs.coerceAtLeast(0L)
            )
            .put(
                "playback_speed",
                resolved.playbackSpeed.coerceIn(0.5f, 3f)
            )
            .put("updated_at_ms", resolved.updatedAtMs)

        resolved.sourceScalarOffset?.let {
            json.put("source_scalar_offset", it)
        }
        resolved.sourceSegmentId?.let {
            json.put("source_segment_id", it)
        }
        resolved.sourceSegmentIndex?.let {
            json.put("source_segment_index", it)
        }
        resolved.voiceId?.let {
            json.put("voice_id", it)
        }
        resolved.renditionId?.let {
            json.put("rendition_id", it)
        }
        resolved.timingMapId?.let {
            json.put("timing_map_id", it)
        }
        resolved.sourceAnchorQuote?.let {
            json.put(
                "source_anchor_quote",
                it
            )
        }
        resolved.sourceAnchorPrefixContext?.let {
            json.put(
                "source_anchor_prefix_context",
                it
            )
        }
        resolved.sourceAnchorSuffixContext?.let {
            json.put(
                "source_anchor_suffix_context",
                it
            )
        }
        resolved.sourceAnchorCursorOffset?.let {
            json.put(
                "source_anchor_cursor_offset",
                it
            )
        }

        preferences.edit()
            .putString(
                key(
                    resolved.documentId,
                    resolved.revisionId
                ),
                json.toString()
            )
            .apply()
    }

    fun remove(
        documentId: String,
        revisionId: String
    ) {
        preferences.edit()
            .remove(key(documentId, revisionId))
            .apply()
    }

    private fun decode(
        raw: String
    ): ReadPlaybackResumeSnapshot? =
        runCatching {
            val json = JSONObject(raw)
            ReadPlaybackResumeSnapshot(
                documentId =
                    json.getString(
                        "document_id"
                    ),
                revisionId =
                    json.getString(
                        "revision_id"
                    ),
                logicalTimeMs =
                    json.getLong(
                        "logical_time_ms"
                    )
                        .coerceAtLeast(0L),
                playbackSpeed =
                    json.getDouble(
                        "playback_speed"
                    )
                        .toFloat()
                        .coerceIn(
                            0.5f,
                            3f
                        ),
                updatedAtMs =
                    json.optLong(
                        "updated_at_ms",
                        0L
                    ),
                sourceScalarOffset =
                    json.optIntOrNull(
                        "source_scalar_offset"
                    ),
                sourceSegmentId =
                    json.optStringOrNull(
                        "source_segment_id"
                    ),
                sourceSegmentIndex =
                    json.optIntOrNull(
                        "source_segment_index"
                    ),
                voiceId =
                    json.optStringOrNull(
                        "voice_id"
                    ),
                renditionId =
                    json.optStringOrNull(
                        "rendition_id"
                    ),
                timingMapId =
                    json.optStringOrNull(
                        "timing_map_id"
                    ),
                sourceAnchorQuote =
                    json.optStringOrNull(
                        "source_anchor_quote"
                    ),
                sourceAnchorPrefixContext =
                    json.optStringOrNull(
                        "source_anchor_prefix_context"
                    ),
                sourceAnchorSuffixContext =
                    json.optStringOrNull(
                        "source_anchor_suffix_context"
                    ),
                sourceAnchorCursorOffset =
                    json.optIntOrNull(
                        "source_anchor_cursor_offset"
                    )
            )
        }.getOrNull()

    private fun logicalTimeMs(
        scalarOffset: Int,
        segment: ReadingManifestSegmentV1
    ): Long {
        val scalarSpan =
            (segment.scalarEnd
                - segment.scalarStart)
                .coerceAtLeast(1)
        val localScalar =
            (scalarOffset
                - segment.scalarStart)
                .coerceIn(
                    0,
                    scalarSpan
                )
        val fraction =
            localScalar.toDouble()
                / scalarSpan
                    .toDouble()
        val logicalSpan =
            (segment.logicalEndMs
                - segment.logicalStartMs)
                .coerceAtLeast(0L)

        return (
            segment.logicalStartMs
                + (
                    logicalSpan
                        .toDouble()
                        * fraction
                    )
                    .toLong()
            )
            .coerceAtLeast(0L)
    }

    private fun scalarSubstring(
        text: String,
        startScalar: Int,
        endScalar: Int
    ): String {
        val start =
            ReadScalarOffsets
                .utf16Offset(
                    text = text,
                    scalarOffset =
                        startScalar
                )
        val end =
            ReadScalarOffsets
                .utf16Offset(
                    text = text,
                    scalarOffset =
                        endScalar
                )

        return text.substring(
            start,
            end
        )
    }

    private fun JSONObject.optStringOrNull(
        key: String
    ): String? =
        if (has(key) && !isNull(key)) {
            optString(key).takeIf { it.isNotBlank() }
        } else {
            null
        }

    private fun JSONObject.optIntOrNull(
        key: String
    ): Int? =
        if (has(key) && !isNull(key)) {
            optInt(key)
        } else {
            null
        }

    private fun key(
        documentId: String,
        revisionId: String
    ): String =
        "resume::" + documentId + "::" + revisionId
}
