package com.floently.create

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class CreateStudioService(private val api: FloentlyApiClient) {
    suspend fun dashboard(): CreateStudioDashboardState {
        return dashboardFromJson(api.get("/api/v1/create/dashboard"))
    }

    suspend fun generate(toolType: CreateStudioToolType, input: String): CreateStudioDashboardState {
        val response = api.post(
            "/api/v1/create/generate",
            JSONObject()
                .put("tool", toolType.apiName())
                .put("input", input)
        )
        return dashboardFromJson(response, fallbackTool = toolType, fallbackInput = input)
    }

    private fun dashboardFromJson(
        json: JSONObject,
        fallbackTool: CreateStudioToolType = CreateStudioToolType.Hooks,
        fallbackInput: String = ""
    ): CreateStudioDashboardState {
        val toolsJson = json.optJSONArray("tools") ?: JSONArray()
        val draftJson = json.optJSONObject("draft") ?: JSONObject()
        val resultJson = json.optJSONObject("latest_result") ?: json.optJSONObject("result")
        val projectsJson = json.optJSONArray("projects") ?: JSONArray()
        val tools = if (toolsJson.length() > 0) {
            List(toolsJson.length()) { index -> toolFromJson(toolsJson.getJSONObject(index)) }
        } else {
            defaultTools()
        }
        return CreateStudioDashboardState(
            tools = tools,
            draft = draftFromJson(draftJson, fallbackTool, fallbackInput),
            latestResult = resultJson?.let { resultFromJson(it, fallbackTool) },
            projects = List(projectsJson.length()) { index -> projectFromJson(projectsJson.getJSONObject(index)) },
            isLoading = false,
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    private fun toolFromJson(json: JSONObject): CreateStudioTool {
        val type = toolTypeFromApi(json.optString("type").ifBlank { json.optString("id") })
        return CreateStudioTool(
            type = type,
            title = json.optString("title").ifBlank { type.defaultTitle() },
            subtitle = json.optString("subtitle").ifBlank { type.defaultSubtitle() },
            inputHint = json.optString("input_hint").ifBlank { type.defaultHint() }
        )
    }

    private fun draftFromJson(json: JSONObject, fallbackTool: CreateStudioToolType, fallbackInput: String): CreateStudioDraft {
        return CreateStudioDraft(
            selectedTool = toolTypeFromApi(json.optString("selected_tool").ifBlank { json.optString("tool") }, fallbackTool),
            input = json.optString("input").ifBlank { fallbackInput },
            status = runStatusFromApi(json.optString("status")),
            message = json.optString("message").takeIf { it.isNotBlank() }
        )
    }

    private fun resultFromJson(json: JSONObject, fallbackTool: CreateStudioToolType): CreateStudioResult {
        val type = toolTypeFromApi(json.optString("tool_type").ifBlank { json.optString("tool") }, fallbackTool)
        return CreateStudioResult(
            id = json.optString("id").ifBlank { "create-result-${type.apiName()}" },
            toolType = type,
            title = json.optString("title").ifBlank { "${type.defaultTitle()} result" },
            body = json.optString("body").ifBlank { json.optString("output") },
            exportReady = json.optBoolean("export_ready", true),
            createdText = json.optString("created_text").ifBlank { json.optString("created_at").ifBlank { "Backend" } }
        )
    }

    private fun projectFromJson(json: JSONObject): CreateStudioProject {
        return CreateStudioProject(
            id = json.optString("id").ifBlank { "create-project" },
            title = json.optString("title").ifBlank { "Create project" },
            summary = json.optString("summary").ifBlank { json.optString("body").take(120) },
            lastEditedText = json.optString("last_edited_text").ifBlank { json.optString("updated_at").ifBlank { "Backend" } }
        )
    }

    private fun defaultTools(): List<CreateStudioTool> = CreateStudioToolType.entries.map { type ->
        CreateStudioTool(type, type.defaultTitle(), type.defaultSubtitle(), type.defaultHint())
    }
}

fun CreateStudioToolType.apiName(): String = when (this) {
    CreateStudioToolType.Hooks -> "hooks"
    CreateStudioToolType.Captions -> "captions"
    CreateStudioToolType.Summarize -> "summarize"
    CreateStudioToolType.Repurpose -> "repurpose"
    CreateStudioToolType.BrandBrain -> "brand_brain"
}

private fun toolTypeFromApi(value: String?, fallback: CreateStudioToolType = CreateStudioToolType.Hooks): CreateStudioToolType {
    return when (value?.trim()?.lowercase()) {
        "hooks", "hook" -> CreateStudioToolType.Hooks
        "captions", "caption" -> CreateStudioToolType.Captions
        "summarize", "summary" -> CreateStudioToolType.Summarize
        "repurpose", "repurpose_pack" -> CreateStudioToolType.Repurpose
        "brand_brain", "brandbrain", "brand" -> CreateStudioToolType.BrandBrain
        else -> fallback
    }
}

private fun runStatusFromApi(value: String?): CreateRunStatus = when (value?.trim()?.lowercase()) {
    "ready" -> CreateRunStatus.Ready
    "generated", "done", "complete" -> CreateRunStatus.Generated
    "service_pending", "pending" -> CreateRunStatus.ServicePending
    else -> CreateRunStatus.Idle
}

private fun CreateStudioToolType.defaultTitle(): String = when (this) {
    CreateStudioToolType.Hooks -> "Hooks"
    CreateStudioToolType.Captions -> "Captions"
    CreateStudioToolType.Summarize -> "Summarize"
    CreateStudioToolType.Repurpose -> "Repurpose pack"
    CreateStudioToolType.BrandBrain -> "Brand brain"
}

private fun CreateStudioToolType.defaultSubtitle(): String = when (this) {
    CreateStudioToolType.Hooks -> "Generate hooks for a topic or product."
    CreateStudioToolType.Captions -> "Write captions from a short idea."
    CreateStudioToolType.Summarize -> "Turn long text into a useful summary."
    CreateStudioToolType.Repurpose -> "Convert one idea into multiple outputs."
    CreateStudioToolType.BrandBrain -> "Capture tone, audience, and style notes."
}

private fun CreateStudioToolType.defaultHint(): String = when (this) {
    CreateStudioToolType.Hooks -> "Paste topic, audience, and goal."
    CreateStudioToolType.Captions -> "Paste the content idea."
    CreateStudioToolType.Summarize -> "Paste long text."
    CreateStudioToolType.Repurpose -> "Paste the source idea or text."
    CreateStudioToolType.BrandBrain -> "Describe the brand."
}
