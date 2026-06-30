package com.floently.learn.roleplay

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.floently.learn.audio.NativeTtsButton
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun RoleplayScreen(
    repository: RoleplayRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(RoleplayLevel.A1) }
    var dashboardState by remember { mutableStateOf<RoleplayDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<RoleplaySession?>(null) }

    val session = activeSession
    if (session != null) {
        RoleplaySessionScreen(
            session = session,
            repository = repository,
            copy = copy,
            onSessionChange = { activeSession = it },
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = copy.roleplayTitle,
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.roleplaySubtitle,
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                RoleplayRouteHeader(palette = palette)
                RoleplayLevelStrip(
                    selectedLevel = selectedLevel,
                    palette = palette,
                    onSelect = { selectedLevel = it }
                )

                statusMessage?.let { message ->
                    RoleplayStatusCard(
                        title = "Huomio",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    RoleplayStatusCard(
                        title = "Ladataan keskusteluja…",
                        body = "Haetaan ${selectedLevel.name}-tason harjoituksia.",
                        palette = palette
                    )
                } else if (dashboard.scenarios.isEmpty()) {
                    RoleplayStatusCard(
                        title = "Ei vielä keskusteluja tasolle ${dashboard.selectedLevel.name}",
                        body = "Valitse toinen taso tai palaa myöhemmin, kun uusia harjoituksia on lisätty.",
                        palette = palette
                    )
                } else {
                    dashboard.scenarios.forEach { scenario ->
                        RoleplayScenarioCard(
                            scenario = scenario,
                            palette = palette,
                            actionLabel = if (scenario.locked) "Katso lukituksen syy" else copy.roleplayAction,
                            onClick = {
                                scope.launch {
                                    when (val result = repository.startSession(scenario.id)) {
                                        is RoleplaySessionResult.Ready -> {
                                            statusMessage = null
                                            activeSession = result.session
                                        }
                                        is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                        is RoleplaySessionResult.Error -> statusMessage = result.message
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
private fun RoleplaySessionScreen(
    session: RoleplaySession,
    repository: RoleplayRepository,
    copy: LearnCopy,
    onSessionChange: (RoleplaySession) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var reply by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.learnerTurns) { mutableStateOf<String?>(null) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = session.scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.roleplaySubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            RoleplaySessionProgressCard(
                turns = session.learnerTurns,
                repeatedCueCount = session.repeatedCueCount,
                palette = palette
            )

            session.messages.forEach { message ->
                RoleplayTranscriptBubble(
                    message = message,
                    palette = palette
                )
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Sinun vastauksesi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Kirjoita suomeksi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Vinkki: yksi selkeä lause riittää. Esimerkiksi: Kiitos, se sopii hyvin.",
                    style = MaterialTheme.typography.bodySmall
                )
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FloentlyPrimaryButton(
                    title = "Lähetä vastaus",
                    product = FloentlyProduct.Learn,
                    onClick = {
                        val cleanReply = reply.trim()
                        if (cleanReply.isBlank()) {
                            statusMessage = "Kirjoita lyhyt suomenkielinen vastaus ennen lähettämistä."
                        } else {
                            scope.launch {
                                when (val result = repository.sendLearnerMessage(session, cleanReply)) {
                                    is RoleplaySessionResult.Ready -> {
                                        reply = ""
                                        statusMessage = null
                                        onSessionChange(result.session)
                                    }
                                    is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                    is RoleplaySessionResult.Error -> statusMessage = result.message
                                }
                            }
                        }
                    }
                )
            }

            FloentlyPrimaryButton(
                title = copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun RoleplayRouteHeader(
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
                text = "SPEAK",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Speaking Lab",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Short turns. Clear repair. Less pressure.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleplayTinyPill("Step 1", "Record once", palette.primary, palette)
                RoleplayTinyPill("Step 2", "Repair once", palette.accent, palette)
            }
        }
    }
}

@Composable
private fun RoleplayTinyPill(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette
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
private fun RoleplayLevelStrip(
    selectedLevel: RoleplayLevel,
    palette: FloentlyPalette,
    onSelect: (RoleplayLevel) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RoleplayLevel.entries.forEach { level ->
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
                    text = level.name,
                    color = if (active) palette.primary else palette.muted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleplayScenarioCard(
    scenario: RoleplayScenario,
    palette: FloentlyPalette,
    actionLabel: String,
    onClick: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (scenario.locked) palette.soft else palette.accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (scenario.locked) "LUKITTU" else "VALMIS",
                    color = if (scenario.locked) palette.soft else palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }

            Text(
                text = scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = scenario.description,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = scenario.helperText(),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            Surface(
                color = if (scenario.locked) palette.card else palette.primary,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (scenario.locked) palette.border else palette.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionLabel,
                    color = if (scenario.locked) palette.muted else Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleplayStatusCard(
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

@Composable
private fun RoleplaySessionProgressCard(
    turns: Int,
    repeatedCueCount: Int,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoleplayMetricBox("Vuorot", turns.toString(), palette.primary, palette, Modifier.weight(1f))
            RoleplayMetricBox("Vaihtelu", repeatedCueCount.toString(), palette.accent, palette, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RoleplayMetricBox(
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
            Text(value, color = palette.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun RoleplayTranscriptBubble(
    message: RoleplayMessage,
    palette: FloentlyPalette
) {
    val isLearner = message.speaker == RoleplaySpeaker.Learner
    val bubbleColor = if (isLearner) palette.primary else palette.cardMuted
    val textColor = if (isLearner) Color.White else palette.text
    val align = if (isLearner) Alignment.CenterEnd else Alignment.CenterStart
    val speakerLabel = message.displayName()

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalAlignment = if (isLearner) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = speakerLabel,
                color = if (isLearner) palette.muted else palette.soft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isLearner) 18.dp else 6.dp,
                    bottomEnd = if (isLearner) 6.dp else 18.dp
                ),
                border = if (isLearner) null else BorderStroke(1.dp, palette.border)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                    if (!isLearner) {
                        NativeTtsButton(text = message.text, label = "Kuuntele")
                    }
                    message.coachingNote?.let { note ->
                        Text(
                            text = "Valmentajan huomio: $note",
                            color = if (isLearner) Color.White.copy(alpha = 0.82f) else palette.soft,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}


private fun RoleplayScenario.helperText(): String {
    val safety = if (beginnerSafe) "Aloittelijaystävällinen" else "Vaativampi"
    return "$safety keskustelu: ${type.name.lowercase()}."
}

private fun RoleplayMessage.displayName(): String = when (speaker) {
    RoleplaySpeaker.Learner -> "Sinä"
    RoleplaySpeaker.Coach -> "Valmentaja"
    RoleplaySpeaker.Partner -> "Keskustelukumppani"
}
