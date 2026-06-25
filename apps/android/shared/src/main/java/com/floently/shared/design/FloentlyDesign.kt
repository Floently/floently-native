package com.floently.shared.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
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
fun floentlyPalette(product: FloentlyProduct): FloentlyPalette {
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
            backgroundTop = Color(0xFF101827),
            backgroundBottom = Color(0xFF26364F),
            card = Color(0xFFFFFFFF),
            text = Color(0xFFFFFFFF),
            muted = Color(0xCCFFFFFF),
            accent = Color(0xFFFFC857)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
            .padding(24.dp)
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
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(20.dp),
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
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title)
    }
}
