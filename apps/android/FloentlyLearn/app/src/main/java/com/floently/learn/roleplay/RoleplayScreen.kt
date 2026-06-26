package com.floently.learn.roleplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun RoleplayScreen(
    repository: RoleplayRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(RoleplayLevel.A1) }
    var dashboardState by remember { mutableStateOf<RoleplayDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<RoleplaySession?>(null) }

    val session = activeSession
    if (session != null) {
        RoleplaySessionScreen(
            session = session,
            repository = repository,
            onSessionChange = { activeSession = it },
            onExit = { activeSession = null }
        )
    } else {
        LaunchedEffect(repository, selectedLevel) {
            val dashboard = repository.dashboard(selectedLevel)
            dashboardState = dashboard
            statusMessage = dashboard.errorMessage
        }

        FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Roleplay",
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Native conversation practice with beginner-safe coaching, anti-repetition state, and OpenAI service boundary.",
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = "Level",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    RoleplayLevel.entries.forEach { level ->
                        FloentlyPrimaryButton(
                            title = if (level == selectedLevel) "Selected: ${level.name}" else level.name,
                            product = FloentlyProduct.Learn,
                            onClick = { selectedLevel = level }
                        )
                    }
                }

                statusMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    Text(
                        text = "Loading roleplays...",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (dashboard.scenarios.isEmpty()) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "No roleplays yet for ${dashboard.selectedLevel.name}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    dashboard.scenarios.forEach { scenario ->
                        FloentlyCard(product = FloentlyProduct.Learn) {
                            Text(
                                text = scenario.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = scenario.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Type: ${scenario.type} | Beginner-safe: ${scenario.beginnerSafe}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            FloentlyPrimaryButton(
                                title = if (scenario.locked) "View lock reason" else "Start roleplay",
                                product = FloentlyProduct.Learn,
                                onClick = {
                                    scope.launch {
                                        when (val result = repository.startSession(scenario.id)) {
                                            is RoleplaySessionResult.Ready -> {
                                                statusMessage = null
                                                activeSession = result.session
                                            }
                                            is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                            is RoleplaySessionResult.Error -> statusMessage = result.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                FloentlyPrimaryButton(
                    title = "Back to Learn",
                    product = FloentlyProduct.Learn,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun RoleplaySessionScreen(
    session: RoleplaySession,
    repository: RoleplayRepository,
    onSessionChange: (RoleplaySession) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var reply by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.learnerTurns) { mutableStateOf<String?>(null) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = session.scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = session.releaseGate,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Learner turns: ${session.learnerTurns}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Anti-repetition events: ${session.repeatedCueCount}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Scenario type: ${session.scenario.type}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            session.messages.forEach { message ->
                AnimatedVisibility(visible = true) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = when (message.speaker) {
                                RoleplaySpeaker.Learner -> "You"
                                RoleplaySpeaker.Coach -> "Coach"
                                RoleplaySpeaker.Partner -> "Partner"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        message.coachingNote?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Your Finnish reply") },
                    modifier = Modifier.fillMaxWidth()
                )
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FloentlyPrimaryButton(
                    title = "Send reply",
                    product = FloentlyProduct.Learn,
                    onClick = {
                        val cleanReply = reply.trim()
                        if (cleanReply.isBlank()) {
                            statusMessage = "Write a reply before sending."
                        } else {
                            scope.launch {
                                when (val result = repository.sendLearnerMessage(session, cleanReply)) {
                                    is RoleplaySessionResult.Ready -> {
                                        reply = ""
                                        statusMessage = null
                                        onSessionChange(result.session)
                                    }
                                    is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                    is RoleplaySessionResult.Error -> statusMessage = result.message
                                }
                            }
                        }
                    }
                )
            }

            FloentlyPrimaryButton(
                title = "Exit roleplay",
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}
