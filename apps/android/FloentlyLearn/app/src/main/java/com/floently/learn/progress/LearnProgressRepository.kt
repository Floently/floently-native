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
                lastActivity = "Native task/session flow verified",
                releaseGate = "Backend scoring and durable progress are still gated."
            ),
            LearnProgressSummary(
                area = LearnProgressArea.ProfessionalFinnish,
                title = "Professional Finnish",
                completedUnits = 1,
                totalUnits = 3,
                streakDays = 1,
                lastActivity = "Scenario foundation verified",
                releaseGate = "Professional feedback and durable progress are still gated."
            ),
            LearnProgressSummary(
                area = LearnProgressArea.Roleplay,
                title = "Roleplay",
                completedUnits = 1,
                totalUnits = 4,
                streakDays = 1,
                lastActivity = "Conversation foundation verified",
                releaseGate = "Dynamic conversation service and durable progress are still gated."
            ),
            LearnProgressSummary(
                area = LearnProgressArea.Cards,
                title = "Cards",
                completedUnits = 1,
                totalUnits = 4,
                streakDays = 1,
                lastActivity = "Deck foundation verified",
                releaseGate = "Spaced repetition sync is still gated."
            )
        )

        return LearnProgressDashboardState(
            summaries = summaries,
            totalCompletedUnits = summaries.sumOf { it.completedUnits },
            activeStreakDays = summaries.maxOf { it.streakDays },
            isLoading = false,
            errorMessage = null
        )
    }
}
