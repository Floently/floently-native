package com.floently.learn.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.learn.i18n.LearnTranslations
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
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
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = copy.appTitle,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = copy.appSubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = copy.welcome(session.user.email.substringBefore("@")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.homeBody,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = copy.recommendedNextStep,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FloentlyPrimaryButton(
                    title = copy.continueRoleplay,
                    product = FloentlyProduct.Learn,
                    onClick = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
                )
            }

            Text(
                text = copy.learnAreas,
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            LearnFeatureDestination.primary.forEach { destination ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = destination.title(copy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = destination.subtitle(copy),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = destination.releaseMessage(copy),
                        style = MaterialTheme.typography.bodySmall
                    )
                    FloentlyPrimaryButton(
                        title = destination.actionTitle(copy),
                        product = FloentlyProduct.Learn,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "${copy.settingsTitle} & ${copy.accountTitle}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.signedIn(session.user.email),
                    style = MaterialTheme.typography.bodyMedium
                )
                FloentlyPrimaryButton(
                    title = copy.settingsAction,
                    product = FloentlyProduct.Learn,
                    onClick = { onDestinationSelected(LearnFeatureDestination.Settings) }
                )
                FloentlyPrimaryButton(
                    title = copy.openAccount,
                    product = FloentlyProduct.Learn,
                    onClick = { onDestinationSelected(LearnFeatureDestination.Account) }
                )
                FloentlyPrimaryButton(
                    title = copy.signOut,
                    product = FloentlyProduct.Learn,
                    onClick = onSignOut
                )
            }

            onBackToSuite?.let { back ->
                FloentlyPrimaryButton(
                    title = copy.backToProducts,
                    product = FloentlyProduct.Learn,
                    onClick = back
                )
            }
        }
    }
}

private fun LearnFeatureDestination.title(copy: LearnCopy): String = when (this) {
    LearnFeatureDestination.YkiPractice -> copy.ykiTitle
    LearnFeatureDestination.ProfessionalFinnish -> copy.professionalTitle
    LearnFeatureDestination.Roleplay -> copy.roleplayTitle
    LearnFeatureDestination.Cards -> copy.cardsTitle
    LearnFeatureDestination.Progress -> copy.progressTitle
    LearnFeatureDestination.Settings -> copy.settingsTitle
    LearnFeatureDestination.Account -> copy.accountTitle
}

private fun LearnFeatureDestination.subtitle(copy: LearnCopy): String = when (this) {
    LearnFeatureDestination.YkiPractice -> copy.ykiSubtitle
    LearnFeatureDestination.ProfessionalFinnish -> copy.professionalSubtitle
    LearnFeatureDestination.Roleplay -> copy.roleplaySubtitle
    LearnFeatureDestination.Cards -> copy.cardsSubtitle
    LearnFeatureDestination.Progress -> copy.progressSubtitle.replace("{count}", "3")
    LearnFeatureDestination.Settings -> copy.settingsSubtitle
    LearnFeatureDestination.Account -> copy.signedInTemplate.replace("{email}", "")
}

private fun LearnFeatureDestination.actionTitle(copy: LearnCopy): String = when (this) {
    LearnFeatureDestination.YkiPractice -> copy.ykiAction
    LearnFeatureDestination.ProfessionalFinnish -> copy.professionalAction
    LearnFeatureDestination.Roleplay -> copy.roleplayAction
    LearnFeatureDestination.Cards -> copy.cardsAction
    LearnFeatureDestination.Progress -> copy.progressAction
    LearnFeatureDestination.Settings -> copy.settingsAction
    LearnFeatureDestination.Account -> copy.openAccount
}

private fun LearnFeatureDestination.releaseMessage(copy: LearnCopy): String = when (this) {
    LearnFeatureDestination.YkiPractice -> copy.ykiMessage
    LearnFeatureDestination.ProfessionalFinnish -> copy.professionalMessage
    LearnFeatureDestination.Roleplay -> copy.roleplayMessage
    LearnFeatureDestination.Cards -> copy.cardsMessage
    LearnFeatureDestination.Progress -> copy.progressMessage.replace("{count}", "3")
    LearnFeatureDestination.Settings -> copy.settingsMessage
    LearnFeatureDestination.Account -> copy.accountTitle
}
