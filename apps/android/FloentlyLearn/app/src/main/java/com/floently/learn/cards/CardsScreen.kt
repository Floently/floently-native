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
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import kotlinx.coroutines.launch

@Composable
fun CardsScreen(
    repository: CardsRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDeckType by remember { mutableStateOf(CardsDeckType.Vocabulary) }
    var dashboardState by remember { mutableStateOf<CardsDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<CardsPracticeSession?>(null) }

    val session = activeSession
    if (session != null) {
        CardsPracticeScreen(
            session = session,
            repository = repository,
            copy = copy,
            onSessionChange = { activeSession = it },
            onExit = { activeSession = null }
        )
        return
    }

    LaunchedEffect(repository, selectedDeckType) {
        val dashboard = repository.dashboard(selectedDeckType)
        dashboardState = dashboard
        statusMessage = dashboard.errorMessage
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = copy.cardsTitle,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.cardsSubtitle,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = copy.cardsMessage, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = copy.cardsSubtitle, style = MaterialTheme.typography.bodyMedium)
                CardsDeckType.entries.forEach { type ->
                    FloentlyPrimaryButton(
                        title = if (type == selectedDeckType) "${type.displayName()} selected" else type.displayName(),
                        product = FloentlyProduct.Learn,
                        onClick = { selectedDeckType = type }
                    )
                }
            }

            statusMessage?.let { message ->
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading cards...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else if (dashboard.decks.isEmpty()) {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "No decks yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Try another review type or come back when new cards are available.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                dashboard.decks.forEach { deck ->
                    val progress = dashboard.progress.firstOrNull { it.deckId == deck.id }
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = deck.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = deck.description, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Due now: ${deck.dueCards} of ${deck.totalCards}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Reviewed: ${progress?.reviewedCards ?: 0} of ${progress?.totalCards ?: 0}", style = MaterialTheme.typography.bodySmall)
                        progress?.lastAccuracyPercent?.let { accuracy ->
                            Text(text = "Last accuracy: $accuracy%", style = MaterialTheme.typography.bodySmall)
                        }
                        FloentlyPrimaryButton(
                            title = if (deck.locked) "See why locked" else copy.cardsAction,
                            product = FloentlyProduct.Learn,
                            onClick = {
                                scope.launch {
                                    when (val result = repository.startSession(deck.id, CardsPracticeMode.Flip)) {
                                        is CardsSessionResult.Ready -> {
                                            statusMessage = null
                                            activeSession = result.session
                                        }
                                        is CardsSessionResult.Blocked -> statusMessage = result.reason
                                        is CardsSessionResult.Error -> statusMessage = result.message
                                    }
                                }
                            }
                        )
                    }
                }
            }

            FloentlyPrimaryButton(title = copy.backToLearn, product = FloentlyProduct.Learn, onClick = onBack)
        }
    }
}

@Composable
private fun CardsPracticeScreen(
    session: CardsPracticeSession,
    repository: CardsRepository,
    copy: LearnCopy,
    onSessionChange: (CardsPracticeSession) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val card = session.currentCard
    var showAnswer by remember(session.id, session.currentCardIndex) { mutableStateOf(false) }
    var statusMessage by remember(session.id, session.currentCardIndex) { mutableStateOf<String?>(null) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = session.deck.title,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Card ${session.reviewedCount + 1} of ${session.cards.size}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            if (session.completed || card == null) {
                val summary = repository.summarize(session)
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = copy.cardsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "You reviewed ${summary.reviewedCards} of ${summary.totalCards} cards.", style = MaterialTheme.typography.bodyMedium)
                    summary.accuracyPreviewPercent?.let { accuracy ->
                        Text(text = "Strong ratings: $accuracy%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "Again: ${summary.againCount} | Hard: ${summary.hardCount} | Good: ${summary.goodCount} | Easy: ${summary.easyCount}", style = MaterialTheme.typography.bodySmall)
                }
                FloentlyPrimaryButton(title = copy.cardsTitle, product = FloentlyProduct.Learn, onClick = onExit)
            } else {
                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = card.front, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(text = card.hint, style = MaterialTheme.typography.bodyMedium)
                    if (showAnswer) {
                        Text(text = card.back, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = card.example, style = MaterialTheme.typography.bodyMedium)
                        if (card.tags.isNotEmpty()) {
                            Text(text = "Tags: ${card.tags.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text(text = "Think of the meaning, then reveal the answer.", style = MaterialTheme.typography.bodySmall)
                    }
                    FloentlyPrimaryButton(
                        title = if (showAnswer) "Hide answer" else "Show answer",
                        product = FloentlyProduct.Learn,
                        onClick = { showAnswer = !showAnswer }
                    )
                }

                if (showAnswer) {
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = "How well did you know it?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        CardsReviewRating.entries.forEach { rating ->
                            FloentlyPrimaryButton(
                                title = rating.displayName(),
                                product = FloentlyProduct.Learn,
                                onClick = {
                                    scope.launch {
                                        when (val result = repository.reviewCard(session, card.id, rating)) {
                                            is CardsSessionResult.Ready -> {
                                                statusMessage = null
                                                showAnswer = false
                                                onSessionChange(result.session)
                                            }
                                            is CardsSessionResult.Blocked -> statusMessage = result.reason
                                            is CardsSessionResult.Error -> statusMessage = result.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                statusMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                FloentlyPrimaryButton(title = copy.backToLearn, product = FloentlyProduct.Learn, onClick = onExit)
            }
        }
    }
}

private fun CardsDeckType.displayName(): String = when (this) {
    CardsDeckType.Vocabulary -> "Vocabulary"
    CardsDeckType.Phrases -> "Phrases"
    CardsDeckType.Grammar -> "Grammar"
    CardsDeckType.Work -> "Work Finnish"
    CardsDeckType.Yki -> "YKI review"
    CardsDeckType.Review -> "Review"
}

private fun CardsReviewRating.displayName(): String = when (this) {
    CardsReviewRating.Again -> "Again"
    CardsReviewRating.Hard -> "Hard"
    CardsReviewRating.Good -> "Good"
    CardsReviewRating.Easy -> "Easy"
}
