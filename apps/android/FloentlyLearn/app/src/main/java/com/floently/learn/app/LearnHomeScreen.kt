package com.floently.learn.app

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.learn.i18n.LearnTranslations
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.design.FloentlySecondaryButton

@Composable
fun LearnHomeScreen(
    session: FloentlyAuthSession,
    copy: LearnCopy = LearnTranslations.copy(LearnLanguage.EN),
    onSignOut: () -> Unit,
    onBackToSuite: (() -> Unit)? = null,
    onDestinationSelected: (LearnFeatureDestination) -> Unit = {}
) {
    var showOldAppHomeHint by remember(session.user.email) { mutableStateOf(true) }
    var showOldAppDrawer by remember(session.user.email) { mutableStateOf(false) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Box {
            OldAppEmberBackground(palette = palette)
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                WebChromeHeader(palette = palette, onHome = {}, onMenu = { showOldAppDrawer = true })
                Spacer(modifier = Modifier.height(28.dp))
                Text(greeting(session), color = palette.text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Kaikki tehty! Valitse seuraavaksi huomiota kaipaava polku.", color = palette.muted, style = MaterialTheme.typography.titleMedium)
                OldAppReadinessPillars(palette = palette)
                WebHeroCard(
                    label = "TYÖELÄMÄN SUOMI",
                    title = "Seuraava paras askel",
                    body = "Bundle-polku käytössä — jatka tänään YKI-valmistautumista, sanastoa tai ammatillista suomea yhdestä näkymästä.",
                    action = "Jatka polkua →",
                    progressText = "–",
                    palette = palette,
                    onAction = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                )
                WebSectionLabel(text = "PIKA-ALOITUS", palette = palette)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    WebPathTile("Sanasto ja roolipeli", "Kortit, fraasituki, kuuntelu ja ohjattu harjoittelu", "Oppiminen", palette.primary, palette, Modifier.weight(1f)) { onDestinationSelected(LearnFeatureDestination.Cards) }
                    WebPathTile("Työpaikan tilanteet", "Ohjeet, vuoronvaihdot, raportointi ja hoitotyön tilanteet", "Tilanteet", palette.warning, palette, Modifier.weight(1f)) { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    WebPathTile("YKI-valmistautuminen", "Koesimulaatio, kirjoittaminen, puhuminen ja virallinen harjoittelu", "YKI", Color(0xFF9D7CFF), palette, Modifier.weight(1f)) { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                    WebPathTile("Roolipeli", "Keskustelut, anti-repetition coaching ja beginner-safe vastaukset", "Puhe", palette.accent, palette, Modifier.weight(1f)) { onDestinationSelected(LearnFeatureDestination.Roleplay) }
                }
                WebStatRow(palette = palette)
                WebInfoCard(
                    label = "OPAS",
                    title = "Valitse harjoituspolku",
                    body = "Avaa vihje, jos haluat nopean suunnan. Avaa Menu, kun haluat vaihtaa kortteihin, YKI-harjoitteluun, työpaikan tilanteisiin, tiliin tai asetuksiin.",
                    primary = "Avaa vihje",
                    secondary = "Avaa Menu",
                    palette = palette,
                    onPrimary = { showOldAppHomeHint = true },
                    onSecondary = { showOldAppDrawer = true }
                )
                FloentlyPrimaryButton(title = copy.signOut, product = FloentlyProduct.Learn, onClick = onSignOut)
                onBackToSuite?.let { back -> FloentlySecondaryButton(title = copy.backToProducts, product = FloentlyProduct.Learn, onClick = back) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            OldAppHomeHintPopup(
                visible = showOldAppHomeHint,
                badgeLabel = "Hyödyllinen vihje",
                title = "Etsitkö YKI-valmistautumista?",
                body = "Avaa sivupalkki ja valitse YKI-valmistautuminen, kun haluat virallista koetyötä, tai käytä Työpaikan tilanteita, kun haluat puheviestintää työtilanteissa.",
                primaryLabel = "Vie minut sinne",
                secondaryLabel = "Sulje",
                onPrimary = {
                    showOldAppHomeHint = false
                    onDestinationSelected(LearnFeatureDestination.YkiPractice)
                },
                onSecondary = { showOldAppHomeHint = false }
            )
            OldAppUtilityDrawer(
                visible = showOldAppDrawer,
                email = session.user.email,
                onClose = { showOldAppDrawer = false },
                onDestinationSelected = { destination ->
                    showOldAppDrawer = false
                    onDestinationSelected(destination)
                },
                onSignOut = {
                    showOldAppDrawer = false
                    onSignOut()
                }
            )
        }
    }
}

@Composable
private fun OldAppEmberBackground(palette: com.floently.shared.design.FloentlyPalette) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (-150).dp, y = (-120).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = 30.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size((10 + index * 3).dp)
                    .offset(x = (34 + index * 54).dp, y = (122 + index * 41).dp)
                    .clip(CircleShape)
                    .background(
                        when (index % 3) {
                            0 -> palette.primary.copy(alpha = 0.22f)
                            1 -> palette.accent.copy(alpha = 0.18f)
                            else -> palette.warning.copy(alpha = 0.18f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun OldAppReadinessPillars(
    palette: com.floently.shared.design.FloentlyPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OldAppReadinessPillar("Kuuntele", "Ääni", palette.primary, palette, Modifier.weight(1f))
        OldAppReadinessPillar("Puhu", "Rooli", palette.accent, palette, Modifier.weight(1f))
        OldAppReadinessPillar("Kirjoita", "YKI", palette.warning, palette, Modifier.weight(1f))
    }
}

@Composable
private fun OldAppReadinessPillar(
    title: String,
    label: String,
    color: Color,
    palette: com.floently.shared.design.FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = Color(0xFF0D1D42),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = title,
                color = palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
private fun OldAppUtilityDrawer(
    visible: Boolean,
    email: String,
    onClose: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit,
    onSignOut: () -> Unit
) {
    if (!visible) return

    val surface = Color(0xFF0D1D42)
    val surfaceRaised = Color(0xFF112346)
    val border = Color(0xFF263B6B)
    val text = Color(0xFFF5F9FF)
    val muted = Color(0xFFA8BAD6)
    val primary = Color(0xFF5A85FF)
    val accent = Color(0xFF3EC5A8)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xA3040A18))
                    .clickable(onClick = onClose)
            )

            Surface(
                color = surface,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(326.dp)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "floently",
                                color = Color(0xFF18B9FF),
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "LEARN",
                                color = primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                        }
                        Surface(
                            color = surfaceRaised,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, border),
                            modifier = Modifier.clickable(onClick = onClose)
                        ) {
                            Text(
                                text = "Sulje",
                                color = text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(
                        color = surfaceRaised,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Kirjautunut", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                            Text(email, color = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Päiväputki 1 - arvioitu taso B1", color = muted, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Harjoittelu",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    OldAppDrawerItem("Sanasto ja kortit", "Kortit, fraasit ja kertaus", primary) { onDestinationSelected(LearnFeatureDestination.Cards) }
                    OldAppDrawerItem("Työpaikan tilanteet", "Ammatillinen suomi", Color(0xFFE8B65E)) { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                    OldAppDrawerItem("YKI-valmistautuminen", "Koe, kirjoittaminen ja puhuminen", Color(0xFF9D7CFF)) { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                    OldAppDrawerItem("Roolipeli", "Ohjattu keskustelu", accent) { onDestinationSelected(LearnFeatureDestination.Roleplay) }

                    Text(
                        text = "Tili",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    OldAppDrawerItem("Tili", "Käyttöoikeudet ja profiili", muted) { onDestinationSelected(LearnFeatureDestination.Account) }
                    OldAppDrawerItem("Asetukset", "Kieli ja sovellusasetukset", muted) { onDestinationSelected(LearnFeatureDestination.Settings) }
                    OldAppDrawerItem("Kirjaudu ulos", "Lopeta istunto", Color(0xFFFF7A7A), onSignOut)
                }
            }
        }
    }
}

@Composable
private fun OldAppDrawerItem(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF112346),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF263B6B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFF5F9FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFA8BAD6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun OldAppHomeHintPopup(
    visible: Boolean,
    badgeLabel: String,
    title: String,
    body: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    if (!visible) return

    val overlay = Color(0x9E040A18)
    val surface = Color(0xFF112346)
    val border = Color(0xFF263B6B)
    val borderStrong = Color(0xFF36508A)
    val text = Color(0xFFF5F9FF)
    val textMuted = Color(0xFFA8BAD6)
    val primary = Color(0xFF5A85FF)
    val primarySurface = Color(0xFF1B2C5D)

    Dialog(
        onDismissRequest = onSecondary,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlay)
                    .clickable(onClick = onSecondary)
            )

            Surface(
                color = surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = primarySurface,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badgeLabel.uppercase(),
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = title,
                        color = text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = body,
                        color = textMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = surface,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, borderStrong),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable(onClick = onSecondary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = secondaryLabel,
                                    color = text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = primary,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .clickable(onClick = onPrimary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = primaryLabel,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun greeting(session: FloentlyAuthSession): String {
    val name = session.user.email.substringBefore("@").split(".", "-", "_").filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "Vitus Idi" }
    return "Hyvää huomenta, $name"
}
