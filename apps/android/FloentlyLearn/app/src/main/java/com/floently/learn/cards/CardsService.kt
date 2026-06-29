package com.floently.learn.cards

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class CardsService(private val api: FloentlyApiClient) {
    private data class RuntimeCardFilters(
        val domain: String,
        val contentType: String,
        val profession: String?,
        val level: String?
    )

    suspend fun dashboard(selectedDeckType: CardsDeckType): CardsDashboardState {
        val response = runCatching {
            api.get(runtimeDeckPath(selectedDeckType))
        }.getOrElse {
            api.get("/api/v1/learn/cards/dashboard?deck_type=${selectedDeckType.apiName()}")
        }
        return dashboardFromJson(response, selectedDeckType)
    }

    suspend fun startSession(deckId: String, mode: CardsPracticeMode): CardsPracticeSession {
        val response = runCatching {
            api.get(runtimeStartPath(deckId, mode))
        }.getOrElse {
            api.post(
                "/api/v1/learn/cards/sessions",
                JSONObject()
                    .put("deck_id", deckId)
                    .put("mode", mode.apiName())
            )
        }
        return sessionFromJson(response.optJSONObject("session") ?: response, root = response)
    }

    suspend fun reviewCard(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): CardsPracticeSession {
        val response = runCatching {
            api.post(
                "/api/v1/cards/session/${session.id}/answer",
                JSONObject().put("user_answer", answerForRuntimeReview(session, cardId, rating))
            )
        }.getOrElse {
            api.post(
                "/api/v1/learn/cards/sessions/${session.id}/review",
                JSONObject()
                    .put("card_id", cardId)
                    .put("rating", rating.apiName())
            )
        }
        val parsed = sessionFromJson(response.optJSONObject("session") ?: response, fallback = session, root = response)
        return parsed.copy(answers = parsed.answers + (cardId to rating))
    }

    suspend fun skipCard(session: CardsPracticeSession): CardsPracticeSession {
        val response = api.get("/api/v1/cards/session/${session.id}/next")
        return sessionFromJson(response.optJSONObject("session") ?: response, fallback = session, root = response)
    }

    suspend fun flagCard(session: CardsPracticeSession, cardId: String, reason: String = "malformed_card"): Boolean {
        api.post(
            "/api/v1/cards/flag",
            JSONObject()
                .put("card_id", cardId)
                .put("reason", reason)
                .put("session_id", session.id)
        )
        return true
    }

    private fun runtimeDeckPath(selectedDeckType: CardsDeckType): String {
        val filters = filtersFor(selectedDeckType.apiName(), selectedDeckType = selectedDeckType)
        return "/api/v1/cards/deck?${filters.toQueryString()}"
    }

    private fun runtimeStartPath(deckId: String, mode: CardsPracticeMode): String {
        val filters = filtersFor(deckId, mode = mode)
        return "/api/v1/cards/session/adaptive/start?${filters.toQueryString(includeLimit = true)}"
    }

    private fun RuntimeCardFilters.toQueryString(includeLimit: Boolean = false): String {
        val parts = mutableListOf(
            "domain=$domain",
            "content_type=$contentType"
        )
        profession?.let { parts += "profession=$it" }
        level?.let { parts += "level=$it" }
        if (includeLimit) parts += "limit=10"
        return parts.joinToString("&")
    }

    private fun filtersFor(
        deckId: String,
        mode: CardsPracticeMode? = null,
        selectedDeckType: CardsDeckType? = null
    ): RuntimeCardFilters {
        val value = "$deckId ${mode?.apiName().orEmpty()} ${selectedDeckType?.apiName().orEmpty()}".lowercase()
        val contentType = when {
            "grammar" in value -> "grammar_card"
            "phrase" in value || "sentence" in value || selectedDeckType == CardsDeckType.Phrases -> "sentence_card"
            else -> "vocabulary_card"
        }
        val professional = "work" in value || "professional" in value || "doctor" in value || "nurse" in value
        val profession = when {
            "doctor" in value -> "doctor"
            "practical" in value -> "practical_nurse"
            "nurse" in value -> "nurse"
            professional -> "general_workplace"
            else -> null
        }
        val level = when {
            "a1_a2" in value || ("a1" in value && "a2" in value) -> "A1_A2"
            "b1_b2" in value || ("b1" in value && "b2" in value) -> "B1_B2"
            "c1_c2" in value || ("c1" in value && "c2" in value) -> "C1_C2"
            "a1" in value -> "A1"
            "a2" in value -> "A2"
            "b1" in value -> "B1"
            "b2" in value -> "B2"
            "c1" in value -> "C1"
            "c2" in value -> "C2"
            else -> null
        }
        return RuntimeCardFilters(
            domain = if (professional) "professional" else "general",
            contentType = contentType,
            profession = profession,
            level = level
        )
    }

    private fun answerForRuntimeReview(session: CardsPracticeSession, cardId: String, rating: CardsReviewRating): String {
        val card = session.cards.firstOrNull { it.id == cardId } ?: session.currentCard
        return when (rating) {
            CardsReviewRating.Good,
            CardsReviewRating.Easy -> card?.back?.takeIf { it.isNotBlank() }
                ?: card?.overlays?.firstOrNull()?.meaning?.takeIf { it.isNotBlank() }
                ?: card?.front?.takeIf { it.isNotBlank() }
                ?: "known"
            CardsReviewRating.Again,
            CardsReviewRating.Hard -> "__needs_review__"
        }
    }

    private fun dashboardFromJson(json: JSONObject, selectedDeckType: CardsDeckType): CardsDashboardState {
        val decksJson = json.optJSONArray("decks") ?: JSONArray()
        val progressJson = json.optJSONArray("progress") ?: JSONArray()
        val banksJson = json.optJSONArray("banks") ?: JSONArray()
        val bucketsJson = json.optJSONObject("buckets")
        val runtimeCardsJson = json.optJSONArray("cards") ?: JSONArray()
        val runtimeCards = List(runtimeCardsJson.length()) { index -> cardFromJson(runtimeCardsJson.getJSONObject(index), selectedDeckType.apiName()) }

        val decks = if (decksJson.length() > 0) {
            List(decksJson.length()) { index -> deckFromJson(decksJson.getJSONObject(index), selectedDeckType) }
        } else if (runtimeCards.isNotEmpty()) {
            listOf(
                CardsDeck(
                    id = selectedDeckType.apiName(),
                    title = "${selectedDeckType.label()} runtime cards",
                    type = selectedDeckType,
                    description = "Cards loaded from the completed web runtime contract.",
                    totalCards = runtimeCards.size,
                    dueCards = runtimeCards.count { it.dueNow },
                    locked = false,
                    bankId = "runtime",
                    bankTitle = "Runtime cards",
                    cefrLevel = runtimeCards.firstNotNullOfOrNull { card -> card.tags.firstOrNull { it.startsWith("A") || it.startsWith("B") || it.startsWith("C") } },
                    overlayLanguageCodes = listOf("en")
                )
            )
        } else {
            emptyList()
        }

        val banks = if (banksJson.length() > 0) {
            List(banksJson.length()) { index -> bankFromJson(banksJson.getJSONObject(index)) }
        } else if (decks.isNotEmpty()) {
            decks.groupBy { it.bankId }.map { (bankId, bankDecks) ->
                CardsDeckBank(
                    id = bankId,
                    title = bankDecks.firstOrNull()?.bankTitle ?: "Card bank",
                    description = "Cards grouped from the completed web card bank structure.",
                    deckCount = bankDecks.size,
                    dueCards = bankDecks.sumOf { it.dueCards },
                    source = "service"
                )
            }
        } else {
            emptyList()
        }

        val parsedBuckets = bucketsFromJson(bucketsJson)
        val buckets = if (parsedBuckets.difficult.isEmpty() && parsedBuckets.learned.isEmpty() && parsedBuckets.learning.isEmpty() && runtimeCards.isNotEmpty()) {
            bucketsFor(runtimeCards)
        } else {
            parsedBuckets
        }

        return CardsDashboardState(
            decks = decks,
            progress = List(progressJson.length()) { index -> progressFromJson(progressJson.getJSONObject(index)) },
            selectedDeckType = selectedDeckType,
            isLoading = false,
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() },
            banks = banks,
            buckets = buckets
        )
    }

    private fun sessionFromJson(
        json: JSONObject,
        fallback: CardsPracticeSession? = null,
        root: JSONObject = json
    ): CardsPracticeSession {
        val deck = json.optJSONObject("deck")?.let { deckFromJson(it, fallback?.deck?.type ?: CardsDeckType.Vocabulary) }
            ?: fallback?.deck
            ?: deckFromJson(JSONObject().put("id", firstNonBlank(json, "deck_id", "deckId", "mode")), CardsDeckType.Vocabulary)

        val cardsJson = json.optJSONArray("cards") ?: JSONArray()
        val directCards = if (cardsJson.length() > 0) {
            List(cardsJson.length()) { index -> cardFromJson(cardsJson.getJSONObject(index), deck.id) }
        } else {
            listOfNotNull(
                root.optJSONObject("first_card") ?: root.optJSONObject("firstCard"),
                root.optJSONObject("next_card") ?: root.optJSONObject("nextCard") ?: root.optJSONObject("card")
            ).map { cardFromJson(it, deck.id) }
        }

        val cards = when {
            directCards.isNotEmpty() && fallback != null -> mergeCards(fallback.cards, directCards)
            directCards.isNotEmpty() -> directCards
            else -> fallback?.cards.orEmpty()
        }

        val runtimeAdvanced = fallback != null && (
            root.has("next_card") ||
                root.has("nextCard") ||
                root.has("session_completed") ||
                root.has("sessionCompleted") ||
                root.has("completed")
            )
        val defaultIndex = if (runtimeAdvanced) fallback.currentCardIndex + 1 else fallback?.currentCardIndex ?: 0

        val answersJson = json.optJSONObject("answers")
        return CardsPracticeSession(
            id = firstNonBlank(json, "id", "session_id", "sessionId").ifBlank { fallback?.id ?: "cards-session" },
            deck = deck,
            cards = cards,
            currentCardIndex = json.optInt("current_card_index", json.optInt("currentCardIndex", defaultIndex)),
            answers = answersFromJson(answersJson, fallback?.answers.orEmpty()),
            mode = practiceModeFromApi(firstNonBlank(json, "mode"), fallback?.mode ?: CardsPracticeMode.Flip),
            releaseGate = firstNonBlank(json, "release_gate", "releaseGate").ifBlank { "Cards practice is ready." }
        )
    }

    private fun mergeCards(existing: List<StudyCard>, incoming: List<StudyCard>): List<StudyCard> {
        val incomingIds = incoming.map { it.id }.toSet()
        return existing.filterNot { it.id in incomingIds } + incoming
    }

    private fun bankFromJson(json: JSONObject): CardsDeckBank = CardsDeckBank(
        id = firstNonBlank(json, "id").ifBlank { "card-bank" },
        title = firstNonBlank(json, "title").ifBlank { "Card bank" },
        description = firstNonBlank(json, "description"),
        deckCount = json.optInt("deck_count", json.optInt("deckCount", 0)),
        dueCards = json.optInt("due_cards", json.optInt("dueCards", 0)),
        source = firstNonBlank(json, "source").ifBlank { "service" }
    )

    private fun deckFromJson(json: JSONObject, fallbackType: CardsDeckType): CardsDeck = CardsDeck(
        id = firstNonBlank(json, "id").ifBlank { "cards-deck" },
        title = firstNonBlank(json, "title").ifBlank { "Cards deck" },
        type = deckTypeFromApi(firstNonBlank(json, "type", "mode"), fallbackType),
        description = firstNonBlank(json, "description"),
        totalCards = json.optInt("total_cards", json.optInt("totalCards", 0)),
        dueCards = json.optInt("due_cards", json.optInt("dueCards", 0)),
        locked = json.optBoolean("locked", false),
        bankId = firstNonBlank(json, "bank_id", "bankId").ifBlank { "core" },
        bankTitle = firstNonBlank(json, "bank_title", "bankTitle").ifBlank { "Core cards" },
        cefrLevel = firstNonBlank(json, "cefr_level", "cefrLevel", "cefr").takeIf { it.isNotBlank() },
        overlayLanguageCodes = stringList(json.optJSONArray("overlay_language_codes") ?: json.optJSONArray("overlayLanguageCodes")).ifEmpty { listOf("en") }
    )

    private fun cardFromJson(json: JSONObject, fallbackDeckId: String): StudyCard {
        val front = firstNonBlank(json, "front", "front_text", "frontText", "term", "word")
        val answerValue = firstNonBlank(json, "answer_value", "answerValue", "meaning", "translation", "back").ifBlank { correctAnswerValue(json) }
        val prompt = firstNonBlank(json, "prompt", "back_prompt", "backPrompt", "follow_up_prompt", "followUpPrompt")
        val explanation = firstNonBlank(json, "explanation", "rule_summary", "ruleSummary")
        val back = firstNonBlank(json, "back", "back_text", "backText").ifBlank { answerValue.ifBlank { explanation.ifBlank { prompt } } }
        val example = firstNonBlank(json, "example", "context_text", "contextText", "stimulus_text", "stimulusText", "blank_template", "blankTemplate").ifBlank { explanation }
        val hint = firstNonBlank(json, "hint", "usage_note", "usageNote").ifBlank { prompt.ifBlank { explanation } }
        val parsedOverlays = overlayList(json.optJSONArray("overlays"))

        return StudyCard(
            id = firstNonBlank(json, "id").ifBlank { "study-card" },
            deckId = firstNonBlank(json, "deck_id", "deckId").ifBlank { fallbackDeckId },
            front = front,
            back = back,
            example = example,
            hint = hint,
            tags = tagsFromJson(json),
            overlays = parsedOverlays.ifEmpty { synthesizedOverlay(back = back, example = example, hint = hint) },
            nextReviewText = firstNonBlank(json, "next_review_text", "nextReviewText", "next_review", "nextReview").takeIf { it.isNotBlank() },
            state = cardStateFromApi(firstNonBlank(json, "state")),
            seenCount = json.optInt("seen_count", json.optInt("seenCount", 0)),
            correctRate = correctRateFromJson(json),
            dueNow = json.optBoolean("due_now", json.optBoolean("dueNow", cardStateFromApi(firstNonBlank(json, "state")) != CardsCardState.Mastered)),
            audioSegments = audioSegmentsFromJson(json.optJSONObject("audio")),
            audioTranscriptVisible = json.optJSONObject("audio")?.optBoolean("transcript_visible", false) ?: false
        )
    }

    private fun overlayList(array: JSONArray?): List<CardI18nOverlay> = if (array == null) emptyList() else List(array.length()) { index ->
        val json = array.getJSONObject(index)
        CardI18nOverlay(
            languageCode = firstNonBlank(json, "language_code", "languageCode", "code").ifBlank { "en" },
            meaning = firstNonBlank(json, "meaning", "back", "answer_value", "answerValue"),
            example = firstNonBlank(json, "example", "context_text", "contextText"),
            hint = firstNonBlank(json, "hint", "prompt"),
            source = firstNonBlank(json, "source").ifBlank { "service" }
        )
    }

    private fun audioSegmentsFromJson(audio: JSONObject?): List<CardAudioSegment> {
        val segments = audio?.optJSONArray("segments") ?: return emptyList()
        return List(segments.length()) { index ->
            val json = segments.getJSONObject(index)
            CardAudioSegment(
                url = firstNonBlank(json, "url"),
                speakerLabel = firstNonBlank(json, "speaker_label", "speakerLabel"),
                durationSeconds = json.optDouble("duration_seconds", json.optDouble("durationSeconds", 0.0)),
                sequenceIndex = json.optInt("sequence_index", json.optInt("sequenceIndex", index))
            )
        }.filter { it.url.isNotBlank() }
    }

    private fun synthesizedOverlay(back: String, example: String, hint: String): List<CardI18nOverlay> =
        if (back.isBlank() && example.isBlank() && hint.isBlank()) {
            emptyList()
        } else {
            listOf(
                CardI18nOverlay(
                    languageCode = "en",
                    meaning = back,
                    example = example,
                    hint = hint,
                    source = "runtime-card-contract"
                )
            )
        }

    private fun bucketsFromJson(json: JSONObject?): CardBankBuckets {
        if (json == null) return CardBankBuckets()
        return CardBankBuckets(
            difficult = cardList(json.optJSONArray("difficult")),
            learned = cardList(json.optJSONArray("learned") ?: json.optJSONArray("mastered")),
            learning = cardList(json.optJSONArray("learning"))
        )
    }

    private fun bucketsFor(cards: List<StudyCard>): CardBankBuckets {
        val learned = cards.filter { it.state == CardsCardState.Mastered }
        val difficult = cards.filter { it.state == CardsCardState.Difficult || ((it.correctRate ?: 1.0) <= 0.45 && it.seenCount >= 4) }
        val learning = cards.filter { card -> learned.none { it.id == card.id } && difficult.none { it.id == card.id } }
        return CardBankBuckets(difficult = difficult, learned = learned, learning = learning)
    }

    private fun cardList(array: JSONArray?): List<StudyCard> =
        if (array == null) emptyList() else List(array.length()) { index -> cardFromJson(array.getJSONObject(index), "cards") }

    private fun progressFromJson(json: JSONObject): CardsDeckProgress = CardsDeckProgress(
        deckId = firstNonBlank(json, "deck_id", "deckId"),
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

    private fun tagsFromJson(json: JSONObject): List<String> {
        val explicit = stringList(json.optJSONArray("tags"))
        val derived = listOf(
            firstNonBlank(json, "cefr"),
            firstNonBlank(json, "domain"),
            firstNonBlank(json, "profession"),
            firstNonBlank(json, "content_type", "contentType"),
            firstNonBlank(json, "mode")
        )
        return (explicit + derived).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun stringList(array: JSONArray?): List<String> =
        if (array == null) emptyList() else List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }

    private fun firstNonBlank(json: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val value = json.opt(key)
            when (value) {
                is JSONObject -> firstNonBlank(value, "value", "text", "label", "answer").takeIf { it.isNotBlank() }?.let { return it }
                is JSONArray -> Unit
                null -> Unit
                else -> value.toString().trim().takeIf { it.isNotBlank() && it != "null" }?.let { return it }
            }
        }
        return ""
    }

    private fun correctAnswerValue(json: JSONObject): String {
        val value = json.opt("correct_answer") ?: json.opt("correctAnswer")
        return when (value) {
            is JSONObject -> firstNonBlank(value, "value", "text", "label", "answer")
            null -> ""
            else -> value.toString()
        }.trim()
    }
}

