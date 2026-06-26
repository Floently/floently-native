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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = session.module.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Guarded YKI task flow. Service scoring, speech features, progress persistence, and sync are still protected by the release contract.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Progress: ${session.currentTaskIndex}/${session.tasks.size}",
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
                        text = "Session complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your answers are held only in the current native session. Durable progress and scoring are not enabled until service parity is wired.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Evaluated: ${summary.evaluatedAnswers.size} answer(s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Progress-ready: ${summary.readyForDurableProgress}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Persisted: ${summary.persisted}",
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
                                    text = "Score: ${result.record.scorePercent?.toString() ?: "Not final"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is YkiProgressSaveResult.Deferred -> {
                                Text(
                                    text = "Progress captured locally",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = result.reason,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Durable: ${result.record.durable}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Score: ${result.record.scorePercent?.toString() ?: "Not final"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            is YkiProgressSaveResult.Failed -> {
                                Text(
                                    text = "Progress capture failed",
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

                summary.evaluatedAnswers.forEach { evaluation ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "Evaluation: ${evaluation.taskId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${evaluation.status}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Score: ${evaluation.scorePercent?.toString() ?: "Not final"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Release gate: ${evaluation.releaseGate}",
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
                            text = "Skill: ${task.skill} | Type: ${task.type}",
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
                        statusMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FloentlyPrimaryButton(
                            title = "Save answer",
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
                title = "Exit session",
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}
