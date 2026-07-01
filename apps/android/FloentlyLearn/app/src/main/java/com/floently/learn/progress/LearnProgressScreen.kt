package com.floently.learn.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Progress",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your learning streak, practice history, sync status, and next useful action in one place.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            ProgressRouteHeader(palette = palette)

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                ProgressStatusCard(
                    title = "Loading progress…",
                    body = "Loading the Learn practice summary.",
                    palette = palette
                )
            } else {
                dashboard.errorMessage?.let { message ->
                    ProgressStatusCard(
                        title = "Notice",
                        body = message,
                        palette = palette
                    )
                }

                ProgressSnapshotCard(
                    completedUnits = dashboard.totalCompletedUnits,
                    streakDays = dashboard.activeStreakDays,
                    syncBoundary = dashboard.syncBoundary,
                    palette = palette
                )

                if (dashboard.summaries.isEmpty()) {
                    ProgressStatusCard(
                        title = "No progress yet",
                        body = "Start a YKI task, roleplay, workplace Finnish session, or card review and progress will appear here.",
                        palette = palette
                    )
                } else {
                    dashboard.summaries.forEach { summary ->
                        val animatedProgress by animateFloatAsState(
                            targetValue = (summary.completionPercent.toFloat() / 100f).coerceIn(0f, 1f),
                            label = "${summary.area} progress"
                        )
                        ProgressSummaryCard(
                            summary = summary,
                            progress = animatedProgress,
                            palette = palette
                        )
                    }
                }

                ProgressSyncBoundaryCard(
                    boundary = dashboard.syncBoundary,
                    palette = palette
                )

                ProgressTimelineCard(
                    items = dashboard.timeline,
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
private fun ProgressRouteHeader(
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PROGRESS",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Learning progress",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Use this view to choose the next useful pathway without opening every feature separately.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressTinyPill("View", "Summary", palette.primary)
                ProgressTinyPill("State", "Sync", palette.accent)
            }
        }
    }
}

