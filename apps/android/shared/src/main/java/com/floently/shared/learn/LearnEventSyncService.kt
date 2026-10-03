package com.floently.shared.learn

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

data class LearnerEventRecordResponseV1(
    val eventId: String,
    val inserted: Boolean
)

class LearnEventSyncService(
    private val api: FloentlyApiClient
) {
    suspend fun record(
        event: LearnerEventV1
    ): LearnerEventRecordResponseV1 {
        val data = api.post(
            "/api/v1/learning/events",
            learnerEventToJson(event)
        )
        val stored = data.optJSONObject("event") ?: JSONObject()
        return LearnerEventRecordResponseV1(
            eventId = stored.optString("eventId", event.eventId),
            inserted = data.optBoolean("inserted")
        )
    }
}

private fun learnerEventToJson(
    event: LearnerEventV1
): JSONObject {
    val json = JSONObject()
        .put("schemaVersion", event.schemaVersion)
        .put("eventId", event.eventId)
        .put("learnerId", event.learnerId)
        .put("occurredAt", event.occurredAt)
        .put("eventKind", event.eventKind.wireValue)
        .put("taskId", event.taskId)
        .put("contentVersion", event.contentVersion)
        .put("pathway", event.pathway.wireValue)
        .put("runtime", event.runtime.wireValue)
        .put(
            "skills",
            JSONArray(event.skills.map { it.wireValue })
        )
        .put("levelBand", event.levelBand)

    event.attemptId?.let { json.put("attemptId", it) }
    event.profession?.let { json.put("profession", it) }
    event.contextId?.let { json.put("contextId", it) }
    event.score?.let { json.put("score", it) }
    event.maxScore?.let { json.put("maxScore", it) }

    event.metadata?.let { metadata ->
        val metadataJson = JSONObject()
        metadata.forEach { (key, value) ->
            metadataJson.put(key, value)
        }
        json.put("metadata", metadataJson)
    }

    return json
}
