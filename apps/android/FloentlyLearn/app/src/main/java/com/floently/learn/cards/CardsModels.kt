package com.floently.learn.cards

enum class CardsDeckType {
    Vocabulary,
    Phrases,
    Grammar,
    Work,
    Yki,
    Review
}

enum class CardsPracticeMode {
    Flip,
    TypeAnswer,
    MultipleChoice,
    Review
}

enum class CardsReviewRating {
    Again,
    Hard,
    Good,
    Easy
}

enum class CardsCardState {
    New,
    Learning,
    Difficult,
    Mastered
}

data class CardBankBuckets(
    val difficult: List<StudyCard> = emptyList(),
    val learned: List<StudyCard> = emptyList(),
    val learning: List<StudyCard> = emptyList()
)

data class CardsDeckBank(
    val id: String,
    val title: String,
    val description: String,
    val deckCount: Int,
    val dueCards: Int,
    val source: String
)

data class CardsDeck(
    val id: String,
    val title: String,
    val type: CardsDeckType,
    val description: String,
    val totalCards: Int,
    val dueCards: Int,
    val locked: Boolean,
    val bankId: String = "core",
    val bankTitle: String = "Core cards",
    val cefrLevel: String? = null,
    val overlayLanguageCodes: List<String> = emptyList()
)

data class CardI18nOverlay(
    val languageCode: String,
    val meaning: String,
    val example: String,
    val hint: String,
    val source: String
)

data class StudyCard(
    val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val example: String,
    val hint: String,
    val tags: List<String>,
    val overlays: List<CardI18nOverlay> = emptyList(),
    val nextReviewText: String? = null,
    val state: CardsCardState = CardsCardState.New,
    val seenCount: Int = 0,
    val correctRate: Double? = null,
    val dueNow: Boolean = true
)

data class CardsDeckProgress(
    val deckId: String,
    val reviewedCards: Int,
    val totalCards: Int,
    val dueCards: Int,
    val streak: Int,
    val lastAccuracyPercent: Int?
)

data class CardsDashboardState(
    val decks: List<CardsDeck>,
    val progress: List<CardsDeckProgress>,
    val selectedDeckType: CardsDeckType,
    val isLoading: Boolean,
    val errorMessage: String?,
    val banks: List<CardsDeckBank> = emptyList(),
    val buckets: CardBankBuckets = CardBankBuckets()
)

data class CardsPracticeSession(
    val id: String,
    val deck: CardsDeck,
    val cards: List<StudyCard>,
    val currentCardIndex: Int,
    val answers: Map<String, CardsReviewRating>,
    val mode: CardsPracticeMode,
    val releaseGate: String
) {
    val currentCard: StudyCard?
        get() = cards.getOrNull(currentCardIndex)

    val completed: Boolean
        get() = cards.isNotEmpty() && currentCardIndex >= cards.size

    val reviewedCount: Int
        get() = answers.size
}

data class CardsSessionSummary(
    val sessionId: String,
    val reviewedCards: Int,
    val totalCards: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val accuracyPreviewPercent: Int?,
    val durable: Boolean,
    val nextReviewText: String = "Review difficult cards again soon."
)

fun StudyCard.overlayFor(languageCode: String): CardI18nOverlay? =
    overlays.firstOrNull { it.languageCode == languageCode }
        ?: overlays.firstOrNull { it.languageCode == "en" }


fun CardsCardState.displayLabel(): String = when (this) {
    CardsCardState.New -> "Fresh card"
    CardsCardState.Learning -> "Learning"
    CardsCardState.Difficult -> "Difficult"
    CardsCardState.Mastered -> "Mastered"
}

fun StudyCard.schedulingLabel(): String = nextReviewText
    ?: when (state) {
        CardsCardState.Mastered -> "Strong recall. Review later."
        CardsCardState.Difficult -> "Needs extra repetition soon."
        CardsCardState.Learning -> if (seenCount >= 2) "Still consolidating." else "Fresh learning card."
        CardsCardState.New -> "Fresh card."
    }
