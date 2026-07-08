package com.floently.learn.everyday

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

private data class EverydayEntry(
    val title: String,
    val body: String,
    val sourceChip: String,
    val action: String,
    val accent: Color,
    val destination: LearnFeatureDestination
)

@Composable
fun EverydayFinnishScreen(
    copy: LearnCopy,
    onBack: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        var selectedBand by remember { mutableStateOf("A1-A2") }
        val entries = listOf(
            EverydayEntry(
                title = "Flashcards",
                body = "Open the shared general-language flashcards for everyday Finnish, work readiness, and YKI-linked fluency.",
                sourceChip = "General flashcards",
                action = "Open flashcards",
                accent = palette.primary,
                destination = LearnFeatureDestination.Cards
            ),
            EverydayEntry(
                title = "Daily roleplay",
                body = "Open the shared general conversation route. This stays connected to the everyday or YKI flow, not the workplace-specific profession roleplay.",
                sourceChip = "Everyday roleplay",
                action = "Open roleplay",
                accent = palette.accent,
                destination = LearnFeatureDestination.Roleplay
            )
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EverydayTopBar(onBack = onBack, palette = palette)

            Text(
                text = "EVERYDAY FINNISH",
                color = palette.soft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = copy.everydayTitle,
                color = palette.text,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Choose the shared daily-language branch first, then open only the general flashcards or the YKI-linked daily roleplay from here.",
                color = palette.muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            EverydayLevelSelector(
                selectedBand = selectedBand,
                onBandSelected = { selectedBand = it },
                palette = palette
            )

            entries.forEach { entry ->
                EverydayEntryCard(
                    entry = entry,
                    selectedBand = selectedBand,
                    palette = palette,
                    onClick = { onDestinationSelected(entry.destination) }
                )
            }
        }
    }
}

@Composable
private fun EverydayTopBar(
    onBack: () -> Unit,
    palette: FloentlyPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0xFF324777)),
            modifier = Modifier
                .height(34.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = Color(0xFFB8C8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = Color(0xFF223763),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0xFF324C82))
        ) {
            Text(
                text = "Everyday Finnish",
                color = palette.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun EverydayLevelSelector(
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF111E39),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF263B68)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LEVEL",
                color = palette.soft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.7.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("A1-A2", "B1-B2", "C1-C2").forEach { level ->
                    val active = level == selectedBand
                    Surface(
                        color = if (active) Color(0xFF8D64FF) else Color(0xFF1A2A4A),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (active) Color(0xFFA98BFF) else Color(0xFF304669)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBandSelected(level) }
                    ) {
                        Text(
                            text = level,
                            color = if (active) Color.White else Color(0xFF9FAECC),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }
            Text(
                text = "$selectedBand daily Finnish path selected.",
                color = palette.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun EverydayEntryCard(
    entry: EverydayEntry,
    selectedBand: String,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF263D6C)),
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
                        .size(30.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(entry.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(entry.accent)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = entry.title,
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = entry.body,
                color = palette.muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EverydayChip(text = entry.sourceChip, color = entry.accent)
                EverydayChip(text = selectedBand, color = palette.primary)
            }

            Surface(
                color = entry.accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, entry.accent.copy(alpha = 0.55f))
            ) {
                Text(
                    text = entry.action,
                    color = entry.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EverydayChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
