package com.floently.learn.roleplay

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class RoleplayService(private val api: FloentlyApiClient) {
    suspend fun dashboard(selectedLevel: RoleplayLevel): RoleplayDashboardState {
        val response = api.get("/api/v1/learn/roleplay/dashboard?level=${selectedLevel.name.lowercase()}")
        val scenariosJson = response.optJSONArray("scenarios") ?: JSONArray()
        return RoleplayDashboardState(
            scenarios = List(scenariosJson.length()) { index -> scenarioFromJson(scenariosJson.getJSONObject(index), selectedLevel) },
            selectedLevel = selectedLevel,
            isLoading = false,
            errorMessage = response.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    suspend fun startSession(scenarioId: String): RoleplaySessionResult {
        val response = api.post(
            "/api/v1/learn/roleplay/sessions",
            JSONObject().put("scenario_id", scenarioId)
        )
        val sessionJson = response.optJSONObject("session") ?: response
        return RoleplaySessionResult.Ready(sessionFromJson(sessionJson))
    }

    suspend fun sendLearnerMessage(session: RoleplaySession, text: String): RoleplaySessionResult {
        val response = api.post(
            "/api/v1/learn/roleplay/sessions/${session.id}/messages",
            JSONObject()
                .put("text", text)
                .put("scenario_id", session.scenario.id)
                .put("learner_turns", session.learnerTurns)
        )
        val sessionJson = response.optJSONObject("session") ?: response
        return RoleplaySessionResult.Ready(sessionFromJson(sessionJson, fallbackSession = session))
    }

    private fun sessionFromJson(json: JSONObject, fallbackSession: RoleplaySession? = null): RoleplaySession {
        val scenario = json.optJSONObject("scenario")?.let { scenarioFromJson(it, fallbackSession?.scenario?.level ?: RoleplayLevel.A1) }
            ?: fallbackSession?.scenario
            ?: scenarioFromJson(JSONObject().put("id", json.optString("scenario_id")), RoleplayLevel.A1)
        val messagesJson = json.optJSONArray("messages") ?: JSONArray()
        val messages = if (messagesJson.length() > 0) {
            List(messagesJson.length()) { index -> messageFromJson(messagesJson.getJSONObject(index)) }
        } else {
            fallbackSession?.messages.orEmpty()
        }
        return RoleplaySession(
            id = json.optString("id").ifBlank { fallbackSession?.id ?: "roleplay-session" },
            scenario = scenario,
            messages = messages,
            learnerTurns = json.optInt("learner_turns", fallbackSession?.learnerTurns ?: messages.count { it.speaker == RoleplaySpeaker.Learner }),
            repeatedCueCount = json.optInt("repeated_cue_count", fallbackSession?.repeatedCueCount ?: 0),
            releaseGate = json.optString("release_gate").ifBlank { "OpenAI-backed dynamic roleplay is connected through the existing service boundary." }
        )
    }

    private fun scenarioFromJson(json: JSONObject, fallbackLevel: RoleplayLevel): RoleplayScenario {
        return RoleplayScenario(
            id = json.optString("id").ifBlank { "roleplay-scenario" },
            title = json.optString("title").ifBlank { "Roleplay" },
            level = levelFromApi(json.optString("level"), fallbackLevel),
            type = scenarioTypeFromApi(json.optString("type")),
            description = json.optString("description").ifBlank { "Practice Finnish conversation." },
            openingLine = json.optString("opening_line").ifBlank { json.optString("openingLine").ifBlank { "Hei!" } },
            targetPhrases = stringList(json.optJSONArray("target_phrases")),
            beginnerSafe = json.optBoolean("beginner_safe", fallbackLevel == RoleplayLevel.A1 || fallbackLevel == RoleplayLevel.A2),
            locked = json.optBoolean("locked", false)
        )
    }

    private fun messageFromJson(json: JSONObject): RoleplayMessage = RoleplayMessage(
        id = json.optString("id").ifBlank { "message" },
        speaker = speakerFromApi(json.optString("speaker")),
        text = json.optString("text"),
        coachingNote = json.optString("coaching_note").takeIf { it.isNotBlank() }
    )

    private fun stringList(array: JSONArray?): List<String> = if (array == null) emptyList() else List(array.length()) { index -> array.optString(index) }

    private fun levelFromApi(value: String?, fallback: RoleplayLevel): RoleplayLevel = when (value?.trim()?.uppercase()) {
        "A1" -> RoleplayLevel.A1
        "A2" -> RoleplayLevel.A2
        "B1" -> RoleplayLevel.B1
        "B2" -> RoleplayLevel.B2
        else -> fallback
    }

    private fun scenarioTypeFromApi(value: String?): RoleplayScenarioType = when (value?.trim()?.lowercase()) {
        "work" -> RoleplayScenarioType.Work
        "healthcare" -> RoleplayScenarioType.Healthcare
        "interview" -> RoleplayScenarioType.Interview
        "phone_call", "phonecall", "phone" -> RoleplayScenarioType.PhoneCall
        "service" -> RoleplayScenarioType.Service
        else -> RoleplayScenarioType.Everyday
    }

    private fun speakerFromApi(value: String?): RoleplaySpeaker = when (value?.trim()?.lowercase()) {
        "learner", "user" -> RoleplaySpeaker.Learner
        "coach" -> RoleplaySpeaker.Coach
        else -> RoleplaySpeaker.Partner
    }
}
