package com.floently.read

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floently.shared.design.FloentlyDesignTokens
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.floentlyPalette

@Composable
fun ReadFullPlayerDialog(
    controller: ReadPlaybackController,
    voiceSettings: ReadVoiceSettings,
    onDismiss: () -> Unit
) {
    val snapshot = controller.snapshot
    if (!snapshot.visible) {
        return
    }

    val palette =
        floentlyPalette(
            FloentlyProduct.Read
        )
    var seekDraft by remember(
        snapshot.durationMs
    ) {
        mutableLongStateOf(-1L)
    }
    var voiceMenuExpanded by remember {
        mutableStateOf(false)
    }
    var moreMenuExpanded by remember {
        mutableStateOf(false)
    }

    val displayedPosition =
        if (seekDraft >= 0L) {
            seekDraft
        } else {
            snapshot.positionMs
        }
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
    val currentVoiceName =
        voiceOptions.firstOrNull {
            it.id == currentVoiceId
        }?.name ?: "Voice"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color =
                FloentlyDesignTokens
                    .Colors
                    .canvas,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(
                        horizontal =
                            FloentlyDesignTokens
                                .Space
                                .s6
                    )
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    FullPlayerIconButton(
                        symbol =
                            FullPlayerSymbol.Down,
                        contentDescription =
                            "Close player",
                        primary = false,
                        onClick = onDismiss
                    )

                    Spacer(
                        Modifier.weight(1f)
                    )

                    Text(
                        "Now listening",
                        color = palette.text,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        Modifier.weight(1f)
                    )

                    Box {
                        FullPlayerIconButton(
                            symbol =
                                FullPlayerSymbol
                                    .More,
                            contentDescription =
                                "Player options",
                            primary = false
                        ) {
                            moreMenuExpanded =
                                true
                        }

                        DropdownMenu(
                            expanded =
                                moreMenuExpanded,
                            onDismissRequest = {
                                moreMenuExpanded =
                                    false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Stop playback"
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded =
                                        false
                                    controller.clear(
                                        onComplete =
                                            onDismiss
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(
                        FloentlyDesignTokens
                            .Space
                            .s6
                    )
                )

                Surface(
                    color =
                        FloentlyDesignTokens
                            .Colors
                            .surface3,
                    shape =
                        RoundedCornerShape(
                            FloentlyDesignTokens
                                .Radius
                                .xl
                        ),
                    modifier =
                        Modifier.size(156.dp)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center,
                        modifier =
                            Modifier.background(
                                FloentlyDesignTokens
                                    .Colors
                                    .brandTint
                            )
                    ) {
                        FullPlayerGlyph(
                            symbol =
                                FullPlayerSymbol
                                    .Waveform,
                            color =
                                FloentlyDesignTokens
                                    .Colors
                                    .brandBright,
                            modifier =
                                Modifier.size(58.dp)
                        )
                    }
                }

                Spacer(
                    Modifier.height(
                        FloentlyDesignTokens
                            .Space
                            .s6
                    )
                )

                Text(
                    snapshot.title,
                    color = palette.text,
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 3
                )

                Spacer(
                    Modifier.height(
                        FloentlyDesignTokens
                            .Space
                            .s2
                    )
                )

                Text(
                    snapshot.status,
                    color = palette.muted,
                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )

                Spacer(
                    Modifier.height(
                        FloentlyDesignTokens
                            .Space
                            .s6
                    )
                )

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            FloentlyDesignTokens
                                .Space
                                .s3
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
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
                            if (seekDraft >= 0L) {
                                controller.seekTo(
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
                                .toFloat()
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            fullPlayerClock(
                                displayedPosition
                            ),
                            color =
                                palette.muted,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )
                        Spacer(
                            Modifier.weight(1f)
                        )
                        Text(
                            fullPlayerClock(
                                snapshot.durationMs
                            ),
                            color =
                                palette.muted,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )
                    }
                }

                Spacer(
                    Modifier.height(28.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            FloentlyDesignTokens
                                .Space
                                .s6
                        )
                ) {
                    FullPlayerTextButton(
                        label = "−15",
                        size =
                            FloentlyDesignTokens
                                .Control
                                .iconTarget,
                        contentDescription =
                            "Back 15 seconds",
                        modifier = Modifier.width(
                            FloentlyDesignTokens
                                .Control
                                .iconTarget
                        )
                    ) {
                        controller.seekBy(
                            -15_000
                        )
                    }

                    FullPlayerIconButton(
                        symbol =
                            if (
                                snapshot.isPlaying
                            ) {
                                FullPlayerSymbol
                                    .Pause
                            } else {
                                FullPlayerSymbol
                                    .Play
                            },
                        contentDescription =
                            if (
                                snapshot.isPlaying
                            ) {
                                "Pause reading"
                            } else {
                                "Play reading"
                            },
                        primary = true,
                        size =
                            FloentlyDesignTokens
                                .Control
                                .primaryPlay
                    ) {
                        controller
                            .togglePlayPause()
                    }

                    FullPlayerTextButton(
                        label = "+15",
                        size =
                            FloentlyDesignTokens
                                .Control
                                .iconTarget,
                        contentDescription =
                            "Forward 15 seconds",
                        modifier = Modifier.width(
                            FloentlyDesignTokens
                                .Control
                                .iconTarget
                        )
                    ) {
                        controller.seekBy(
                            15_000
                        )
                    }
                }

                Spacer(
                    Modifier.height(
                        FloentlyDesignTokens
                            .Space
                            .s6
                    )
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            FloentlyDesignTokens
                                .Space
                                .s3
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
                    FullPlayerTextButton(
                        label =
                            fullPlayerSpeedLabel(
                                snapshot.speed
                            ),
                        size =
                            FloentlyDesignTokens
                                .Control
                                .iconTarget,
                        contentDescription =
                            "Reading speed",
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        controller.setSpeed(
                            nextFullPlayerSpeed(
                                snapshot.speed
                            )
                        )
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        FullPlayerTextButton(
                            label =
                                currentVoiceName,
                            size =
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget,
                            contentDescription =
                                "Reading voice",
                            modifier =
                                Modifier.fillMaxWidth()
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
                            voiceOptions.forEach {
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
                }

                Spacer(
                    Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class FullPlayerSymbol {
    Down,
    More,
    Waveform,
    Play,
    Pause
}

@Composable
private fun FullPlayerIconButton(
    symbol: FullPlayerSymbol,
    contentDescription: String,
    primary: Boolean,
    size: androidx.compose.ui.unit.Dp =
        FloentlyDesignTokens
            .Control
            .iconTarget,
    onClick: () -> Unit
) {
    val symbolColor =
        if (primary) {
            FloentlyDesignTokens
                .Colors
                .textOnBrand
        } else {
            FloentlyDesignTokens
                .Colors
                .textPrimary
        }

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
                contentColor = symbolColor
            ),
        shape = RoundedCornerShape(
            if (primary) {
                FloentlyDesignTokens
                    .Radius
                    .xl
            } else {
                FloentlyDesignTokens
                    .Radius
                    .m
            }
        ),
        contentPadding =
            PaddingValues(0.dp),
        modifier = Modifier
            .size(size)
            .semantics {
                this.contentDescription =
                    contentDescription
            }
    ) {
        FullPlayerGlyph(
            symbol = symbol,
            color = symbolColor,
            modifier =
                Modifier.size(
                    if (primary) {
                        26.dp
                    } else {
                        22.dp
                    }
                )
        )
    }
}

@Composable
private fun FullPlayerTextButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
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
        contentPadding =
            PaddingValues(
                horizontal =
                    FloentlyDesignTokens
                        .Space
                        .s3,
                vertical = 0.dp
            ),
        modifier = modifier
            .height(size)
            .semantics {
                this.contentDescription =
                    contentDescription
            }
    ) {
        Text(
            label,
            fontWeight =
                FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun FullPlayerGlyph(
    symbol: FullPlayerSymbol,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f

        when (symbol) {
            FullPlayerSymbol.Down -> {
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.28f,
                        size.height * 0.40f
                    ),
                    end = Offset(
                        cx,
                        size.height * 0.64f
                    ),
                    strokeWidth = stroke
                )
                drawLine(
                    color = color,
                    start = Offset(
                        cx,
                        size.height * 0.64f
                    ),
                    end = Offset(
                        size.width * 0.72f,
                        size.height * 0.40f
                    ),
                    strokeWidth = stroke
                )
            }

            FullPlayerSymbol.More -> {
                listOf(
                    size.width * 0.30f,
                    size.width * 0.50f,
                    size.width * 0.70f
                ).forEach { x ->
                    drawCircle(
                        color = color,
                        radius = stroke * 0.9f,
                        center = Offset(
                            x,
                            cy
                        )
                    )
                }
            }

            FullPlayerSymbol.Waveform -> {
                val xs = listOf(
                    size.width * 0.18f,
                    size.width * 0.36f,
                    size.width * 0.54f,
                    size.width * 0.72f,
                    size.width * 0.88f
                )
                val halves = listOf(
                    size.height * 0.18f,
                    size.height * 0.34f,
                    size.height * 0.48f,
                    size.height * 0.30f,
                    size.height * 0.16f
                )

                xs.zip(halves).forEach {
                    (x, half) ->
                    drawLine(
                        color = color,
                        start = Offset(
                            x,
                            cy - half
                        ),
                        end = Offset(
                            x,
                            cy + half
                        ),
                        strokeWidth = stroke * 1.2f
                    )
                }
            }

            FullPlayerSymbol.Play -> {
                val path = Path().apply {
                    moveTo(
                        size.width * 0.34f,
                        size.height * 0.22f
                    )
                    lineTo(
                        size.width * 0.78f,
                        cy
                    )
                    lineTo(
                        size.width * 0.34f,
                        size.height * 0.78f
                    )
                    close()
                }
                drawPath(
                    path = path,
                    color = color
                )
            }

            FullPlayerSymbol.Pause -> {
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.40f,
                        size.height * 0.24f
                    ),
                    end = Offset(
                        size.width * 0.40f,
                        size.height * 0.76f
                    ),
                    strokeWidth = stroke * 1.8f
                )
                drawLine(
                    color = color,
                    start = Offset(
                        size.width * 0.62f,
                        size.height * 0.24f
                    ),
                    end = Offset(
                        size.width * 0.62f,
                        size.height * 0.76f
                    ),
                    strokeWidth = stroke * 1.8f
                )
            }
        }
    }
}

private fun nextFullPlayerSpeed(
    current: Float
): Float {
    val values = listOf(
        0.75f,
        1f,
        1.25f,
        1.5f,
        2f,
        2.5f,
        3f
    )
    val index =
        values.indexOfFirst {
            kotlin.math.abs(
                it - current
            ) < 0.01f
        }

    return if (
        index < 0
        || index == values.lastIndex
    ) {
        values.first()
    } else {
        values[index + 1]
    }
}

private fun fullPlayerSpeedLabel(
    value: Float
): String =
    if (
        kotlin.math.abs(
            value - value.toInt()
        ) < 0.01f
    ) {
        value.toInt().toString()
            + "×"
    } else {
        "%.2g".format(value) + "×"
    }

private fun fullPlayerClock(
    milliseconds: Long
): String {
    val totalSeconds =
        milliseconds
            .coerceAtLeast(0L)
            .div(1_000L)
    val hours =
        totalSeconds / 3_600L
    val minutes =
        (totalSeconds % 3_600L)
            / 60L
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
