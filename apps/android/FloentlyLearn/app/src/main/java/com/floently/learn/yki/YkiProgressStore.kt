package com.floently.learn.yki

data class YkiProgressRecord(
    val moduleId: String,
    val sessionId: String,
    val completedTaskCount: Int,
    val totalTaskCount: Int,
    val scorePercent: Int?,
    val savedAtEpochMillis: Long?,
    val durable: Boolean
)

sealed interface YkiProgressSaveResult {
    data class Saved(val record: YkiProgressRecord) : YkiProgressSaveResult
    data class Deferred(val reason: String, val record: YkiProgressRecord) : YkiProgressSaveResult
    data class Failed(val message: String) : YkiProgressSaveResult
}

interface YkiProgressStore {
    suspend fun save(session: YkiSession, summary: YkiSessionEvaluationSummary): YkiProgressSaveResult
}

class PreviewYkiProgressStore : YkiProgressStore {
    override suspend fun save(session: YkiSession, summary: YkiSessionEvaluationSummary): YkiProgressSaveResult {
        val finalScores = summary.evaluatedAnswers.mapNotNull { it.scorePercent }
        val averageScore = if (finalScores.isEmpty()) null else finalScores.average().toInt()
        val record = YkiProgressRecord(
            moduleId = session.module.id,
            sessionId = session.id,
            completedTaskCount = summary.completedTaskCount,
            totalTaskCount = summary.totalTaskCount,
            scorePercent = averageScore,
            savedAtEpochMillis = null,
            durable = false
        )

        return YkiProgressSaveResult.Deferred(
            reason = "Progress saved for this practice.",
            record = record
        )
    }
}
