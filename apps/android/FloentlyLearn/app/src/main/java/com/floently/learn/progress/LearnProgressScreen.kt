package com.floently.learn.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnProgressScreen(
    repository: LearnProgressRepository,
    onBack: () -> Unit
) {
    var dashboardState by remember { mutableStateOf<LearnProgressDashboardState?>(null) }

    LaunchedEffect(repository) {
        dashboardState = repository.dashboard()
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Progress",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See your Finnish practice progress, streak, and recent activity.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading progress...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                dashboard.errorMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = "Progress note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Your learning snapshot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Completed practice units: ${dashboard.totalCompletedUnits}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Current streak: ${dashboard.activeStreakDays} day(s)", style = MaterialTheme.typography.bodyMedium)
                    Text(text = dashboard.syncBoundary.friendlyStatus(), style = MaterialTheme.typography.bodySmall)
                    if (dashboard.syncBoundary.pendingEvents > 0) {
                        Text(text = "Waiting to sync: ${dashboard.syncBoundary.pendingEvents} item(s)", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(text = dashboard.syncBoundary.lastSyncText, style = MaterialTheme.typography.bodySmall)
                }

                if (dashboard.summaries.isEmpty()) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = "No progress yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Start a YKI task, roleplay, professional Finnish session, or card review to see progress here.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    dashboard.summaries.forEach { summary ->
                        val animatedProgress by animateFloatAsState(
                            targetValue = (summary.completionPercent.toFloat() / 100f).coerceIn(0f, 1f),
                            label = "${summary.area} progress"
                        )
                        FloentlyCard(product = FloentlyProduct.Learn) {
                            Text(text = summary.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                            Text(text = "${summary.completionPercent}% complete", style = MaterialTheme.typography.bodySmall)
                            Text(text = "${summary.completedUnits} of ${summary.totalUnits} units done", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Streak: ${summary.streakDays} day(s)", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Last activity: ${summary.lastActivity}", style = MaterialTheme.typography.bodySmall)
                            Text(text = summary.syncStatus.friendlyText(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (dashboard.timeline.isEmpty()) {
                        Text(text = "Your recent practice will appear here after you complete Learn activities.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        dashboard.timeline.forEach { item ->
                            Text(text = item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = item.detail, style = MaterialTheme.typography.bodySmall)
                            Text(text = item.whenText, style = MaterialTheme.typography.bodySmall)
                            Text(text = if (item.durable) "Saved to your progress" else "Captured for this session", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Learn", product = FloentlyProduct.Learn, onClick = onBack)
        }
    }
}

private fun LearnProgressSyncBoundary.friendlyStatus(): String = when (status) {
    LearnProgressSyncStatus.LocalPreview -> "Progress is shown for this device."
    LearnProgressSyncStatus.ReadyToSync -> "Progress is ready to sync."
    LearnProgressSyncStatus.Synced -> "Progress is up to date."
    LearnProgressSyncStatus.ServicePending -> "Progress sync is being prepared."
}

private fun LearnProgressSyncStatus.friendlyText(): String = when (this) {
    LearnProgressSyncStatus.LocalPreview -> "Saved for this practice view"
    LearnProgressSyncStatus.ReadyToSync -> "Ready to sync"
    LearnProgressSyncStatus.Synced -> "Synced"
    LearnProgressSyncStatus.ServicePending -> "Sync pending"
}
