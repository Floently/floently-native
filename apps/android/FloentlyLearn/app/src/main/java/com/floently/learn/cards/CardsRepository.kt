package com.floently.learn.cards

interface CardsRepository {
    suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState
    suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsSessionResult
    suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsSessionResult
    suspend fun skipCard(session: CardsPracticeSession): CardsSessionResult
    suspend fun flagCard(session: CardsPracticeSession, cardId: String, reason: String = "malformed_card"): Boolean
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
                    ?: "Cards are temporarily unavailable. Try again soon."
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

    override suspend fun skipCard(session: CardsPracticeSession): CardsSessionResult {
        return runCatching { CardsSessionResult.Ready(service.skipCard(session)) }.getOrElse {
            fallback.skipCard(session)
        }
    }

    override suspend fun flagCard(session: CardsPracticeSession, cardId: String, reason: String): Boolean {
        return runCatching { service.flagCard(session, cardId, reason) }.getOrElse {
            fallback.flagCard(session, cardId, reason)
        }
    }

    override fun summarize(session: CardsPracticeSession): CardsSessionSummary = fallback.summarize(session)
}

class PreviewCardsRepository : CardsRepository {
    private val banks = listOf(
        CardsDeckBank(
            id = "daily-core",
            title = "Daily Finnish banks",
            description = "High-frequency words and phrases for everyday retention.",
            deckCount = 2,
            dueCards = 6,
            source = "Native preview mirroring completed web card banks"
        ),
        CardsDeckBank(
            id = "work-yki",
            title = "Work and YKI banks",
            description = "Professional Finnish and YKI review decks.",
            deckCount = 2,
            dueCards = 5,
            source = "Native preview mirroring completed web card banks"
        ),
        CardsDeckBank(
            id = "review-audio",
            title = "Review and audio banks",
            description = "Review cards and future listening-style review.",
            deckCount = 1,
            dueCards = 0,
            source = "Native preview mirroring completed web card banks"
        )
    )

    private val allOverlayCodes = listOf(
        "fi", "sv", "ru", "et", "uk", "ar", "en", "so", "fa", "zh",
        "sq", "ku", "vi", "bn", "tr", "tl", "th", "ne", "es", "ur"
    )

    private fun overlays(meaning: String, example: String, hint: String): List<CardI18nOverlay> =
        allOverlayCodes.map { code ->
            CardI18nOverlay(
                languageCode = code,
                meaning = meaning,
                example = example,
                hint = hint,
                source = "card-overlay-contract"
            )
        }

    private val decks = listOf(
        CardsDeck(
            id = "cards-vocabulary-a1",
            title = "A1 daily vocabulary",
            type = CardsDeckType.Vocabulary,
            description = "High-frequency everyday Finnish.",
            totalCards = 3,
            dueCards = 3,
            locked = false,
            bankId = "daily-core",
            bankTitle = "Daily Finnish banks",
            cefrLevel = "A1",
            overlayLanguageCodes = allOverlayCodes
        ),
        CardsDeck(
            id = "cards-phrases-a2",
            title = "A2 useful phrases",
            type = CardsDeckType.Phrases,
            description = "Reusable phrases for daily life and services.",
            totalCards = 3,
            dueCards = 3,
            locked = false,
            bankId = "daily-core",
            bankTitle = "Daily Finnish banks",
            cefrLevel = "A2",
            overlayLanguageCodes = allOverlayCodes
        ),
        CardsDeck(
            id = "cards-work-finnish",
            title = "Work Finnish cards",
            type = CardsDeckType.Work,
            description = "Professional Finnish phrases for meetings and job search.",
            totalCards = 3,
            dueCards = 3,
            locked = false,
            bankId = "work-yki",
            bankTitle = "Work and YKI banks",
            cefrLevel = "A2-B1",
            overlayLanguageCodes = allOverlayCodes
        ),
        CardsDeck(
            id = "cards-yki-keskitaso",
            title = "YKI keskitaso review",
            type = CardsDeckType.Yki,
            description = "YKI-style vocabulary review.",
            totalCards = 2,
            dueCards = 2,
            locked = false,
            bankId = "work-yki",
            bankTitle = "Work and YKI banks",
            cefrLevel = "B1",
            overlayLanguageCodes = allOverlayCodes
        ),
        CardsDeck(
            id = "cards-sound-review",
            title = "Sound review cards",
            type = CardsDeckType.Review,
            description = "Listening-style card review will be available with audio practice.",
            totalCards = 0,
            dueCards = 0,
            locked = true,
            bankId = "review-audio",
            bankTitle = "Review and audio banks",
            cefrLevel = null,
            overlayLanguageCodes = allOverlayCodes
        )
    )

