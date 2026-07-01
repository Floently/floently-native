package com.floently.learn.professional

import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
fun ProfessionalFinnishScreen(
    repository: ProfessionalFinnishRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDomain by remember { mutableStateOf(ProfessionalFinnishDomain.Healthcare) }
    var dashboardState by remember { mutableStateOf<ProfessionalFinnishDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<ProfessionalFinnishSession?>(null) }

    val session = activeSession
    if (session != null) {
        ProfessionalFinnishSessionScreen(
            initialSession = session,
            repository = repository,
            copy = copy,
            onExit = { activeSession = null }
        )
    } else {
        LaunchedEffect(repository, selectedDomain) {
            val dashboard = repository.dashboard(selectedDomain)
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
                    text = "Doctor",
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep this page profession-focused: flashcards, roleplay, and interview practice for the work context you paid for.",
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                ProfessionalRouteHeader(palette = palette)
                ProfessionalDomainStrip(
                    selectedDomain = selectedDomain,
                    palette = palette,
                    onSelect = { selectedDomain = it }
                )

                statusMessage?.let { message ->
                    ProfessionalStatusCard(
                        title = "Notice",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    ProfessionalStatusCard(
                        title = "Loading workplace Finnish…",
                        body = "Loading ${selectedDomain.displayName()} exercises.",
                        palette = palette
                    )
                } else if (dashboard.modules.isEmpty()) {
                    ProfessionalStatusCard(
                        title = "No modules yet",
                        body = "Choose another workplace area or come back later when new exercises have been added.",
                        palette = palette
                    )
                } else {
                    dashboard.modules.forEach { module ->
                        val progress = dashboard.progress.firstOrNull { it.moduleId == module.id }
                        ProfessionalModuleCard(
                            module = module,
                            progress = progress,
                            palette = palette,
                            actionLabel = if (module.locked) "Locked" else "Open practice",
                            onClick = {
                                scope.launch {
                                    when (val result = repository.startSession(module.id)) {
                                        is ProfessionalFinnishSessionResult.Ready -> {
                                            statusMessage = null
                                            activeSession = result.session
                                        }
                                        is ProfessionalFinnishSessionResult.Blocked -> statusMessage = result.reason
                                        is ProfessionalFinnishSessionResult.Error -> statusMessage = result.message
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
private fun ProfessionalFinnishSessionScreen(
    initialSession: ProfessionalFinnishSession,
    repository: ProfessionalFinnishRepository,
    copy: LearnCopy,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var session by remember(initialSession.id) { mutableStateOf(initialSession) }
    val scenario = session.currentScenario
    var response by remember(session.id, session.currentScenarioIndex) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.currentScenarioIndex) { mutableStateOf<String?>(null) }
    val progressTarget = if (session.scenarios.isEmpty()) 0f else session.currentScenarioIndex.toFloat() / session.scenarios.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        label = "Professional Finnish progress"
    )

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = session.module.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Keep this page profession-focused: flashcards, roleplay, and interview practice for the work context you paid for.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            ProfessionalSessionProgressCard(
                progress = animatedProgress,
                current = session.currentScenarioIndex + 1,
                total = session.scenarios.size,
                responses = session.responses.size,
                palette = palette
            )

            if (session.completed) {
                ProfessionalCompletionCard(
                    responseCount = session.responses.size,
                    palette = palette
                )
            } else if (scenario != null) {
                ProfessionalScenarioPracticeCard(
                    scenario = scenario,
                    response = response,
                    statusMessage = statusMessage,
                    palette = palette,
                    onResponseChange = { response = it },
                    onContinue = {
                        val cleanResponse = response.trim()
                        if (cleanResponse.isBlank()) {
                            statusMessage = "Write an answer before continuing."
                        } else {
                            scope.launch {
                                session = repository.saveResponse(session, scenario.id, cleanResponse)
                                response = ""
                                statusMessage = null
                            }
                        }
                    }
                )
            }

            FloentlyPrimaryButton(
                title = if (session.completed) copy.professionalTitle else copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun ProfessionalSessionProgressCard(
    progress: Float,
    current: Int,
    total: Int,
    responses: Int,
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
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = palette.warning,
                trackColor = palette.border.copy(alpha = 0.45f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProfessionalSessionMetricBox("Scenario", "$current/$total", palette.warning, palette, Modifier.weight(1f))
                ProfessionalSessionMetricBox("Responses", responses.toString(), palette.accent, palette, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfessionalSessionMetricBox(
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
            Text(value, color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun ProfessionalScenarioPracticeCard(
    scenario: ProfessionalFinnishScenario,
    response: String,
    statusMessage: String?,
    palette: FloentlyPalette,
    onResponseChange: (String) -> Unit,
    onContinue: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(palette.warning)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = scenario.type.displayName().uppercase(),
                    color = palette.warning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            ProfessionalPromptBlock(
                label = "Situation",
                body = scenario.context,
                palette = palette
            )

            ProfessionalPromptBlock(
                label = "Task",
                body = scenario.prompt,
                palette = palette
            )

            if (scenario.modelPhrases.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MODEL PHRASES",
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                    scenario.modelPhrases.forEach { phrase ->
                        ProfessionalPhraseCard(
                            phrase = phrase,
                            palette = palette
                        )
                    }
                }
            }

            OutlinedTextField(
                value = response,
                onValueChange = onResponseChange,
                label = { Text("Suomenkielinen vastauksesi") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Vinkki: käytä yhtä mallilausetta ja muokkaa se tilanteeseen sopivaksi.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = palette.primary,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onContinue)
            ) {
                Text(
                    text = "Save and continue",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfessionalPromptBlock(
    label: String,
    body: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = palette.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = body,
                color = palette.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProfessionalPhraseCard(
    phrase: ProfessionalFinnishPhrase,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = phrase.finnish,
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = phrase.english,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = phrase.usageNote,
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ProfessionalCompletionCard(
    responseCount: Int,
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
                text = "Practice complete",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Teit $responseCount työpaikan suomen vastausta. Toista harjoitus, kun haluat lisää varmuutta.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Seuraava askel: kokeile toista työtilannetta tai harjoittele roolipelissä.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
private fun ProfessionalRouteHeader(
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
                text = "TYÖPAIKAN SUOMI",
                color = palette.warning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Doctor",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Harjoittele hoitotyötä, asiakaspalvelua, toimistoa, työturvallisuutta ja työnhakua eri polkuina.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfessionalTinyPill("Tilanne", "Ammatillinen", palette.warning)
                ProfessionalTinyPill("Tavoite", "Selkeä puhe", palette.accent)
            }
        }
    }
}

@Composable
private fun ProfessionalTinyPill(
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
private fun ProfessionalDomainStrip(
    selectedDomain: ProfessionalFinnishDomain,
    palette: FloentlyPalette,
    onSelect: (ProfessionalFinnishDomain) -> Unit
) {
    val rows = ProfessionalFinnishDomain.entries.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { domain ->
                    val active = domain == selectedDomain
                    Surface(
                        color = if (active) palette.warning.copy(alpha = 0.18f) else palette.cardMuted,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (active) palette.warning else palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(domain) }
                    ) {
                        Text(
                            text = domain.displayName(),
                            color = if (active) palette.warning else palette.muted,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfessionalModuleCard(
    module: ProfessionalFinnishModule,
    progress: ProfessionalFinnishProgress?,
    palette: FloentlyPalette,
    actionLabel: String,
    onClick: () -> Unit
) {
    val accent = module.domain.domainColor(palette)

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
                        .background(if (module.locked) palette.soft else accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (module.locked) "LUKITTU" else module.domain.displayName().uppercase(),
                    color = if (module.locked) palette.soft else accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                ProfessionalMetricBox(
                    label = "Tilanteet",
                    value = "${progress?.completedScenarios ?: 0}/${progress?.totalScenarios ?: 0}",
                    color = accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                ProfessionalMetricBox(
                    label = "Aika",
                    value = "${module.estimatedMinutes} min",
                    color = palette.warning,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            progress?.lastPracticeSummary?.let { summary ->
                Text(
                    text = summary,
                    color = palette.soft,
                    style = MaterialTheme.typography.bodySmall
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
private fun ProfessionalMetricBox(
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
private fun ProfessionalStatusCard(
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

private fun ProfessionalFinnishDomain.domainColor(palette: FloentlyPalette): Color = when (this) {
    ProfessionalFinnishDomain.Healthcare -> palette.warning
    ProfessionalFinnishDomain.Office -> palette.primary
    ProfessionalFinnishDomain.CustomerService -> palette.accent
    ProfessionalFinnishDomain.JobSearch -> Color(0xFF9D7CFF)
    ProfessionalFinnishDomain.Safety -> Color(0xFFFF7A7A)
    ProfessionalFinnishDomain.SmallTalk -> Color(0xFF3EC5A8)
}


private fun ProfessionalFinnishDomain.displayName(): String = when (this) {
    ProfessionalFinnishDomain.Healthcare -> "Nurse"
    ProfessionalFinnishDomain.Office -> "Doctor"
    ProfessionalFinnishDomain.CustomerService -> "Practical Nurse"
    ProfessionalFinnishDomain.JobSearch -> "Työnhaku"
    ProfessionalFinnishDomain.Safety -> "Turvallisuus"
    ProfessionalFinnishDomain.SmallTalk -> "Small talk"
}

private fun ProfessionalFinnishScenarioType.displayName(): String = when (this) {
    ProfessionalFinnishScenarioType.PhrasePractice -> "Phrase practice"
    ProfessionalFinnishScenarioType.DialoguePractice -> "Dialogue practice"
    ProfessionalFinnishScenarioType.EmailWriting -> "Email writing"
    ProfessionalFinnishScenarioType.MeetingResponse -> "Meeting response"
    ProfessionalFinnishScenarioType.PhoneCall -> "Phone call"
}
