package com.floently.learn.roleplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.floently.shared.design.FloentlyPalette

@Composable
internal fun OldSourceRoleplayScenarioHeader(
    scenario: RoleplayScenario,
    palette: FloentlyPalette,
    onExit: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OldSourceSpeakingPill(scenario.level.displayName, scenario.type.name, palette.primary)
                Surface(
                    color = palette.cardMuted,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.clickable(onClick = onExit)
                ) {
                    Text(
                        text = "Exit",
                        color = palette.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
            Text(
                text = scenario.title,
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = scenario.description,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            if (scenario.targetPhrases.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target phrases",
                        color = palette.soft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    scenario.targetPhrases.take(3).forEach { phrase ->
                        Text(
                            text = "• $phrase",
                            color = palette.text,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun OldSourceRoleplayMicPanel(
    phase: RoleplaySpeechPhase,
    recordedText: String,
    speechAvailable: Boolean,
    concluded: Boolean,
    palette: FloentlyPalette,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    val isListening = phase == RoleplaySpeechPhase.Listening
    val status = when {
        concluded -> "Conversation complete. Download the PDF or Word document/book."
        phase == RoleplaySpeechPhase.Idle -> "Tap the mic and answer in Finnish. Tap again to stop and send."
        phase == RoleplaySpeechPhase.Listening -> "Listening now… tap the mic again to stop and send."
        phase == RoleplaySpeechPhase.Processing -> "Transcribing and sending…"
        phase == RoleplaySpeechPhase.Ready -> "Transcription ready and sending."
        phase == RoleplaySpeechPhase.Error -> "Try again or type your response."
        else -> "Speech recognition is unavailable on this device."
    }

    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (concluded) "ROLEPLAY COMPLETE" else "SPEAKING PRACTICE",
                color = palette.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
                Surface(
                    color = palette.primary.copy(alpha = if (isListening) 0.16f else 0.08f),
                    shape = CircleShape,
                    modifier = Modifier.size(152.dp)
                ) {}
                Surface(
                    color = palette.primary.copy(alpha = if (isListening) 0.24f else 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(118.dp)
                ) {}
                Surface(
                    color = when {
                        concluded -> palette.soft
                        isListening -> palette.accent
                        else -> palette.primary
                    },
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .size(84.dp)
                        .clickable(enabled = speechAvailable && !concluded) { if (isListening) onStop() else onStart() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                concluded -> "DONE"
                                isListening -> "STOP"
                                else -> "MIC"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            OldSourceWaveform(active = isListening, palette = palette)
            Text(
                text = if (speechAvailable || concluded) status else "Speech recognition is unavailable on this device. Type your answer below.",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            if (recordedText.isNotBlank()) {
                Text(
                    text = recordedText,
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun OldSourceRecordedResponseCard(
    recordedText: String,
    palette: FloentlyPalette,
    onUseResponse: () -> Unit
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Transcription",
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = recordedText,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            OldSourceSpeakingAction(
                label = "Use this text",
                palette = palette,
                filled = true,
                enabled = recordedText.isNotBlank(),
                onClick = onUseResponse,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OldSourceWaveform(
    active: Boolean,
    palette: FloentlyPalette
) {
    val heights = if (active) listOf(18, 32, 24, 44, 30, 38, 22) else listOf(12, 16, 14, 18, 14, 16, 12)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) palette.accent else palette.border)
            )
        }
    }
}

@Composable
private fun OldSourceSpeakingPill(
    label: String,
    value: String,
    color: Color
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
private fun OldSourceSpeakingAction(
    label: String,
    palette: FloentlyPalette,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            !enabled -> palette.cardMuted
            filled -> palette.primary
            else -> palette.card
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (filled && enabled) palette.primary else palette.border),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> palette.soft
                filled -> Color.White
                else -> palette.text
            },
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