    private val cardsByDeckId = mapOf(
        "cards-vocabulary-a1" to listOf(
            StudyCard("a1-card-1", "cards-vocabulary-a1", "kauppa", "shop", "Menen kauppaan.", "A place where you buy food or items.", listOf("A1", "daily"), overlays("shop", "Menen kauppaan.", "A place where you buy food or items."), "Review tomorrow if hard."),
            StudyCard("a1-card-2", "cards-vocabulary-a1", "tänään", "today", "Tänään on hyvä päivä.", "This day.", listOf("A1", "time"), overlays("today", "Tänään on hyvä päivä.", "This day."), "Review tomorrow if hard."),
            StudyCard("a1-card-3", "cards-vocabulary-a1", "tarvitsen", "I need", "Tarvitsen apua.", "Useful when asking for help.", listOf("A1", "verb"), overlays("I need", "Tarvitsen apua.", "Useful when asking for help."), "Review tomorrow if hard.")
        ),
        "cards-phrases-a2" to listOf(
            StudyCard("phrase-card-1", "cards-phrases-a2", "Voisitko auttaa minua?", "Could you help me?", "Anteeksi, voisitko auttaa minua?", "Polite help request.", listOf("A2", "service"), overlays("Could you help me?", "Anteeksi, voisitko auttaa minua?", "Polite help request."), "Review again this week."),
            StudyCard("phrase-card-2", "cards-phrases-a2", "En ymmärtänyt.", "I did not understand.", "Anteeksi, en ymmärtänyt.", "Use when you need repetition.", listOf("A2", "clarification"), overlays("I did not understand.", "Anteeksi, en ymmärtänyt.", "Use when you need repetition."), "Review again this week."),
            StudyCard("phrase-card-3", "cards-phrases-a2", "Voisitko sanoa sen uudelleen?", "Could you say that again?", "Voisitko sanoa sen uudelleen, kiitos?", "Polite repetition request.", listOf("A2", "clarification"), overlays("Could you say that again?", "Voisitko sanoa sen uudelleen, kiitos?", "Polite repetition request."), "Review again this week.")
        ),
        "cards-work-finnish" to listOf(
            StudyCard("work-card-1", "cards-work-finnish", "Olen käytettävissä ensi viikolla.", "I am available next week.", "Kiitos viestistä. Olen käytettävissä ensi viikolla.", "Useful for interviews and scheduling.", listOf("work", "job-search"), overlays("I am available next week.", "Kiitos viestistä. Olen käytettävissä ensi viikolla.", "Useful for interviews and scheduling."), "Review before interview practice."),
            StudyCard("work-card-2", "cards-work-finnish", "Voisitko tarkentaa?", "Could you clarify?", "Voisitko tarkentaa tätä kohtaa?", "Good meeting phrase.", listOf("work", "meeting"), overlays("Could you clarify?", "Voisitko tarkentaa tätä kohtaa?", "Good meeting phrase."), "Review before meeting practice."),
            StudyCard("work-card-3", "cards-work-finnish", "Sopiiko tämä aikataulu?", "Does this schedule work?", "Sopiiko tämä aikataulu sinulle?", "Useful for arranging work.", listOf("work", "schedule"), overlays("Does this schedule work?", "Sopiiko tämä aikataulu sinulle?", "Useful for arranging work."), "Review before roleplay.")
        ),
        "cards-yki-keskitaso" to listOf(
            StudyCard("yki-card-1", "cards-yki-keskitaso", "mielestäni", "in my opinion", "Mielestäni tämä on hyvä ratkaisu.", "Useful in YKI writing and speaking.", listOf("YKI", "opinion"), overlays("in my opinion", "Mielestäni tämä on hyvä ratkaisu.", "Useful in YKI writing and speaking."), "Review before YKI writing."),
            StudyCard("yki-card-2", "cards-yki-keskitaso", "toisaalta", "on the other hand", "Toisaalta se voi olla vaikeaa.", "Useful for balanced arguments.", listOf("YKI", "argument"), overlays("on the other hand", "Toisaalta se voi olla vaikeaa.", "Useful for balanced arguments."), "Review before YKI writing.")
        )
    )

