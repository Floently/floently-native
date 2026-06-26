package com.floently.learn.roleplay

interface RoleplayRepository {
    suspend fun dashboard(selectedLevel: RoleplayLevel): RoleplayDashboardState
    suspend fun startSession(scenarioId: String): RoleplaySessionResult
    suspend fun sendLearnerMessage(session: RoleplaySession, text: String): RoleplaySessionResult
}

sealed interface RoleplaySessionResult {
    data class Ready(val session: RoleplaySession) : RoleplaySessionResult
    data class Blocked(val reason: String) : RoleplaySessionResult
    data class Error(val message: String) : RoleplaySessionResult
}

interface RoleplayConversationEngine {
    fun next(session: RoleplaySession, learnerText: String): RoleplayCoachResponse
}

class PreviewRoleplayConversationEngine : RoleplayConversationEngine {
    private val safeCues = listOf(
        "Hyvä alku. Vastaa yhdellä helpolla lauseella.",
        "Jatka rauhassa. Voit käyttää lyhyttä lausetta.",
        "Hyvin tehty. Kokeile lisätä yksi tarkka tieto.",
        "Selvä. Vastaa kohteliaasti ja pidä lause yksinkertaisena."
    )

    override fun next(session: RoleplaySession, learnerText: String): RoleplayCoachResponse {
        val normalized = learnerText.trim()
        val cueIndex = (session.learnerTurns + session.scenario.id.length) % safeCues.size
        val cue = safeCues[cueIndex]
        val repeatedPrevented = session.messages.takeLast(4).any { it.text == cue }

        val partnerText = when (session.scenario.type) {
            RoleplayScenarioType.Work -> "Kiitos. Voisitko kertoa vielä, milloin tämä sopii sinulle?"
            RoleplayScenarioType.Healthcare -> "Selvä. Tarvitsetko apua tai lisätietoja?"
            RoleplayScenarioType.Interview -> "Hyvä. Miksi olet kiinnostunut tästä tehtävästä?"
            RoleplayScenarioType.PhoneCall -> "Ymmärrän. Voitko toistaa nimesi ja asiasi lyhyesti?"
            RoleplayScenarioType.Service -> "Kiitos tiedosta. Miten voin auttaa seuraavaksi?"
            RoleplayScenarioType.Everyday -> "Kiva kuulla. Mitä haluaisit tehdä seuraavaksi?"
        }

        val safeCue = if (repeatedPrevented) {
            "Hyvä. Kokeile nyt erilaista vastausta kuin aiemmin."
        } else {
            cue
        }

        return RoleplayCoachResponse(
            partnerMessage = RoleplayMessage(
                id = "partner-${session.learnerTurns + 1}",
                speaker = RoleplaySpeaker.Partner,
                text = partnerText
            ),
            coachMessage = RoleplayMessage(
                id = "coach-${session.learnerTurns + 1}",
                speaker = RoleplaySpeaker.Coach,
                text = safeCue,
                coachingNote = "Preview coaching. OpenAI-backed dynamic generation is still behind the service boundary."
            ),
            repeatedCuePrevented = repeatedPrevented || normalized.isNotBlank()
        )
    }
}

class PreviewRoleplayRepository(
    private val engine: RoleplayConversationEngine = PreviewRoleplayConversationEngine()
) : RoleplayRepository {
    private val scenarios = listOf(
        RoleplayScenario(
            id = "roleplay-a1-shop",
            title = "At the shop",
            level = RoleplayLevel.A1,
            type = RoleplayScenarioType.Everyday,
            description = "Simple beginner-safe everyday Finnish conversation.",
            openingLine = "Hei! Miten voin auttaa?",
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-a2-work-schedule",
            title = "Work schedule",
            level = RoleplayLevel.A2,
            type = RoleplayScenarioType.Work,
            description = "Practice asking about shifts, availability, and timing.",
            openingLine = "Hei, sopiiko sinulle työvuoro maanantaina?",
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-b1-interview",
            title = "Job interview",
            level = RoleplayLevel.B1,
            type = RoleplayScenarioType.Interview,
            description = "Practice concise interview answers with coaching.",
            openingLine = "Tervetuloa haastatteluun. Kerro lyhyesti itsestäsi.",
            beginnerSafe = false,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-b2-phone",
            title = "Professional phone call",
            level = RoleplayLevel.B2,
            type = RoleplayScenarioType.PhoneCall,
            description = "Native speech-style roleplay remains gated until audio parity is complete.",
            openingLine = "Hyvää päivää, miten voin auttaa?",
            beginnerSafe = false,
            locked = true
        )
    )

    override suspend fun dashboard(selectedLevel: RoleplayLevel): RoleplayDashboardState =
        RoleplayDashboardState(
            scenarios = scenarios.filter { it.level == selectedLevel },
            selectedLevel = selectedLevel,
            isLoading = false,
            errorMessage = null
        )

    override suspend fun startSession(scenarioId: String): RoleplaySessionResult {
        val scenario = scenarios.firstOrNull { it.id == scenarioId }
            ?: return RoleplaySessionResult.Error("Roleplay scenario was not found.")

        if (scenario.locked) {
            return RoleplaySessionResult.Blocked("This roleplay is gated until native audio and service parity are complete.")
        }

        return RoleplaySessionResult.Ready(
            RoleplaySession(
                id = "session-$scenarioId-preview",
                scenario = scenario,
                messages = listOf(
                    RoleplayMessage(
                        id = "opening-$scenarioId",
                        speaker = RoleplaySpeaker.Partner,
                        text = scenario.openingLine
                    ),
                    RoleplayMessage(
                        id = "coach-opening-$scenarioId",
                        speaker = RoleplaySpeaker.Coach,
                        text = if (scenario.beginnerSafe) {
                            "Vastaa lyhyesti. Yksi helppo lause riittää."
                        } else {
                            "Respond naturally. Keep it clear and professional."
                        },
                        coachingNote = "Beginner-safe preview coaching."
                    )
                ),
                learnerTurns = 0,
                repeatedCueCount = 0,
                releaseGate = "OpenAI dynamic conversation, anti-repetition audit, audio parity, and durable progress are required before release."
            )
        )
    }

    override suspend fun sendLearnerMessage(session: RoleplaySession, text: String): RoleplaySessionResult {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return RoleplaySessionResult.Error("Write a reply before sending.")
        }

        val learnerMessage = RoleplayMessage(
            id = "learner-${session.learnerTurns + 1}",
            speaker = RoleplaySpeaker.Learner,
            text = cleanText
        )
        val response = engine.next(session, cleanText)
        val nextMessages = buildList {
            addAll(session.messages)
            add(learnerMessage)
            add(response.partnerMessage)
            response.coachMessage?.let { add(it) }
        }

        return RoleplaySessionResult.Ready(
            session.copy(
                messages = nextMessages,
                learnerTurns = session.learnerTurns + 1,
                repeatedCueCount = session.repeatedCueCount + if (response.repeatedCuePrevented) 1 else 0
            )
        )
    }
}
