package com.floently.shared.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class FloentlyProduct {
    Learn,
    Read,
    Create
}

/**
 * Native adapter for the frozen Iloadi UI 1.0.0 token contract.
 *
 * Source of truth:
 * docs/design/ILOADI_DESIGN_TOKENS.md
 */
object FloentlyDesignTokens {
    const val version = "1.0.0"

    object Colors {
        val canvas = Color(0xFF07060A)
        val canvasRaised = Color(0xFF0B0810)
        val surface1 = Color(0xFF0F0C14)
        val surface2 = Color(0xFF15101D)
        val surface3 = Color(0xFF1B1427)
        val brand = Color(0xFF8B5CFF)
        val brandBright = Color(0xFFB998FF)
        val brandDeep = Color(0xFF5A34CC)
        val brandTint = Color(0xFF24183A)

        val textPrimary = Color(0xFFF7F3FF)
        val textSecondary = Color(0xFFB9B1C4)
        val textTertiary = Color(0xFF81788D)
        val textDisabled = Color(0xFF5E5669)
        val textOnBrand = Color(0xFFFFFFFF)

        val borderSoft = Color(0xFF241E2C)
        val border = Color(0xFF30263A)
        val success = Color(0xFF67D8A8)
        val warning = Color(0xFFF3C66F)
        val danger = Color(0xFFFF718D)
        val info = Color(0xFF6CB9FF)
    }

    object Space {
        val s1 = 4.dp
        val s2 = 8.dp
        val s3 = 12.dp
        val s4 = 16.dp
        val s5 = 20.dp
        val s6 = 24.dp
        val s8 = 32.dp
        val s10 = 40.dp
        val s12 = 48.dp
        val s16 = 64.dp
    }

    object Radius {
        val s = 10.dp
        val m = 14.dp
        val l = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val pill = 999.dp
    }

    object Control {
        val primaryHeight = 52.dp
        val compactHeight = 44.dp
        val searchHeight = 48.dp
        val iconTarget = 48.dp
        val floatingAction = 56.dp
        val primaryPlay = 64.dp
    }

    object TypeScale {
        val display = 42
        val h1 = 34
        val h2 = 28
        val h3 = 22
        val title = 18
        val body = 16
        val small = 14
        val caption = 12
        val micro = 11
        val readerDefault = 20
        val readerLarge = 22
        val readerCompact = 18
    }

    object Motion {
        const val instantMs = 100
        const val fastMs = 160
        const val standardMs = 240
        const val enterMs = 320
        const val exitMs = 220
        const val contentMs = 360
    }
}

@Immutable
data class FloentlyPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val accent: Color
)

@Composable
fun floentlyPalette(
    product: FloentlyProduct
): FloentlyPalette {
    return when (product) {
        FloentlyProduct.Learn -> FloentlyPalette(
            backgroundTop = Color(0xFF07111F),
            backgroundBottom = Color(0xFF102A43),
            card = Color(0xFFFFFFFF),
            text = Color(0xFFFFFFFF),
            muted = Color(0xCCFFFFFF),
            accent = Color(0xFF30D5C8)
        )
        FloentlyProduct.Read -> FloentlyPalette(
            backgroundTop =
                FloentlyDesignTokens.Colors.canvas,
            backgroundBottom =
                FloentlyDesignTokens.Colors.canvasRaised,
            card =
                FloentlyDesignTokens.Colors.surface2,
            text =
                FloentlyDesignTokens.Colors.textPrimary,
            muted =
                FloentlyDesignTokens.Colors.textSecondary,
            accent =
                FloentlyDesignTokens.Colors.brand
        )
        FloentlyProduct.Create -> FloentlyPalette(
            backgroundTop = Color(0xFF120A23),
            backgroundBottom = Color(0xFF33205F),
            card = Color(0xFFFFFFFF),
            text = Color(0xFFFFFFFF),
            muted = Color(0xCCFFFFFF),
            accent = Color(0xFFFF4FD8)
        )
    }
}

@Composable
fun FloentlyScreen(
    product: FloentlyProduct,
    content: @Composable (FloentlyPalette) -> Unit
) {
    val palette = floentlyPalette(product)
    val padding =
        if (product == FloentlyProduct.Read) {
            FloentlyDesignTokens.Space.s5
        } else {
            24.dp
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.backgroundTop,
                        palette.backgroundBottom
                    )
                )
            )
            .padding(padding)
    ) {
        content(palette)
    }
}

@Composable
fun FloentlyCard(
    product: FloentlyProduct,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = floentlyPalette(product)
    val read =
        product == FloentlyProduct.Read

    Surface(
        color =
            if (read) {
                FloentlyDesignTokens.Colors.surface2
            } else {
                palette.card
            },
        shape = RoundedCornerShape(
            if (read) {
                FloentlyDesignTokens.Radius.xl
            } else {
                28.dp
            }
        ),
        tonalElevation =
            if (read) 0.dp else 8.dp,
        shadowElevation =
            if (read) 8.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    if (read) {
                        FloentlyDesignTokens.Space.s3
                    } else {
                        14.dp
                    }
                ),
            modifier = Modifier.padding(
                if (read) {
                    FloentlyDesignTokens.Space.s5
                } else {
                    20.dp
                }
            ),
            content = content
        )
    }
}

@Composable
fun FloentlyPrimaryButton(
    title: String,
    product: FloentlyProduct,
    onClick: () -> Unit
) {
    val palette = floentlyPalette(product)
    val read =
        product == FloentlyProduct.Read

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (read) {
                    FloentlyDesignTokens.Colors.brand
                } else {
                    palette.accent
                }
        ),
        shape = RoundedCornerShape(
            if (read) {
                FloentlyDesignTokens.Radius.l
            } else {
                18.dp
            }
        ),
        contentPadding = PaddingValues(
            horizontal =
                if (read) {
                    FloentlyDesignTokens.Space.s5
                } else {
                    20.dp
                },
            vertical =
                if (read) 0.dp else 14.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (read) {
                    Modifier.height(
                        FloentlyDesignTokens
                            .Control
                            .primaryHeight
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            title,
            color =
                if (read) {
                    FloentlyDesignTokens
                        .Colors
                        .textOnBrand
                } else {
                    Color.Unspecified
                }
        )
    }
}
