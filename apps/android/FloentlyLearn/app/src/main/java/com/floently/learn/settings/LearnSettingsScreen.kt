package com.floently.learn.settings

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
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnSettingsScreen(
    copy: LearnCopy,
    selectedLanguage: LearnLanguage,
    onLanguageSelected: (LearnLanguage) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = copy.settingsTitle,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.settingsSubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = copy.selectedLanguage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedLanguage.displayLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = copy.languageSaved,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = copy.enabledLanguages,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.futureLanguagesBody,
                    style = MaterialTheme.typography.bodySmall
                )
                LearnLanguage.entries.forEach { language ->
                    FloentlyPrimaryButton(
                        title = language.optionLabel(selectedLanguage),
                        product = FloentlyProduct.Learn,
                        onClick = { onLanguageSelected(language) }
                    )
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = copy.privacySupportTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.privacySupportBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Billing and plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage Learn access, trial status, subscription, checkout, and plan changes from the Account and billing surface. Learn access stays separate from Read and Create.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Mobile release note: Android digital access must use Google Play Billing when live purchases are enabled. Web checkout remains web-only.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Support, privacy, terms, and account deletion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Support: https://learn.floently.com/support",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Privacy Policy: https://learn.floently.com/privacy-policy",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Terms of Use: https://learn.floently.com/terms-of-use",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Account deletion: https://learn.floently.com/account-deletion",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Deletion requests are started from Settings and normally completed within 24 hours, subject to legal retention, fraud prevention, and accounting obligations.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

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

private fun LearnLanguage.optionLabel(selectedLanguage: LearnLanguage): String =
    if (this == selectedLanguage) "$displayLabel ✓" else displayLabel
