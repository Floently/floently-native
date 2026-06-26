package com.floently.learn.professional

enum class ProfessionalFinnishDomain {
    Healthcare,
    Office,
    CustomerService,
    JobSearch,
    Safety,
    SmallTalk
}

enum class ProfessionalFinnishScenarioType {
    PhrasePractice,
    DialoguePractice,
    EmailWriting,
    MeetingResponse,
    PhoneCall
}

data class ProfessionalFinnishModule(
    val id: String,
    val title: String,
    val domain: ProfessionalFinnishDomain,
    val description: String,
    val estimatedMinutes: Int,
    val locked: Boolean
)

data class ProfessionalFinnishPhrase(
    val finnish: String,
    val english: String,
    val usageNote: String
)

data class ProfessionalFinnishScenario(
    val id: String,
    val moduleId: String,
    val title: String,
    val type: ProfessionalFinnishScenarioType,
    val context: String,
    val prompt: String,
    val modelPhrases: List<ProfessionalFinnishPhrase>,
    val releaseGate: String
)

data class ProfessionalFinnishSession(
    val id: String,
    val module: ProfessionalFinnishModule,
    val scenarios: List<ProfessionalFinnishScenario>,
    val currentScenarioIndex: Int,
    val responses: Map<String, String>
) {
    val currentScenario: ProfessionalFinnishScenario?
        get() = scenarios.getOrNull(currentScenarioIndex)

    val completed: Boolean
        get() = scenarios.isNotEmpty() && currentScenarioIndex >= scenarios.size
}

data class ProfessionalFinnishProgress(
    val moduleId: String,
    val completedScenarios: Int,
    val totalScenarios: Int,
    val lastPracticeSummary: String?
)

data class ProfessionalFinnishDashboardState(
    val modules: List<ProfessionalFinnishModule>,
    val progress: List<ProfessionalFinnishProgress>,
    val selectedDomain: ProfessionalFinnishDomain,
    val isLoading: Boolean,
    val errorMessage: String?
)
