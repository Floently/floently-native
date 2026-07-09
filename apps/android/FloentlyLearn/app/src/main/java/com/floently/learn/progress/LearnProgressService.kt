package com.floently.learn.progress

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class LearnProgressService(private val api: FloentlyApiClient) {
    suspend fun dashboard(): LearnProgressDashboardState {
        return dashboardFromJson(api.get("/api/v1/learn/progress/dashboard"))
    }

    private fun dashboardFromJson(json: JSONObject): LearnProgressDashboardState {
        val summariesJson = json.optJSONArray("summaries") ?: JSONArray()
        val timelineJson = json.optJSONArray("timeline") ?: JSONArray()
        val summaries = List(summariesJson.length()) { index -> summaryFromJson(summariesJson.getJSONObject(index)) }
        val timeline = List(timelineJson.length()) { index -> timelineItemFromJson(timelineJson.getJSONObject(index)) }
        val syncBoundary = json.optJSONObject("sync_boundary")?.let { syncBoundaryFromJson(it) }
            ?: LearnProgressSyncBoundary(
                status = LearnProgressSyncStatus.ServicePending,
                pendingEvents = timeline.count { !it.durable },
                lastSyncText = json.optString("last_sync_text").ifBlank { "Progress backend boundary is waiting for verified service data." },
                releaseGate = json.optString("release_gate").ifBlank { "Durable backend progress must be verified before release." }
            )
        return LearnProgressDashboardState(
            summaries = summaries,
            timeline = timeline,
            syncBoundary = syncBoundary,
            totalCompletedUnits = json.optInt("total_completed_units", summaries.sumOf { it.completedUnits }),
            activeStreakDays = json.optInt("active_streak_days", summaries.maxOfOrNull { it.streakDays } ?: 0),
            isLoading = false,
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    private fun summaryFromJson(json: JSONObject): LearnProgressSummary = LearnProgressSummary(
        area = areaFromApi(json.optString("area")),
        title = json.optString("title").ifBlank { "Learning area" },
        completedUnits = json.optInt("completed_units", json.optInt("completedUnits", 0)),
        totalUnits = json.optInt("total_units", json.optInt("totalUnits", 0)),
        streakDays = json.optInt("streak_days", json.optInt("streakDays", 0)),
        lastActivity = json.optString("last_activity").ifBlank { json.optString("lastActivity") },
        releaseGate = json.optString("release_gate").ifBlank { "Progress backend service boundary." },
        syncStatus = syncStatusFromApi(json.optString("sync_status"))
    )

    private fun timelineItemFromJson(json: JSONObject): LearnProgressTimelineItem = LearnProgressTimelineItem(
        id = json.optString("id").ifBlank { "timeline-item" },
        area = areaFromApi(json.optString("area")),
        title = json.optString("title").ifBlank { "Progress activity" },
        detail = json.optString("detail"),
        whenText = json.optString("when_text").ifBlank { json.optString("whenText") },
        durable = json.optBoolean("durable", false)
    )

    private fun syncBoundaryFromJson(json: JSONObject): LearnProgressSyncBoundary = LearnProgressSyncBoundary(
        status = syncStatusFromApi(json.optString("status")),
        pendingEvents = json.optInt("pending_events", json.optInt("pendingEvents", 0)),
        lastSyncText = json.optString("last_sync_text").ifBlank { json.optString("lastSyncText") },
        releaseGate = json.optString("release_gate").ifBlank { "Progress backend sync service boundary." }
    )

    private fun areaFromApi(value: String?): LearnProgressArea = when (value?.trim()?.lowercase()) {
        "professional_finnish", "professionalfinnish", "professional" -> LearnProgressArea.ProfessionalFinnish
        "roleplay" -> LearnProgressArea.Roleplay
        "cards" -> LearnProgressArea.Cards
        else -> LearnProgressArea.Yki
    }

    private fun syncStatusFromApi(value: String?): LearnProgressSyncStatus = when (value?.trim()?.lowercase()) {
        "ready_to_sync", "readytosync" -> LearnProgressSyncStatus.ReadyToSync
        "synced" -> LearnProgressSyncStatus.Synced
        "service_pending", "pending" -> LearnProgressSyncStatus.ServicePending
        else -> LearnProgressSyncStatus.LocalPreview
    }
}
