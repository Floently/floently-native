package com.floently.learn.yki

data class YkiAnswerEvaluation(
    val taskId: String,
    val status: YkiEvaluationStatus,
    val scorePercent: Int?,
    val feedback: String,
    val releaseGate: YkiEvaluationReleaseGate
)

enum class YkiEvaluationStatus {
    NotEvaluated,
    NeedsServiceScoring,
    NeedsSpeechReview,
    PreviewAccepted
}

enum class YkiEvaluationReleaseGate {
    PreviewOnly,
    RequiresServiceScoring,
    RequiresSpeechFeature,
    RequiresProgressPersistence,
    RequiresParityReview
}

data class YkiSessionEvaluationSummary(
    val sessionId: String,
    val evaluatedAnswers: List<YkiAnswerEvaluation>,
    val completedTaskCount: Int,
    val totalTaskCount: Int,
    val persisted: Boolean
) {
    val readyForDurableProgress: Boolean
        get() = persisted && evaluatedAnswers.all { it.scorePercent != null }
}

interface YkiEvaluator {
    fun evaluate(session: YkiSession): YkiSessionEvaluationSummary
}

class PreviewYkiEvaluator : YkiEvaluator {
    override fun evaluate(session: YkiSession): YkiSessionEvaluationSummary {
        val evaluations = session.answers.map { (taskId, answer) ->
            val task = session.tasks.firstOrNull { it.id == taskId }
            val status = when (task?.type) {
                YkiTaskType.SpeakingPrompt,
                YkiTaskType.ListeningPrompt -> YkiEvaluationStatus.NeedsSpeechReview
                YkiTaskType.WritingPrompt,
                YkiTaskType.ShortAnswer,
                YkiTaskType.Cloze -> YkiEvaluationStatus.NeedsServiceScoring
                YkiTaskType.MultipleChoice -> YkiEvaluationStatus.PreviewAccepted
                null -> YkiEvaluationStatus.NotEvaluated
            }

            YkiAnswerEvaluation(
                taskId = taskId,
                status = status,
                scorePercent = if (status == YkiEvaluationStatus.PreviewAccepted && answer.isNotBlank()) 100 else null,
                feedback = when (status) {
                    YkiEvaluationStatus.PreviewAccepted -> "Preview answer captured. Final scoring still requires parity checks."
                    YkiEvaluationStatus.NeedsServiceScoring -> "Answer captured. Service scoring is required before release."
                    YkiEvaluationStatus.NeedsSpeechReview -> "Speech-style task captured as text preview. Speech feature parity is required before release."
                    YkiEvaluationStatus.NotEvaluated -> "Answer captured but no matching task was found."
                },
                releaseGate = when (status) {
                    YkiEvaluationStatus.PreviewAccepted -> YkiEvaluationReleaseGate.RequiresParityReview
                    YkiEvaluationStatus.NeedsServiceScoring -> YkiEvaluationReleaseGate.RequiresServiceScoring
                    YkiEvaluationStatus.NeedsSpeechReview -> YkiEvaluationReleaseGate.RequiresSpeechFeature
                    YkiEvaluationStatus.NotEvaluated -> YkiEvaluationReleaseGate.PreviewOnly
                }
            )
        }

        return YkiSessionEvaluationSummary(
            sessionId = session.id,
            evaluatedAnswers = evaluations,
            completedTaskCount = session.answers.size,
            totalTaskCount = session.tasks.size,
            persisted = false
        )
    }
}
