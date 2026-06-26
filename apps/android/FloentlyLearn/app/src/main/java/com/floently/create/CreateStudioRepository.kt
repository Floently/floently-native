package com.floently.create

interface CreateStudioRepository {
    suspend fun dashboard(): CreateStudioDashboardState
    suspend fun selectTool(toolType: CreateStudioToolType): CreateStudioDashboardState
    suspend fun generate(toolType: CreateStudioToolType, input: String): CreateStudioDashboardState
}

class PreviewCreateStudioRepository : CreateStudioRepository {
    private val tools = listOf(
        CreateStudioTool(CreateStudioToolType.Hooks, "Hooks", "Generate hooks for a topic or product.", "Paste topic, audience, and goal."),
        CreateStudioTool(CreateStudioToolType.Captions, "Captions", "Write captions from a short idea.", "Paste the content idea."),
        CreateStudioTool(CreateStudioToolType.Summarize, "Summarize", "Turn long text into a useful summary.", "Paste long text."),
        CreateStudioTool(CreateStudioToolType.Repurpose, "Repurpose pack", "Convert one idea into multiple outputs.", "Paste the source idea or text."),
        CreateStudioTool(CreateStudioToolType.BrandBrain, "Brand brain", "Capture tone, audience, and style notes.", "Describe the brand.")
    )

    override suspend fun dashboard(): CreateStudioDashboardState = CreateStudioDashboardState(
        tools = tools,
        draft = CreateStudioDraft(
            selectedTool = CreateStudioToolType.Hooks,
            input = "",
            status = CreateRunStatus.Idle,
            message = "Choose a direct Create function and enter source text."
        ),
        latestResult = null,
        projects = previewProjects(),
        isLoading = false,
        errorMessage = null
    )

    override suspend fun selectTool(toolType: CreateStudioToolType): CreateStudioDashboardState = dashboard().copy(
        draft = CreateStudioDraft(
            selectedTool = toolType,
            input = "",
            status = CreateRunStatus.Ready,
            message = "${toolType.name} is ready."
        )
    )

    override suspend fun generate(toolType: CreateStudioToolType, input: String): CreateStudioDashboardState {
        val cleanedInput = input.trim().ifBlank { "No source text provided yet." }
        val result = CreateStudioResult(
            id = "create-preview-${toolType.name.lowercase()}",
            toolType = toolType,
            title = "${toolType.name} result preview",
            body = "Native Create Studio preview from: $cleanedInput",
            exportReady = true,
            createdText = "Preview"
        )
        return CreateStudioDashboardState(
            tools = tools,
            draft = CreateStudioDraft(
                selectedTool = toolType,
                input = input,
                status = CreateRunStatus.Generated,
                message = "Result generated through the native Create boundary."
            ),
            latestResult = result,
            projects = listOf(CreateStudioProject(result.id, result.title, result.body.take(120), "Just now")) + previewProjects(),
            isLoading = false,
            errorMessage = null
        )
    }

    private fun previewProjects(): List<CreateStudioProject> = listOf(
        CreateStudioProject("project-hooks-preview", "Hooks preview", "Saved Create Studio projects will appear here.", "Preview"),
        CreateStudioProject("project-brand-preview", "Brand brain preview", "Brand outputs stay separate from Read.", "Preview")
    )
}
