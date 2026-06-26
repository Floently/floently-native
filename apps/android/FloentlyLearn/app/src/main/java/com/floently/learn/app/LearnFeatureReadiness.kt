package com.floently.learn.app

import com.floently.learn.navigation.LearnFeatureDestination

enum class LearnNativeReadinessStatus {
    VerifiedNativeFoundation,
    PendingNativeVerification,
    ServiceParityGated,
    AccessGated
}

data class LearnFeatureReadiness(
    val destination: LearnFeatureDestination,
    val status: LearnNativeReadinessStatus,
    val nativeSummary: String,
    val remainingGate: String,
    val verifiedCommit: String?
)

object PreviewLearnFeatureReadiness {
    val items: Map<LearnFeatureDestination, LearnFeatureReadiness> = mapOf(
        LearnFeatureDestination.YkiPractice to LearnFeatureReadiness(
            destination = LearnFeatureDestination.YkiPractice,
            status = LearnNativeReadinessStatus.VerifiedNativeFoundation,
            nativeSummary = "Native modules, task sessions, answer capture, evaluation summary, and progress boundary are in place.",
            remainingGate = "Backend scoring, durable progress, and full content parity are still gated.",
            verifiedCommit = "cf6e93e"
        ),
        LearnFeatureDestination.ProfessionalFinnish to LearnFeatureReadiness(
            destination = LearnFeatureDestination.ProfessionalFinnish,
            status = LearnNativeReadinessStatus.VerifiedNativeFoundation,
            nativeSummary = "Native workplace scenario dashboard, phrase cards, response capture, and progress animation are in place.",
            remainingGate = "Professional feedback, audio, and durable progress are still gated.",
            verifiedCommit = "8e7eb70"
        ),
        LearnFeatureDestination.Roleplay to LearnFeatureReadiness(
            destination = LearnFeatureDestination.Roleplay,
            status = LearnNativeReadinessStatus.VerifiedNativeFoundation,
            nativeSummary = "Native roleplay dashboard, conversation flow, coaching notes, and anti-repetition state are in place.",
            remainingGate = "OpenAI dynamic service, speech, audit, and durable progress are still gated.",
            verifiedCommit = "14d4e80"
        ),
        LearnFeatureDestination.Cards to LearnFeatureReadiness(
            destination = LearnFeatureDestination.Cards,
            status = LearnNativeReadinessStatus.VerifiedNativeFoundation,
            nativeSummary = "Native card deck models, repository boundary, dashboard, and start-session boundary are in place.",
            remainingGate = "Full swipe/flip practice, spaced repetition sync, and durable progress are still gated.",
            verifiedCommit = "57b6573"
        ),
        LearnFeatureDestination.Progress to LearnFeatureReadiness(
            destination = LearnFeatureDestination.Progress,
            status = LearnNativeReadinessStatus.VerifiedNativeFoundation,
            nativeSummary = "Native progress overview, repository boundary, and animated completion bars are in place.",
            remainingGate = "Real activity sync and durable progress service are still gated.",
            verifiedCommit = "efae0f7"
        ),
        LearnFeatureDestination.Account to LearnFeatureReadiness(
            destination = LearnFeatureDestination.Account,
            status = LearnNativeReadinessStatus.AccessGated,
            nativeSummary = "Native profile, access, device, and sign-out sections are in place.",
            remainingGate = "Subscription details, device list, and revoke actions are still gated by account service parity.",
            verifiedCommit = "efae0f7"
        )
    )
}
