package com.floently.learn.yki

internal object YkiM35ParityContract {
    private val requiredLevelBands = listOf("A1-A2", "B1-B2", "C1-C2")

    fun requireReady(mode: YkiScreenMode): YkiMaterialSource {
        val source = YkiMaterialWiring.requireReady(mode)
        requirePracticeBankReady()
        requireMockBankReady()
        requireRoleplayMaterialsReady()
        return source
    }

    private fun requirePracticeBankReady() {
        val tasks = YkiPracticeBank.practiceTasks()
        require(YkiPracticeBank.sourceId.isNotBlank()) { "YKI Practice source id is missing." }
        require(YkiPracticeBank.authority.isNotBlank()) { "YKI Practice authority is missing." }
        require(YkiPracticeBank.materialLayer.isNotBlank()) { "YKI Practice material layer is missing." }
        require(tasks.size == YkiPracticeBank.lockedTaskCount) {
            "YKI Practice bank count must match the screenshot-locked task count."
        }
        require(tasks.any { it.isReadingPractice() }) { "YKI Practice reading material missing." }
        require(tasks.any { it.isListeningPractice() }) { "YKI Practice listening material missing." }
        require(tasks.any { it.isWritingPractice() }) { "YKI Practice writing material missing." }
        require(tasks.any { it.isSpeakingPractice() }) { "YKI Practice speaking material missing." }
    }

    private fun requireMockBankReady() {
        val labels = YkiMockLevelBand.values().map { it.label }
        require(labels == requiredLevelBands) { "YKI Mock must expose A1-A2, B1-B2, C1-C2." }

        YkiMockLevelBand.values().forEach { level ->
            val tasks = YkiMockExamBank.tasks(level)
            require(tasks.size == YkiMockExamBank.totalExamTasks) {
                "YKI Mock ${level.label} must expose the full exam task count."
            }
            require(tasks.all { it.levelBand == level }) {
                "YKI Mock ${level.label} tasks must carry the selected level band."
            }
            require(tasks.all { it.bankSource.contains(level.label.replace("-", "_")) }) {
                "YKI Mock ${level.label} tasks must carry level-specific bank source."
            }
            require(tasks.any { it.skill == YkiMockSkill.Reading }) { "YKI Mock ${level.label} reading material missing." }
            require(tasks.any { it.skill == YkiMockSkill.Listening }) { "YKI Mock ${level.label} listening material missing." }
            require(tasks.any { it.skill == YkiMockSkill.Writing }) { "YKI Mock ${level.label} writing material missing." }
            require(tasks.any { it.skill == YkiMockSkill.Speaking }) { "YKI Mock ${level.label} speaking material missing." }
        }
    }

    private fun requireRoleplayMaterialsReady() {
        val scenarios = YkiRoleplayMaterials.requireReady()
        require(YkiRoleplayMaterials.levelLabels == requiredLevelBands) {
            "YKI roleplay level labels must match the YKI exam level bands."
        }
        requiredLevelBands.forEach { level ->
            require(scenarios.any { it.levelBand == level }) {
                "YKI roleplay material missing for $level."
            }
        }
    }

    private fun YkiPracticeBankTask.isReadingPractice(): Boolean =
        title.lowercase().startsWith("lue ")

    private fun YkiPracticeBankTask.isListeningPractice(): Boolean =
        audioScript.isNotBlank() || title.lowercase().startsWith("kuuntele ")

    private fun YkiPracticeBankTask.isWritingPractice(): Boolean =
        wordCounter.isNotBlank() || title.lowercase().startsWith("kirjoita ")

    private fun YkiPracticeBankTask.isSpeakingPractice(): Boolean {
        val normalizedTitle = title.lowercase()
        return wordCounter.isBlank() &&
            audioScript.isBlank() &&
            (
                normalizedTitle.contains("keskustelu") ||
                normalizedTitle.contains("roolipeli") ||
                normalizedTitle.contains("puhelimitse") ||
                normalizedTitle.startsWith("mielipide ")
            )
    }
}
