package com.floently.learn.roleplay

enum class RoleplayLevel {
    A1,
    A2,
    B1,
    B2
}

enum class RoleplayScenarioType {
    Everyday,
    Work,
    Healthcare,
    Interview,
    PhoneCall,
    Service
}

enum class RoleplaySpeaker {
    Learner,
    Coach,
    Partner
}

enum class RoleplayCoachingMode {
    BeginnerSafe,
    Natural,
    Professional,
    ExamStyle
}

data class RoleplayScenario(
    val id: String,
    val title: String,
    val level: RoleplayLevel,
    val type: RoleplayScenarioType,
    val description: String,
    val openingLine: String,
    val targetPhrases: List<String> = emptyList(),
    val beginnerSafe: Boolean,
    val locked: Boolean
)

data class RoleplayMessage(
    val id: String,
    val speaker: RoleplaySpeaker,
    val text: String,
    val coachingNote: String? = null
)

data class RoleplaySession(
    val id: String,
    val scenario: RoleplayScenario,
    val messages: List<RoleplayMessage>,
    val learnerTurns: Int,
    val repeatedCueCount: Int,
    val releaseGate: String
)

data class RoleplayDashboardState(
    val scenarios: List<RoleplayScenario>,
    val selectedLevel: RoleplayLevel,
    val isLoading: Boolean,
    val errorMessage: String?
)

data class RoleplayCoachResponse(
    val partnerMessage: RoleplayMessage,
    val coachMessage: RoleplayMessage?,
    val repeatedCuePrevented: Boolean
)
