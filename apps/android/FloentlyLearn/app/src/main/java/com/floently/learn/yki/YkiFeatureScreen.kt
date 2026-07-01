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
import androidx.compose.foundation.layout.height
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

enum class YkiScreenMode {
    Practice,
    MockExam
}

@Composable
fun YkiFeatureScreen(
    repository: YkiRepository,
    copy: LearnCopy,
    mode: YkiScreenMode = YkiScreenMode.Practice,
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
            mode = mode,
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
                YkiTopBar(mode = mode, onBack = onBack, palette = palette)
                YkiExamHeader(mode = mode, palette = palette)
                YkiModeSummary(mode = mode, palette = palette)
                YkiLevelStrip(
                    selectedLevel = selectedLevel,
                    palette = palette,
                    onSelect = { selectedLevel = it }
                )

                statusMessage?.let { message ->
                    YkiStatusCard(
                        title = "Notice",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    YkiStatusCard(
                        title = if (mode == YkiScreenMode.MockExam) "Loading mock exam…" else "Loading YKI practice…",
                        body = "Haetaan ${selectedLevel.displayName()} -tason moduuleja.",
                        palette = palette
                    )
                } else if (dashboard.modules.isEmpty()) {
                    YkiStatusCard(
                        title = "No YKI modules yet",
                        body = "Choose another level or come back later when new modules have been added.",
                        palette = palette
                    )
                } else {
                    dashboard.modules.forEach { module ->
                        val progress = dashboard.progress.firstOrNull { it.moduleId == module.id }
                        YkiModuleCard(
                            module = module,
                            progress = progress,
                            palette = palette,
                            actionLabel = if (module.locked) "Locked" else if (mode == YkiScreenMode.MockExam) "Start mock section" else "Start practice",
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
                    title = "Back to Learn",
                    product = FloentlyProduct.Learn,
                    onClick = onBack
                )
            }
        }
    }
}


@Composable
private fun YkiTopBar(
    mode: YkiScreenMode,
    onBack: () -> Unit,
    palette: FloentlyPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(2.dp, Color(0xFF30456F)),
            modifier = Modifier
                .height(48.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = Color(0xFF9EB3EF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 23.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (mode == YkiScreenMode.MockExam) "YKI Mock Exam" else "YKI Practice",
            color = palette.text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun YkiExamHeader(
    mode: YkiScreenMode,
    palette: FloentlyPalette
) {
    val isMock = mode == YkiScreenMode.MockExam
    Surface(
        color = Color(0xFF101D38),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, Color(0xFF263A68)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text(
                text = if (isMock) "FULL YKI EXAM" else "YKI PRACTICE",
                color = Color(0xFF9D7CFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.5.sp
            )
            Text(
                text = if (isMock) "YKI Mock Exam" else "YKI Practice",
                color = palette.text,
                fontSize = 34.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (isMock)
                    "Full exam-style simulation with separate sections and final readiness review."
                else
                    "Guided practice for reading, writing, listening and speaking before the official exam.",
                color = palette.muted,
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YkiTinyPill("Level", "Intermediate", palette.primary)
                YkiTinyPill("Mode", if (isMock) "Mock exam" else "Practice", Color(0xFF9D7CFF))
            }
        }
    }
}

@Composable
private fun YkiModeSummary(
    mode: YkiScreenMode,
    palette: FloentlyPalette
) {
    val isMock = mode == YkiScreenMode.MockExam
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        YkiMetricBox(
            label = if (isMock) "Sections" else "Skills",
            value = if (isMock) "4" else "5",
            color = Color(0xFF9D7CFF),
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        YkiMetricBox(
            label = if (isMock) "Flow" else "Practice",
            value = if (isMock) "Exam" else "Guided",
            color = palette.accent,
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        YkiMetricBox(
            label = "Goal",
            value = "B1",
            color = palette.warning,
            palette = palette,
            modifier = Modifier.weight(1f)
        )
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
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
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
                    text = if (module.locked) "LOCKED" else module.level.displayName().uppercase(),
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
                YkiMetricBox("Tasks", "${progress?.completedTasks ?: 0}/${progress?.totalTasks ?: 0}", skillAccent, palette, Modifier.weight(1f))
                YkiMetricBox("Time", "${module.estimatedMinutes} min", palette.warning, palette, Modifier.weight(1f))
            }

            Text(
                text = "Skills: ${module.skills.joinToString { it.displayName() }}",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            progress?.lastScorePercent?.let { score ->
                Text(
                    text = "Latest score: $score%",
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
    YkiLevel.Perustaso -> "Basic"
    YkiLevel.Keskitaso -> "Intermediate"
    YkiLevel.YlinTaso -> "Advanced"
}

private fun YkiSkill.displayName(): String = when (this) {
    YkiSkill.Reading -> "Reading"
    YkiSkill.Writing -> "Writing"
    YkiSkill.Listening -> "Listening"
    YkiSkill.Speaking -> "Speaking"
    YkiSkill.Vocabulary -> "Vocabulary"
    YkiSkill.Grammar -> "Grammar"
}
