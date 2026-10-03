package com.floently.shared.learn

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONObject

data class YKIDailyPracticeOverview(
    val status: String,
    val focus: String,
    val minutes: Int
)

data class YKIPracticeOverview(
    val levelBand: String,
    val displayLevelBand: String,
    val bankKind: String,
    val totalTasks: Int,
    val recommendedSections: List<String>,
    val nextFocus: String,
    val nextTask: String,
    val countsBySkill: Map<String, Int>,
    val dailyPractice: YKIDailyPracticeOverview
)

data class ProfessionalWorkTrack(
    val domain: String,
    val title: String,
    val coreTasks: List<String>,
    val keyLanguageTargets: List<String>,
    val speakingScenarios: List<String>,
    val writingTasks: List<String>,
    val vocabularyClusters: List<String>
)

data class ProfessionalOverview(
    val tracks: List<ProfessionalWorkTrack>
)

class LearnOverviewService(
    private val api: FloentlyApiClient
) {
    suspend fun fetchYkiOverview(
        levelBand: String = "B1-B2"
    ): YKIPracticeOverview {
        return ykiOverviewFromJson(
            api.get(
                path = "/api/v1/yki-practice/overview",
                query = mapOf("level_band" to levelBand)
            )
        )
    }

    suspend fun fetchProfessionalOverview(): ProfessionalOverview {
        return professionalOverviewFromJson(
            api.get("/api/v1/professional/overview")
        )
    }
}

private fun ykiOverviewFromJson(json: JSONObject): YKIPracticeOverview {
    val sectionsArray = json.optJSONArray("recommendedSections")
    val sections = buildList {
        if (sectionsArray != null) {
            for (index in 0 until sectionsArray.length()) {
                sectionsArray.optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    val countsObject = json.optJSONObject("countsBySkill") ?: JSONObject()
    val counts = buildMap {
        countsObject.keys().forEach { key ->
            put(key, countsObject.optInt(key))
        }
    }

    val daily = json.optJSONObject("dailyPractice") ?: JSONObject()

    return YKIPracticeOverview(
        levelBand = json.optString("level_band"),
        displayLevelBand = json.optString("display_level_band"),
        bankKind = json.optString("bank_kind"),
        totalTasks = json.optInt("total_tasks"),
        recommendedSections = sections,
        nextFocus = json.optString("nextFocus"),
        nextTask = json.optString("nextTask"),
        countsBySkill = counts,
        dailyPractice = YKIDailyPracticeOverview(
            status = daily.optString("status"),
            focus = daily.optString("focus"),
            minutes = daily.optInt("minutes")
        )
    )
}

private fun professionalOverviewFromJson(
    json: JSONObject
): ProfessionalOverview {
    val tracksArray = json.optJSONArray("tracks")

    val tracks = buildList {
        if (tracksArray != null) {
            for (index in 0 until tracksArray.length()) {
                val track = tracksArray.optJSONObject(index) ?: continue
                add(
                    ProfessionalWorkTrack(
                        domain = track.optString("domain"),
                        title = track.optString("title"),
                        coreTasks = track.stringList("core_tasks"),
                        keyLanguageTargets = track.stringList("key_language_targets"),
                        speakingScenarios = track.stringList("speaking_scenarios"),
                        writingTasks = track.stringList("writing_tasks"),
                        vocabularyClusters = track.stringList("vocabulary_clusters")
                    )
                )
            }
        }
    }

    return ProfessionalOverview(tracks = tracks)
}

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index)
                .takeIf { it.isNotBlank() }
                ?.let(::add)
        }
    }
}
