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
import com.floently.learn.i18n.LearnTranslationStatus
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
                    text = "All ${LearnLanguage.entries.size} old Learn language options are preserved here. Finnish, Swedish, and English have native UI copy now; other selected languages remain available and fall back safely while full translations are completed.",
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
                    text = copy.futureLanguages,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = copy.futureLanguagesBody,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = LearnLanguage.reviewLanguages.joinToString { it.displayLabel },
                    style = MaterialTheme.typography.bodySmall
                )
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

private fun LearnLanguage.optionLabel(selectedLanguage: LearnLanguage): String {
    val selectedText = if (this == selectedLanguage) " selected" else ""
    val statusText = when (translationStatus) {
        LearnTranslationStatus.Complete -> "complete"
        LearnTranslationStatus.Fallback -> "fallback"
        LearnTranslationStatus.InProgress -> "in progress"
    }
    return "$displayLabel - $statusText$selectedText"
}
