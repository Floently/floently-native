package com.floently.learn.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun CardsScreen(
    repository: CardsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDeckType by remember { mutableStateOf(CardsDeckType.Vocabulary) }
    var dashboardState by remember { mutableStateOf<CardsDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, selectedDeckType) {
        val dashboard = repository.dashboard(selectedDeckType)
        dashboardState = dashboard
        statusMessage = dashboard.errorMessage
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cards",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Native flashcard foundation with deck filters, start boundary, and progress model.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = "Deck type", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                CardsDeckType.entries.forEach { type ->
                    FloentlyPrimaryButton(
                        title = if (type == selectedDeckType) "Selected: ${type.name}" else type.name,
                        product = FloentlyProduct.Learn,
                        onClick = { selectedDeckType = type }
                    )
                }
            }

            statusMessage?.let { message ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading cards...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else if (dashboard.decks.isEmpty()) {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "No decks yet for ${dashboard.selectedDeckType.name}.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                dashboard.decks.forEach { deck ->
                    val progress = dashboard.progress.firstOrNull { it.deckId == deck.id }
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = deck.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = deck.description, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Due: ${deck.dueCards}/${deck.totalCards}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Progress: ${progress?.reviewedCards ?: 0}/${progress?.totalCards ?: 0}", style = MaterialTheme.typography.bodySmall)
                        FloentlyPrimaryButton(
                            title = if (deck.locked) "View lock reason" else "Start cards",
                            product = FloentlyProduct.Learn,
                            onClick = {
                                scope.launch {
                                    statusMessage = when (val result = repository.startSession(deck.id, CardsPracticeMode.Flip)) {
                                        is CardsSessionResult.Ready -> "Session ready: ${result.session.currentCard?.front ?: deck.title}"
                                        is CardsSessionResult.Blocked -> result.reason
                                        is CardsSessionResult.Error -> result.message
                                    }
                                }
                            }
                        )
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Learn", product = FloentlyProduct.Learn, onClick = onBack)
        }
    }
}
