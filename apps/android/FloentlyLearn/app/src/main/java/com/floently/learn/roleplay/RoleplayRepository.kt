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
        return runCatching { service.dashboard(selectedLevel) }.getOrElse {
            fallback.dashboard(selectedLevel)
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
                text = "$safeCue ${session.scenario.coachingMode.turnHint()}",
                coachingNote = session.scenario.coachingMode.practiceNote()
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
            id = "roleplay-a1a2-shop",
            title = "At the shop",
            level = RoleplayLevel.A1_A2,
            type = RoleplayScenarioType.Everyday,
            description = "Buy something simple, ask the price, and answer politely.",
            openingLine = "Hei! Miten voin auttaa?",
            targetPhrases = listOf("Haluaisin tämän.", "Paljonko tämä maksaa?", "Kiitos, se sopii."),
            beginnerSafe = true,
            locked = false,
            coachingMode = RoleplayCoachingMode.BeginnerSafe,
            materialSource = "Generated fallback",
            recommended = true
        ),
        RoleplayScenario(
            id = "roleplay-a1a2-cafe",
            title = "At the cafe",
            level = RoleplayLevel.A1_A2,
            type = RoleplayScenarioType.Service,
            description = "Order a drink, confirm the size, and say thank you.",
            openingLine = "Hei! Mitä saisi olla?",
            targetPhrases = listOf("Yksi kahvi, kiitos.", "Pieni koko, kiitos.", "Voinko maksaa kortilla?"),
            beginnerSafe = true,
            locked = false,
            coachingMode = RoleplayCoachingMode.BeginnerSafe,
            materialSource = "Generated fallback"
        ),
        RoleplayScenario(
            id = "roleplay-a1a2-health",
            title = "Doctor appointment",
            level = RoleplayLevel.A1_A2,
            type = RoleplayScenarioType.Healthcare,
            description = "Explain a simple symptom and ask what happens next.",
            openingLine = "Hei, mikä sinulla on vaivana?",
            targetPhrases = listOf("Minulla on päänsärky.", "Se alkoi eilen.", "Mitä minun pitäisi tehdä?"),
            beginnerSafe = true,
            locked = false,
            coachingMode = RoleplayCoachingMode.BeginnerSafe,
            materialSource = "Generated fallback"
        ),
        RoleplayScenario(
            id = "roleplay-b1b2-interview",
            title = "Job interview",
            level = RoleplayLevel.B1_B2,
            type = RoleplayScenarioType.Interview,
            description = "Practice concise interview answers with coaching.",
            openingLine = "Tervetuloa haastatteluun. Kerro lyhyesti itsestäsi.",
            targetPhrases = listOf("Minulla on kokemusta...", "Olen kiinnostunut tehtävästä, koska...", "Vahvuuteni on..."),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.Professional,
            materialSource = "Generated fallback",
            recommended = true
        ),
        RoleplayScenario(
            id = "roleplay-b1b2-service-problem",
            title = "Service problem",
            level = RoleplayLevel.B1_B2,
            type = RoleplayScenarioType.Service,
            description = "Explain a problem clearly and ask for a practical solution.",
            openingLine = "Hei, kerro miten voin auttaa tässä tilanteessa.",
            targetPhrases = listOf("Ongelma on se, että...", "Tarvitsisin ratkaisun tänään.", "Voisimmeko sopia näin?"),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.Natural,
            materialSource = "Generated fallback"
        ),
        RoleplayScenario(
            id = "roleplay-b1b2-professional-phone",
            title = "Professional phone call",
            level = RoleplayLevel.B1_B2,
            type = RoleplayScenarioType.PhoneCall,
            description = "Practice a clear professional phone call with speech capture and transcript review.",
            openingLine = "Hyvää päivää, miten voin auttaa?",
            targetPhrases = listOf("Soitan koskien asiaa...", "Voisin tarkentaa vielä...", "Sopiiko, että palaan asiaan?"),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.Professional,
            materialSource = "Generated fallback"
        ),
        RoleplayScenario(
            id = "roleplay-c1c2-work-negotiation",
            title = "Workplace negotiation",
            level = RoleplayLevel.C1_C2,
            type = RoleplayScenarioType.Work,
            description = "Negotiate timing, explain constraints, and keep the tone precise.",
            openingLine = "Meidän pitäisi sopia tästä aikataulusta tänään.",
            targetPhrases = listOf("Ymmärrän tilanteen kokonaiskuvan.", "Minun näkökulmastani keskeistä on...", "Voisimmeko tehdä kompromissin?"),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.ExamStyle,
            materialSource = "Generated fallback",
            recommended = true
        ),
        RoleplayScenario(
            id = "roleplay-c1c2-policy-discussion",
            title = "Policy discussion",
            level = RoleplayLevel.C1_C2,
            type = RoleplayScenarioType.Work,
            description = "Defend a viewpoint, give reasons, and respond diplomatically.",
            openingLine = "Miten perustelisit tämän muutoksen työyhteisölle?",
            targetPhrases = listOf("Perusteluni on se, että...", "Toisaalta on huomioitava...", "Ehdotan, että etenemme vaiheittain."),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.ExamStyle,
            materialSource = "Generated fallback"
        ),
        RoleplayScenario(
            id = "roleplay-c1c2-exam-opinion",
            title = "Advanced opinion task",
            level = RoleplayLevel.C1_C2,
            type = RoleplayScenarioType.Interview,
            description = "Give a structured opinion with a clear reason and example.",
            openingLine = "Mitä mieltä olet tästä väitteestä? Perustele vastauksesi.",
            targetPhrases = listOf("Olen osittain samaa mieltä.", "Käytännön esimerkkinä voisi mainita...", "Johtopäätökseni on..."),
            beginnerSafe = false,
            locked = false,
            coachingMode = RoleplayCoachingMode.ExamStyle,
            materialSource = "Generated fallback"
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
                        text = scenario.coachingMode.openingCue(),
                        coachingNote = scenario.coachingMode.practiceNote()
                    )
                ),
                learnerTurns = 0,
                repeatedCueCount = 0,
                releaseGate = "Roleplay uses backend/generated material when available and a generated safety fallback when offline."
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

private fun RoleplayCoachingMode.openingCue(): String = when (this) {
    RoleplayCoachingMode.BeginnerSafe -> "Vastaa lyhyesti. Yksi helppo lause riittää."
    RoleplayCoachingMode.Natural -> "Respond naturally. Keep the answer short and useful."
    RoleplayCoachingMode.Professional -> "Respond clearly and professionally. Add one useful detail."
    RoleplayCoachingMode.ExamStyle -> "Answer like an oral exam: clear reason, complete sentence, polite tone."
}

private fun RoleplayCoachingMode.turnHint(): String = when (this) {
    RoleplayCoachingMode.BeginnerSafe -> "Yksi lyhyt lause riittää."
    RoleplayCoachingMode.Natural -> "Make it sound natural and conversational."
    RoleplayCoachingMode.Professional -> "Keep the tone professional and specific."
    RoleplayCoachingMode.ExamStyle -> "Use a complete sentence and explain your reason."
}

private fun RoleplayCoachingMode.practiceNote(): String = when (this) {
    RoleplayCoachingMode.BeginnerSafe -> "Beginner-safe mode: short, simple Finnish is enough."
    RoleplayCoachingMode.Natural -> "Natural mode: answer like a real conversation, not a translation exercise."
    RoleplayCoachingMode.Professional -> "Professional mode: stay polite, direct, and work-ready."
    RoleplayCoachingMode.ExamStyle -> "Exam-style mode: answer fully enough for spoken Finnish assessment practice."
}
