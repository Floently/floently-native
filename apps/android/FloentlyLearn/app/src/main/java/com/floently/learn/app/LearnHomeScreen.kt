package com.floently.learn.app

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
                    label = "HYÖDYLLINEN VIHJE",
                    title = "Etsitkö nopeinta etenemistä?",
                    body = "Aloita kortista, kuuntele ääni, avaa vihje vain tarvittaessa ja siirry sitten roolipeliin. Näin vanhan web-kokemuksen ulkoasu säilyy, mutta uusi natiivisovellus lisää paremman ohjauksen.",
                    primary = "Avaa kortit",
                    secondary = "Avaa roolipeli",
                    palette = palette,
                    onPrimary = { onDestinationSelected(LearnFeatureDestination.Cards) },
                    onSecondary = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
                )
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
        }
    }
}

private fun greeting(session: FloentlyAuthSession): String {
    val name = session.user.email.substringBefore("@").split(".", "-", "_").filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "Vitus Idi" }
    return "Hyvää huomenta, $name"
}
