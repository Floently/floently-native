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

data class CardsDeck(
    val id: String,
    val title: String,
    val type: CardsDeckType,
    val description: String,
    val totalCards: Int,
    val dueCards: Int,
    val locked: Boolean
)

data class StudyCard(
    val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val example: String,
    val hint: String,
    val tags: List<String>
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
    val errorMessage: String?
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
    val durable: Boolean
)
