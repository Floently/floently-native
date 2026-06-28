package com.floently.learn.professional

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
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun ProfessionalFinnishScreen(
    repository: ProfessionalFinnishRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDomain by remember { mutableStateOf(ProfessionalFinnishDomain.Healthcare) }
    var dashboardState by remember { mutableStateOf<ProfessionalFinnishDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<ProfessionalFinnishSession?>(null) }

    val session = activeSession
    if (session != null) {
        ProfessionalFinnishSessionScreen(
            initialSession = session,
            repository = repository,
            copy = copy,
            onExit = { activeSession = null }
        )
    } else {
        LaunchedEffect(repository, selectedDomain) {
            val dashboard = repository.dashboard(selectedDomain)
            dashboardState = dashboard
            statusMessage = dashboard.errorMessage
        }

        FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = copy.professionalTitle,
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.professionalSubtitle,
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = copy.professionalMessage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = copy.professionalSubtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ProfessionalFinnishDomain.entries.forEach { domain ->
                        FloentlyPrimaryButton(
                            title = if (domain == selectedDomain) "${domain.displayName()} selected" else domain.displayName(),
                            product = FloentlyProduct.Learn,
                            onClick = { selectedDomain = domain }
                        )
                    }
                }

                statusMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    Text(
                        text = "Loading professional Finnish...",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (dashboard.modules.isEmpty()) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "No modules yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try another work situation or come back when new practice is available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    dashboard.modules.forEach { module ->
                        val progress = dashboard.progress.firstOrNull { it.moduleId == module.id }
                        FloentlyCard(product = FloentlyProduct.Learn) {
                            Text(
                                text = module.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = module.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Area: ${module.domain.displayName()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Time: about ${module.estimatedMinutes} minutes",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Completed: ${progress?.completedScenarios ?: 0} of ${progress?.totalScenarios ?: 0}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            progress?.lastPracticeSummary?.let { summary ->
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            FloentlyPrimaryButton(
                                title = if (module.locked) "See why locked" else copy.professionalAction,
                                product = FloentlyProduct.Learn,
                                onClick = {
                                    scope.launch {
                                        when (val result = repository.startSession(module.id)) {
                                            is ProfessionalFinnishSessionResult.Ready -> {
                                                statusMessage = null
                                                activeSession = result.session
                                            }
                                            is ProfessionalFinnishSessionResult.Blocked -> statusMessage = result.reason
                                            is ProfessionalFinnishSessionResult.Error -> statusMessage = result.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                FloentlyPrimaryButton(
                    title = copy.backToLearn,
                    product = FloentlyProduct.Learn,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun ProfessionalFinnishSessionScreen(
    initialSession: ProfessionalFinnishSession,
    repository: ProfessionalFinnishRepository,
    copy: LearnCopy,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var session by remember(initialSession.id) { mutableStateOf(initialSession) }
    val scenario = session.currentScenario
    var response by remember(session.id, session.currentScenarioIndex) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.currentScenarioIndex) { mutableStateOf<String?>(null) }
    val progressTarget = if (session.scenarios.isEmpty()) 0f else session.currentScenarioIndex.toFloat() / session.scenarios.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        label = "Professional Finnish progress"
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
                text = copy.professionalSubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Scenario ${session.currentScenarioIndex + 1} of ${session.scenarios.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Responses: ${session.responses.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (session.completed) {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = "Practice complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You completed ${session.responses.size} professional Finnish response(s). Repeat this practice when you want more confidence.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Next step: try another work situation or practise roleplay.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (scenario != null) {
                AnimatedVisibility(visible = true) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scenario.type.displayName(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = scenario.context,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = scenario.prompt,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        scenario.modelPhrases.forEach { phrase ->
                            FloentlyCard(product = FloentlyProduct.Learn) {
                                Text(
                                    text = phrase.finnish,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = phrase.english,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = phrase.usageNote,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        OutlinedTextField(
                            value = response,
                            onValueChange = { response = it },
                            label = { Text("Your Finnish response") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Tip: use one model phrase and adapt it to the situation.",
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
                                val cleanResponse = response.trim()
                                if (cleanResponse.isBlank()) {
                                    statusMessage = "Write a response before continuing."
                                } else {
                                    scope.launch {
                                        session = repository.saveResponse(session, scenario.id, cleanResponse)
                                        response = ""
                                        statusMessage = null
                                    }
                                }
                            }
                        )
                    }
                }
            }

            FloentlyPrimaryButton(
                title = if (session.completed) copy.professionalTitle else copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

private fun ProfessionalFinnishDomain.displayName(): String = when (this) {
    ProfessionalFinnishDomain.Healthcare -> "Healthcare"
    ProfessionalFinnishDomain.Office -> "Office"
    ProfessionalFinnishDomain.CustomerService -> "Customer service"
    ProfessionalFinnishDomain.JobSearch -> "Job search"
    ProfessionalFinnishDomain.Safety -> "Safety"
    ProfessionalFinnishDomain.SmallTalk -> "Small talk"
}

private fun ProfessionalFinnishScenarioType.displayName(): String = when (this) {
    ProfessionalFinnishScenarioType.PhrasePractice -> "Phrase practice"
    ProfessionalFinnishScenarioType.DialoguePractice -> "Dialogue practice"
    ProfessionalFinnishScenarioType.EmailWriting -> "Email writing"
    ProfessionalFinnishScenarioType.MeetingResponse -> "Meeting response"
    ProfessionalFinnishScenarioType.PhoneCall -> "Phone call"
}
