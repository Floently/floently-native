package com.floently.learn.cards

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class CardsService(private val api: FloentlyApiClient) {
    suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState {
        val response = api.get("/api/v1/learn/cards/dashboard?deck_type=${selectedDeckType.apiName()}")
        return dashboardFromJson(response, selectedDeckType)
    }

    suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsPracticeSession {
        val response = api.post(
            "/api/v1/learn/cards/sessions",
            JSONObject()
                .put("deck_id", deckId)
                .put("mode", mode.apiName())
        )
        return sessionFromJson(response.optJSONObject("session") ?: response)
    }

    suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsPracticeSession {
        val response = api.post(
            "/api/v1/learn/cards/sessions/${session.id}/review",
            JSONObject()
                .put("card_id", cardId)
                .put("rating", rating.apiName())
        )
        return sessionFromJson(response.optJSONObject("session") ?: response, fallback = session)
    }

    private fun dashboardFromJson(json: JSONObject, selectedDeckType: CardsDeckType): CardsDashboardState {
        val decksJson = json.optJSONArray("decks") ?: JSONArray()
        val progressJson = json.optJSONArray("progress") ?: JSONArray()
        return CardsDashboardState(
            decks = List(decksJson.length()) { index -> deckFromJson(decksJson.getJSONObject(index), selectedDeckType) },
            progress = List(progressJson.length()) { index -> progressFromJson(progressJson.getJSONObject(index)) },
            selectedDeckType = selectedDeckType,
            isLoading = false,
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
        )
    }

    private fun sessionFromJson(json: JSONObject, fallback: CardsPracticeSession? = null): CardsPracticeSession {
        val deck = json.optJSONObject("deck")?.let { deckFromJson(it, fallback?.deck?.type ?: CardsDeckType.Vocabulary) }
            ?: fallback?.deck
            ?: deckFromJson(JSONObject().put("id", json.optString("deck_id")), CardsDeckType.Vocabulary)
        val cardsJson = json.optJSONArray("cards") ?: JSONArray()
        val cards = if (cardsJson.length() > 0) {
            List(cardsJson.length()) { index -> cardFromJson(cardsJson.getJSONObject(index), deck.id) }
        } else {
            fallback?.cards.orEmpty()
        }
        val answersJson = json.optJSONObject("answers")
        return CardsPracticeSession(
            id = json.optString("id").ifBlank { fallback?.id ?: "cards-session" },
            deck = deck,
            cards = cards,
            currentCardIndex = json.optInt("current_card_index", fallback?.currentCardIndex ?: 0),
            answers = answersFromJson(answersJson, fallback?.answers.orEmpty()),
            mode = practiceModeFromApi(json.optString("mode"), fallback?.mode ?: CardsPracticeMode.Flip),
            releaseGate = json.optString("release_gate").ifBlank { "Cards scheduling is connected through the existing service boundary." }
        )
    }

    private fun deckFromJson(json: JSONObject, fallbackType: CardsDeckType): CardsDeck = CardsDeck(
        id = json.optString("id").ifBlank { "cards-deck" },
        title = json.optString("title").ifBlank { "Cards deck" },
        type = deckTypeFromApi(json.optString("type"), fallbackType),
        description = json.optString("description"),
        totalCards = json.optInt("total_cards", json.optInt("totalCards", 0)),
        dueCards = json.optInt("due_cards", json.optInt("dueCards", 0)),
        locked = json.optBoolean("locked", false)
    )

    private fun cardFromJson(json: JSONObject, fallbackDeckId: String): StudyCard = StudyCard(
        id = json.optString("id").ifBlank { "study-card" },
        deckId = json.optString("deck_id").ifBlank { fallbackDeckId },
        front = json.optString("front"),
        back = json.optString("back"),
        example = json.optString("example"),
        hint = json.optString("hint"),
        tags = stringList(json.optJSONArray("tags"))
    )

    private fun progressFromJson(json: JSONObject): CardsDeckProgress = CardsDeckProgress(
        deckId = json.optString("deck_id").ifBlank { json.optString("deckId") },
        reviewedCards = json.optInt("reviewed_cards", json.optInt("reviewedCards", 0)),
        totalCards = json.optInt("total_cards", json.optInt("totalCards", 0)),
        dueCards = json.optInt("due_cards", json.optInt("dueCards", 0)),
        streak = json.optInt("streak", 0),
        lastAccuracyPercent = if (json.has("last_accuracy_percent")) json.optInt("last_accuracy_percent") else null
    )

    private fun answersFromJson(json: JSONObject?, fallback: Map<String, CardsReviewRating>): Map<String, CardsReviewRating> {
        if (json == null) return fallback
        val map = mutableMapOf<String, CardsReviewRating>()
        json.keys().forEach { key -> map[key] = reviewRatingFromApi(json.optString(key)) }
        return map
    }

    private fun stringList(array: JSONArray?): List<String> = if (array == null) emptyList() else List(array.length()) { index -> array.optString(index) }
}

private fun CardsDeckType.apiName(): String = name.lowercase()
private fun CardsPracticeMode.apiName(): String = name.lowercase()
private fun CardsReviewRating.apiName(): String = name.lowercase()

private fun deckTypeFromApi(value: String?, fallback: CardsDeckType): CardsDeckType = when (value?.trim()?.lowercase()) {
    "vocabulary" -> CardsDeckType.Vocabulary
    "phrases" -> CardsDeckType.Phrases
    "grammar" -> CardsDeckType.Grammar
    "work" -> CardsDeckType.Work
    "yki" -> CardsDeckType.Yki
    "review" -> CardsDeckType.Review
    else -> fallback
}

private fun practiceModeFromApi(value: String?, fallback: CardsPracticeMode): CardsPracticeMode = when (value?.trim()?.lowercase()) {
    "type_answer", "typeanswer" -> CardsPracticeMode.TypeAnswer
    "multiple_choice", "multiplechoice" -> CardsPracticeMode.MultipleChoice
    "review" -> CardsPracticeMode.Review
    "flip" -> CardsPracticeMode.Flip
    else -> fallback
}

private fun reviewRatingFromApi(value: String?): CardsReviewRating = when (value?.trim()?.lowercase()) {
    "hard" -> CardsReviewRating.Hard
    "good" -> CardsReviewRating.Good
    "easy" -> CardsReviewRating.Easy
    else -> CardsReviewRating.Again
}
