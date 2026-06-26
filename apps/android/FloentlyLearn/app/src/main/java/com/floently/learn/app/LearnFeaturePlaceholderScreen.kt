package com.floently.learn.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnFeaturePlaceholderScreen(
    destination: LearnFeatureDestination,
    onBack: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = destination.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Native rebuild shell",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "This screen is intentionally guarded. It is not release-ready until the rebuilt native feature matches the existing Learn product behavior, animations, access rules, and user flow.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Guard: ${destination.releaseGuard.name}",
                    style = MaterialTheme.typography.labelMedium
                )
                FloentlyPrimaryButton(
                    title = "Back to Learn",
                    product = FloentlyProduct.Learn,
                    onClick = onBack
                )
            }
        }
    }
}
