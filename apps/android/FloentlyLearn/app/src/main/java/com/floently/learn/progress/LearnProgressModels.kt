package com.floently.learn.progress

enum class LearnProgressArea {
    Yki,
    ProfessionalFinnish,
    Roleplay,
    Cards
}

data class LearnProgressSummary(
    val area: LearnProgressArea,
    val title: String,
    val completedUnits: Int,
    val totalUnits: Int,
    val streakDays: Int,
    val lastActivity: String,
    val releaseGate: String
) {
    val completionPercent: Int
        get() = if (totalUnits == 0) 0 else ((completedUnits.toDouble() / totalUnits.toDouble()) * 100).toInt()
}

data class LearnProgressDashboardState(
    val summaries: List<LearnProgressSummary>,
    val totalCompletedUnits: Int,
    val activeStreakDays: Int,
    val isLoading: Boolean,
    val errorMessage: String?
)
