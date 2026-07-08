package com.floently.learn.roleplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.audio.NativeTtsButton
import com.floently.shared.design.FloentlyPalette

@Composable
internal fun OldSourceRoleplayRouteHeader(
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ROLEPLAY",
                color = palette.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.4.sp
            )
            Text(
                text = "Conversation practice",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Short turns, clear repair, and less pressure.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OldSourceRoleplayTinyPill("Step 1", "Listen", palette.primary, palette)
                OldSourceRoleplayTinyPill("Step 2", "Reply", palette.accent, palette)
            }
        }
    }
}

@Composable
private fun OldSourceRoleplayTinyPill(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f))
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
internal fun OldSourceRoleplayScenarioCard(
    scenario: RoleplayScenario,
    palette: FloentlyPalette,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (scenario.locked) palette.soft else palette.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (scenario.locked) "LOCKED" else "READY",
                    color = if (scenario.locked) palette.soft else palette.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OldSourceRoleplayTinyPill(scenario.level.name, scenario.type.name, palette.primary, palette)
                OldSourceRoleplayTinyPill("Mode", scenario.coachingMode.oldSourceLabel(), palette.accent, palette)
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
                text = scenario.oldSourceHelperText(),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )

            Surface(
                color = if (scenario.locked) palette.cardMuted else palette.primary,
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
internal fun OldSourceRoleplaySessionProgressCard(
    turns: Int,
    repeatedCueCount: Int,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OldSourceRoleplayMetricBox("Turns", turns.toString(), palette.primary, palette, Modifier.weight(1f))
            OldSourceRoleplayMetricBox("Variation", repeatedCueCount.toString(), palette.accent, palette, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OldSourceRoleplayMetricBox(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(16.dp),
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
internal fun OldSourceRoleplayTranscriptBubble(
    message: RoleplayMessage,
    palette: FloentlyPalette
) {
    val isLearner = message.speaker == RoleplaySpeaker.Learner
    val bubbleColor = if (isLearner) palette.primary else palette.cardMuted
    val textColor = if (isLearner) Color.White else palette.text
    val align = if (isLearner) Alignment.CenterEnd else Alignment.CenterStart
    val speakerLabel = message.oldSourceDisplayName()

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Column(
            modifier = Modifier.fillMaxWidth(0.84f),
            horizontalAlignment = if (isLearner) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
                            text = "Coach note: $note",
                            color = if (isLearner) Color.White.copy(alpha = 0.82f) else palette.soft,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun RoleplayScenario.oldSourceHelperText(): String {
    val safety = if (beginnerSafe) "Beginner-friendly" else "More demanding"
    return "$safety ${coachingMode.oldSourceLabel().lowercase()} conversation: ${type.name.lowercase()}."
}

private fun RoleplayCoachingMode.oldSourceLabel(): String = when (this) {
    RoleplayCoachingMode.BeginnerSafe -> "Beginner"
    RoleplayCoachingMode.Natural -> "Natural"
    RoleplayCoachingMode.Professional -> "Professional"
    RoleplayCoachingMode.ExamStyle -> "Exam style"
}

private fun RoleplayMessage.oldSourceDisplayName(): String = when (speaker) {
    RoleplaySpeaker.Learner -> "You"
    RoleplaySpeaker.Coach -> "Coach"
    RoleplaySpeaker.Partner -> "Conversation partner"
}
