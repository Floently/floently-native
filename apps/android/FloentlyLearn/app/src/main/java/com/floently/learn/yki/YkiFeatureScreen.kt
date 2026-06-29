package com.floently.learn.yki

import com.floently.learn.app.LearnSmartHelperCard
import com.floently.learn.app.learnYkiSmartHelperActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
fun YkiFeatureScreen(
    repository: YkiRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(YkiLevel.Keskitaso) }
    var dashboardState by remember { mutableStateOf<YkiDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<YkiSession?>(null) }

    val session = activeSession
    if (session != null) {
        YkiSessionScreen(
            initialSession = session,
            repository = repository,
            copy = copy,
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
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = copy.ykiTitle,
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.ykiSubtitle,
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

            LearnSmartHelperCard(
                title = "YKI route guidance",
                body = "Guided, step-by-step YKI preparation stays visible without blocking the task flow.",
                actions = learnYkiSmartHelperActions()
            )

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = copy.ykiMessage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = copy.ykiSubtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    YkiLevel.entries.forEach { level ->
                        FloentlyPrimaryButton(
                            title = if (level == selectedLevel) "${level.displayName()} selected" else level.displayName(),
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
                        text = "Loading YKI practice...",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (dashboard.modules.isEmpty()) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = "No YKI modules yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try another level or come back when new practice is available.",
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
                                text = "Skills: ${module.skills.joinToString { it.displayName() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Time: about ${module.estimatedMinutes} minutes",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Completed: ${progress?.completedTasks ?: 0} of ${progress?.totalTasks ?: 0}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            progress?.lastScorePercent?.let { score ->
                                Text(
                                    text = "Last score: $score%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            FloentlyPrimaryButton(
                                title = if (module.locked) "See why locked" else copy.ykiAction,
                                product = FloentlyProduct.Learn,
                                onClick = {
                                    scope.launch {
                                        when (val result = repository.startSession(module.id)) {
                                            is YkiSessionResult.Ready -> {
                                                statusMessage = null
                                                activeSession = result.session
                                            }
                                            is YkiSessionResult.Blocked -> statusMessage = result.reason
                                            is YkiSessionResult.Error -> statusMessage = result.message
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

private fun YkiLevel.displayName(): String = when (this) {
    YkiLevel.Perustaso -> "Perustaso"
    YkiLevel.Keskitaso -> "Keskitaso"
    YkiLevel.YlinTaso -> "Ylin taso"
}

private fun YkiSkill.displayName(): String = when (this) {
    YkiSkill.Reading -> "Reading"
    YkiSkill.Writing -> "Writing"
    YkiSkill.Listening -> "Listening"
    YkiSkill.Speaking -> "Speaking"
    YkiSkill.Vocabulary -> "Vocabulary"
    YkiSkill.Grammar -> "Grammar"
}
