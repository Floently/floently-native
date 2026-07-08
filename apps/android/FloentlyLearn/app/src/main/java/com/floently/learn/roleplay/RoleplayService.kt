package com.floently.learn.roleplay

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class RoleplayService(private val api: FloentlyApiClient) {
    suspend fun dashboard(selectedLevel: RoleplayLevel): RoleplayDashboardState {
        val response = firstGet(dashboardPaths(selectedLevel))
        val scenariosJson = response.optJSONArray("scenarios")
            ?: response.optJSONArray("topics")
            ?: response.optJSONArray("items")
            ?: JSONArray()
        return RoleplayDashboardState(
            scenarios = List(scenariosJson.length()) { index -> scenarioFromJson(scenariosJson.getJSONObject(index), selectedLevel) },
            selectedLevel = selectedLevel,
            isLoading = false,
            errorMessage = response.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    suspend fun startSession(scenarioId: String): RoleplaySessionResult {
        val body = JSONObject()
            .put("scenario_id", scenarioId)
            .put("topic_id", scenarioId)
        val response = firstPost(sessionPaths(), body)
        val sessionJson = response.optJSONObject("session") ?: response
        return RoleplaySessionResult.Ready(sessionFromJson(sessionJson))
    }

    suspend fun sendLearnerMessage(session: RoleplaySession, text: String): RoleplaySessionResult {
        val body = JSONObject()
            .put("text", text)
            .put("scenario_id", session.scenario.id)
            .put("topic_id", session.scenario.id)
            .put("learner_turns", session.learnerTurns)
        val response = firstPost(messagePaths(session.id), body)
        val sessionJson = response.optJSONObject("session") ?: response
        return RoleplaySessionResult.Ready(sessionFromJson(sessionJson, fallbackSession = session))
    }

    private suspend fun firstGet(paths: List<String>): JSONObject {
        var lastError: Throwable? = null
        paths.forEach { path ->
            runCatching { api.get(path) }
                .onSuccess { return it }
                .onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Roleplay endpoint was not available.")
    }

    private suspend fun firstPost(paths: List<String>, body: JSONObject): JSONObject {
        var lastError: Throwable? = null
        paths.forEach { path ->
            runCatching { api.post(path, body) }
                .onSuccess { return it }
                .onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("Roleplay endpoint was not available.")
    }

    private fun dashboardPaths(level: RoleplayLevel): List<String> = listOf(
        "/api/v1/learn/roleplay/dashboard?level=${level.apiValue}",
        "/api/v1/learn/roleplay/topics?level=${level.apiValue}",
        "/api/v1/roleplay/dashboard?level=${level.apiValue}",
        "/api/v1/roleplay/topics?level=${level.apiValue}",
        "/api/learn/roleplay/dashboard?level=${level.apiValue}",
        "/api/learn/roleplay/topics?level=${level.apiValue}",
        "/api/roleplay/dashboard?level=${level.apiValue}",
        "/api/roleplay/topics?level=${level.apiValue}"
    )

    private fun sessionPaths(): List<String> = listOf(
        "/api/v1/learn/roleplay/sessions",
        "/api/v1/roleplay/sessions",
        "/api/learn/roleplay/sessions",
        "/api/roleplay/sessions"
    )

    private fun messagePaths(sessionId: String): List<String> = listOf(
        "/api/v1/learn/roleplay/sessions/$sessionId/messages",
        "/api/v1/roleplay/sessions/$sessionId/messages",
        "/api/learn/roleplay/sessions/$sessionId/messages",
        "/api/roleplay/sessions/$sessionId/messages"
    )

    private fun sessionFromJson(json: JSONObject, fallbackSession: RoleplaySession? = null): RoleplaySession {
        val scenario = json.optJSONObject("scenario")?.let { scenarioFromJson(it, fallbackSession?.scenario?.level ?: RoleplayLevel.A1_A2) }
            ?: fallbackSession?.scenario
            ?: scenarioFromJson(JSONObject().put("id", json.optString("scenario_id")), RoleplayLevel.A1_A2)
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
            releaseGate = json.optString("release_gate").ifBlank { "Backend/generated roleplay material is connected through the roleplay service boundary." }
        )
    }

    private fun scenarioFromJson(json: JSONObject, fallbackLevel: RoleplayLevel): RoleplayScenario {
        val level = levelFromApi(json.optString("level"), fallbackLevel)
        return RoleplayScenario(
            id = json.optString("id").ifBlank { json.optString("topic_id").ifBlank { "roleplay-scenario" } },
            title = json.optString("title").ifBlank { json.optString("name").ifBlank { "Roleplay" } },
            level = level,
            type = scenarioTypeFromApi(json.optString("type")),
            description = json.optString("description").ifBlank { json.optString("summary").ifBlank { "Practice Finnish conversation." } },
            openingLine = json.optString("opening_line").ifBlank { json.optString("openingLine").ifBlank { "Hei!" } },
            targetPhrases = stringList(json.optJSONArray("target_phrases") ?: json.optJSONArray("phrases")),
            beginnerSafe = json.optBoolean("beginner_safe", level == RoleplayLevel.A1_A2),
            locked = json.optBoolean("locked", false),
            coachingMode = coachingModeFromApi(json.optString("coaching_mode"), level),
            materialSource = json.optString("material_source").ifBlank { "Backend/generated" },
            recommended = json.optBoolean("recommended", false)
        )
    }

    private fun messageFromJson(json: JSONObject): RoleplayMessage = RoleplayMessage(
        id = json.optString("id").ifBlank { "message" },
        speaker = speakerFromApi(json.optString("speaker")),
        text = json.optString("text"),
        coachingNote = json.optString("coaching_note").takeIf { it.isNotBlank() }
    )

    private fun stringList(array: JSONArray?): List<String> = if (array == null) emptyList() else List(array.length()) { index -> array.optString(index) }

    private fun levelFromApi(value: String?, fallback: RoleplayLevel): RoleplayLevel = when (value?.trim()?.uppercase()?.replace("_", "-")) {
        "A1", "A2", "A1-A2" -> RoleplayLevel.A1_A2
        "B1", "B2", "B1-B2" -> RoleplayLevel.B1_B2
        "C1", "C2", "C1-C2" -> RoleplayLevel.C1_C2
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

    private fun coachingModeFromApi(value: String?, fallbackLevel: RoleplayLevel): RoleplayCoachingMode = when (value?.trim()?.lowercase()) {
        "beginner_safe", "beginnersafe", "beginner" -> RoleplayCoachingMode.BeginnerSafe
        "professional", "work" -> RoleplayCoachingMode.Professional
        "exam_style", "examstyle", "exam" -> RoleplayCoachingMode.ExamStyle
        "natural" -> RoleplayCoachingMode.Natural
        else -> when (fallbackLevel) {
            RoleplayLevel.A1_A2 -> RoleplayCoachingMode.BeginnerSafe
            RoleplayLevel.B1_B2 -> RoleplayCoachingMode.Professional
            RoleplayLevel.C1_C2 -> RoleplayCoachingMode.ExamStyle
        }
    }

    private fun speakerFromApi(value: String?): RoleplaySpeaker = when (value?.trim()?.lowercase()) {
        "learner", "user" -> RoleplaySpeaker.Learner
        "coach" -> RoleplaySpeaker.Coach
        else -> RoleplaySpeaker.Partner
    }
}
