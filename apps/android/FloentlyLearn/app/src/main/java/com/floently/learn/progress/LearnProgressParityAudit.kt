package com.floently.learn.progress

internal object LearnProgressParityAudit {
    fun requireServicePendingFallbackOnly(state: LearnProgressDashboardState): LearnProgressDashboardState {
        val servicePendingFallback = state.syncBoundary.status == LearnProgressSyncStatus.ServicePending &&
            state.timeline.isEmpty()

        if (servicePendingFallback) {
            require(state.totalCompletedUnits == 0) {
                "Fallback progress must not report completed units."
            }
            require(state.activeStreakDays == 0) {
                "Fallback progress must not report an active streak."
            }
            require(state.summaries.all { it.completedUnits == 0 }) {
                "Fallback progress summaries must not report completed units."
            }
            require(state.summaries.all { it.streakDays == 0 }) {
                "Fallback progress summaries must not report streaks."
            }
            require(state.summaries.all { it.syncStatus == LearnProgressSyncStatus.ServicePending }) {
                "Fallback progress summaries must stay service-pending."
            }
        }

        return state
    }
}