    override suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState {
        val visibleDecks = decks.filter { it.type == selectedDeckType }
        val visibleCards = visibleDecks.flatMap { deck -> cardsByDeckId[deck.id].orEmpty() }
        return CardsDashboardState(
            decks = visibleDecks,
            progress = visibleDecks.map { deck -> CardsDeckProgress(deck.id, 0, deck.totalCards, deck.dueCards, 0, null) },
            selectedDeckType = selectedDeckType,
            isLoading = false,
            errorMessage = null,
            banks = banks,
            buckets = bucketsFor(visibleCards)
        )
    }

    override suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsSessionResult {
        val deck = decks.firstOrNull { it.id == deckId } ?: return CardsSessionResult.Error("Cards deck was not found.")
        if (deck.locked) return CardsSessionResult.Blocked("This deck will be available when audio review is ready.")
        val cards = cardsByDeckId[deckId].orEmpty()
        if (cards.isEmpty()) return CardsSessionResult.Error("Cards deck has no cards yet.")
        return CardsSessionResult.Ready(
            CardsPracticeSession(
                id = "session-$deckId-local",
                deck = deck,
                cards = cards,
                currentCardIndex = 0,
                answers = emptyMap(),
                mode = mode,
                releaseGate = "Cards practice is ready."
            )
        )
    }

    override suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsSessionResult {
        val updatedCards = session.cards.map { card ->
            if (card.id == cardId) {
                card.copy(
                    state = nextState(card, rating),
                    seenCount = card.seenCount + 1,
                    dueNow = rating == CardsReviewRating.Again || rating == CardsReviewRating.Hard,
                    nextReviewText = nextReviewText(rating)
                )
            } else {
                card
            }
        }
        return CardsSessionResult.Ready(
            session.copy(
                cards = updatedCards,
                answers = session.answers + (cardId to rating),
                currentCardIndex = session.currentCardIndex + 1
            )
        )
    }

    override suspend fun skipCard(session: CardsPracticeSession): CardsSessionResult =
        CardsSessionResult.Ready(session.copy(currentCardIndex = session.currentCardIndex + 1))

    override suspend fun flagCard(session: CardsPracticeSession, cardId: String, reason: String): Boolean = true

    override fun summarize(session: CardsPracticeSession): CardsSessionSummary {
        val ratings = session.answers.values
        val positive = ratings.count { it == CardsReviewRating.Good || it == CardsReviewRating.Easy }
        val accuracy = if (ratings.isEmpty()) null else ((positive.toDouble() / ratings.size.toDouble()) * 100).toInt()
        val nextReview = when {
            ratings.any { it == CardsReviewRating.Again } -> "Review again soon: you marked at least one card Again."
            ratings.any { it == CardsReviewRating.Hard } -> "Review hard cards again this week."
            ratings.isNotEmpty() -> "Good session. Continue with the next card bank."
            else -> "Start a short review session when you are ready."
        }
        return CardsSessionSummary(
            sessionId = session.id,
            reviewedCards = session.answers.size,
            totalCards = session.cards.size,
            againCount = ratings.count { it == CardsReviewRating.Again },
            hardCount = ratings.count { it == CardsReviewRating.Hard },
            goodCount = ratings.count { it == CardsReviewRating.Good },
            easyCount = ratings.count { it == CardsReviewRating.Easy },
            accuracyPreviewPercent = accuracy,
            durable = false,
            nextReviewText = nextReview
        )
    }
}


private fun bucketsFor(cards: List<StudyCard>): CardBankBuckets {
    val learned = cards.filter { it.state == CardsCardState.Mastered }
    val difficult = cards.filter { it.state == CardsCardState.Difficult || ((it.correctRate ?: 1.0) <= 0.45 && it.seenCount >= 4) }
    val learning = cards.filter { card -> learned.none { it.id == card.id } && difficult.none { it.id == card.id } }
    return CardBankBuckets(difficult = difficult, learned = learned, learning = learning)
}

private fun nextState(card: StudyCard, rating: CardsReviewRating): CardsCardState = when (rating) {
    CardsReviewRating.Again -> if (card.seenCount >= 1) CardsCardState.Difficult else CardsCardState.Learning
    CardsReviewRating.Hard -> CardsCardState.Difficult
    CardsReviewRating.Good -> if (card.seenCount >= 1) CardsCardState.Mastered else CardsCardState.Learning
    CardsReviewRating.Easy -> CardsCardState.Mastered
}

private fun nextReviewText(rating: CardsReviewRating): String = when (rating) {
    CardsReviewRating.Again -> "Review again soon."
    CardsReviewRating.Hard -> "Review hard cards again this week."
    CardsReviewRating.Good -> "Good recall. Review later."
    CardsReviewRating.Easy -> "Strong recall. Keep moving."
}
