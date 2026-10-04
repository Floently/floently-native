package com.floently.read

import android.content.Context
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
    val sourceAnchorQuote: String? = null,
    val sourceAnchorPrefixContext: String? = null,
    val sourceAnchorSuffixContext: String? = null,
    val sourceAnchorCursorOffset: Int? = null
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

    fun save(snapshot: ReadPlaybackResumeSnapshot) {
        val existing =
            load(
                documentId =
                    resolved.documentId,
                revisionId =
                    resolved.revisionId
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
