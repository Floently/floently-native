package com.floently.learn.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.design.FloentlyThemeMode

@Composable
fun LearnSettingsScreen(
    copy: LearnCopy,
    selectedLanguage: LearnLanguage,
    onLanguageSelected: (LearnLanguage) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    themeMode: FloentlyThemeMode = FloentlyThemeMode.System,
    onThemeModeChange: (FloentlyThemeMode) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Settings",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Change only the things that make your learning smoother: speed, appearance, profile, and support.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            SettingsRouteHeader(palette = palette)
            SettingsProfileCard(palette = palette)
            SettingsPathwayCard(palette = palette)

            SettingsLanguageCard(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                palette = palette
            )

            SettingsInfoCard(
                badge = "ACCESS",
                title = "Learn stays separate",
                body = "Floently Learn, Read and Create Studio are checked as separate access rights. The account can be shared, but access and payments must not mix unless a bundle is added.",
                accent = palette.primary,
                palette = palette
            )

            SettingsInfoCard(
                badge = "BILLING",
                title = "Mobile payment boundary",
                body = "When Android digital purchases are enabled, they must go through Google Play Billing. Web checkout stays on the web.",
                accent = palette.warning,
                palette = palette
            )

            SettingsSupportCard(
                palette = palette,
                onOpenUrl = { url -> uriHandler.openUri(url) }
            )

            FloentlyPrimaryButton(
                title = copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onBack
            )
            FloentlyPrimaryButton(
                title = copy.signOut,
                product = FloentlyProduct.Learn,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun SettingsRouteHeader(
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PREFERENCES",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Settings",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Change only the things that make your learning smoother: speed, appearance, profile, and support.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTinyPill("Product", "Learn", palette.primary)
                SettingsTinyPill("Mode", "Native", palette.accent)
            }
        }
    }
}

@Composable
private fun SettingsTinyPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
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
private fun SettingsProfileCard(
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF5A85FF),
        shape = RoundedCornerShape(34.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "floently",
                            color = Color(0xFF2459D6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Choose from gallery",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
            Text(
                text = "Vitus Idi",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "vitus.idi@floently.com",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 18.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Use initials",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp)
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Use logo",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPathwayCard(
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "My pathway",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Track your current goal, pathway, and access model.",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            SettingsPathwayRow("MAIN GOAL", "YKI, work, and life in Finland", palette)
            SettingsPathwayRow("CURRENT PROFESSION", "doctor, nurse, practical nurse", palette)
            SettingsPathwayRow("CURRENT PATHWAY", "Professional Pathway · doctor", palette)
            SettingsPathwayRow("ACCESS TYPE", "Individual", palette)
        }
    }
}

@Composable
private fun SettingsPathwayRow(
    label: String,
    value: String,
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF172541),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.4.sp
            )
            Text(
                text = value,
                color = palette.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
@Composable
private fun SettingsLanguageCard(
    selectedLanguage: LearnLanguage,
    onLanguageSelected: (LearnLanguage) -> Unit,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Language",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Selected language: ${selectedLanguage.displayLabel}",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )

            LearnLanguage.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { language ->
                        SettingsLanguageChip(
                            language = language,
                            selected = language == selectedLanguage,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { onLanguageSelected(language) }
                        )
                    }
                    repeat(2 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Text(
                text = "Language selection is saved on this device and stays inside Learn.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsLanguageChip(
    language: LearnLanguage,
    selected: Boolean,
    palette: FloentlyPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) palette.primary.copy(alpha = 0.18f) else palette.cardMuted,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) palette.primary else palette.border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = if (selected) "${language.displayLabel} ✓" else language.displayLabel,
            color = if (selected) palette.primary else palette.muted,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsInfoCard(
    badge: String,
    title: String,
    body: String,
    accent: Color,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = badge,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Text(
                text = title,
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = body,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsSupportCard(
    palette: FloentlyPalette,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "Tuki, tietosuoja ja poistopyyntö",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            SettingsLinkRow("Tuki", "https://learn.floently.com/support", palette, onOpenUrl)
            SettingsLinkRow("Tietosuojaseloste", "https://learn.floently.com/privacy-policy", palette, onOpenUrl)
            SettingsLinkRow("Käyttöehdot", "https://learn.floently.com/terms-of-use", palette, onOpenUrl)
            SettingsLinkRow("Tilin poistaminen", "https://learn.floently.com/account-deletion", palette, onOpenUrl)
            Text(
                text = "Poistopyyntö aloitetaan asetuksista ja käsitellään normaalisti 24 tunnin sisällä, ellei laki, väärinkäytösten ehkäisy tai kirjanpito edellytä säilytystä.",
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    value: String,
    palette: FloentlyPalette,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(value) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
            Text(
                text = value,
                color = palette.text,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Avaa linkki",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
        }
    }
}
