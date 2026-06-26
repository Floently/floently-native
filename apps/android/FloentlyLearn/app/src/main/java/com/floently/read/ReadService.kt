package com.floently.read

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class ReadService(private val api: FloentlyApiClient) {
    suspend fun dashboard(): ReadDashboardState {
        return dashboardFromJson(api.get("/api/v1/read/dashboard"))
    }

    suspend fun detectLanguage(input: String): ReadInputDraft {
        val response = api.post("/api/v1/read/detect", JSONObject().put("input", input))
        return draftFromJson(response.optJSONObject("draft") ?: response, fallbackInput = input)
    }

    suspend fun generate(input: String, readAutomatically: Boolean): ReadDashboardState {
        val response = api.post(
            "/api/v1/read/generate",
            JSONObject()
                .put("input", input)
                .put("read_automatically", readAutomatically)
        )
        return dashboardFromJson(response)
    }

    suspend fun toggleReadAutomatically(enabled: Boolean): ReadDashboardState {
        val response = api.post(
            "/api/v1/read/settings/read-automatically",
            JSONObject().put("enabled", enabled)
        )
        return dashboardFromJson(response)
    }

    private fun dashboardFromJson(json: JSONObject): ReadDashboardState {
        val draftJson = json.optJSONObject("draft") ?: JSONObject()
        val sessionJson = json.optJSONObject("active_session") ?: json.optJSONObject("session")
        val documentsJson = json.optJSONArray("saved_documents") ?: json.optJSONArray("documents") ?: JSONArray()
        return ReadDashboardState(
            draft = draftFromJson(draftJson),
            activeSession = sessionJson?.let { sessionFromJson(it) },
            savedDocuments = List(documentsJson.length()) { index -> documentFromJson(documentsJson.getJSONObject(index)) },
            isLoading = false,
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    private fun draftFromJson(json: JSONObject, fallbackInput: String = ""): ReadInputDraft {
        return ReadInputDraft(
            sourceType = sourceTypeFromApi(json.optString("source_type")),
            rawInput = json.optString("raw_input").ifBlank { json.optString("input").ifBlank { fallbackInput } },
            detectedLanguage = languageFromApi(json.optString("detected_language").ifBlank { json.optString("language") }),
            readAutomatically = json.optBoolean("read_automatically", true),
            status = generationStatusFromApi(json.optString("status")),
            message = json.optString("message").takeIf { it.isNotBlank() }
        )
    }

    private fun sessionFromJson(json: JSONObject): ReadReaderSession {
        val document = documentFromJson(json.optJSONObject("document") ?: json)
        return ReadReaderSession(
            document = document,
            currentChunk = json.optString("current_chunk").ifBlank { document.preview },
            progressPercent = json.optInt("progress_percent", 0).coerceIn(0, 100),
            isPlaying = json.optBoolean("is_playing", false),
            readAutomatically = json.optBoolean("read_automatically", false)
        )
    }

    private fun documentFromJson(json: JSONObject): ReadDocument {
        return ReadDocument(
            id = json.optString("id").ifBlank { "read-document" },
            title = json.optString("title").ifBlank { "Reading document" },
            language = languageFromApi(json.optString("language")),
            preview = json.optString("preview").ifBlank { json.optString("text").take(140) },
            createdText = json.optString("created_text").ifBlank { json.optString("created_at").ifBlank { "Backend" } }
        )
    }

    private fun sourceTypeFromApi(value: String?): ReadSourceType = when (value?.trim()?.lowercase()) {
        "url" -> ReadSourceType.Url
        "file", "upload" -> ReadSourceType.File
        else -> ReadSourceType.Text
    }

    private fun languageFromApi(value: String?): ReadLanguage = when (value?.trim()?.lowercase()) {
        "fi", "fin", "finnish", "suomi" -> ReadLanguage.Finnish
        "en", "eng", "english" -> ReadLanguage.English
        "sv", "swe", "swedish" -> ReadLanguage.Swedish
        "fr", "fra", "french" -> ReadLanguage.French
        "auto", "" -> ReadLanguage.Auto
        else -> ReadLanguage.Unknown
    }

    private fun generationStatusFromApi(value: String?): ReadGenerationStatus = when (value?.trim()?.lowercase()) {
        "detecting", "detecting_language" -> ReadGenerationStatus.DetectingLanguage
        "ready", "ready_to_generate" -> ReadGenerationStatus.ReadyToGenerate
        "generated", "done", "complete" -> ReadGenerationStatus.Generated
        "service_pending", "pending" -> ReadGenerationStatus.ServicePending
        else -> ReadGenerationStatus.Idle
    }
}
