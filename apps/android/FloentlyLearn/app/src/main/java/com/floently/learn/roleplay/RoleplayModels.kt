package com.floently.learn.roleplay

enum class RoleplayLevel {
    A1,
    A2,
    B1,
    B2
}

enum class RoleplayScenarioType {
    EverydayLife,
    Work,
    Healthcare,
    Interview,
    CustomerService,
    OfficialErrand
}

enum class RoleplayMessageRole {
    User,
    Assistant,
    Coach,
    System
}

enum class RoleplayCoachingMode {
    BeginnerSafe,
    Balanced,
    ExamStyle
}

data class RoleplayScenario(
    val id: String,
    val title: String,
    val level: RoleplayLevel,
    val type: RoleplayScenarioType,
    val description: String,
    val firstPrompt: String,
    val targetPhrases: List<String>,
    val estimatedMinutes: Int,
    val locked: Boolean
)

data class RoleplayMessage(
    val id: String,
    val role: RoleplayMessageRole,
    val text: String,
    val coachingNote: String? = null
)

data class RoleplaySession(
    val id: String,
    val scenario: RoleplayScenario,
    val coachingMode: RoleplayCoachingMode,
    val messages: List<RoleplayMessage>,
    val userPhraseHistory: List<String>,
    val turnCount: Int,
    val isComplete: Boolean
)

data class RoleplayProgress(
    val scenarioId: String,
    val completedSessions: Int,
    val lastFeedback: String?
)

data class RoleplayDashboardState(
    val scenarios: List<RoleplayScenario>,
    val progress: List<RoleplayProgress>,
    val selectedLevel: RoleplayLevel,
    val isLoading: Boolean,
    val errorMessage: String?
)
