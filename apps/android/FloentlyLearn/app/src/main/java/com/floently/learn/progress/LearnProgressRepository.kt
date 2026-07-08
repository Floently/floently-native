package com.floently.learn.progress

interface LearnProgressRepository {
    suspend fun dashboard(): LearnProgressDashboardState
}

class ServiceLearnProgressRepository(
    private val service: LearnProgressService,
    private val fallback: LearnProgressRepository = PreviewLearnProgressRepository()
) : LearnProgressRepository {
    override suspend fun dashboard(): LearnProgressDashboardState {
        return runCatching { service.dashboard() }.getOrElse { error ->
            fallback.dashboard().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Learn progress service is not available yet."
            )
        }
    }
}

class PreviewLearnProgressRepository : LearnProgressRepository {
    override suspend fun dashboard(): LearnProgressDashboardState {
        val summaries = listOf(
            LearnProgressSummary(
                area = LearnProgressArea.Yki,
                title = "YKI readiness",
                completedUnits = 0,
                totalUnits = 0,
                streakDays = 0,
                lastActivity = "No saved YKI progress has been returned yet.",
                releaseGate = "This fallback does not count as completed YKI progress.",
                syncStatus = LearnProgressSyncStatus.ServicePending
            ),
            LearnProgressSummary(
                area = LearnProgressArea.ProfessionalFinnish,
                title = "Workplace communication",
                completedUnits = 0,
                totalUnits = 0,
                streakDays = 0,
                lastActivity = "No saved workplace communication progress has been returned yet.",
                releaseGate = "This fallback does not count as completed workplace progress.",
                syncStatus = LearnProgressSyncStatus.ServicePending
            ),
            LearnProgressSummary(
                area = LearnProgressArea.Cards,
                title = "Profession vocabulary",
                completedUnits = 0,
                totalUnits = 0,
                streakDays = 0,
                lastActivity = "No saved vocabulary progress has been returned yet.",
                releaseGate = "This fallback does not count as completed vocabulary progress.",
                syncStatus = LearnProgressSyncStatus.ServicePending
            )
        )

        return LearnProgressDashboardState(
            summaries = summaries,
            timeline = emptyList(),
            syncBoundary = LearnProgressSyncBoundary(
                status = LearnProgressSyncStatus.ServicePending,
                pendingEvents = 0,
                lastSyncText = "Progress service has not returned saved learning activity yet.",
                releaseGate = "Only verified service data is shown as completed progress."
            ),
            totalCompletedUnits = 0,
            activeStreakDays = 0,
            isLoading = false,
            errorMessage = null
        )
    }
}
