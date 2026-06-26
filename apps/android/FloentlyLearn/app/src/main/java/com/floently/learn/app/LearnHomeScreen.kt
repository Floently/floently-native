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
    onDestinationSelected: (LearnFeatureDestination) -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Native foundation status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All core Learn destinations now open into native screens or native service boundaries. Remaining work is parity wiring, durable sync, and production data.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LearnFeatureDestination.primary.forEach { destination ->
                val readiness = PreviewLearnFeatureReadiness.items[destination]
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
                    readiness?.let { item ->
                        Text(
                            text = "Native status: ${item.status.name}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = item.nativeSummary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Remaining gate: ${item.remainingGate}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        item.verifiedCommit?.let { commit ->
                            Text(
                                text = "Verified through: $commit",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } ?: Text(
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
