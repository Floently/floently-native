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

fun learnYkiSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Guided YKI prep",
        body = "Use this route when you want exam-style reading, listening, writing, or speaking practice with section-based guidance."
    ),
    LearnSmartHelperAction(
        title = "Formal readiness",
        body = "Short tasks are useful for repair. Longer mock-style work belongs to the exam readiness flow."
    ),
    LearnSmartHelperAction(
        title = "Keep it simple",
        body = "For written answers, give the clearest answer you can first. You can improve wording after the main idea is correct."
    )
)

fun learnProfessionalSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Workplace Finnish hub",
        body = "Choose everyday work Finnish or the profession-specific path. Do not mix professions inside one practice flow."
    ),
    LearnSmartHelperAction(
        title = "Role-specific language",
        body = "Use model phrases, handover language, reporting language, and safety wording that match the selected profession."
    ),
    LearnSmartHelperAction(
        title = "Next best action",
        body = "Open cards for vocabulary, roleplay for dialogue, and professional tasks for practical workplace situations."
    )
)

fun learnRoleplaySmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Why this route matters",
        body = "Roleplay builds practical spoken Finnish for real workplace and everyday conversations, not only isolated phrases."
    ),
    LearnSmartHelperAction(
        title = "Track focus",
        body = "Stay inside the selected route. Nurse, doctor, practical nurse, and general workplace scenarios should not be mixed."
    ),
    LearnSmartHelperAction(
        title = "Beginner-safe reply",
        body = "One clear Finnish sentence is enough. The coach should keep variety and avoid repeating the same cue every session."
    )
)

fun learnProgressSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Check YKI readiness",
        body = "Return to YKI Prep when you need section-based repair or more confidence for a formal exam situation."
    ),
    LearnSmartHelperAction(
        title = "Strengthen vocabulary and roleplay",
        body = "Use cards for quick recall and roleplay for practical spoken flow when progress shows weak momentum."
    ),
    LearnSmartHelperAction(
        title = "Sync boundary",
        body = "Progress may start as a local preview, then move toward backend-synced history without blocking practice."
    )
)

fun learnAccountSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Account and access",
        body = "Use Account for sign-in state, Learn access, subscription status, and device visibility."
    ),
    LearnSmartHelperAction(
        title = "Plans and access",
        body = "Learn, Read, and Create access remain separate unless a real bundle plan is added later."
    ),
    LearnSmartHelperAction(
        title = "Support path",
        body = "Settings keeps support, privacy, terms, account deletion, and billing guidance visible."
    )
)

fun learnCardsSmartHelperActions(): List<LearnSmartHelperAction> = listOf(
    LearnSmartHelperAction(
        title = "Reveal hints only when needed",
        body = "Try the card first, then reveal the coach hint. This preserves recall before explanation."
    ),
    LearnSmartHelperAction(
        title = "Overlay language",
        body = "Meanings and helper text should follow the selected overlay language when available."
    ),
    LearnSmartHelperAction(
        title = "Review rhythm",
        body = "Use difficult, learning, and mastered buckets to decide what needs another short review."
    )
)
