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

class ServiceRoleplayRepository(
    private val service: RoleplayService,
    private val fallback: RoleplayRepository = PreviewRoleplayRepository()
) : RoleplayRepository {
    override suspend fun dashboard(selectedLevel: RoleplayLevel): RoleplayDashboardState {
        return runCatching { service.dashboard(selectedLevel) }.getOrElse { error ->
            fallback.dashboard(selectedLevel).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Roleplay is temporarily unavailable. Try again soon."
            )
        }
    }

    override suspend fun startSession(scenarioId: String): RoleplaySessionResult {
        return runCatching { service.startSession(scenarioId) }.getOrElse {
            fallback.startSession(scenarioId)
        }
    }

    override suspend fun sendLearnerMessage(session: RoleplaySession, text: String): RoleplaySessionResult {
        return runCatching { service.sendLearnerMessage(session, text) }.getOrElse {
            fallback.sendLearnerMessage(session, text)
        }
    }
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
                coachingNote = "Practice tip: keep your answer short, clear, and natural."
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
            description = "Buy something simple, ask the price, and answer politely.",
            openingLine = "Hei! Miten voin auttaa?",
            targetPhrases = listOf("Haluaisin tämän.", "Paljonko tämä maksaa?", "Kiitos, se sopii."),
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-a1-cafe",
            title = "At the cafe",
            level = RoleplayLevel.A1,
            type = RoleplayScenarioType.Service,
            description = "Order a drink, confirm the size, and say thank you.",
            openingLine = "Hei! Mitä saisi olla?",
            targetPhrases = listOf("Yksi kahvi, kiitos.", "Pieni koko, kiitos.", "Voinko maksaa kortilla?"),
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-a1-meeting-friend",
            title = "Meeting a friend",
            level = RoleplayLevel.A1,
            type = RoleplayScenarioType.Everyday,
            description = "Greet someone, say how you are, and suggest a simple plan.",
            openingLine = "Hei! Mitä kuuluu?",
            targetPhrases = listOf("Hyvää kuuluu.", "Entä sinulle?", "Mennäänkö kävelylle?"),
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
            targetPhrases = listOf("Mihin aikaan vuoro alkaa?", "Se sopii minulle.", "Voinko vaihtaa vuoroa?"),
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-a2-doctor-appointment",
            title = "Doctor appointment",
            level = RoleplayLevel.A2,
            type = RoleplayScenarioType.Healthcare,
            description = "Explain a simple symptom and ask what happens next.",
            openingLine = "Hei, mikä sinulla on vaivana?",
            targetPhrases = listOf("Minulla on päänsärky.", "Se alkoi eilen.", "Mitä minun pitäisi tehdä?"),
            beginnerSafe = true,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-a2-phone-time",
            title = "Short phone call",
            level = RoleplayLevel.A2,
            type = RoleplayScenarioType.PhoneCall,
            description = "Say who you are, why you call, and confirm a time.",
            openingLine = "Hyvää päivää, miten voin auttaa?",
            targetPhrases = listOf("Tässä on...", "Soitan ajan takia.", "Voisitteko toistaa?"),
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
            targetPhrases = listOf("Minulla on kokemusta...", "Olen kiinnostunut tehtävästä, koska...", "Vahvuuteni on..."),
            beginnerSafe = false,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-b1-service-problem",
            title = "Service problem",
            level = RoleplayLevel.B1,
            type = RoleplayScenarioType.Service,
            description = "Explain a problem clearly and ask for a practical solution.",
            openingLine = "Hei, kerro miten voin auttaa tässä tilanteessa.",
            targetPhrases = listOf("Ongelma on se, että...", "Tarvitsisin ratkaisun tänään.", "Voisimmeko sopia näin?"),
            beginnerSafe = false,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-b2-professional-phone",
            title = "Professional phone call",
            level = RoleplayLevel.B2,
            type = RoleplayScenarioType.PhoneCall,
            description = "Practice a clear professional phone call with speech capture and transcript review.",
            openingLine = "Hyvää päivää, miten voin auttaa?",
            targetPhrases = listOf("Soitan koskien asiaa...", "Voisin tarkentaa vielä...", "Sopiiko, että palaan asiaan?"),
            beginnerSafe = false,
            locked = false
        ),
        RoleplayScenario(
            id = "roleplay-b2-work-negotiation",
            title = "Workplace negotiation",
            level = RoleplayLevel.B2,
            type = RoleplayScenarioType.Work,
            description = "Negotiate timing, explain constraints, and keep the tone professional.",
            openingLine = "Meidän pitäisi sopia tästä aikataulusta tänään.",
            targetPhrases = listOf("Ymmärrän tilanteen.", "Minun näkökulmastani...", "Voisimmeko tehdä kompromissin?"),
            beginnerSafe = false,
            locked = false
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
            return RoleplaySessionResult.Blocked("This roleplay needs audio practice before it can be opened.")
        }

        return RoleplaySessionResult.Ready(
            RoleplaySession(
                id = "session-$scenarioId-local",
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
                        coachingNote = "Use the target phrases if they fit the situation."
                    )
                ),
                learnerTurns = 0,
                repeatedCueCount = 0,
                releaseGate = "Conversation practice is ready for speech verification."
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
