package com.floently.learn.yki

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
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun YkiFeatureScreen(
    repository: YkiRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(YkiLevel.Keskitaso) }
    var dashboardState by remember { mutableStateOf<YkiDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, selectedLevel) {
        val dashboard = repository.dashboard(selectedLevel)
        dashboardState = dashboard
        statusMessage = dashboard.errorMessage
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "YKI practice",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Native YKI foundation. Content, scoring, audio, progress, and release checks stay guarded until parity is complete.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Level",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                YkiLevel.entries.forEach { level ->
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
                    text = "Loading YKI modules...",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (dashboard.modules.isEmpty()) {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = "No modules yet for ${dashboard.selectedLevel.name}.",
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
                            text = "Skills: ${module.skills.joinToString()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Estimated time: ${module.estimatedMinutes} min",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Progress: ${progress?.completedTasks ?: 0}/${progress?.totalTasks ?: 0}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        FloentlyPrimaryButton(
                            title = if (module.locked) "View lock reason" else "Start module",
                            product = FloentlyProduct.Learn,
                            onClick = {
                                scope.launch {
                                    statusMessage = when (val result = repository.startSession(module.id)) {
                                        is YkiSessionResult.Ready -> "Session ready: ${result.session.currentTask?.title ?: module.title}"
                                        is YkiSessionResult.Blocked -> result.reason
                                        is YkiSessionResult.Error -> result.message
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
