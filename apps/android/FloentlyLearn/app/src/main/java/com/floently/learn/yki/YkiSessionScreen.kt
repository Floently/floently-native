package com.floently.learn.yki

import com.floently.shared.design.FloentlyPalette
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun YkiSessionScreen(
    initialSession: YkiSession,
    repository: YkiRepository,
    copy: LearnCopy,
    mode: YkiScreenMode = YkiScreenMode.Practice,
    onExit: () -> Unit,
    evaluator: YkiEvaluator = PreviewYkiEvaluator(),
    progressStore: YkiProgressStore = PreviewYkiProgressStore()
) {
    val scope = rememberCoroutineScope()
    var session by remember(initialSession.id) { mutableStateOf(initialSession) }
    val task = session.currentTask
    var answer by remember(session.id, session.currentTaskIndex) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.currentTaskIndex) { mutableStateOf<String?>(null) }
    var progressSaveResult by remember(session.id) { mutableStateOf<YkiProgressSaveResult?>(null) }
    val progressTarget = if (session.tasks.isEmpty()) 0f else session.currentTaskIndex.toFloat() / session.tasks.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        label = "YKI session progress"
    )

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (mode == YkiScreenMode.MockExam) "YKI Mock Exam" else "YKI Practice",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = session.module.title,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            YkiSessionProgressCard(
                progress = animatedProgress,
                current = session.currentTaskIndex + 1,
                total = session.tasks.size,
                answered = session.answers.size,
                palette = palette
            )

            if (session.completed) {
                val summary = remember(session) { evaluator.evaluate(session) }

                LaunchedEffect(session.id, summary.evaluatedAnswers.size) {
                    progressSaveResult = progressStore.save(session, summary)
                }

                YkiCompletionCard(
                    answerCount = summary.evaluatedAnswers.size,
                    durable = summary.readyForDurableProgress,
                    palette = palette
                )

                progressSaveResult?.let { result ->
                    YkiProgressResultCard(
                        result = result,
                        palette = palette
                    )
                }

                summary.evaluatedAnswers.forEachIndexed { index, evaluation ->
                    YkiFeedbackCard(
                        index = index + 1,
                        evaluation = evaluation,
                        palette = palette
                    )
                }

                FloentlyPrimaryButton(
                    title = copy.ykiTitle,
                    product = FloentlyProduct.Learn,
                    onClick = onExit
                )
            } else if (task != null) {
                YkiTaskCard(
                    task = task,
                    answer = answer,
                    statusMessage = statusMessage,
                    palette = palette,
                    onAnswerChange = { answer = it },
                    onContinue = {
                        val cleanAnswer = answer.trim()
                        if (cleanAnswer.isBlank()) {
                            statusMessage = "Write an answer before continuing."
                        } else {
                            scope.launch {
                                session = repository.saveAnswer(session, task.id, cleanAnswer)
                                answer = ""
                                statusMessage = null
                            }
                        }
                    }
                )
            }

            FloentlyPrimaryButton(
                title = copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun YkiSessionProgressCard(
    progress: Float,
    current: Int,
    total: Int,
    answered: Int,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = palette.primary,
                trackColor = palette.border.copy(alpha = 0.45f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                YkiSessionMetricBox("Task", "$current/$total", palette.primary, palette, Modifier.weight(1f))
                YkiSessionMetricBox("Answered", answered.toString(), palette.accent, palette, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun YkiSessionMetricBox(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun YkiTaskCard(
    task: YkiTask,
    answer: String,
    statusMessage: String?,
    palette: FloentlyPalette,
    onAnswerChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(task.skill.skillColor(palette))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.skill.displayName()} • ${task.type.displayName()}",
                    color = task.skill.skillColor(palette),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = task.title,
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Surface(
                color = palette.cardMuted,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.prompt,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(14.dp)
                )
            }

            if (task.choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Options", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    task.choices.forEach { choice ->
                        Surface(
                            color = palette.cardMuted,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = choice,
                                color = palette.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = { Text("Your answer") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Tip: write the best answer you can. A short answer is enough when the task asks for one.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = palette.primary,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onContinue)
            ) {
                Text(
                    text = "Save and continue",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun YkiCompletionCard(
    answerCount: Int,
    durable: Boolean,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Practice complete",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "You completed $answerCount answers. Review feedback below and repeat the module when you want more practice.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (durable) "Progress is ready to sync." else "Progress was saved for this practice run.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun YkiProgressResultCard(
    result: YkiProgressSaveResult,
    palette: FloentlyPalette
) {
    val title = when (result) {
        is YkiProgressSaveResult.Saved -> "Progress saved"
        is YkiProgressSaveResult.Deferred -> "Progress recorded"
        is YkiProgressSaveResult.Failed -> "Progress was not saved"
    }
    val body = when (result) {
        is YkiProgressSaveResult.Saved -> "Tulos: ${result.record.scorePercent?.toString() ?: "ei vielä lopullinen"}"
        is YkiProgressSaveResult.Deferred -> result.reason
        is YkiProgressSaveResult.Failed -> result.message
    }
    val score = when (result) {
        is YkiProgressSaveResult.Saved -> result.record.scorePercent
        is YkiProgressSaveResult.Deferred -> result.record.scorePercent
        is YkiProgressSaveResult.Failed -> null
    }

    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            if (score != null) {
                Text("Tulos: $score%", color = palette.accent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun YkiFeedbackCard(
    index: Int,
    evaluation: YkiAnswerEvaluation,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Palaute $index",
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Tila: ${evaluation.status}",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Tulos: ${evaluation.scorePercent?.toString() ?: "ei vielä lopullinen"}",
                color = palette.accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = evaluation.feedback,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun YkiSkill.skillColor(palette: FloentlyPalette): Color = when (this) {
    YkiSkill.Reading -> palette.primary
    YkiSkill.Writing -> palette.accent
    YkiSkill.Listening -> palette.warning
    YkiSkill.Speaking -> Color(0xFF9D7CFF)
    YkiSkill.Vocabulary -> Color(0xFF3EC5A8)
    YkiSkill.Grammar -> Color(0xFFE8B65E)
}


private fun YkiSkill.displayName(): String = when (this) {
    YkiSkill.Reading -> "Lukeminen"
    YkiSkill.Writing -> "Kirjoittaminen"
    YkiSkill.Listening -> "Kuuntelu"
    YkiSkill.Speaking -> "Puhuminen"
    YkiSkill.Vocabulary -> "Sanasto"
    YkiSkill.Grammar -> "Kielioppi"
}

private fun YkiTaskType.displayName(): String = when (this) {
    YkiTaskType.MultipleChoice -> "Monivalinta"
    YkiTaskType.ShortAnswer -> "Lyhyt vastaus"
    YkiTaskType.WritingPrompt -> "Kirjoittaminen"
    YkiTaskType.ListeningPrompt -> "Kuuntelu"
    YkiTaskType.SpeakingPrompt -> "Puhuminen"
    YkiTaskType.Cloze -> "Täydennä aukko"
}
