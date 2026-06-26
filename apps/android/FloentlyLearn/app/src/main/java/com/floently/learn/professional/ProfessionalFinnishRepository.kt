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
            id = "professional-healthcare-handover-1",
            title = "Healthcare handover Finnish",
            domain = ProfessionalFinnishDomain.Healthcare,
            description = "Practice safe, clear Finnish for short patient handover and daily ward communication.",
            estimatedMinutes = 12,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-office-meeting-1",
            title = "Office meeting Finnish",
            domain = ProfessionalFinnishDomain.Office,
            description = "Practice polite meeting turns, agreeing, disagreeing, and asking for clarification.",
            estimatedMinutes = 10,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-job-search-1",
            title = "Job search Finnish",
            domain = ProfessionalFinnishDomain.JobSearch,
            description = "Practice recruiter messages, interview replies, and concise work-history answers.",
            estimatedMinutes = 15,
            locked = false
        ),
        ProfessionalFinnishModule(
            id = "professional-phone-call-1",
            title = "Phone call Finnish",
            domain = ProfessionalFinnishDomain.CustomerService,
            description = "Native phone-call flow is gated until speech and call-style interaction parity are complete.",
            estimatedMinutes = 8,
            locked = true
        )
    )

    private val scenariosByModuleId = mapOf(
        "professional-healthcare-handover-1" to listOf(
            ProfessionalFinnishScenario(
                id = "handover-1",
                moduleId = "professional-healthcare-handover-1",
                title = "Short handover",
                type = ProfessionalFinnishScenarioType.DialoguePractice,
                context = "You need to give a short and clear update to a colleague.",
                prompt = "Write how you would say: The patient slept well, ate breakfast, and needs help walking.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Potilas nukkui hyvin.",
                        english = "The patient slept well.",
                        usageNote = "Simple and safe ward update."
                    ),
                    ProfessionalFinnishPhrase(
                        finnish = "Hän tarvitsee apua kävelyssä.",
                        english = "They need help walking.",
                        usageNote = "Use clear helper language for safety."
                    )
                ),
                releaseGate = "Requires healthcare content parity review before release."
            )
        ),
        "professional-office-meeting-1" to listOf(
            ProfessionalFinnishScenario(
                id = "meeting-1",
                moduleId = "professional-office-meeting-1",
                title = "Ask for clarification",
                type = ProfessionalFinnishScenarioType.MeetingResponse,
                context = "You are in a team meeting and did not fully understand one point.",
                prompt = "Write a polite Finnish response asking the speaker to repeat or explain the last point.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Voisitko selittää tämän vielä uudelleen?",
                        english = "Could you explain this once more?",
                        usageNote = "Polite and direct in meetings."
                    )
                ),
                releaseGate = "Requires professional scenario parity review before release."
            )
        ),
        "professional-job-search-1" to listOf(
            ProfessionalFinnishScenario(
                id = "job-search-1",
                moduleId = "professional-job-search-1",
                title = "Reply to recruiter",
                type = ProfessionalFinnishScenarioType.EmailWriting,
                context = "A recruiter asks when you are available for an interview.",
                prompt = "Write a short Finnish reply confirming availability and thanking them.",
                modelPhrases = listOf(
                    ProfessionalFinnishPhrase(
                        finnish = "Kiitos viestistä. Olen käytettävissä haastatteluun ensi viikolla.",
                        english = "Thank you for the message. I am available for an interview next week.",
                        usageNote = "Useful for simple interview scheduling."
                    )
                ),
                releaseGate = "Requires job-search content parity review before release."
            )
        ),
        "professional-phone-call-1" to listOf(
            ProfessionalFinnishScenario(
                id = "phone-1",
                moduleId = "professional-phone-call-1",
                title = "Opening a call",
                type = ProfessionalFinnishScenarioType.PhoneCall,
                context = "You call a customer or colleague.",
                prompt = "Start the call politely and state why you are calling.",
                modelPhrases = emptyList(),
                releaseGate = "Requires native speech/call interaction parity before release."
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
                    lastPracticeSummary = null
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
            return ProfessionalFinnishSessionResult.Blocked("This module is still gated until native interaction parity is complete.")
        }

        val scenarios = scenariosByModuleId[moduleId].orEmpty()
        if (scenarios.isEmpty()) {
            return ProfessionalFinnishSessionResult.Error("Professional Finnish module has no scenarios yet.")
        }

        return ProfessionalFinnishSessionResult.Ready(
            ProfessionalFinnishSession(
                id = "session-$moduleId-preview",
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
