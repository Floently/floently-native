package com.floently.learn.professional

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
import androidx.compose.animation.AnimatedVisibility
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
                    text = copy.professionalTitle,
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.professionalSubtitle,
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
                        title = "Huomio",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    ProfessionalStatusCard(
                        title = "Ladataan työpaikan suomea…",
                        body = "Haetaan ${selectedDomain.displayName()} -harjoituksia.",
                        palette = palette
                    )
                } else if (dashboard.modules.isEmpty()) {
                    ProfessionalStatusCard(
                        title = "Ei vielä moduuleja",
                        body = "Valitse toinen työtilanne tai palaa myöhemmin, kun uusia harjoituksia on lisätty.",
                        palette = palette
                    )
                } else {
                    dashboard.modules.forEach { module ->
                        val progress = dashboard.progress.firstOrNull { it.moduleId == module.id }
                        ProfessionalModuleCard(
                            module = module,
                            progress = progress,
                            palette = palette,
                            actionLabel = if (module.locked) "Katso lukituksen syy" else copy.professionalAction,
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
                text = copy.professionalSubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Scenario ${session.currentScenarioIndex + 1} of ${session.scenarios.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Responses: ${session.responses.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (session.completed) {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = "Practice complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You completed ${session.responses.size} professional Finnish response(s). Repeat this practice when you want more confidence.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Next step: try another work situation or practise roleplay.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else if (scenario != null) {
                AnimatedVisibility(visible = true) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scenario.type.displayName(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = scenario.context,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = scenario.prompt,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        scenario.modelPhrases.forEach { phrase ->
                            FloentlyCard(product = FloentlyProduct.Learn) {
                                Text(
                                    text = phrase.finnish,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = phrase.english,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = phrase.usageNote,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        OutlinedTextField(
                            value = response,
                            onValueChange = { response = it },
                            label = { Text("Your Finnish response") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Tip: use one model phrase and adapt it to the situation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        statusMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FloentlyPrimaryButton(
                            title = "Save and continue",
                            product = FloentlyProduct.Learn,
                            onClick = {
                                val cleanResponse = response.trim()
                                if (cleanResponse.isBlank()) {
                                    statusMessage = "Write a response before continuing."
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
                }
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
                text = "Työpaikan tilanteet",
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
    ProfessionalFinnishDomain.Healthcare -> "Hoitotyö"
    ProfessionalFinnishDomain.Office -> "Toimisto"
    ProfessionalFinnishDomain.CustomerService -> "Asiakaspalvelu"
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
