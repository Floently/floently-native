package com.floently.learn.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnProgressScreen(
    repository: LearnProgressRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    var dashboardState by remember { mutableStateOf<LearnProgressDashboardState?>(null) }

    LaunchedEffect(repository) {
        dashboardState = repository.dashboard()
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val dashboard = dashboardState

            ProgressHeaderCard(palette = palette)

            if (dashboard == null || dashboard.isLoading) {
                ProgressStatusCard(
                    title = "Loading progress",
                    body = "Loading saved Learn activity.",
                    palette = palette
                )
            } else {
                dashboard.errorMessage?.let { message ->
                    ProgressStatusCard(
                        title = "Progress notice",
                        body = message,
                        palette = palette
                    )
                }

                ProgressReadinessCard(
                    score = dashboard.readinessScore(),
                    label = dashboard.readinessLabel(),
                    body = "Your YKI, workplace communication, and vocabulary readiness are shown from saved progress data.",
                    palette = palette
                )

                ProgressPillarStack(
                    summaries = dashboard.summaries,
                    palette = palette
                )

                ProgressNextActionsCard(palette = palette)

                ProgressTimelineCard(
                    items = dashboard.timeline,
                    palette = palette
                )

                ProgressSyncBoundaryCard(
                    boundary = dashboard.syncBoundary,
                    palette = palette
                )
            }

            FloentlyPrimaryButton(
                title = "Back to Learn",
                product = FloentlyProduct.Learn,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun ProgressHeaderCard(
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "PROGRESS",
                color = palette.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Learning progress",
                color = palette.text,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "See your readiness, practice direction, and saved activity in one place.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProgressReadinessCard(
    score: Int,
    label: String,
    body: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "OVERALL READINESS",
                color = palette.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "$score% · $label",
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = body,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProgressPillarStack(
    summaries: List<LearnProgressSummary>,
    palette: FloentlyPalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        summaries.forEach { summary ->
            val animatedProgress by animateFloatAsState(
                targetValue = summary.safeProgress(),
                label = "${summary.title} progress"
            )
            ProgressPillarCard(
                summary = summary,
                progress = animatedProgress,
                palette = palette
            )
        }
    }
}

@Composable
private fun ProgressPillarCard(
    summary: LearnProgressSummary,
    progress: Float,
    palette: FloentlyPalette
) {
    val accent = summary.area.areaColor(palette)
    val percent = (progress * 100f).toInt().coerceIn(0, 100)

    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = accent,
                    shape = CircleShape,
                    modifier = Modifier.size(12.dp)
                ) {}
                Text(
                    text = summary.title.localizedProgressTitle(),
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$percent%",
                    color = palette.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = palette.primary,
                trackColor = palette.card
            )

            Text(
                text = summary.lastActivity.localizedProgressText(),
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressMiniMetric(
                    label = "Units",
                    value = if (summary.totalUnits > 0) "${summary.completedUnits}/${summary.totalUnits}" else "Not saved",
                    color = accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                ProgressMiniMetric(
                    label = "Streak",
                    value = "${summary.streakDays}",
                    color = palette.warning,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = summary.syncStatus.friendlyText(),
                color = accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressMiniMetric(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Text(
                text = value,
                color = palette.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressNextActionsCard(
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
            Text(
                text = "Next useful actions",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            ProgressActionRow(
                title = "Strengthen vocabulary and roleplay",
                detail = "Review useful phrases, then practise them in a spoken session.",
                meta = "Vocabulary + roleplay",
                accent = palette.primary,
                palette = palette
            )
            ProgressActionRow(
                title = "Practice workplace scenarios",
                detail = "Build confidence for professional Finnish situations.",
                meta = "Workplace scenarios",
                accent = palette.accent,
                palette = palette
            )
            ProgressActionRow(
                title = "Check YKI readiness",
                detail = "Use YKI Practice or the full mock exam to test exam readiness.",
                meta = "YKI preparation",
                accent = palette.warning,
                palette = palette
            )
        }
    }
}

@Composable
private fun ProgressActionRow(
    title: String,
    detail: String,
    meta: String,
    accent: Color,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = accent,
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(11.dp)
            ) {}
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = detail,
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = meta,
                    color = accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressTimelineCard(
    items: List<LearnProgressTimelineItem>,
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
            Text(
                text = "Recent practice",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            if (items.isEmpty()) {
                Text(
                    text = "Completed sessions will appear here after the progress service returns saved events.",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                items.forEach { item ->
                    ProgressTimelineRow(item = item, palette = palette)
                }
            }
        }
    }
}

@Composable
private fun ProgressTimelineRow(
    item: LearnProgressTimelineItem,
    palette: FloentlyPalette
) {
    val accent = item.area.areaColor(palette)

    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = accent,
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
            ) {}
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.localizedProgressText(),
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.detail.localizedProgressText(),
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = item.whenText.localizedProgressText(),
                    color = palette.soft,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (item.durable) "Saved progress" else "Session-local event",
                    color = accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressSyncBoundaryCard(
    boundary: LearnProgressSyncBoundary,
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
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "Progress data",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = boundary.lastSyncText.localizedProgressText(),
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = boundary.releaseGate.localizedProgressText(),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
            if (boundary.pendingEvents > 0) {
                Text(
                    text = "Pending events: ${boundary.pendingEvents}",
                    color = palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressStatusCard(
    title: String,
    body: String,
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
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun LearnProgressDashboardState.readinessScore(): Int {
    if (summaries.isEmpty()) return 0
    val measurable = summaries.filter { it.totalUnits > 0 }
    if (measurable.isEmpty()) return 0
    return measurable.map { it.completionPercent }.average().toInt().coerceIn(0, 100)
}

private fun LearnProgressDashboardState.readinessLabel(): String = when (readinessScore()) {
    in 75..100 -> "Work-ready momentum"
    in 55..74 -> "Building readiness"
    else -> "Early pathway stage"
}

private fun LearnProgressSummary.safeProgress(): Float {
    return if (totalUnits <= 0) 0f else (completionPercent.toFloat() / 100f).coerceIn(0f, 1f)
}

private fun LearnProgressArea.areaColor(palette: FloentlyPalette): Color = when (this) {
    LearnProgressArea.Yki -> Color(0xFF9D7CFF)
    LearnProgressArea.ProfessionalFinnish -> palette.warning
    LearnProgressArea.Roleplay -> palette.accent
    LearnProgressArea.Cards -> palette.primary
}

private fun LearnProgressSyncStatus.friendlyText(): String = when (this) {
    LearnProgressSyncStatus.LocalPreview -> "Stored only in this practice view"
    LearnProgressSyncStatus.ReadyToSync -> "Ready to sync"
    LearnProgressSyncStatus.Synced -> "Synced"
    LearnProgressSyncStatus.ServicePending -> "Waiting for saved service data"
}

private fun String.localizedProgressTitle(): String = when (this) {
    "YKI practice" -> "YKI readiness"
    "YKI readiness" -> "YKI readiness"
    "Professional Finnish" -> "Workplace communication"
    "Workplace communication" -> "Workplace communication"
    "Roleplay" -> "Conversation practice"
    "Cards" -> "Profession vocabulary"
    "Profession vocabulary" -> "Profession vocabulary"
    else -> this
}

private fun String.localizedProgressText(): String = when (this) {
    "No saved YKI progress has been returned yet." -> "No saved YKI progress has been returned yet."
    "No saved workplace communication progress has been returned yet." -> "No saved workplace communication progress has been returned yet."
    "No saved vocabulary progress has been returned yet." -> "No saved vocabulary progress has been returned yet."
    "This fallback does not count as completed YKI progress." -> "This fallback does not count as completed YKI progress."
    "This fallback does not count as completed workplace progress." -> "This fallback does not count as completed workplace progress."
    "This fallback does not count as completed vocabulary progress." -> "This fallback does not count as completed vocabulary progress."
    "Progress service has not returned saved learning activity yet." -> "Progress service has not returned saved learning activity yet."
    "Only verified service data is shown as completed progress." -> "Only verified service data is shown as completed progress."
    else -> this
}
