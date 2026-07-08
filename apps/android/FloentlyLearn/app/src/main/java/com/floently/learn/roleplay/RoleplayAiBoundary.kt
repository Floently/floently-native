package com.floently.learn.roleplay

data class RoleplayAiRequest(
    val scenario: RoleplayScenario,
    val coachingMode: RoleplayCoachingMode,
    val visibleMessages: List<RoleplayMessage>,
    val userMessage: String,
    val blockedPhrases: List<String>
)

data class RoleplayAiResponse(
    val assistantText: String,
    val coachingNote: String,
    val suggestedPhrase: String?
)

interface RoleplayAiClient {
    suspend fun generate(request: RoleplayAiRequest): RoleplayAiResponse
}

class PreviewRoleplayAiClient : RoleplayAiClient {
    override suspend fun generate(request: RoleplayAiRequest): RoleplayAiResponse {
        val phraseHint = request.scenario.targetPhrases.firstOrNull()
        val repetitionHint = if (request.blockedPhrases.isEmpty()) {
            ""
        } else {
            " Try a new phrase instead of repeating: ${request.blockedPhrases.joinToString()}."
        }

        return RoleplayAiResponse(
            assistantText = when (request.scenario.level) {
                RoleplayLevel.A1_A2 -> "Hyvä. Vastaa lyhyesti: voitko kertoa vielä yhden asian?"
                RoleplayLevel.B1_B2 -> "Kiitos. Se kuulostaa selkeältä. Miten jatkaisit keskustelua?"
                RoleplayLevel.C1_C2 -> "Hyvä perustelu. Voitko tarkentaa näkökulmaasi yhdellä esimerkillä?"
            },
            coachingNote = "Preview roleplay reply. Production must call the OpenAI-backed conversation service with anti-repetition and beginner-safe checks.$repetitionHint",
            suggestedPhrase = phraseHint
        )
    }
}
