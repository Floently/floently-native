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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progress",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Native learning progress overview with clear parity gates before durable sync is released.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading progress...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Completed units: ${dashboard.totalCompletedUnits}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Active streak: ${dashboard.activeStreakDays} day(s)", style = MaterialTheme.typography.bodyMedium)
                }

                dashboard.summaries.forEach { summary ->
                    val animatedProgress by animateFloatAsState(
                        targetValue = (summary.completionPercent.toFloat() / 100f).coerceIn(0f, 1f),
                        label = "${summary.area} progress"
                    )
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = summary.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                        Text(text = "${summary.completionPercent}% complete", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${summary.completedUnits}/${summary.totalUnits} units", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Last activity: ${summary.lastActivity}", style = MaterialTheme.typography.bodySmall)
                        Text(text = summary.releaseGate, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Learn", product = FloentlyProduct.Learn, onClick = onBack)
        }
    }
}
