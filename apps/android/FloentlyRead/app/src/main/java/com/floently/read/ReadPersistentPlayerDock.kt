package com.floently.read

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyDesignTokens
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.floentlyPalette

@Composable
fun ReadPersistentPlayerDock(
    controller: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings,
    modifier: Modifier = Modifier
) {
    val snapshot = controller.snapshot
    if (!snapshot.visible) return

    val palette =
        floentlyPalette(
            FloentlyProduct.Read
        )
    var seekDraft by remember(
        snapshot.durationMs
    ) {
        mutableLongStateOf(-1L)
    }
    var toolsExpanded by remember {
        mutableStateOf(false)
    }
    val displayedPosition =
        if (seekDraft >= 0L) {
            seekDraft
        } else {
            snapshot.positionMs
        }
    val selectionRevision =
        voiceSettings.selectionRevision
    val activeLanguage =
        controller.activeLanguage
    val currentVoiceId =
        controller.activeVoiceId
            ?: voiceSettings.voiceId(
                activeLanguage
            )
    val voiceOptions =
        voiceSettings.availableVoices(
            activeLanguage
        )
    var voiceMenuExpanded by remember {
        mutableStateOf(false)
    }

    @Suppress("UNUSED_VARIABLE")
    val voiceSelectionVersion =
        selectionRevision

    Surface(
        color =
            FloentlyDesignTokens
                .Colors
                .surface2,
        shape = RoundedCornerShape(
            FloentlyDesignTokens
                .Radius
                .l
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal =
                    FloentlyDesignTokens
                        .Space
                        .s3,
                vertical = 6.dp
            )
    ) {
        Column {
            LinearProgressIndicator(
                progress = {
                    if (
                        snapshot.durationMs > 0L
                    ) {
                        (
                            snapshot.positionMs
                                .coerceIn(
                                    0L,
                                    snapshot.durationMs
                                )
                                .toFloat()
                                / snapshot.durationMs
                                    .toFloat()
                            ).coerceIn(
                                0f,
                                1f
                            )
                    } else {
                        0f
                    }
                },
                color =
                    FloentlyDesignTokens
                        .Colors
                        .brand,
                trackColor =
                    FloentlyDesignTokens
                        .Colors
                        .borderSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        FloentlyDesignTokens
                            .Space
                            .s3
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(
                        horizontal =
                            FloentlyDesignTokens
                                .Space
                                .s3
                    )
            ) {
                Surface(
                    color =
                        FloentlyDesignTokens
                            .Colors
                            .surface1,
                    shape =
                        RoundedCornerShape(
                            FloentlyDesignTokens
                                .Radius
                                .m
                        ),
                    modifier = Modifier.size(
                        FloentlyDesignTokens
                            .Control
                            .compactHeight
                    )
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            "≈",
                            color =
                                FloentlyDesignTokens
                                    .Colors
                                    .brandBright,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        text = snapshot.title,
                        color = palette.text,
                        fontWeight =
                            FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = snapshot.status,
                        color = palette.muted,
                        style =
                            androidx.compose
                                .material3
                                .MaterialTheme
                                .typography
                                .bodySmall,
                        maxLines = 1
                    )
                }

                MiniPlayerButton(
                    label =
                        if (
                            snapshot.isPlaying
                        ) {
                            "Ⅱ"
                        } else {
                            "▶"
                        },
                    primary = true
                ) {
                    controller
                        .togglePlayPause()
                }

                MiniPlayerButton(
                    label =
                        if (toolsExpanded) {
                            "⌄"
                        } else {
                            "⋯"
                        },
                    primary = false
                ) {
                    toolsExpanded =
                        !toolsExpanded
                }
            }

            if (toolsExpanded) {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            FloentlyDesignTokens
                                .Space
                                .s3
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                FloentlyDesignTokens
                                    .Space
                                    .s3
                        )
                        .padding(
                            bottom =
                                FloentlyDesignTokens
                                    .Space
                                    .s3
                        )
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                            ),
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Spacer(
                            Modifier.weight(1f)
                        )

                        PlayerToolButton(
                            label = "−15"
                        ) {
                            controller.seekBy(
                                -15_000
                            )
                        }

                        PlayerToolButton(
                            label =
                                speedLabel(
                                    snapshot.speed
                                )
                        ) {
                            controller.setSpeed(
                                nextSpeed(
                                    snapshot.speed
                                )
                            )
                        }

                        Box {
                            PlayerToolButton(
                                label = "Voice"
                            ) {
                                voiceMenuExpanded =
                                    true
                            }

                            DropdownMenu(
                                expanded =
                                    voiceMenuExpanded,
                                onDismissRequest = {
                                    voiceMenuExpanded =
                                        false
                                }
                            ) {
                                voiceOptions
                                    .forEach {
                                        voice ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (
                                                        voice.id
                                                            == currentVoiceId
                                                    ) {
                                                        "✓ "
                                                            + voice.name
                                                    } else {
                                                        voice.name
                                                    }
                                                )
                                            },
                                            onClick = {
                                                voiceSettings
                                                    .select(
                                                        voiceId =
                                                            voice.id,
                                                        language =
                                                            activeLanguage
                                                    )
                                                controller
                                                    .changeVoice(
                                                        voice.id
                                                    )
                                                voiceMenuExpanded =
                                                    false
                                            }
                                        )
                                    }
                            }
                        }

                        PlayerToolButton(
                            label = "+15"
                        ) {
                            controller.seekBy(
                                15_000
                            )
                        }

                        Spacer(
                            Modifier.weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                            ),
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = clock(
                                displayedPosition
                            ),
                            color = palette.muted,
                            modifier =
                                Modifier.width(
                                    48.dp
                                )
                        )

                        Slider(
                            value =
                                displayedPosition
                                    .coerceIn(
                                        0L,
                                        snapshot
                                            .durationMs
                                            .coerceAtLeast(
                                                1L
                                            )
                                    )
                                    .toFloat(),
                            onValueChange = {
                                seekDraft =
                                    it.toLong()
                            },
                            onValueChangeFinished = {
                                if (
                                    seekDraft >= 0L
                                ) {
                                    controller
                                        .seekTo(
                                            seekDraft
                                        )
                                    seekDraft = -1L
                                }
                            },
                            valueRange =
                                0f..snapshot
                                    .durationMs
                                    .coerceAtLeast(
                                        1L
                                    )
                                    .toFloat(),
                            modifier =
                                Modifier.weight(1f)
                        )

                        Text(
                            text = clock(
                                snapshot.durationMs
                            ),
                            color = palette.muted,
                            modifier =
                                Modifier.width(
                                    48.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults
            .buttonColors(
                containerColor =
                    if (primary) {
                        FloentlyDesignTokens
                            .Colors
                            .brand
                    } else {
                        FloentlyDesignTokens
                            .Colors
                            .surface1
                    },
                contentColor =
                    if (primary) {
                        FloentlyDesignTokens
                            .Colors
                            .textOnBrand
                    } else {
                        FloentlyDesignTokens
                            .Colors
                            .textPrimary
                    }
            ),
        shape = RoundedCornerShape(
            FloentlyDesignTokens
                .Radius
                .m
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(
            FloentlyDesignTokens
                .Control
                .iconTarget
        )
    ) {
        Text(
            label,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerToolButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults
            .buttonColors(
                containerColor =
                    FloentlyDesignTokens
                        .Colors
                        .surface1,
                contentColor =
                    FloentlyDesignTokens
                        .Colors
                        .textPrimary
            ),
        shape = RoundedCornerShape(
            FloentlyDesignTokens
                .Radius
                .m
        ),
        contentPadding = PaddingValues(
            horizontal =
                FloentlyDesignTokens
                    .Space
                    .s3,
            vertical = 0.dp
        ),
        modifier = Modifier.height(
            FloentlyDesignTokens
                .Control
                .iconTarget
        )
    ) {
        Text(
            label,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun nextSpeed(
    current: Float
): Float {
    val speeds = listOf(
        0.75f,
        1f,
        1.25f,
        1.5f,
        2f,
        2.5f,
        3f
    )
    val index =
        speeds.indexOfFirst {
            kotlin.math.abs(
                it - current
            ) < 0.01f
        }

    return if (
        index < 0
        || index == speeds.lastIndex
    ) {
        speeds.first()
    } else {
        speeds[index + 1]
    }
}

private fun speedLabel(
    value: Float
): String =
    if (
        kotlin.math.abs(
            value - value.toInt()
        ) < 0.01f
    ) {
        "${value.toInt()}×"
    } else {
        "${"%.2g".format(value)}×"
    }

private fun clock(
    milliseconds: Long
): String {
    val totalSeconds =
        milliseconds
            .coerceAtLeast(0L)
            .div(1_000L)
    val hours =
        totalSeconds / 3_600L
    val minutes =
        (totalSeconds % 3_600L) / 60L
    val seconds =
        totalSeconds % 60L

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
