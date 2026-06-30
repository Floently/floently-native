package com.floently.learn.yki

import com.floently.shared.design.FloentlyPalette
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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

                YkiExamHeader(palette = palette)
                YkiLevelStrip(
                    selectedLevel = selectedLevel,
                    palette = palette,
                    onSelect = { selectedLevel = it }
                )

                statusMessage?.let { message ->
                    YkiStatusCard(
                        title = "Huomio",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    YkiStatusCard(
                        title = "Ladataan YKI-harjoituksia…",
                        body = "Haetaan ${selectedLevel.displayName()} -tason moduuleja.",
                        palette = palette
                    )
                } else if (dashboard.modules.isEmpty()) {
                    YkiStatusCard(
                        title = "Ei vielä YKI-moduuleja",
                        body = "Valitse toinen taso tai palaa myöhemmin, kun uusia harjoituksia on lisätty.",
                        palette = palette
                    )
                } else {
                    dashboard.modules.forEach { module ->
                        val progress = dashboard.progress.firstOrNull { it.moduleId == module.id }
                        YkiModuleCard(
                            module = module,
                            progress = progress,
                            palette = palette,
                            actionLabel = if (module.locked) "Katso lukituksen syy" else copy.ykiAction,
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
private fun YkiExamHeader(
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "YKI",
                color = Color(0xFF9D7CFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Text(
                text = "YKI-valmistautuminen",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Harjoittele virallisen kokeen osia: lukeminen, kirjoittaminen, kuuntelu, puhuminen ja sanasto.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YkiTinyPill("Taso", "Keskitaso", palette.primary)
                YkiTinyPill("Muoto", "Koeharjoitus", Color(0xFF9D7CFF))
            }
        }
    }
}

@Composable
private fun YkiTinyPill(
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
private fun YkiLevelStrip(
    selectedLevel: YkiLevel,
    palette: FloentlyPalette,
    onSelect: (YkiLevel) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        YkiLevel.entries.forEach { level ->
            val active = level == selectedLevel
            Surface(
                color = if (active) palette.primary.copy(alpha = 0.18f) else palette.cardMuted,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (active) palette.primary else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(level) }
            ) {
                Text(
                    text = level.displayName(),
                    color = if (active) palette.primary else palette.muted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun YkiModuleCard(
    module: YkiModule,
    progress: YkiModuleProgress?,
    palette: FloentlyPalette,
    actionLabel: String,
    onClick: () -> Unit
) {
    val skillAccent = module.skills.firstOrNull()?.skillColor(palette) ?: palette.primary

    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                        .background(if (module.locked) palette.soft else skillAccent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (module.locked) "LUKITTU" else module.level.displayName().uppercase(),
                    color = if (module.locked) palette.soft else skillAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = module.title,
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = module.description,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                YkiMetricBox("Tehtävät", "${progress?.completedTasks ?: 0}/${progress?.totalTasks ?: 0}", skillAccent, palette, Modifier.weight(1f))
                YkiMetricBox("Aika", "${module.estimatedMinutes} min", palette.warning, palette, Modifier.weight(1f))
            }

            Text(
                text = "Taidot: ${module.skills.joinToString { it.displayName() }}",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            progress?.lastScorePercent?.let { score ->
                Text(
                    text = "Viimeisin tulos: $score%",
                    color = palette.accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = if (module.locked) palette.card else palette.primary,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (module.locked) palette.border else palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionLabel,
                    color = if (module.locked) palette.muted else Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun YkiMetricBox(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.card,
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
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
private fun YkiStatusCard(
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

private fun YkiSkill.skillColor(palette: FloentlyPalette): Color = when (this) {
    YkiSkill.Reading -> palette.primary
    YkiSkill.Writing -> palette.accent
    YkiSkill.Listening -> palette.warning
    YkiSkill.Speaking -> Color(0xFF9D7CFF)
    YkiSkill.Vocabulary -> Color(0xFF3EC5A8)
    YkiSkill.Grammar -> Color(0xFFE8B65E)
}


private fun YkiLevel.displayName(): String = when (this) {
    YkiLevel.Perustaso -> "Perustaso"
    YkiLevel.Keskitaso -> "Keskitaso"
    YkiLevel.YlinTaso -> "Ylin taso"
}

private fun YkiSkill.displayName(): String = when (this) {
    YkiSkill.Reading -> "Lukeminen"
    YkiSkill.Writing -> "Kirjoittaminen"
    YkiSkill.Listening -> "Kuuntelu"
    YkiSkill.Speaking -> "Puhuminen"
    YkiSkill.Vocabulary -> "Sanasto"
    YkiSkill.Grammar -> "Kielioppi"
}
