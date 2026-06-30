package com.floently.learn.navigation

enum class LearnFeatureDestination(
    val title: String,
    val subtitle: String,
    val releaseGuard: LearnReleaseGuard
) {
    YkiPractice(
        title = "YKI practice",
        subtitle = "Exam-style Finnish practice rebuilt natively with existing learning behavior preserved.",
        releaseGuard = LearnReleaseGuard.MustMatchExistingProduct
    ),
    ProfessionalFinnish(
        title = "Professional Finnish",
        subtitle = "Workplace Finnish modules, job language, and professional scenarios.",
        releaseGuard = LearnReleaseGuard.MustMatchExistingProduct
    ),
    Roleplay(
        title = "Roleplay",
        subtitle = "OpenAI-backed conversation practice with anti-repetition and beginner-safe coaching.",
        releaseGuard = LearnReleaseGuard.MustMatchExistingProduct
    ),
    Cards(
        title = "Cards",
        subtitle = "Vocabulary and sentence practice with native interaction and animation parity.",
        releaseGuard = LearnReleaseGuard.MustMatchExistingProduct
    ),
    Progress(
        title = "Progress",
        subtitle = "Learning progress, streaks, and completed activity history.",
        releaseGuard = LearnReleaseGuard.RequiresParityAudit
    ),
    Settings(
        title = "Settings",
        subtitle = "Language, preferences, support, privacy, and Learn controls.",
        releaseGuard = LearnReleaseGuard.RequiresParityAudit
    ),
    Account(
        title = "Account",
        subtitle = "Subscription, device access, profile, and sign-out controls.",
        releaseGuard = LearnReleaseGuard.RequiresAccessCheck
    );

    companion object {
        val primary: List<LearnFeatureDestination> = listOf(
            YkiPractice,
            ProfessionalFinnish,
            Roleplay,
            Cards,
            Progress
        )
    }
}

enum class LearnReleaseGuard {
    MustMatchExistingProduct,
    RequiresParityAudit,
    RequiresAccessCheck
}
