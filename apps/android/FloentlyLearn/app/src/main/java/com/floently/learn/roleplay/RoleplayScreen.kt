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
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun RoleplayScreen(
    repository: RoleplayRepository,
    copy: LearnCopy,
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
            copy = copy,
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = copy.roleplayTitle,
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.roleplaySubtitle,
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = copy.roleplayMessage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = copy.roleplaySubtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    RoleplayLevel.entries.forEach { level ->
                        FloentlyPrimaryButton(
                            title = if (level == selectedLevel) "${level.name} selected" else "Practice ${level.name}",
                            product = FloentlyProduct.Learn,
                            onClick = { selectedLevel = level }
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
                        text = "Loading conversations...",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (dashboard.scenarios.isEmpty()) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "No conversations yet for ${dashboard.selectedLevel.name}.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try another level or come back after new practice content is available.",
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
                                text = scenario.helperText(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            FloentlyPrimaryButton(
                                title = if (scenario.locked) "See why locked" else copy.roleplayAction,
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
                    title = copy.backToLearn,
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
    copy: LearnCopy,
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = session.scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.roleplaySubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Conversation progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your turns: ${session.learnerTurns}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Coaching variety checks: ${session.repeatedCueCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            session.messages.forEach { message ->
                AnimatedVisibility(visible = true) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = message.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        message.coachingNote?.let { note ->
                            Text(
                                text = "Coach note: $note",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Your reply",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Write in Finnish") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tip: one clear sentence is enough. Example: Kiitos, se sopii hyvin.",
                    style = MaterialTheme.typography.bodySmall
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
                            statusMessage = "Write a short Finnish reply before sending."
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
                title = copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

private fun RoleplayScenario.helperText(): String {
    val safety = if (beginnerSafe) "Beginner-friendly" else "More advanced"
    return "$safety conversation for ${type.name.lowercase()} practice."
}

private fun RoleplayMessage.displayName(): String = when (speaker) {
    RoleplaySpeaker.Learner -> "You"
    RoleplaySpeaker.Coach -> "Coach"
    RoleplaySpeaker.Partner -> "Partner"
}
