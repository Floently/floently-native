package com.floently.learn.app

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

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Box {
            WebBackgroundGlow(palette = palette)
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                WebChromeHeader(palette = palette, onHome = {}, onMenu = { onDestinationSelected(LearnFeatureDestination.Settings) })
                Spacer(modifier = Modifier.height(28.dp))
                Text(greeting(session), color = palette.text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Kaikki tehty! Valitse seuraavaksi huomiota kaipaava polku.", color = palette.muted, style = MaterialTheme.typography.titleMedium)
                WebHeroCard(
                    label = "TYÖELÄMÄN SUOMI",
                    title = "Seuraava paras askel",
                    body = "Bundle-polku käytössä — jatka tänään YKI-valmistautumista, sanastoa tai ammatillista suomea yhdestä näkymästä.",
                    action = "Jatka polkua →",
                    progressText = "–",
                    palette = palette,
                    onAction = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                )
                WebSectionLabel(text = "POLUT", palette = palette)
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
                    label = "TILI JA KÄYTTÖOIKEUDET",
                    title = "Floently Learn",
                    body = "Kirjautuneena: ${session.user.email}. Learn, Read ja Create pidetään erillään, mutta Learn-polut pysyvät yhdessä web-tyylisessä näkymässä.",
                    primary = "Avaa tili",
                    secondary = "Asetukset",
                    palette = palette,
                    onPrimary = { onDestinationSelected(LearnFeatureDestination.Account) },
                    onSecondary = { onDestinationSelected(LearnFeatureDestination.Settings) }
                )
                FloentlyPrimaryButton(title = copy.signOut, product = FloentlyProduct.Learn, onClick = onSignOut)
                onBackToSuite?.let { back -> FloentlySecondaryButton(title = copy.backToProducts, product = FloentlyProduct.Learn, onClick = back) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            OldAppHomeHintPopup(
                visible = showOldAppHomeHint,
                badgeLabel = "HYÖDYLLINEN VIHJE",
                title = "Etsitkö YKI-valmistautumista?",
                body = "YKI-polku on valmiina harjoitteluun. Siirry kokeeseen, kirjoittamiseen ja puhumiseen yhdestä paikasta.",
                primaryLabel = "Vie minut sinne",
                secondaryLabel = "Sulje",
                onPrimary = {
                    showOldAppHomeHint = false
                    onDestinationSelected(LearnFeatureDestination.YkiPractice)
                },
                onSecondary = { showOldAppHomeHint = false }
            )
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
                            text = badgeLabel,
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
