package com.floently.read

import android.content.Context
import org.json.JSONObject

data class ReadPlaybackResumeSnapshot(
    val documentId: String,
    val revisionId: String,
    val logicalTimeMs: Long,
    val playbackSpeed: Float,
    val updatedAtMs: Long
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

        return runCatching {
            val json = JSONObject(raw)
            val snapshot = ReadPlaybackResumeSnapshot(
                documentId = json.getString("document_id"),
                revisionId = json.getString("revision_id"),
                logicalTimeMs = json.getLong("logical_time_ms")
                    .coerceAtLeast(0L),
                playbackSpeed = json.getDouble("playback_speed")
                    .toFloat()
                    .coerceIn(0.5f, 3f),
                updatedAtMs = json.optLong(
                    "updated_at_ms",
                    0L
                )
            )

            snapshot.takeIf {
                it.documentId == documentId
                    && it.revisionId == revisionId
            }
        }.getOrNull()
    }

    fun save(snapshot: ReadPlaybackResumeSnapshot) {
        val json = JSONObject()
            .put("document_id", snapshot.documentId)
            .put("revision_id", snapshot.revisionId)
            .put(
                "logical_time_ms",
                snapshot.logicalTimeMs.coerceAtLeast(0L)
            )
            .put(
                "playback_speed",
                snapshot.playbackSpeed.coerceIn(0.5f, 3f)
            )
            .put("updated_at_ms", snapshot.updatedAtMs)

        preferences.edit()
            .putString(
                key(
                    snapshot.documentId,
                    snapshot.revisionId
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

    private fun key(
        documentId: String,
        revisionId: String
    ): String =
        "resume::" + documentId + "::" + revisionId
}
