package com.floently.learn.yki

internal data class YkiMaterialSource(
    val route: YkiScreenMode,
    val sourceId: String,
    val authority: String,
    val materialLayer: String,
    val levelLabels: List<String>,
    val taskCount: Int
)

internal object YkiMaterialWiring {
    private val ykiLevelLabels = listOf("A1-A2", "B1-B2", "C1-C2")

    fun requireReady(mode: YkiScreenMode): YkiMaterialSource {
        val source = when (mode) {
            YkiScreenMode.Practice -> practiceSource()
            YkiScreenMode.MockExam -> mockSource()
        }

        require(source.sourceId.isNotBlank()) { "YKI material source id is missing for ${source.route}." }
        require(source.authority.isNotBlank()) { "YKI material authority is missing for ${source.route}." }
        require(source.materialLayer.isNotBlank()) { "YKI material layer is missing for ${source.route}." }
        require(source.levelLabels == ykiLevelLabels) { "YKI level coverage must be A1-A2, B1-B2, C1-C2." }
        require(source.taskCount > 0) { "YKI material source has no tasks for ${source.route}." }

        return source
    }

    private fun practiceSource(): YkiMaterialSource =
        YkiMaterialSource(
            route = YkiScreenMode.Practice,
            sourceId = YkiPracticeBank.sourceId,
            authority = YkiPracticeBank.authority,
            materialLayer = YkiPracticeBank.materialLayer,
            levelLabels = ykiLevelLabels,
            taskCount = YkiPracticeBank.lockedTaskCount
        )

    private fun mockSource(): YkiMaterialSource {
        val tasks = YkiMockExamBank.tasks(YkiMockLevelBand.B1_B2)
        return YkiMaterialSource(
            route = YkiScreenMode.MockExam,
            sourceId = "native_yki_mock_exam_bank_img_0432_0479",
            authority = YkiMockExamBank.authority,
            materialLayer = "engine_v3_2_certified_level_aware_mock_material",
            levelLabels = YkiMockLevelBand.values().map { it.label },
            taskCount = tasks.size
        )
    }
}
