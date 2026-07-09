package com.floently.learn.yki

internal data class YkiRoleplayScenario(
    val id: String,
    val levelBand: String,
    val skill: String,
    val title: String,
    val openingLineFi: String,
    val userGoal: String,
    val bankSource: String
)

internal object YkiRoleplayMaterials {
    const val sourceId: String = "native_yki_roleplay_materials_m35"
    const val authority: String = "old_source_yki_roleplay_speaking_bank"
    const val materialLayer: String = "yki_exam_roleplay_speaking_material"
    val levelLabels: List<String> = listOf("A1-A2", "B1-B2", "C1-C2")

    private val scenarios = listOf(
        YkiRoleplayScenario(
            id = "yki-roleplay-a1a2-service-counter",
            levelBand = "A1-A2",
            skill = "speaking",
            title = "Service counter appointment",
            openingLineFi = "Hei, miten voin auttaa sinua tänään?",
            userGoal = "Book or change a simple appointment and answer basic follow-up questions.",
            bankSource = "${sourceId}_A1_A2"
        ),
        YkiRoleplayScenario(
            id = "yki-roleplay-b1b2-workplace-delay",
            levelBand = "B1-B2",
            skill = "speaking",
            title = "Workplace delay explanation",
            openingLineFi = "Hei, kerro lyhyesti miksi tehtävä viivästyi ja mitä ehdotat seuraavaksi.",
            userGoal = "Explain a delay, give reasons, and propose a practical next step.",
            bankSource = "${sourceId}_B1_B2"
        ),
        YkiRoleplayScenario(
            id = "yki-roleplay-c1c2-public-decision",
            levelBand = "C1-C2",
            skill = "speaking",
            title = "Public decision discussion",
            openingLineFi = "Keskustellaan päätöksestä. Mitä hyötyjä, riskejä ja vaihtoehtoja näet?",
            userGoal = "Give a structured opinion, compare alternatives, and respond to counterarguments.",
            bankSource = "${sourceId}_C1_C2"
        )
    )

    fun scenariosFor(levelBand: String): List<YkiRoleplayScenario> =
        scenarios.filter { it.levelBand == levelBand }

    fun allScenarios(): List<YkiRoleplayScenario> = scenarios

    fun requireReady(): List<YkiRoleplayScenario> {
        require(sourceId.isNotBlank()) { "YKI roleplay source id is missing." }
        require(authority.isNotBlank()) { "YKI roleplay authority is missing." }
        require(materialLayer.isNotBlank()) { "YKI roleplay material layer is missing." }
        require(levelLabels == listOf("A1-A2", "B1-B2", "C1-C2")) {
            "YKI roleplay must expose A1-A2, B1-B2, C1-C2."
        }
        require(levelLabels.all { level -> scenarios.any { it.levelBand == level } }) {
            "YKI roleplay must have material for every level band."
        }
        return scenarios
    }
}
