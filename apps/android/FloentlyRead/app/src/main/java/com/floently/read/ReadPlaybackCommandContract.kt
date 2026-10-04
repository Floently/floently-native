package com.floently.read

import android.os.Bundle
import androidx.media3.session.SessionCommand
import org.json.JSONArray
import org.json.JSONObject

data class ReadManifestLoadRequest(
    val manifest: ReadingManifestV1,
    val voiceId: String,
    val autoplay: Boolean,
    val startingAt: Int
)

object ReadPlaybackCommandContract {
    const val ACTION_LOAD_DOCUMENT =
        "com.floently.read.command.LOAD_DOCUMENT"
    const val ACTION_LOAD_MANIFEST =
        "com.floently.read.command.LOAD_MANIFEST"
    const val ACTION_CHANGE_VOICE =
        "com.floently.read.command.CHANGE_VOICE"
    const val ACTION_CLEAR_PLAYBACK =
        "com.floently.read.command.CLEAR_PLAYBACK"

    const val EXTRA_DOCUMENT_JSON = "document_json"
    const val EXTRA_MANIFEST_HANDOFF_ID = "manifest_handoff_id"
    const val EXTRA_AUTOPLAY = "autoplay"
    const val EXTRA_VOICE_ID = "voice_id"

    val loadDocumentCommand: SessionCommand
        get() = SessionCommand(
            ACTION_LOAD_DOCUMENT,
            Bundle.EMPTY
        )

    val loadManifestCommand: SessionCommand
        get() = SessionCommand(
            ACTION_LOAD_MANIFEST,
            Bundle.EMPTY
        )

    val changeVoiceCommand: SessionCommand
        get() = SessionCommand(
            ACTION_CHANGE_VOICE,
            Bundle.EMPTY
        )

    val clearPlaybackCommand: SessionCommand
        get() = SessionCommand(
            ACTION_CLEAR_PLAYBACK,
            Bundle.EMPTY
        )

    fun encodeChangeVoice(
        voiceId: String
    ): Bundle = Bundle().apply {
        putString(EXTRA_VOICE_ID, voiceId)
    }

    fun decodeChangeVoice(
        args: Bundle
    ): String? =
        args.getString(EXTRA_VOICE_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun encodeLoadManifestHandoff(
        handoffId: String
    ): Bundle = Bundle().apply {
        putString(
            EXTRA_MANIFEST_HANDOFF_ID,
            handoffId
        )
    }

    fun decodeLoadManifest(
        args: Bundle
    ): ReadManifestLoadRequest? {
        val handoffId = args.getString(
            EXTRA_MANIFEST_HANDOFF_ID
        )?.takeIf { it.isNotBlank() }
            ?: return null

        return ReadManifestHandoffRegistry.take(
            handoffId
        )
    }

    fun encodeLoadDocument(
        document: ReadPlayableDocument,
        autoplay: Boolean
    ): Bundle = Bundle().apply {
        putString(
            EXTRA_DOCUMENT_JSON,
            encodeDocument(document).toString()
        )
        putBoolean(EXTRA_AUTOPLAY, autoplay)
    }

    fun decodeLoadDocument(
        args: Bundle
    ): Pair<ReadPlayableDocument, Boolean>? {
        val raw = args.getString(EXTRA_DOCUMENT_JSON)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val document = runCatching {
            decodeDocument(JSONObject(raw))
        }.getOrNull() ?: return null

        return document to args.getBoolean(
            EXTRA_AUTOPLAY,
            false
        )
    }

    private fun encodeDocument(
        document: ReadPlayableDocument
    ): JSONObject {
        val segments = JSONArray()

        document.segments.forEach { segment ->
            segments.put(
                JSONObject()
                    .put("id", segment.id)
                    .put("index", segment.index)
                    .put("logical_start_ms", segment.logicalStartMs)
                    .put("logical_end_ms", segment.logicalEndMs)
                    .put("audio_uri", segment.audioUri)
                    .put("actual_duration_ms", segment.actualDurationMs)
                    .put("rendition_id", segment.renditionId)
                    .put("timing_map_id", segment.timingMapId)
            )
        }

        return JSONObject()
            .put("id", document.id)
            .put("revision_id", document.revisionId)
            .put("title", document.title)
            .put("author", document.author)
            .put(
                "estimated_duration_ms",
                document.estimatedDurationMs
            )
            .put("segments", segments)
    }

    private fun decodeDocument(
        json: JSONObject
    ): ReadPlayableDocument {
        val segmentsJson = json.optJSONArray("segments")
            ?: JSONArray()

        val segments = buildList {
            for (index in 0 until segmentsJson.length()) {
                val item = segmentsJson.optJSONObject(index)
                    ?: continue

                add(
                    ReadPlaybackSegment(
                        id = item.getString("id"),
                        index = item.getInt("index"),
                        logicalStartMs = item.getLong(
                            "logical_start_ms"
                        ),
                        logicalEndMs = item.getLong(
                            "logical_end_ms"
                        ),
                        audioUri = if (
                            item.has("audio_uri")
                            && !item.isNull("audio_uri")
                        ) {
                            item.optString("audio_uri")
                                .takeIf { it.isNotBlank() }
                        } else {
                            null
                        },
                        actualDurationMs = if (
                            item.has("actual_duration_ms")
                            && !item.isNull("actual_duration_ms")
                        ) {
                            item.getLong("actual_duration_ms")
                        } else {
                            null
                        },
                        renditionId =
                            item.optString(
                                "rendition_id"
                            )
                                .takeIf {
                                    it.isNotBlank()
                                },
                        timingMapId =
                            item.optString(
                                "timing_map_id"
                            )
                                .takeIf {
                                    it.isNotBlank()
                                }
                    )
                )
            }
        }.sortedBy { it.index }

        require(
            segments.all {
                it.logicalEndMs >= it.logicalStartMs
                    && !it.audioUri.isNullOrBlank()
            }
        )
        require(
            segments.zipWithNext().all { (left, right) ->
                right.index > left.index
                    && right.logicalStartMs >= left.logicalStartMs
            }
        )

        return ReadPlayableDocument(
            id = json.getString("id"),
            revisionId = json.getString("revision_id"),
            title = json.getString("title"),
            author = if (
                json.has("author")
                && !json.isNull("author")
            ) {
                json.optString("author")
                    .takeIf { it.isNotBlank() }
            } else {
                null
            },
            estimatedDurationMs = json.getLong(
                "estimated_duration_ms"
            ).coerceAtLeast(0L),
            segments = segments
        )
    }
}