private fun CardsDeckType.apiName(): String = name.lowercase()
private fun CardsPracticeMode.apiName(): String = name.lowercase()
private fun CardsReviewRating.apiName(): String = name.lowercase()

private fun CardsDeckType.label(): String = when (this) {
    CardsDeckType.Vocabulary -> "Vocabulary"
    CardsDeckType.Phrases -> "Phrases"
    CardsDeckType.Grammar -> "Grammar"
    CardsDeckType.Work -> "Work Finnish"
    CardsDeckType.Yki -> "YKI"
    CardsDeckType.Review -> "Review"
}

private fun deckTypeFromApi(value: String?, fallback: CardsDeckType): CardsDeckType = when (value?.trim()?.lowercase()) {
    "vocabulary" -> CardsDeckType.Vocabulary
    "phrases", "sentence", "sentences" -> CardsDeckType.Phrases
    "grammar" -> CardsDeckType.Grammar
    "work", "professional" -> CardsDeckType.Work
    "yki" -> CardsDeckType.Yki
    "review" -> CardsDeckType.Review
    else -> fallback
}

private fun practiceModeFromApi(value: String?, fallback: CardsPracticeMode): CardsPracticeMode = when (value?.trim()?.lowercase()) {
    "type_answer", "typeanswer", "typed_recall" -> CardsPracticeMode.TypeAnswer
    "multiple_choice", "multiplechoice", "recognition" -> CardsPracticeMode.MultipleChoice
    "review" -> CardsPracticeMode.Review
    "flip" -> CardsPracticeMode.Flip
    else -> fallback
}

private fun correctRateFromJson(json: JSONObject): Double? = when {
    json.has("correct_rate") -> json.optDouble("correct_rate")
    json.has("correctRate") -> json.optDouble("correctRate")
    else -> null
}

private fun cardStateFromApi(value: String?): CardsCardState = when (value?.trim()?.lowercase()) {
    "mastered", "learned" -> CardsCardState.Mastered
    "difficult" -> CardsCardState.Difficult
    "learning" -> CardsCardState.Learning
    "new", "fresh", "fresh_card" -> CardsCardState.New
    else -> CardsCardState.New
}

private fun reviewRatingFromApi(value: String?): CardsReviewRating = when (value?.trim()?.lowercase()) {
    "hard" -> CardsReviewRating.Hard
    "good" -> CardsReviewRating.Good
    "easy" -> CardsReviewRating.Easy
    else -> CardsReviewRating.Again
}
