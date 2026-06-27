package com.floently.learn.yki

import androidx.compose.animation.AnimatedVisibility
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
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun YkiSessionScreen(
    initialSession: YkiSession,
    repository: YkiRepository,
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
                text = session.module.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Answer one task at a time. Write clearly, then continue to the next exercise.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Task ${session.currentTaskIndex + 1} of ${session.tasks.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Answered: ${session.answers.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (session.completed) {
                val summary = remember(session) { evaluator.evaluate(session) }

                LaunchedEffect(session.id, summary.evaluatedAnswers.size) {
                    progressSaveResult = progressStore.save(session, summary)
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = "Practice complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You completed ${summary.evaluatedAnswers.size} answer(s). Review the feedback below and repeat this module when you want more practice.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (summary.readyForDurableProgress) "Progress is ready to sync." else "Progress is captured for this practice session.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                AnimatedVisibility(visible = progressSaveResult != null) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        when (val result = progressSaveResult) {
                            is YkiProgressSaveResult.Saved -> {
                                Text(
                                    text = "Progress saved",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Score: ${result.record.scorePercent?.toString() ?: "Not final yet"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is YkiProgressSaveResult.Deferred -> {
                                Text(
                                    text = "Progress captured",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = result.reason,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Score: ${result.record.scorePercent?.toString() ?: "Not final yet"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is YkiProgressSaveResult.Failed -> {
                                Text(
                                    text = "Progress was not saved",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            null -> Unit
                        }
                    }
                }

                summary.evaluatedAnswers.forEachIndexed { index, evaluation ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "Feedback ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${evaluation.status}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Score: ${evaluation.scorePercent?.toString() ?: "Not final yet"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = evaluation.feedback,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                FloentlyPrimaryButton(
                    title = "Back to YKI modules",
                    product = FloentlyProduct.Learn,
                    onClick = onExit
                )
            } else if (task != null) {
                AnimatedVisibility(visible = true) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${task.skill.displayName()} • ${task.type.displayName()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = task.prompt,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (task.choices.isNotEmpty()) {
                            Text(
                                text = "Choices: ${task.choices.joinToString()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Your answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Tip: write the best answer you can. Short answers are okay when the task asks for them.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        statusMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FloentlyPrimaryButton(
                            title = "Save and continue",
                            product = FloentlyProduct.Learn,
                            onClick = {
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
                }
            }

            FloentlyPrimaryButton(
                title = "Exit practice",
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

private fun YkiSkill.displayName(): String = when (this) {
    YkiSkill.Reading -> "Reading"
    YkiSkill.Writing -> "Writing"
    YkiSkill.Listening -> "Listening"
    YkiSkill.Speaking -> "Speaking"
    YkiSkill.Vocabulary -> "Vocabulary"
    YkiSkill.Grammar -> "Grammar"
}

private fun YkiTaskType.displayName(): String = when (this) {
    YkiTaskType.MultipleChoice -> "Multiple choice"
    YkiTaskType.ShortAnswer -> "Short answer"
    YkiTaskType.WritingPrompt -> "Writing"
    YkiTaskType.ListeningPrompt -> "Listening"
    YkiTaskType.SpeakingPrompt -> "Speaking"
    YkiTaskType.Cloze -> "Fill the gap"
}
