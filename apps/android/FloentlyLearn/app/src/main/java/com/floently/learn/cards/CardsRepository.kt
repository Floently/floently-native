package com.floently.learn.cards

interface CardsRepository {
    suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState
    suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsSessionResult
    suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsSessionResult
    fun summarize(session: CardsPracticeSession): CardsSessionSummary
}

sealed interface CardsSessionResult {
    data class Ready(val session: CardsPracticeSession) : CardsSessionResult
    data class Blocked(val reason: String) : CardsSessionResult
    data class Error(val message: String) : CardsSessionResult
}

class ServiceCardsRepository(
    private val service: CardsService,
    private val fallback: CardsRepository = PreviewCardsRepository()
) : CardsRepository {
    override suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState {
        return runCatching { service.dashboard(selectedDeckType) }.getOrElse { error ->
            fallback.dashboard(selectedDeckType).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Cards service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsSessionResult {
        return runCatching { CardsSessionResult.Ready(service.startSession(deckId, mode)) }.getOrElse {
            fallback.startSession(deckId, mode)
        }
    }

    override suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsSessionResult {
        return runCatching { CardsSessionResult.Ready(service.reviewCard(session, cardId, rating)) }.getOrElse {
            fallback.reviewCard(session, cardId, rating)
        }
    }

    override fun summarize(session: CardsPracticeSession): CardsSessionSummary = fallback.summarize(session)
}

class PreviewCardsRepository : CardsRepository {
    private val decks = listOf(
        CardsDeck("cards-vocabulary-a1", "A1 daily vocabulary", CardsDeckType.Vocabulary, "High-frequency everyday Finnish.", 3, 3, false),
        CardsDeck("cards-phrases-a2", "A2 useful phrases", CardsDeckType.Phrases, "Reusable phrases for daily life and services.", 3, 3, false),
        CardsDeck("cards-work-finnish", "Work Finnish cards", CardsDeckType.Work, "Professional Finnish phrases for meetings and job search.", 3, 3, false),
        CardsDeck("cards-yki-keskitaso", "YKI keskitaso review", CardsDeckType.Yki, "YKI-style vocabulary review.", 2, 2, false),
        CardsDeck("cards-sound-review", "Sound review cards", CardsDeckType.Review, "Listening-style card review remains behind the native media parity gate.", 0, 0, true)
    )

    private val cardsByDeckId = mapOf(
        "cards-vocabulary-a1" to listOf(
            StudyCard("a1-card-1", "cards-vocabulary-a1", "kauppa", "shop", "Menen kauppaan.", "A place where you buy food or items.", listOf("A1", "daily")),
            StudyCard("a1-card-2", "cards-vocabulary-a1", "tänään", "today", "Tänään on hyvä päivä.", "This day.", listOf("A1", "time")),
            StudyCard("a1-card-3", "cards-vocabulary-a1", "tarvitsen", "I need", "Tarvitsen apua.", "Useful when asking for help.", listOf("A1", "verb"))
        ),
        "cards-phrases-a2" to listOf(
            StudyCard("phrase-card-1", "cards-phrases-a2", "Voisitko auttaa minua?", "Could you help me?", "Anteeksi, voisitko auttaa minua?", "Polite help request.", listOf("A2", "service")),
            StudyCard("phrase-card-2", "cards-phrases-a2", "En ymmärtänyt.", "I did not understand.", "Anteeksi, en ymmärtänyt.", "Use when you need repetition.", listOf("A2", "clarification")),
            StudyCard("phrase-card-3", "cards-phrases-a2", "Voisitko sanoa sen uudelleen?", "Could you say that again?", "Voisitko sanoa sen uudelleen, kiitos?", "Polite repetition request.", listOf("A2", "clarification"))
        ),
        "cards-work-finnish" to listOf(
            StudyCard("work-card-1", "cards-work-finnish", "Olen käytettävissä ensi viikolla.", "I am available next week.", "Kiitos viestistä. Olen käytettävissä ensi viikolla.", "Useful for interviews and scheduling.", listOf("work", "job-search")),
            StudyCard("work-card-2", "cards-work-finnish", "Voisitko tarkentaa?", "Could you clarify?", "Voisitko tarkentaa tätä kohtaa?", "Good meeting phrase.", listOf("work", "meeting")),
            StudyCard("work-card-3", "cards-work-finnish", "Sopiiko tämä aikataulu?", "Does this schedule work?", "Sopiiko tämä aikataulu sinulle?", "Useful for arranging work.", listOf("work", "schedule"))
        ),
        "cards-yki-keskitaso" to listOf(
            StudyCard("yki-card-1", "cards-yki-keskitaso", "mielestäni", "in my opinion", "Mielestäni tämä on hyvä ratkaisu.", "Useful in YKI writing and speaking.", listOf("YKI", "opinion")),
            StudyCard("yki-card-2", "cards-yki-keskitaso", "toisaalta", "on the other hand", "Toisaalta se voi olla vaikeaa.", "Useful for balanced arguments.", listOf("YKI", "argument"))
        )
    )

    override suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState {
        val visibleDecks = decks.filter { it.type == selectedDeckType }
        return CardsDashboardState(
            decks = visibleDecks,
            progress = visibleDecks.map { deck -> CardsDeckProgress(deck.id, 0, deck.totalCards, deck.dueCards, 0, null) },
            selectedDeckType = selectedDeckType,
            isLoading = false,
            errorMessage = null
        )
    }

    override suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsSessionResult {
        val deck = decks.firstOrNull { it.id == deckId } ?: return CardsSessionResult.Error("Cards deck was not found.")
        if (deck.locked) return CardsSessionResult.Blocked("This deck is still behind its native parity gate.")
        val cards = cardsByDeckId[deckId].orEmpty()
        if (cards.isEmpty()) return CardsSessionResult.Error("Cards deck has no cards yet.")
        return CardsSessionResult.Ready(
            CardsPracticeSession(
                id = "session-$deckId-preview",
                deck = deck,
                cards = cards,
                currentCardIndex = 0,
                answers = emptyMap(),
                mode = mode,
                releaseGate = "Spaced repetition scheduling, sync, media cards, and durable progress are required before release."
            )
        )
    }

    override suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsSessionResult =
        CardsSessionResult.Ready(session.copy(answers = session.answers + (cardId to rating), currentCardIndex = session.currentCardIndex + 1))

    override fun summarize(session: CardsPracticeSession): CardsSessionSummary {
        val ratings = session.answers.values
        val positive = ratings.count { it == CardsReviewRating.Good || it == CardsReviewRating.Easy }
        val accuracy = if (ratings.isEmpty()) null else ((positive.toDouble() / ratings.size.toDouble()) * 100).toInt()
        return CardsSessionSummary(
            sessionId = session.id,
            reviewedCards = session.answers.size,
            totalCards = session.cards.size,
            againCount = ratings.count { it == CardsReviewRating.Again },
            hardCount = ratings.count { it == CardsReviewRating.Hard },
            goodCount = ratings.count { it == CardsReviewRating.Good },
            easyCount = ratings.count { it == CardsReviewRating.Easy },
            accuracyPreviewPercent = accuracy,
            durable = false
        )
    }
}
