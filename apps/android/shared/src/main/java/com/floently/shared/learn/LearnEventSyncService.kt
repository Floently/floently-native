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

    suspend fun listEvidence(
        pathway: LearnPathwayV1? = null,
        skill: LearnSkillV1? = null,
        since: String? = null,
        limit: Int = 200
    ): List<SkillEvidenceV1> {
        val query = linkedMapOf(
            "limit" to limit.coerceIn(1, 500).toString()
        )
        pathway?.let { query["pathway"] = it.wireValue }
        skill?.let { query["skill"] = it.wireValue }
        since?.let { query["since"] = it }

        val data = api.get(
            path = "/api/v1/learning/evidence",
            query = query
        )
        val array = data.optJSONArray("evidence") ?: JSONArray()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    SkillEvidenceV1(
                        schemaVersion = item.optString("schemaVersion", "learning.v1"),
                        evidenceId = item.optString("evidenceId"),
                        learnerId = item.optString("learnerId"),
                        sourceEventId = item.optString("sourceEventId"),
                        observedAt = item.optString("observedAt"),
                        skill = LearnSkillV1.entries.first {
                            it.wireValue == item.optString("skill")
                        },
                        levelBand = item.optString("levelBand"),
                        evidenceType = LearnEvidenceTypeV1.entries.first {
                            it.wireValue == item.optString("evidenceType")
                        },
                        score = item.optDouble("score").takeUnless { it.isNaN() },
                        maxScore = item.optDouble("maxScore").takeUnless { it.isNaN() },
                        pathway = LearnPathwayV1.entries.first {
                            it.wireValue == item.optString("pathway")
                        },
                        profession = item.optString("profession")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }
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
