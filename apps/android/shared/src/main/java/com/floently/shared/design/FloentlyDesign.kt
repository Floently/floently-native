package com.floently.shared.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class FloentlyProduct {
    Learn,
    Read,
    Create
}

@Immutable
data class FloentlyPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val backgroundGlow: Color,
    val card: Color,
    val cardMuted: Color,
    val cardText: Color,
    val text: Color,
    val muted: Color,
    val soft: Color,
    val border: Color,
    val primary: Color,
    val accent: Color,
    val warning: Color
)

@Composable
fun floentlyPalette(product: FloentlyProduct): FloentlyPalette {
    return when (product) {
        FloentlyProduct.Learn -> FloentlyPalette(
            // Old app source: packages/ui/theme/floentlyPalette.ts darkPalette
            backgroundTop = Color(0xFF0A1838),
            backgroundBottom = Color(0xFF0A1838),
            backgroundGlow = Color(0xFF17306A),
            card = Color(0xFF112346),
            cardMuted = Color(0xFF0D1D42),
            cardText = Color(0xFFF5F9FF),
            text = Color(0xFFF5F9FF),
            muted = Color(0xFFA8BAD6),
            soft = Color(0xFF7A8CAE),
            border = Color(0xFF263B6B),
            primary = Color(0xFF5A85FF),
            accent = Color(0xFF3EC5A8),
            warning = Color(0xFFE8B65E)
        )

        FloentlyProduct.Read -> FloentlyPalette(
            backgroundTop = Color(0xFF071124),
            backgroundBottom = Color(0xFF101827),
            backgroundGlow = Color(0xFF26364F),
            card = Color(0xFF111B30),
            cardMuted = Color(0xFF16233E),
            cardText = Color(0xFFF7FAFF),
            text = Color(0xFFF7FAFF),
            muted = Color(0xFFB8C6E6),
            soft = Color(0xFF8EA3C3),
            border = Color(0xFF243652),
            primary = Color(0xFFF0C86D),
            accent = Color(0xFF6387FF),
            warning = Color(0xFFF0C86D)
        )

        FloentlyProduct.Create -> FloentlyPalette(
            backgroundTop = Color(0xFF071124),
            backgroundBottom = Color(0xFF160D2F),
            backgroundGlow = Color(0xFF33205F),
            card = Color(0xFF111B30),
            cardMuted = Color(0xFF1E2441),
            cardText = Color(0xFFF7FAFF),
            text = Color(0xFFF7FAFF),
            muted = Color(0xFFB8C6E6),
            soft = Color(0xFF8EA3C3),
            border = Color(0xFF2E315B),
            primary = Color(0xFF7A9FFF),
            accent = Color(0xFFFF4FD8),
            warning = Color(0xFFF0C86D)
        )
    }
}

@Composable
fun FloentlyScreen(
    product: FloentlyProduct,
    content: @Composable (FloentlyPalette) -> Unit
) {
    val palette = floentlyPalette(product)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.backgroundTop,
                        palette.backgroundBottom,
                        Color(0xFF071124)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
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
    Surface(
        color = palette.card,
        contentColor = palette.cardText,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, palette.border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp),
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
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Black)
    }
}

@Composable
fun FloentlySecondaryButton(
    title: String,
    product: FloentlyProduct,
    onClick: () -> Unit
) {
    val palette = floentlyPalette(product)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.cardMuted,
            contentColor = palette.text
        ),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, palette.border),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Black)
    }
}
