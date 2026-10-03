package com.floently.shared.learn

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LearnPathwayV1(val wireValue: String) {
    Everyday("everyday"),
    Yki("yki"),
    Professional("professional")
}

enum class LearnSkillV1(val wireValue: String) {
    Vocabulary("vocabulary"),
    Grammar("grammar"),
    Listening("listening"),
    Speaking("speaking"),
    Reading("reading"),
    Writing("writing")
}

enum class LearnEventTypeV1(val wireValue: String) {
    CardShown("card_shown"),
    CardAnswered("card_answered"),
    CardCorrect("card_correct"),
    CardIncorrect("card_incorrect"),
    ConfidenceCaptured("confidence_captured"),
    ReviewScheduled("review_scheduled"),
    ReviewCompleted("review_completed"),
    VocabularyMastered("vocabulary_mastered"),
    GrammarActivity("grammar_activity"),
    ReadingTask("reading_task"),
    ListeningTask("listening_task"),
    WritingSubmitted("writing_submitted"),
    WritingFeedback("writing_feedback"),
    SpeakingSession("speaking_session"),
    RoleplayTurn("roleplay_turn"),
    RoleplayCompleted("roleplay_completed"),
    YkiTaskAttempt("yki_task_attempt"),
    YkiSkillResult("yki_skill_result"),
    ProfessionalScenarioAttempt("professional_scenario_attempt"),
    ProfessionalMissionCompleted("professional_mission_completed"),
    PhraseCaptured("phrase_captured"),
    PhraseReviewed("phrase_reviewed"),
    LearnerCorrection("learner_correction"),
    QualifiedActivity("qualified_activity")
}

data class ActivityDefinitionV1(
    val schemaVersion: Int = 1,
    val activityId: String,
    val activityType: String,
    val pathway: LearnPathwayV1,
    val skill: LearnSkillV1,
    val contentRef: String,
    val responseContract: Map<String, Any?>,
    val evaluationPolicy: Map<String, Any?>? = null,
    val mediaRefs: List<String> = emptyList()
)

data class LearningSessionPlanV1(
    val schemaVersion: Int = 1,
    val sessionId: String,
    val revision: Int,
    val pathway: LearnPathwayV1,
    val levelBand: String,
    val objective: String,
    val policyVersion: String,
    val activities: List<ActivityDefinitionV1>,
    val resumeCursor: Int,
    val generatedAt: String,
    val expiresAt: String? = null
)

data class LearningEventV1(
    val schemaVersion: Int = 1,
    val eventId: String = UUID.randomUUID().toString(),
    val learnerId: String,
    val sessionId: String,
    val activityId: String?,
    val eventType: LearnEventTypeV1,
    val pathway: LearnPathwayV1,
    val skill: LearnSkillV1?,
    val occurredAt: String = Instant.now().toString(),
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val evidence: Map<String, Any?>
)

class LearningSessionStateV1 {
    private val _plan = MutableStateFlow<LearningSessionPlanV1?>(null)
    val plan: StateFlow<LearningSessionPlanV1?> = _plan.asStateFlow()

    private val _currentActivityIndex = MutableStateFlow(0)
    val currentActivityIndex: StateFlow<Int> = _currentActivityIndex.asStateFlow()

    val currentActivity: ActivityDefinitionV1?
        get() {
            val currentPlan = _plan.value ?: return null
            return currentPlan.activities.getOrNull(_currentActivityIndex.value)
        }

    val isComplete: Boolean
        get() {
            val currentPlan = _plan.value ?: return false
            return currentPlan.activities.isNotEmpty() &&
                _currentActivityIndex.value >= currentPlan.activities.size
        }

    fun load(plan: LearningSessionPlanV1) {
        _plan.value = plan
        _currentActivityIndex.value = plan.resumeCursor.coerceIn(
            0,
            plan.activities.size
        )
    }

    fun moveToActivity(index: Int) {
        val currentPlan = _plan.value ?: return
        _currentActivityIndex.value = index.coerceIn(
            0,
            currentPlan.activities.size
        )
    }

    fun advance() {
        val currentPlan = _plan.value ?: return
        _currentActivityIndex.update {
            (it + 1).coerceAtMost(currentPlan.activities.size)
        }
    }

    fun clear() {
        _plan.value = null
        _currentActivityIndex.value = 0
    }
}

class LearningEventOutboxV1 {
    private val _pending = MutableStateFlow<List<LearningEventV1>>(emptyList())
    val pending: StateFlow<List<LearningEventV1>> = _pending.asStateFlow()

    fun enqueue(event: LearningEventV1) {
        _pending.update { current ->
            if (current.any { it.idempotencyKey == event.idempotencyKey }) {
                current
            } else {
                current + event
            }
        }
    }

    fun acknowledge(idempotencyKey: String) {
        _pending.update { current ->
            current.filterNot { it.idempotencyKey == idempotencyKey }
        }
    }

    fun replacePending(events: List<LearningEventV1>) {
        _pending.value = events.distinctBy { it.idempotencyKey }
    }

    fun clear() {
        _pending.value = emptyList()
    }
}
