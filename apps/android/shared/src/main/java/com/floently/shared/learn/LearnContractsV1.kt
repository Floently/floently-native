package com.floently.shared.learn

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LearnPathwayV1(val wireValue: String) {
    Everyday("everyday"),
    Professional("professional"),
    Yki("yki")
}

enum class LearnSkillV1(val wireValue: String) {
    Vocabulary("vocabulary"),
    Grammar("grammar"),
    Listening("listening"),
    Speaking("speaking"),
    Reading("reading"),
    Writing("writing")
}

enum class LearnRuntimeV1(val wireValue: String) {
    Cards("cards"),
    Roleplay("roleplay"),
    Reading("reading"),
    Writing("writing"),
    Listening("listening"),
    Yki("yki"),
    ProfessionalMission("professional_mission"),
    Grammar("grammar")
}

enum class LearnTaskHealthV1(val wireValue: String) {
    Available("available"),
    Degraded("degraded"),
    Unavailable("unavailable")
}

enum class LearnEventKindV1(val wireValue: String) {
    TaskStarted("task_started"),
    TaskCompleted("task_completed"),
    TaskSkipped("task_skipped"),
    TaskAbandoned("task_abandoned"),
    AnswerSubmitted("answer_submitted"),
    AnswerCorrected("answer_corrected"),
    RetryCompleted("retry_completed"),
    WritingSubmitted("writing_submitted"),
    WritingRetried("writing_retried"),
    SpeakingSubmitted("speaking_submitted"),
    ReadingCompleted("reading_completed"),
    ListeningCompleted("listening_completed")
}

data class LearnTaskModalityV1(
    val audio: Boolean? = null,
    val microphone: Boolean? = null,
    val keyboard: Boolean? = null,
    val visual: Boolean? = null
)

data class LearnTaskLaunchTargetV1(
    val route: String,
    val params: Map<String, String>? = null
)

data class TaskDescriptorV1(
    val schemaVersion: String = "learning.v1",
    val taskId: String,
    val contentVersion: String,
    val runtime: LearnRuntimeV1,
    val pathway: LearnPathwayV1,
    val skills: List<LearnSkillV1>,
    val levelBand: String,
    val estimatedMinutes: Int,
    val modality: LearnTaskModalityV1,
    val requiredEntitlements: List<String>,
    val launch: LearnTaskLaunchTargetV1,
    val health: LearnTaskHealthV1,
    val featureFlag: String? = null,
    val profession: String? = null,
    val topic: String? = null,
    val contextId: String? = null,
    val prerequisites: List<String>? = null,
    val tags: List<String>? = null,
    val ykiMode: String? = null
)

data class PracticeSelectionReasonV1(
    val code: String,
    val message: String,
    val evidenceMode: String
)

data class PracticeSessionTaskV1(
    val order: Int,
    val task: TaskDescriptorV1,
    val reasons: List<PracticeSelectionReasonV1>
)

data class PracticeSessionManifestV1(
    val schemaVersion: String = "learning.v1",
    val sessionId: String,
    val learnerId: String,
    val createdAt: String,
    val scope: String,
    val targetMinutes: Int,
    val tasks: List<PracticeSessionTaskV1>,
    val composerVersion: String
)

data class LearnerEventV1(
    val schemaVersion: String = "learning.v1",
    val eventId: String = UUID.randomUUID().toString(),
    val learnerId: String,
    val occurredAt: String = Instant.now().toString(),
    val eventKind: LearnEventKindV1,
    val taskId: String,
    val contentVersion: String,
    val attemptId: String? = null,
    val pathway: LearnPathwayV1,
    val runtime: LearnRuntimeV1,
    val skills: List<LearnSkillV1>,
    val levelBand: String,
    val profession: String? = null,
    val contextId: String? = null,
    val score: Double? = null,
    val maxScore: Double? = null,
    val metadata: Map<String, Any>? = null
)

class LearningSessionStateV1 {
    private val _manifest = MutableStateFlow<PracticeSessionManifestV1?>(null)
    val manifest: StateFlow<PracticeSessionManifestV1?> = _manifest.asStateFlow()

    private val _currentTaskIndex = MutableStateFlow(0)
    val currentTaskIndex: StateFlow<Int> = _currentTaskIndex.asStateFlow()

    val currentTask: PracticeSessionTaskV1?
        get() {
            val current = _manifest.value ?: return null
            return current.tasks.getOrNull(_currentTaskIndex.value)
        }

    val isComplete: Boolean
        get() {
            val current = _manifest.value ?: return false
            return current.tasks.isNotEmpty() &&
                _currentTaskIndex.value >= current.tasks.size
        }

    fun load(manifest: PracticeSessionManifestV1) {
        _manifest.value = manifest
        _currentTaskIndex.value = 0
    }

    fun moveToTask(index: Int) {
        val current = _manifest.value ?: return
        _currentTaskIndex.value = index.coerceIn(0, current.tasks.size)
    }

    fun advance() {
        val current = _manifest.value ?: return
        _currentTaskIndex.update {
            (it + 1).coerceAtMost(current.tasks.size)
        }
    }

    fun clear() {
        _manifest.value = null
        _currentTaskIndex.value = 0
    }
}

class LearningEventOutboxV1 {
    private val _pending = MutableStateFlow<List<LearnerEventV1>>(emptyList())
    val pending: StateFlow<List<LearnerEventV1>> = _pending.asStateFlow()

    fun enqueue(event: LearnerEventV1) {
        _pending.update { current ->
            if (current.any { it.eventId == event.eventId }) current else current + event
        }
    }

    fun acknowledge(eventId: String) {
        _pending.update { current ->
            current.filterNot { it.eventId == eventId }
        }
    }

    fun replacePending(events: List<LearnerEventV1>) {
        _pending.value = events.distinctBy { it.eventId }
    }

    fun clear() {
        _pending.value = emptyList()
    }
}
