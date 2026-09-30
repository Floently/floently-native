package com.floently.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.floentlyPalette

@Composable
fun ReadPersistentPlayerDock(
    controller: ReadPlaybackController,
    modifier: Modifier = Modifier
) {
    val snapshot = controller.snapshot
    if (!snapshot.visible) return

    val palette = floentlyPalette(FloentlyProduct.Read)
    var seekDraft by remember(snapshot.durationMs) {
        mutableLongStateOf(-1L)
    }
    val displayedPosition = if (seekDraft >= 0L) {
        seekDraft
    } else {
        snapshot.positionMs
    }

    Surface(
        color = palette.backgroundBottom.copy(alpha = 0.98f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = snapshot.title,
                        color = palette.text,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = snapshot.status,
                        color = palette.muted,
                        maxLines = 1
                    )
                }

                PlayerButton(
                    label = "−15",
                    surface = palette.backgroundTop,
                    textColor = palette.text
                ) {
                    controller.seekBy(-15_000)
                }

                PlayerButton(
                    label = if (snapshot.isPlaying) "Pause" else "Play",
                    surface = palette.accent,
                    textColor = Color.White
                ) {
                    controller.togglePlayPause()
                }

                PlayerButton(
                    label = "+15",
                    surface = palette.backgroundTop,
                    textColor = palette.text
                ) {
                    controller.seekBy(15_000)
                }

                PlayerButton(
                    label = speedLabel(snapshot.speed),
                    surface = palette.backgroundTop,
                    textColor = palette.text
                ) {
                    controller.setSpeed(nextSpeed(snapshot.speed))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = clock(displayedPosition),
                    color = palette.muted,
                    modifier = Modifier.width(48.dp)
                )

                Slider(
                    value = displayedPosition
                        .coerceIn(
                            0L,
                            snapshot.durationMs.coerceAtLeast(1L)
                        )
                        .toFloat(),
                    onValueChange = {
                        seekDraft = it.toLong()
                    },
                    onValueChangeFinished = {
                        if (seekDraft >= 0L) {
                            controller.seekTo(seekDraft)
                            seekDraft = -1L
                        }
                    },
                    valueRange = 0f..snapshot.durationMs
                        .coerceAtLeast(1L)
                        .toFloat(),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = clock(snapshot.durationMs),
                    color = palette.muted,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerButton(
    label: String,
    surface: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = surface,
            contentColor = textColor
        )
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

private fun nextSpeed(current: Float): Float {
    val speeds = listOf(
        0.75f,
        1f,
        1.25f,
        1.5f,
        2f,
        2.5f,
        3f
    )
    val index = speeds.indexOfFirst {
        kotlin.math.abs(it - current) < 0.01f
    }
    return if (index < 0 || index == speeds.lastIndex) {
        speeds.first()
    } else {
        speeds[index + 1]
    }
}

private fun speedLabel(value: Float): String =
    if (kotlin.math.abs(value - value.toInt()) < 0.01f) {
        "${value.toInt()}×"
    } else {
        "${"%.2g".format(value)}×"
    }

private fun clock(milliseconds: Long): String {
    val totalSeconds = milliseconds
        .coerceAtLeast(0L)
        .div(1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        "%d:%02d:%02d".format(
            hours,
            minutes,
            seconds
        )
    } else {
        "%d:%02d".format(
            minutes,
            seconds
        )
    }
}
