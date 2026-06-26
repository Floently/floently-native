package com.floently.learn.progress

enum class LearnProgressArea {
    Yki,
    ProfessionalFinnish,
    Roleplay,
    Cards
}

enum class LearnProgressSyncStatus {
    LocalPreview,
    ReadyToSync,
    Synced,
    ServicePending
}

data class LearnProgressSummary(
    val area: LearnProgressArea,
    val title: String,
    val completedUnits: Int,
    val totalUnits: Int,
    val streakDays: Int,
    val lastActivity: String,
    val releaseGate: String,
    val syncStatus: LearnProgressSyncStatus
) {
    val completionPercent: Int
        get() = if (totalUnits == 0) 0 else ((completedUnits.toDouble() / totalUnits.toDouble()) * 100).toInt()
}

data class LearnProgressTimelineItem(
    val id: String,
    val area: LearnProgressArea,
    val title: String,
    val detail: String,
    val whenText: String,
    val durable: Boolean
)

data class LearnProgressSyncBoundary(
    val status: LearnProgressSyncStatus,
    val pendingEvents: Int,
    val lastSyncText: String,
    val releaseGate: String
)

data class LearnProgressDashboardState(
    val summaries: List<LearnProgressSummary>,
    val timeline: List<LearnProgressTimelineItem>,
    val syncBoundary: LearnProgressSyncBoundary,
    val totalCompletedUnits: Int,
    val activeStreakDays: Int,
    val isLoading: Boolean,
    val errorMessage: String?
)
