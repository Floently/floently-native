package com.floently.create

enum class CreateStudioToolType {
    Hooks,
    Captions,
    Summarize,
    Repurpose,
    BrandBrain
}

enum class CreateRunStatus {
    Idle,
    Ready,
    Generated,
    ServicePending
}

data class CreateStudioTool(
    val type: CreateStudioToolType,
    val title: String,
    val subtitle: String,
    val inputHint: String
)

data class CreateStudioDraft(
    val selectedTool: CreateStudioToolType,
    val input: String,
    val status: CreateRunStatus,
    val message: String?
)

data class CreateStudioResult(
    val id: String,
    val toolType: CreateStudioToolType,
    val title: String,
    val body: String,
    val exportReady: Boolean,
    val createdText: String
)

data class CreateStudioProject(
    val id: String,
    val title: String,
    val summary: String,
    val lastEditedText: String
)

data class CreateStudioDashboardState(
    val tools: List<CreateStudioTool>,
    val draft: CreateStudioDraft,
    val latestResult: CreateStudioResult?,
    val projects: List<CreateStudioProject>,
    val isLoading: Boolean,
    val errorMessage: String?
)
