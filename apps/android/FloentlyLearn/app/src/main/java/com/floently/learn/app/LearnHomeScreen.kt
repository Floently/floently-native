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
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnHomeScreen(
    session: FloentlyAuthSession,
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
                text = "Floently Learn",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Practice Finnish for real life, work, YKI, and everyday conversations.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Welcome back, ${session.user.email.substringBefore("@")}.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose one focused practice area and keep moving. Learn is now the first release target, so these screens stay clean, stable, and close to the original Floently learning experience.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Recommended next step: conversation practice",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FloentlyPrimaryButton(
                    title = "Continue with Roleplay",
                    product = FloentlyProduct.Learn,
                    onClick = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
                )
            }

            Text(
                text = "Learn areas",
                color = palette.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            LearnFeatureDestination.primary.forEach { destination ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = destination.subtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = destination.releaseMessage(),
                        style = MaterialTheme.typography.bodySmall
                    )
                    FloentlyPrimaryButton(
                        title = destination.actionTitle(),
                        product = FloentlyProduct.Learn,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Signed in as ${session.user.email}. Manage your account or sign out when you are done.",
                    style = MaterialTheme.typography.bodyMedium
                )
                FloentlyPrimaryButton(
                    title = "Open account",
                    product = FloentlyProduct.Learn,
                    onClick = { onDestinationSelected(LearnFeatureDestination.Account) }
                )
                FloentlyPrimaryButton(
                    title = "Sign out",
                    product = FloentlyProduct.Learn,
                    onClick = onSignOut
                )
            }

            onBackToSuite?.let { back ->
                FloentlyPrimaryButton(
                    title = "Back to Floently products",
                    product = FloentlyProduct.Learn,
                    onClick = back
                )
            }
        }
    }
}

private fun LearnFeatureDestination.actionTitle(): String = when (this) {
    LearnFeatureDestination.YkiPractice -> "Practice YKI"
    LearnFeatureDestination.ProfessionalFinnish -> "Practice work Finnish"
    LearnFeatureDestination.Roleplay -> "Start roleplay"
    LearnFeatureDestination.Cards -> "Review cards"
    LearnFeatureDestination.Progress -> "View progress"
    LearnFeatureDestination.Account -> "Open account"
}

private fun LearnFeatureDestination.releaseMessage(): String = when (this) {
    LearnFeatureDestination.YkiPractice -> "Exam-style tasks for structured practice and store-ready Learn release polish."
    LearnFeatureDestination.ProfessionalFinnish -> "Workplace Finnish for interviews, meetings, messages, and professional confidence."
    LearnFeatureDestination.Roleplay -> "Dynamic conversation practice with coaching and anti-repetition verification before release."
    LearnFeatureDestination.Cards -> "Fast vocabulary and sentence review for daily retention."
    LearnFeatureDestination.Progress -> "Track streaks, activity, and learning progress."
    LearnFeatureDestination.Account -> "Profile, access, and sign-out controls."
}
