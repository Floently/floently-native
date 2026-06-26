package com.floently.learn.yki

import kotlinx.datetime.Instant

enum class YkiLevel {
    Perustaso,
    Keskitaso,
    YlinTaso
}

enum class YkiSkill {
    Reading,
    Writing,
    Listening,
    Speaking,
    Vocabulary,
    Grammar
}

enum class YkiTaskType {
    MultipleChoice,
    ShortAnswer,
    WritingPrompt,
    ListeningPrompt,
    SpeakingPrompt,
    Cloze
}

data class YkiModule(
    val id: String,
    val title: String,
    val level: YkiLevel,
    val skills: List<YkiSkill>,
    val description: String,
    val estimatedMinutes: Int,
    val locked: Boolean
)

data class YkiTask(
    val id: String,
    val moduleId: String,
    val title: String,
    val type: YkiTaskType,
    val skill: YkiSkill,
    val prompt: String,
    val choices: List<String> = emptyList(),
    val expectedAnswer: String? = null,
    val explanation: String? = null
)

data class YkiSession(
    val id: String,
    val module: YkiModule,
    val tasks: List<YkiTask>,
    val currentTaskIndex: Int,
    val answers: Map<String, String>,
    val startedAt: Instant?
) {
    val currentTask: YkiTask?
        get() = tasks.getOrNull(currentTaskIndex)

    val completed: Boolean
        get() = tasks.isNotEmpty() && currentTaskIndex >= tasks.size
}

data class YkiModuleProgress(
    val moduleId: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val lastScorePercent: Int?,
    val updatedAt: Instant?
)

data class YkiDashboardState(
    val modules: List<YkiModule>,
    val progress: List<YkiModuleProgress>,
    val selectedLevel: YkiLevel,
    val isLoading: Boolean,
    val errorMessage: String?
)
