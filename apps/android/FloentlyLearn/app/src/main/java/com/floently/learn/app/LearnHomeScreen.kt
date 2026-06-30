package com.floently.learn.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.learn.i18n.LearnTranslations
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnHomeScreen(
    session: FloentlyAuthSession,
    copy: LearnCopy = LearnTranslations.copy(LearnLanguage.EN),
    onSignOut: () -> Unit,
    onBackToSuite: (() -> Unit)? = null,
    onDestinationSelected: (LearnFeatureDestination) -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Box {
            DashboardGlow(palette = palette)

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DashboardHeader(
                    palette = palette,
                    onHome = {},
                    onMenu = { onDestinationSelected(LearnFeatureDestination.Settings) }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = greeting(session),
                    color = palette.text,
                    style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Kaikki tehty! Valitse seuraavaksi huomiota kaipaava polku.",
                    color = palette.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )

                NextBestStepCard(
                    palette = palette,
                    onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                )

                SectionLabel(text = "POLUT", palette = palette)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth > 420.dp
                    if (isWide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            PathTile(
                                title = "Sanasto ja roolipeli",
                                body = "Kortit, fraasituki ja ohjattu harjoittelu",
                                tag = "Oppiminen",
                                dot = palette.primary,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.Cards) }
                            )
                            PathTile(
                                title = "Työpaikan tilanteet",
                                body = "Ohjeet, vuoronvaihdot ja raportointi",
                                tag = "Tilanteet",
                                dot = palette.warning,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            PathTile(
                                title = "Sanasto ja roolipeli",
                                body = "Kortit, fraasituki ja ohjattu harjoittelu",
                                tag = "Oppiminen",
                                dot = palette.primary,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.Cards) }
                            )
                            PathTile(
                                title = "Työpaikan tilanteet",
                                body = "Ohjeet, vuoronvaihdot ja raportointi",
                                tag = "Tilanteet",
                                dot = palette.warning,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            PathTile(
                                title = "YKI-valmistautuminen",
                                body = "Harjoitukset ja koesimulaatio",
                                tag = "YKI",
                                dot = Color(0xFF9D7CFF),
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                            )
                            PathTile(
                                title = "Kaikki ammatit",
                                body = "Sanasto, haastattelu ja työvalmis suomi",
                                tag = "Ammatti",
                                dot = palette.accent,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                            )
                        }
                    }
                }

                StatsRow(palette = palette)

                WebRouteCard(
                    label = "HYÖDYLLINEN VIHJE",
                    title = "Etsitkö YKI-valmistautumista?",
                    body = "Avaa sivupalkki ja valitse YKI-valmistautuminen, kun haluat virallista koetyötä, tai käytä Työpaikan tilanteita, kun haluat puheviestintää työtilanteissa.",
                    primary = "Vie minut sinne",
                    secondary = "Sulje",
                    palette = palette,
                    onPrimary = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                )

                WebRouteCard(
                    label = "TILI JA KÄYTTÖOIKEUDET",
                    title = "Floently Learn",
                    body = "Kirjautuneena: ${session.user.email}. Learn, Read ja Create pidetään erillään, mutta oppimisen polut pysyvät yhdessä näkymässä.",
                    primary = "Avaa tili",
                    secondary = "Asetukset",
                    palette = palette,
                    onPrimary = { onDestinationSelected(LearnFeatureDestination.Account) },
                    onSecondary = { onDestinationSelected(LearnFeatureDestination.Settings) }
                )

                FloentlyPrimaryButton(
                    title = copy.signOut,
                    product = FloentlyProduct.Learn,
                    onClick = onSignOut
                )

                onBackToSuite?.let { back ->
                    FloentlyPrimaryButton(
                        title = copy.backToProducts,
                        product = FloentlyProduct.Learn,
                        onClick = back
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    palette: FloentlyPalette,
    onHome: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "floently",
                color = Color(0xFF18B9FF),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "FLOENTLY",
                color = palette.primary,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HeaderPill(text = "Home", palette = palette, selected = false, onClick = onHome)
        Spacer(modifier = Modifier.width(10.dp))
        HeaderPill(text = "Menu", palette = palette, selected = true, onClick = onMenu)
    }
}

@Composable
private fun HeaderPill(
    text: String,
    palette: FloentlyPalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF183168) else Color(0xFF111B30),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF355AA0) else palette.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = palette.primary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun NextBestStepCard(
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF13264A),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFF264170)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(text = "TYÖELÄMÄN SUOMI", palette = palette)
                    Spacer(modifier = Modifier.width(10.dp))
                    StatusPill(text = "• Vire päällä", palette = palette)
                }
                Text(
                    text = "Seuraava paras askel",
                    color = palette.text,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Bundle-polku käytössä — jatka tänään YKI-valmistautumista tai ammattipolkuasi.",
                    color = palette.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(onClick = onClick)
                ) {
                    Text(
                        text = "Jatka polkua →",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(9.dp, palette.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "–", color = palette.muted, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PathTile(
    title: String,
    body: String,
    tag: String,
    dot: Color,
    palette: FloentlyPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF101B2F),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF223653)),
        modifier = modifier.aspectRatio(0.9f).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(dot.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(13.dp).clip(CircleShape).background(dot))
            }
            Text(
                text = title,
                color = palette.text,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = body,
                color = palette.muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = dot.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = tag,
                    color = dot,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontWeight = FontWeight.Black,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun StatsRow(palette: FloentlyPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatBox(value = "0", label = "Sanastokohteet", palette = palette, modifier = Modifier.weight(1f))
        StatBox(value = "1", label = "Päiväputki", palette = palette, modifier = Modifier.weight(1f))
        StatBox(value = "B1", label = "Arvioitu taso", palette = palette, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(value: String, label: String, palette: FloentlyPalette, modifier: Modifier) {
    Surface(
        color = Color(0xFF101B2F),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF223653)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = palette.text,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = palette.muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WebRouteCard(
    label: String,
    title: String,
    body: String,
    primary: String,
    secondary: String,
    palette: FloentlyPalette,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null
) {
    Surface(
        color = Color(0xFF13264A),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color(0xFF2A4372)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionLabel(text = label, palette = palette)
            Text(
                text = title,
                color = palette.text,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = body,
                color = palette.muted,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFF355AA0)),
                    modifier = Modifier.weight(1f).clickable { onSecondary?.invoke() }
                ) {
                    Text(
                        text = secondary,
                        color = palette.text,
                        modifier = Modifier.padding(vertical = 13.dp),
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f).clickable(onClick = onPrimary)
                ) {
                    Text(
                        text = primary,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 13.dp),
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, palette: FloentlyPalette) {
    Text(
        text = text,
        color = palette.accent,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 4.sp
    )
}

@Composable
private fun StatusPill(text: String, palette: FloentlyPalette) {
    Surface(
        color = Color(0xFF103B3C),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF1D7671))
    ) {
        Text(
            text = text,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Black,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun DashboardGlow(palette: FloentlyPalette) {
    Box(
        modifier = Modifier
            .size(260.dp)
            .offset(x = (-120).dp, y = (-80).dp)
            .clip(CircleShape)
            .background(palette.backgroundGlow.copy(alpha = 0.30f))
    )
}

private fun greeting(session: FloentlyAuthSession): String {
    val name = session.user.email.substringBefore("@").split(".", "-", "_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        .ifBlank { "Vitus Idi" }

    return "Hyvää huomenta, $name"
}
