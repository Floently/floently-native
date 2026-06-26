package com.floently.learn.yki

interface YkiRepository {
    suspend fun dashboard(selectedLevel: YkiLevel): YkiDashboardState
    suspend fun startSession(moduleId: String): YkiSessionResult
    suspend fun saveAnswer(session: YkiSession, taskId: String, answer: String): YkiSession
}

sealed interface YkiSessionResult {
    data class Ready(val session: YkiSession) : YkiSessionResult
    data class Blocked(val reason: String) : YkiSessionResult
    data class Error(val message: String) : YkiSessionResult
}

class PreviewYkiRepository : YkiRepository {
    private val modules = listOf(
        YkiModule(
            id = "yki-keskitaso-reading-1",
            title = "Keskitaso reading practice",
            level = YkiLevel.Keskitaso,
            skills = listOf(YkiSkill.Reading, YkiSkill.Vocabulary),
            description = "Native YKI reading foundation for exam-style comprehension tasks.",
            estimatedMinutes = 12,
            locked = false
        ),
        YkiModule(
            id = "yki-keskitaso-writing-1",
            title = "Keskitaso writing practice",
            level = YkiLevel.Keskitaso,
            skills = listOf(YkiSkill.Writing, YkiSkill.Grammar),
            description = "Native YKI writing foundation for short practical Finnish answers.",
            estimatedMinutes = 18,
            locked = false
        ),
        YkiModule(
            id = "yki-ylin-speaking-1",
            title = "Ylin taso speaking practice",
            level = YkiLevel.YlinTaso,
            skills = listOf(YkiSkill.Speaking, YkiSkill.Vocabulary),
            description = "Native speaking shell for advanced YKI practice. Audio capture is still gated.",
            estimatedMinutes = 15,
            locked = true
        )
    )

    private val tasksByModuleId = mapOf(
        "yki-keskitaso-reading-1" to listOf(
            YkiTask(
                id = "reading-1-task-1",
                moduleId = "yki-keskitaso-reading-1",
                title = "Read and choose",
                type = YkiTaskType.MultipleChoice,
                skill = YkiSkill.Reading,
                prompt = "Valitse paras vastaus tekstin perusteella.",
                choices = listOf("A", "B", "C")
            )
        ),
        "yki-keskitaso-writing-1" to listOf(
            YkiTask(
                id = "writing-1-task-1",
                moduleId = "yki-keskitaso-writing-1",
                title = "Write a short answer",
                type = YkiTaskType.WritingPrompt,
                skill = YkiSkill.Writing,
                prompt = "Kirjoita lyhyt vastaus tilanteeseen: pyydä lisätietoja tapaamisesta."
            )
        ),
        "yki-ylin-speaking-1" to listOf(
            YkiTask(
                id = "speaking-1-task-1",
                moduleId = "yki-ylin-speaking-1",
                title = "Speaking prompt",
                type = YkiTaskType.SpeakingPrompt,
                skill = YkiSkill.Speaking,
                prompt = "Kerro mielipiteesi aiheesta ja perustele se."
            )
        )
    )

    override suspend fun dashboard(selectedLevel: YkiLevel): YkiDashboardState {
        val visibleModules = modules.filter { it.level == selectedLevel }
        return YkiDashboardState(
            modules = visibleModules,
            progress = visibleModules.map { module ->
                val taskCount = tasksByModuleId[module.id].orEmpty().size
                YkiModuleProgress(
                    moduleId = module.id,
                    completedTasks = 0,
                    totalTasks = taskCount,
                    lastScorePercent = null,
                    updatedAtEpochMillis = null
                )
            },
            selectedLevel = selectedLevel,
            isLoading = false,
            errorMessage = null
        )
    }

    override suspend fun startSession(moduleId: String): YkiSessionResult {
        val module = modules.firstOrNull { it.id == moduleId }
            ?: return YkiSessionResult.Error("YKI module was not found.")

        if (module.locked) {
            return YkiSessionResult.Blocked("This YKI module is still gated until audio, progress, and release checks are complete.")
        }

        val tasks = tasksByModuleId[moduleId].orEmpty()
        if (tasks.isEmpty()) {
            return YkiSessionResult.Error("YKI module has no tasks yet.")
        }

        return YkiSessionResult.Ready(
            YkiSession(
                id = "session-$moduleId-preview",
                module = module,
                tasks = tasks,
                currentTaskIndex = 0,
                answers = emptyMap(),
                startedAtEpochMillis = null
            )
        )
    }

    override suspend fun saveAnswer(session: YkiSession, taskId: String, answer: String): YkiSession =
        session.copy(
            answers = session.answers + (taskId to answer),
            currentTaskIndex = session.currentTaskIndex + 1
        )
}