@Composable
private fun ProgressTinyPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
    ) {
        Text(
            text = "$label · $value",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressSnapshotCard(
    completedUnits: Int,
    streakDays: Int,
    syncBoundary: LearnProgressSyncBoundary,
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Learning progress",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProgressMetricBox(
                    label = "Done",
                    value = completedUnits.toString(),
                    color = palette.primary,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                ProgressMetricBox(
                    label = "Streak",
                    value = streakDays.toString(),
                    color = palette.accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = syncBoundary.friendlyStatus(),
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )

            if (syncBoundary.pendingEvents > 0) {
                Text(
                    text = "Waiting for sync: ${syncBoundary.pendingEvents}",
                    color = palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressMetricBox(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ProgressSummaryCard(
    summary: LearnProgressSummary,
    progress: Float,
    palette: FloentlyPalette
) {
    val accent = summary.area.areaColor(palette)

    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = summary.area.displayName().uppercase(),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = summary.title.localizedProgressTitle(),
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = palette.border.copy(alpha = 0.45f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ProgressMetricBox(
                    label = "Complete",
                    value = "${summary.completionPercent}%",
                    color = accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                ProgressMetricBox(
                    label = "Units",
                    value = "${summary.completedUnits}/${summary.totalUnits}",
                    color = palette.warning,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                ProgressMetricBox(
                    label = "Putki",
                    value = summary.streakDays.toString(),
                    color = palette.accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Viimeisin: ${summary.lastActivity.localizedProgressText()}",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = summary.syncStatus.friendlyText(),
                color = accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary.releaseGate.localizedProgressText(),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
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
                text = "Synkronoinnin raja",
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
                text = "Viimeaikainen harjoittelu",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            if (items.isEmpty()) {
                Text(
                    text = "Viimeisimmät harjoitukset näkyvät täällä, kun teet Learn-tehtäviä.",
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
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
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
                    text = if (item.durable) "Tallennettu edistymiseen" else "Kirjattu tähän harjoituskertaan",
                    color = accent,
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

private fun LearnProgressArea.areaColor(palette: FloentlyPalette): Color = when (this) {
    LearnProgressArea.Yki -> Color(0xFF9D7CFF)
    LearnProgressArea.ProfessionalFinnish -> palette.warning
    LearnProgressArea.Roleplay -> palette.accent
    LearnProgressArea.Cards -> palette.primary
}

private fun LearnProgressArea.displayName(): String = when (this) {
    LearnProgressArea.Yki -> "YKI"
    LearnProgressArea.ProfessionalFinnish -> "Työpaikan suomi"
    LearnProgressArea.Roleplay -> "Roolipeli"
    LearnProgressArea.Cards -> "Kortit"
}

private fun LearnProgressSyncBoundary.friendlyStatus(): String = when (status) {
    LearnProgressSyncStatus.LocalPreview -> "Edistyminen näkyy tällä laitteella."
    LearnProgressSyncStatus.ReadyToSync -> "Edistyminen on valmis synkronoitavaksi."
    LearnProgressSyncStatus.Synced -> "Edistyminen on ajan tasalla."
    LearnProgressSyncStatus.ServicePending -> "Edistymisen synkronointia valmistellaan."
}

private fun LearnProgressSyncStatus.friendlyText(): String = when (this) {
    LearnProgressSyncStatus.LocalPreview -> "Tallennettu tähän harjoitusnäkymään"
    LearnProgressSyncStatus.ReadyToSync -> "Valmis synkronoitavaksi"
    LearnProgressSyncStatus.Synced -> "Synkronoitu"
    LearnProgressSyncStatus.ServicePending -> "Synkronointi odottaa"
}

private fun String.localizedProgressTitle(): String = when (this) {
    "YKI practice" -> "YKI-harjoittelu"
    "Professional Finnish" -> "Työpaikan suomi"
    "Roleplay" -> "Roolipeli"
    "Cards" -> "Kortit"
    else -> this
}

private fun String.localizedProgressText(): String = when (this) {
    "Native task flow verified" -> "Natiivi tehtäväpolku varmistettu"
    "Scenario flow verified" -> "Tilanneharjoituksen polku varmistettu"
    "Conversation flow verified" -> "Keskustelupolku varmistettu"
    "Deck flow verified" -> "Korttipakan polku varmistettu"
    "Real scoring and durable progress remain service-gated." -> "Todellinen arviointi ja pysyvä edistyminen odottavat palvelinrajapintaa."
    "Feedback and saved progress remain service-gated." -> "Palaute ja tallennettu edistyminen odottavat palvelinrajapintaa."
    "Dynamic generation and saved progress remain service-gated." -> "Dynaaminen generointi ja tallennettu edistyminen odottavat palvelinrajapintaa."
    "Review scheduling and saved progress remain service-gated." -> "Kertausaikataulu ja tallennettu edistyminen odottavat palvelinrajapintaa."
    "Server progress sync is not connected yet." -> "Palvelimen edistymissynkronointia ei ole vielä kytketty."
    "Durable progress needs authenticated server write/read, offline queue, and merge handling before release." -> "Pysyvä edistyminen vaatii kirjautuneen palvelintallennuksen, offline-jonon ja yhdistämisen ennen julkaisua."
    "YKI native session completed" -> "YKI:n natiivi harjoitus tehty"
    "Answer capture, evaluation summary, and progress boundary are available." -> "Vastausten tallennus, arviointiyhteenveto ja edistymisen raja ovat käytössä."
    "Roleplay native session started" -> "Roolipelin natiivi harjoitus aloitettu"
    "Conversation, coaching, and anti-repetition state are available." -> "Keskustelu, valmennus ja toistonesto ovat käytössä."
    "Cards native deck opened" -> "Natiivi korttipakka avattu"
    "Deck data and start-session boundary are available." -> "Pakkatiedot ja harjoituksen aloitusraja ovat käytössä."
    "Preview milestone" -> "Esikatselun välietappi"
    else -> this
}
