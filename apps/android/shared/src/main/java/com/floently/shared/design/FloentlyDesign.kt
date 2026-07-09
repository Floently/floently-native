package com.floently.shared.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FloentlyProduct {
    Learn,
    Read,
    Create
}

enum class FloentlyThemeMode {
    System,
    Light,
    Dark
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

@Immutable
data class FloentlyDesignTokens(
    val screenHorizontalPadding: Int = 18,
    val screenVerticalPadding: Int = 18,
    val cardRadius: Int = 26,
    val chipRadius: Int = 999,
    val cardPadding: Int = 18,
    val cardSpacing: Int = 14,
    val borderWidth: Int = 1
)

val LocalFloentlyDarkTheme = compositionLocalOf { true }
val LocalFloentlyDesignTokens = compositionLocalOf { FloentlyDesignTokens() }

@Composable
fun FloentlyTheme(
    themeMode: FloentlyThemeMode = FloentlyThemeMode.System,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        FloentlyThemeMode.System -> systemDark
        FloentlyThemeMode.Light -> false
        FloentlyThemeMode.Dark -> true
    }
    val basePalette = floentlyPalette(FloentlyProduct.Learn, darkTheme = darkTheme)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = basePalette.primary,
            secondary = basePalette.accent,
            tertiary = basePalette.warning,
            background = basePalette.backgroundBottom,
            surface = basePalette.card,
            onPrimary = basePalette.primary.contrastText(),
            onSecondary = Color(0xFF071124),
            onTertiary = Color(0xFF071124),
            onBackground = basePalette.text,
            onSurface = basePalette.text
        )
    } else {
        lightColorScheme(
            primary = basePalette.primary,
            secondary = basePalette.accent,
            tertiary = basePalette.warning,
            background = basePalette.backgroundBottom,
            surface = basePalette.card,
            onPrimary = basePalette.primary.contrastText(),
            onSecondary = Color.White,
            onTertiary = Color(0xFF071124),
            onBackground = basePalette.text,
            onSurface = basePalette.text
        )
    }

    CompositionLocalProvider(
        LocalFloentlyDarkTheme provides darkTheme,
        LocalFloentlyDesignTokens provides FloentlyDesignTokens()
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@Composable
fun floentlyPalette(product: FloentlyProduct): FloentlyPalette =
    floentlyPalette(product = product, darkTheme = LocalFloentlyDarkTheme.current)

fun floentlyPalette(product: FloentlyProduct, darkTheme: Boolean): FloentlyPalette {
    return if (darkTheme) darkFloentlyPalette(product) else lightFloentlyPalette(product)
}

private fun darkFloentlyPalette(product: FloentlyProduct): FloentlyPalette {
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

private fun lightFloentlyPalette(product: FloentlyProduct): FloentlyPalette {
    return when (product) {
        FloentlyProduct.Learn -> FloentlyPalette(
            backgroundTop = Color(0xFFF6F8FF),
            backgroundBottom = Color(0xFFEAF0FF),
            backgroundGlow = Color(0xFFDCE7FF),
            card = Color(0xFFFFFFFF),
            cardMuted = Color(0xFFF0F4FF),
            cardText = Color(0xFF071124),
            text = Color(0xFF071124),
            muted = Color(0xFF4E607C),
            soft = Color(0xFF7282A0),
            border = Color(0xFFD6E0F5),
            primary = Color(0xFF315DDB),
            accent = Color(0xFF0B8F77),
            warning = Color(0xFF9C6B05)
        )

        FloentlyProduct.Read -> FloentlyPalette(
            backgroundTop = Color(0xFFFFFBF1),
            backgroundBottom = Color(0xFFF6F0DF),
            backgroundGlow = Color(0xFFFFE8A8),
            card = Color(0xFFFFFFFF),
            cardMuted = Color(0xFFFFF7E3),
            cardText = Color(0xFF16120A),
            text = Color(0xFF16120A),
            muted = Color(0xFF665B45),
            soft = Color(0xFF84775E),
            border = Color(0xFFEADDBE),
            primary = Color(0xFF9A6A00),
            accent = Color(0xFF4168D8),
            warning = Color(0xFF9A6A00)
        )

        FloentlyProduct.Create -> FloentlyPalette(
            backgroundTop = Color(0xFFF8F5FF),
            backgroundBottom = Color(0xFFF1EAFF),
            backgroundGlow = Color(0xFFE9D7FF),
            card = Color(0xFFFFFFFF),
            cardMuted = Color(0xFFF7F0FF),
            cardText = Color(0xFF171026),
            text = Color(0xFF171026),
            muted = Color(0xFF62576F),
            soft = Color(0xFF857894),
            border = Color(0xFFE4D9F5),
            primary = Color(0xFF4868D8),
            accent = Color(0xFFC20DA0),
            warning = Color(0xFF9C6B05)
        )
    }
}

@Composable
fun FloentlyScreen(
    product: FloentlyProduct,
    content: @Composable (FloentlyPalette) -> Unit
) {
    val palette = floentlyPalette(product)
    val tokens = LocalFloentlyDesignTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.backgroundTop,
                        palette.backgroundBottom,
                        palette.backgroundGlow.copy(alpha = if (LocalFloentlyDarkTheme.current) 0.38f else 0.30f)
                    )
                )
            )
            .padding(
                horizontal = tokens.screenHorizontalPadding.dp,
                vertical = tokens.screenVerticalPadding.dp
            )
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
    val tokens = LocalFloentlyDesignTokens.current
    Surface(
        color = palette.card,
        contentColor = palette.cardText,
        shape = RoundedCornerShape(tokens.cardRadius.dp),
        border = BorderStroke(tokens.borderWidth.dp, palette.border),
        tonalElevation = if (LocalFloentlyDarkTheme.current) 0.dp else 1.dp,
        shadowElevation = if (LocalFloentlyDarkTheme.current) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.cardSpacing.dp),
            modifier = Modifier.padding(tokens.cardPadding.dp),
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
    val tokens = LocalFloentlyDesignTokens.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = palette.primary.contrastText()
        ),
        shape = RoundedCornerShape(tokens.chipRadius.dp),
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
    val tokens = LocalFloentlyDesignTokens.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.cardMuted,
            contentColor = palette.text
        ),
        shape = RoundedCornerShape(tokens.chipRadius.dp),
        border = BorderStroke(tokens.borderWidth.dp, palette.border),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Black)
    }
}

@Composable
fun FloentlyLogoMark(
    product: FloentlyProduct,
    modifier: Modifier = Modifier
) {
    val palette = floentlyPalette(product)
    Column(modifier = modifier) {
        Text(
            text = "floently",
            color = palette.primary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "FLOENTLY",
            color = palette.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.5.sp
        )
    }
}

private fun Color.contrastText(): Color {
    val luminance = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)
    return if (luminance > 0.55f) Color(0xFF071124) else Color.White
}
