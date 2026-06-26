package com.floently.learn.progress

interface LearnProgressRepository {
    suspend fun dashboard(): LearnProgressDashboardState
}

class PreviewLearnProgressRepository : LearnProgressRepository {
    override suspend fun dashboard(): LearnProgressDashboardState {
        val summaries = listOf(
            LearnProgressSummary(
                area = LearnProgressArea.Yki,
                title = "YKI practice",
                completedUnits = 1,
                totalUnits = 4,
                streakDays = 1,
                lastActivity = "Native task flow verified",
                releaseGate = "Real scoring and durable progress remain service-gated.",
                syncStatus = LearnProgressSyncStatus.ReadyToSync
            ),
            LearnProgressSummary(
                area = LearnProgressArea.ProfessionalFinnish,
                title = "Professional Finnish",
                completedUnits = 1,
                totalUnits = 3,
                streakDays = 1,
                lastActivity = "Scenario flow verified",
                releaseGate = "Feedback and saved progress remain service-gated.",
                syncStatus = LearnProgressSyncStatus.ReadyToSync
            ),
            LearnProgressSummary(
                area = LearnProgressArea.Roleplay,
                title = "Roleplay",
                completedUnits = 1,
                totalUnits = 4,
                streakDays = 1,
                lastActivity = "Conversation flow verified",
                releaseGate = "Dynamic generation and saved progress remain service-gated.",
                syncStatus = LearnProgressSyncStatus.LocalPreview
            ),
            LearnProgressSummary(
                area = LearnProgressArea.Cards,
                title = "Cards",
                completedUnits = 1,
                totalUnits = 4,
                streakDays = 1,
                lastActivity = "Deck flow verified",
                releaseGate = "Review scheduling and saved progress remain service-gated.",
                syncStatus = LearnProgressSyncStatus.LocalPreview
            )
        )

        val timeline = listOf(
            LearnProgressTimelineItem(
                id = "timeline-yki-native",
                area = LearnProgressArea.Yki,
                title = "YKI native session completed",
                detail = "Answer capture, evaluation summary, and progress boundary are available.",
                whenText = "Preview milestone",
                durable = false
            ),
            LearnProgressTimelineItem(
                id = "timeline-roleplay-native",
                area = LearnProgressArea.Roleplay,
                title = "Roleplay native session started",
                detail = "Conversation, coaching, and anti-repetition state are available.",
                whenText = "Preview milestone",
                durable = false
            ),
            LearnProgressTimelineItem(
                id = "timeline-cards-native",
                area = LearnProgressArea.Cards,
                title = "Cards native deck opened",
                detail = "Deck data and start-session boundary are available.",
                whenText = "Preview milestone",
                durable = false
            )
        )

        return LearnProgressDashboardState(
            summaries = summaries,
            timeline = timeline,
            syncBoundary = LearnProgressSyncBoundary(
                status = LearnProgressSyncStatus.ServicePending,
                pendingEvents = timeline.count { !it.durable },
                lastSyncText = "Server progress sync is not connected yet.",
                releaseGate = "Durable progress needs authenticated server write/read, offline queue, and merge handling before release."
            ),
            totalCompletedUnits = summaries.sumOf { it.completedUnits },
            activeStreakDays = summaries.maxOf { it.streakDays },
            isLoading = false,
            errorMessage = null
        )
    }
}
