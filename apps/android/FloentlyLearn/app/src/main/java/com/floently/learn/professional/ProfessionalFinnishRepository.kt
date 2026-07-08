package com.floently.learn.professional

interface ProfessionalFinnishRepository {
    suspend fun dashboard(selectedDomain: ProfessionalFinnishDomain): ProfessionalFinnishDashboardState
    suspend fun startSession(moduleId: String): ProfessionalFinnishSessionResult
    suspend fun saveResponse(session: ProfessionalFinnishSession, scenarioId: String, response: String): ProfessionalFinnishSession
}

sealed interface ProfessionalFinnishSessionResult {
    data class Ready(val session: ProfessionalFinnishSession) : ProfessionalFinnishSessionResult
    data class Blocked(val reason: String) : ProfessionalFinnishSessionResult
    data class Error(val message: String) : ProfessionalFinnishSessionResult
}

class PreviewProfessionalFinnishRepository : ProfessionalFinnishRepository {
    private val modules = listOf(
        ProfessionalFinnishModule(
            id = "professional-healthcare-a1-help",
            title = "Nurse basics: greeting and help",
            domain = ProfessionalFinnishDomain.Healthcare,
            description = "A1 workplace Finnish for greeting a patient, offering help, and checking simple needs.",
            estimatedMinutes = 8,
            cefrLevel = ProfessionalFinnishLevel.A1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-healthcare-b1-handover",
            title = "Healthcare handover Finnish",
            domain = ProfessionalFinnishDomain.Healthcare,
            description = "B1 safe, clear Finnish for patient handover, shift updates, and daily ward communication.",
            estimatedMinutes = 12,
            cefrLevel = ProfessionalFinnishLevel.B1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-healthcare-c1-doctor",
            title = "Doctor: explain care clearly",
            domain = ProfessionalFinnishDomain.Healthcare,
            description = "C1 professional Finnish for explaining symptoms, treatment plans, risk, and follow-up calmly.",
            estimatedMinutes = 16,
            cefrLevel = ProfessionalFinnishLevel.C1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-office-a2-meeting",
            title = "Office meeting basics",
            domain = ProfessionalFinnishDomain.Office,
            description = "A2 meeting Finnish for joining a call, asking for repetition, and confirming tasks.",
            estimatedMinutes = 10,
            cefrLevel = ProfessionalFinnishLevel.A2,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-office-b2-report",
            title = "Report and task update",
            domain = ProfessionalFinnishDomain.Office,
            description = "B2 work communication for progress updates, blockers, reports, and next steps.",
            estimatedMinutes = 14,
            cefrLevel = ProfessionalFinnishLevel.B2,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-office-c2-expert",
            title = "Expert workplace argument",
            domain = ProfessionalFinnishDomain.Office,
            description = "C2 advanced Finnish for presenting trade-offs, disagreeing diplomatically, and defending a recommendation.",
            estimatedMinutes = 18,
            cefrLevel = ProfessionalFinnishLevel.C2,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-customer-a2-phone",
            title = "Phone service Finnish",
            domain = ProfessionalFinnishDomain.CustomerService,
            description = "A2 telephone Finnish for opening a call, identifying the issue, and promising a follow-up.",
            estimatedMinutes = 9,
            cefrLevel = ProfessionalFinnishLevel.A2,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-customer-b1-practical-nurse",
            title = "Practical nurse: family update",
            domain = ProfessionalFinnishDomain.CustomerService,
            description = "B1 practical-nurse communication for a clear family update without sharing unnecessary details.",
            estimatedMinutes = 12,
            cefrLevel = ProfessionalFinnishLevel.B1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-job-b1-interview",
            title = "Interview: tell your background",
            domain = ProfessionalFinnishDomain.JobSearch,
            description = "B1 interview Finnish for education, work history, strengths, and availability.",
            estimatedMinutes = 15,
            cefrLevel = ProfessionalFinnishLevel.B1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-job-c1-specialist",
            title = "Interview: specialist value",
            domain = ProfessionalFinnishDomain.JobSearch,
            description = "C1 interview Finnish for explaining impact, regulated work, and why your profile fits the role.",
            estimatedMinutes = 18,
            cefrLevel = ProfessionalFinnishLevel.C1,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-safety-b2-incident",
            title = "Work safety incident report",
            domain = ProfessionalFinnishDomain.Safety,
            description = "B2 Finnish for reporting hazards, incidents, prevention steps, and responsibility clearly.",
            estimatedMinutes = 14,
            cefrLevel = ProfessionalFinnishLevel.B2,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-smalltalk-a1-breakroom",
            title = "Coffee room small talk",
            domain = ProfessionalFinnishDomain.SmallTalk,
            description = "A1 friendly workplace Finnish for greetings, breaks, schedules, and simple everyday talk.",
            estimatedMinutes = 7,
            cefrLevel = ProfessionalFinnishLevel.A1,
            locked = false
        )
    )

