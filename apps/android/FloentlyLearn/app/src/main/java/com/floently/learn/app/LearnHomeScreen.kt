package com.floently.learn.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onDestinationSelected: (LearnFeatureDestination) -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Floently Learn",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
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
                        text = "Release guard: ${destination.releaseGuard.name}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    FloentlyPrimaryButton(
                        title = "Open",
                        product = FloentlyProduct.Learn,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
            }

            FloentlyPrimaryButton(
                title = "Sign out",
                product = FloentlyProduct.Learn,
                onClick = onSignOut
            )
        }
    }
}
