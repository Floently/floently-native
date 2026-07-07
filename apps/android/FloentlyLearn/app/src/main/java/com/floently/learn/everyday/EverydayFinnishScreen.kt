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
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun EverydayFinnishScreen(
    copy: LearnCopy,
    onBack: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var selectedBand by remember { mutableStateOf("A1-A2") }

            Text(
                text = copy.everydayTitle,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = copy.everydaySubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            EverydayOldSourceHero(copy = copy, palette = palette)

            EverydayOldSourceLevelRail(selectedBand = selectedBand, onBandSelected = { selectedBand = it }, copy = copy, palette = palette)

            EverydaySectionLabel(copy.everydayChoosePractice, palette)

            EverydayOldSourceEntryCard(
                label = "CARDS",
                title = "Cards",
                body = copy.everydayCardsBody,
                accent = palette.primary,
                palette = palette,
                copy = copy,
                onClick = { onDestinationSelected(LearnFeatureDestination.Cards) }
            )

            EverydayOldSourceEntryCard(
                label = "ROLEPLAY",
                title = "Roleplay",
                body = copy.everydayRoleplayBody,
                accent = palette.accent,
                palette = palette,
                copy = copy,
                onClick = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
            )

            FloentlyPrimaryButton(
                title = "Back to Learn",
                product = FloentlyProduct.Learn,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun EverydayOldSourceHero(
    copy: LearnCopy,
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.primary)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = copy.everydayHeroEyebrow,
                    color = palette.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.2.sp
                )
            }

            Text(
                text = copy.everydayHeroTitle,
                color = palette.text,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = copy.everydayHeroBody,
                color = palette.muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun EverydayOldSourceLevelRail(
    selectedBand: String,
    onBandSelected: (String) -> Unit,
    copy: LearnCopy,
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF0F1B33),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF22365F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = copy.everydayLevelEyebrow,
                color = palette.soft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("A1-A2", "B1-B2", "C1-C2").forEach { level ->
                    Surface(
                        color = if (level == selectedBand) palette.primary.copy(alpha = 0.22f) else palette.cardMuted,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (level == selectedBand) palette.primary else palette.border),
                        modifier = Modifier.weight(1f).clickable { onBandSelected(level) }
                    ) {
                        Text(
                            text = level,
                            color = if (level == selectedBand) palette.primary else palette.muted,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EverydaySectionLabel(
    text: String,
    palette: FloentlyPalette
) {
    Text(
        text = text,
        color = palette.soft,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.4.sp,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun EverydayOldSourceEntryCard(
    label: String,
    title: String,
    body: String,
    accent: Color,
    palette: FloentlyPalette,
    copy: LearnCopy,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
            ) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Text(
                text = title,
                color = palette.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = body,
                color = palette.muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Text(
                text = copy.everydayOpenTemplate.replace("{title}", title),
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