    private val scenariosByModuleId = mapOf(
        "professional-healthcare-a1-help" to listOf(
            ProfessionalFinnishScenario(
                id = "healthcare-a1-help-1",
                moduleId = "professional-healthcare-a1-help",
                title = "Offer simple help",
                type = ProfessionalFinnishScenarioType.PhrasePractice,
                context = "You enter a patient room and need to greet the patient and ask what they need.",
                prompt = "Write two short Finnish sentences: greet the patient and ask if they need help.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Hei, miten voitte tänään?",
                        english = "Hello, how are you today?",
                        usageNote = "Safe basic greeting for a patient."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Tarvitsetteko apua?",
                        english = "Do you need help?",
                        usageNote = "Polite A1 work question."
                    )
                ),
                releaseGate = "Healthcare A1 practice ready."
            )
        ),
        "professional-healthcare-b1-handover" to listOf(
            ProfessionalFinnishScenario(
                id = "healthcare-b1-handover-1",
                moduleId = "professional-healthcare-b1-handover",
                title = "Short handover",
                type = ProfessionalFinnishScenarioType.DialoguePractice,
                context = "You need to give a short and clear update to a colleague at the end of a shift.",
                prompt = "Write how you would say: The patient slept well, ate breakfast, and needs help walking.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Potilas nukkui hyvin ja söi aamupalan.",
                        english = "The patient slept well and ate breakfast.",
                        usageNote = "Simple and safe ward update."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Hän tarvitsee apua kävelyssä.",
                        english = "They need help walking.",
                        usageNote = "Clear safety-focused helper language."
                    )
                ),
                releaseGate = "Healthcare B1 handover ready."
            )
        ),
        "professional-healthcare-c1-doctor" to listOf(
            ProfessionalFinnishScenario(
                id = "healthcare-c1-doctor-1",
                moduleId = "professional-healthcare-c1-doctor",
                title = "Explain the follow-up plan",
                type = ProfessionalFinnishScenarioType.DialoguePractice,
                context = "A patient is worried about symptoms and wants to understand the next step.",
                prompt = "Write a calm Finnish explanation: what you checked, what the plan is, and when the patient should contact care again.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Tutkimusten perusteella tilanne ei vaikuta kiireelliseltä.",
                        english = "Based on the examinations, the situation does not seem urgent.",
                        usageNote = "Professional but reassuring doctor language."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Ottakaa yhteyttä uudelleen, jos oireet pahenevat.",
                        english = "Contact us again if the symptoms get worse.",
                        usageNote = "Clear safety-net instruction."
                    )
                ),
                releaseGate = "Doctor C1 explanation ready."
            )
        ),
        "professional-office-a2-meeting" to listOf(
            ProfessionalFinnishScenario(
                id = "office-a2-meeting-1",
                moduleId = "professional-office-a2-meeting",
                title = "Ask for clarification",
                type = ProfessionalFinnishScenarioType.MeetingResponse,
                context = "You are in a team meeting and did not fully understand one point.",
                prompt = "Write a polite Finnish response asking the speaker to repeat or explain the last point.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Voisitko selittää tämän vielä uudelleen?",
                        english = "Could you explain this once more?",
                        usageNote = "Polite and direct in meetings."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "En ole varma, ymmärsinkö oikein.",
                        english = "I am not sure if I understood correctly.",
                        usageNote = "Useful when you need a safe clarification."
                    )
                ),
                releaseGate = "Office A2 meeting ready."
            )
        ),
        "professional-office-b2-report" to listOf(
            ProfessionalFinnishScenario(
                id = "office-b2-report-1",
                moduleId = "professional-office-b2-report",
                title = "Progress update",
                type = ProfessionalFinnishScenarioType.EmailWriting,
                context = "Your manager asks for a short written update about a task that is delayed.",
                prompt = "Write a concise Finnish update: what is done, what is blocked, and what you will do next.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Työ on edennyt suunnitellusti tähän vaiheeseen asti.",
                        english = "The work has progressed according to plan up to this stage.",
                        usageNote = "Neutral report opening."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Seuraavaksi selvitän esteen ja ehdotan ratkaisua.",
                        english = "Next I will investigate the blocker and propose a solution.",
                        usageNote = "Shows ownership and next step."
                    )
                ),
                releaseGate = "Office B2 reporting ready."
            )
        ),
        "professional-office-c2-expert" to listOf(
            ProfessionalFinnishScenario(
                id = "office-c2-expert-1",
                moduleId = "professional-office-c2-expert",
                title = "Defend a recommendation",
                type = ProfessionalFinnishScenarioType.MeetingResponse,
                context = "A stakeholder questions your proposal and asks why your option is better than a faster shortcut.",
                prompt = "Write a balanced Finnish answer that acknowledges the shortcut, explains the risk, and recommends a safer path.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Ymmärrän, että nopeampi ratkaisu houkuttelee, mutta siihen liittyy selkeä ylläpitoriski.",
                        english = "I understand that the faster solution is tempting, but it has a clear maintenance risk.",
                        usageNote = "Advanced diplomatic disagreement."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Suosittelen etenemään tavalla, joka kestää myös tuotantokäytössä.",
                        english = "I recommend proceeding in a way that also holds up in production use.",
                        usageNote = "C2 professional recommendation."
                    )
                ),
                releaseGate = "Office C2 expert argument ready."
            )
        ),
        "professional-customer-a2-phone" to listOf(
            ProfessionalFinnishScenario(
                id = "customer-a2-phone-1",
                moduleId = "professional-customer-a2-phone",
                title = "Open a call",
                type = ProfessionalFinnishScenarioType.PhoneCall,
                context = "You call a customer or colleague and need to start politely.",
                prompt = "Write the first three Finnish sentences: greeting, your name, and why you are calling.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Hei, täällä on Vitus Floentlystä.",
                        english = "Hello, this is Vitus from Floently.",
                        usageNote = "Natural phone opening."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Soitan asiastanne ja haluan varmistaa yhden tiedon.",
                        english = "I am calling about your matter and want to confirm one detail.",
                        usageNote = "Clear reason for calling."
                    )
                ),
                releaseGate = "Phone A2 practice ready."
            )
        ),
        "professional-customer-b1-practical-nurse" to listOf(
            ProfessionalFinnishScenario(
                id = "customer-b1-practical-nurse-1",
                moduleId = "professional-customer-b1-practical-nurse",
                title = "Family update",
                type = ProfessionalFinnishScenarioType.DialoguePractice,
                context = "A family member asks how the day went. You need to answer clearly and respectfully.",
                prompt = "Write a Finnish update that says the day went calmly, the client ate, and you will tell the nurse about one observation.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Päivä on mennyt rauhallisesti.",
                        english = "The day has gone calmly.",
                        usageNote = "Common practical-nurse update."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Kerron havainnosta sairaanhoitajalle.",
                        english = "I will tell the nurse about the observation.",
                        usageNote = "Shows safe escalation."
                    )
                ),
                releaseGate = "Practical nurse B1 update ready."
            )
        ),
        "professional-job-b1-interview" to listOf(
            ProfessionalFinnishScenario(
                id = "job-b1-interview-1",
                moduleId = "professional-job-b1-interview",
                title = "Tell your background",
                type = ProfessionalFinnishScenarioType.MeetingResponse,
                context = "An interviewer asks you to briefly describe your education and recent work.",
                prompt = "Write a Finnish answer that mentions your background, current learning, and what kind of work you want next.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Minulla on tausta terveydenhuollossa ja opiskelen parhaillaan tietotekniikkaa.",
                        english = "I have a background in healthcare and I am currently studying information technology.",
                        usageNote = "Clear career-transition answer."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Etsin tehtävää, jossa voin yhdistää käytännön kokemuksen ja tekniset taidot.",
                        english = "I am looking for a role where I can combine practical experience and technical skills.",
                        usageNote = "Good interview bridge."
                    )
                ),
                releaseGate = "Interview B1 practice ready."
            )
        ),
        "professional-job-c1-specialist" to listOf(
            ProfessionalFinnishScenario(
                id = "job-c1-specialist-1",
                moduleId = "professional-job-c1-specialist",
                title = "Explain specialist value",
                type = ProfessionalFinnishScenarioType.MeetingResponse,
                context = "A hiring manager asks why your mixed healthcare and technology background is useful.",
                prompt = "Write a confident Finnish answer connecting regulated environments, user needs, data, and practical delivery.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Vahvuuteni on se, että ymmärrän sekä käyttäjän arkea että teknisen toteutuksen vaatimuksia.",
                        english = "My strength is that I understand both the user's everyday work and the requirements of technical delivery.",
                        usageNote = "Strong specialist positioning."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Siksi pystyn auttamaan ratkaisuissa, joiden pitää toimia luotettavasti käytännössä.",
                        english = "That is why I can help with solutions that must work reliably in practice.",
                        usageNote = "Connects experience to business value."
                    )
                ),
                releaseGate = "Interview C1 specialist practice ready."
            )
        ),
        "professional-safety-b2-incident" to listOf(
            ProfessionalFinnishScenario(
                id = "safety-b2-incident-1",
                moduleId = "professional-safety-b2-incident",
                title = "Report a hazard",
                type = ProfessionalFinnishScenarioType.EmailWriting,
                context = "You noticed a workplace safety risk and must document it clearly.",
                prompt = "Write a Finnish report with: what happened, what risk it caused, and what should be done next.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Huomasin tilanteen, joka voi aiheuttaa turvallisuusriskin.",
                        english = "I noticed a situation that may cause a safety risk.",
                        usageNote = "Neutral incident-report opening."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Ehdotan, että asia tarkistetaan ennen työn jatkamista.",
                        english = "I suggest that the matter is checked before work continues.",
                        usageNote = "Clear prevention step."
                    )
                ),
                releaseGate = "Safety B2 incident report ready."
            )
        ),
        "professional-smalltalk-a1-breakroom" to listOf(
            ProfessionalFinnishScenario(
                id = "smalltalk-a1-breakroom-1",
                moduleId = "professional-smalltalk-a1-breakroom",
                title = "Coffee room greeting",
                type = ProfessionalFinnishScenarioType.PhrasePractice,
                context = "You meet a colleague in the break room before work starts.",
                prompt = "Write three short Finnish lines: greeting, simple question, and friendly answer.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Huomenta! Miten menee?",
                        english = "Good morning! How are you?",
                        usageNote = "Friendly workplace small talk."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Hyvin, kiitos. Entä sinulla?",
                        english = "Fine, thank you. And you?",
                        usageNote = "Simple reciprocal answer."
                    )
                ),
                releaseGate = "Small-talk A1 practice ready."
            )
        )
    )

    override suspend fun dashboard(selectedDomain: ProfessionalFinnishDomain): ProfessionalFinnishDashboardState {
        val visibleModules = modules.filter { it.domain == selectedDomain }
        return ProfessionalFinnishDashboardState(
            modules = visibleModules,
            progress = visibleModules.map { module ->
                ProfessionalFinnishProgress(
                    moduleId = module.id,
                    completedScenarios = 0,
                    totalScenarios = scenariosByModuleId[module.id].orEmpty().size,
                    lastPracticeSummary = "${module.cefrLevel.name} path · ${module.estimatedMinutes} min"
                )
            },
            selectedDomain = selectedDomain,
            isLoading = false,
            errorMessage = null
        )
    }

    override suspend fun startSession(moduleId: String): ProfessionalFinnishSessionResult {
        val module = modules.firstOrNull { it.id == moduleId }
            ?: return ProfessionalFinnishSessionResult.Error("Professional Finnish module was not found.")

        if (module.locked) {
            return ProfessionalFinnishSessionResult.Blocked("This Professional Finnish path is locked for this account.")
        }

        val scenarios = scenariosByModuleId[moduleId].orEmpty()
        if (scenarios.isEmpty()) {
            return ProfessionalFinnishSessionResult.Error("Professional Finnish module has no scenarios yet.")
        }

        return ProfessionalFinnishSessionResult.Ready(
            ProfessionalFinnishSession(
                id = "session-$moduleId-local",
                module = module,
                scenarios = scenarios,
                currentScenarioIndex = 0,
                responses = emptyMap()
            )
        )
    }

    override suspend fun saveResponse(
        session: ProfessionalFinnishSession,
        scenarioId: String,
        response: String
    ): ProfessionalFinnishSession = session.copy(
        responses = session.responses + (scenarioId to response),
        currentScenarioIndex = session.currentScenarioIndex + 1
    )
}
