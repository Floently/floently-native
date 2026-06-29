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

data class LearnSmartHelperAction(
    val title: String,
    val body: String,
    val destination: LearnFeatureDestination? = null
)

@Composable
fun LearnSmartHelperCard(
    title: String,
    body: String,
    actions: List<LearnSmartHelperAction>,
    product: FloentlyProduct = FloentlyProduct.Learn,
    onOpenDestination: (LearnFeatureDestination) -> Unit = {}
) {
    FloentlyCard(product = product) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            actions.forEach { action ->
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = action.body, style = MaterialTheme.typography.bodySmall)
                val destination = action.destination
                if (destination != null) {
                    FloentlyPrimaryButton(
                        title = "Open ${action.title}",
                        product = product,
                        onClick = { onOpenDestination(destination) }
                    )
                }
            }
        }
    }
}

fun learnHomeSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Cards",
        body = "Use a short card bank when you want a quick vocabulary or sentence review.",
        destination = LearnFeatureDestination.Cards
    ),
    LearnSmartHelperAction(
        title = "Roleplay",
        body = "Open roleplay when you need spoken workplace Finnish and practical dialogue flow.",
        destination = LearnFeatureDestination.Roleplay
    ),
    LearnSmartHelperAction(
        title = "Progress",
        body = "Check progress when you need the next recommended learning direction.",
        destination = LearnFeatureDestination.Progress
    )
)

fun learnSettingsSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Hints stay lightweight",
        body = "Native helper cards replace the old web utility drawer without hiding the main learning flow."
    ),
    LearnSmartHelperAction(
        title = "Route guidance",
        body = "Use Home for quick next actions, Settings for support/legal/account guidance, and feature screens for task-specific hints."
    ),
    LearnSmartHelperAction(
        title = "Support path",
        body = "Support, privacy, terms, account deletion, and billing access remain visible from Settings."
    )
)
