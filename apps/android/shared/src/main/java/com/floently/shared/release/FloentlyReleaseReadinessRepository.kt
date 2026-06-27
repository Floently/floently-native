package com.floently.shared.release

interface FloentlyReleaseReadinessRepository {
    suspend fun dashboard(): FloentlyReleaseReadinessState
}

class PreviewFloentlyReleaseReadinessRepository : FloentlyReleaseReadinessRepository {
    override suspend fun dashboard(): FloentlyReleaseReadinessState = FloentlyReleaseReadinessState(
        gates = listOf(
            FloentlyReleaseGate(
                id = "suite-shell",
                area = FloentlyReleaseArea.AppShell,
                title = "Native suite shell",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Learn, Read, and Create are separated inside the native suite with product-specific access checks.",
                nextAction = "Keep product separation stable during final QA."
            ),
            FloentlyReleaseGate(
                id = "android-builds",
                area = FloentlyReleaseArea.Store,
                title = "Android debug, release, and bundle builds",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Debug, release, and Play bundle builds compile successfully. Signing remains intentionally local.",
                nextAction = "Configure signing outside Git only when store upload is ready."
            ),
            FloentlyReleaseGate(
                id = "read-service-wiring",
                area = FloentlyReleaseArea.Read,
                title = "Read service wiring",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Read has service routes, repository fallback, app container injection, and suite shell injection.",
                nextAction = "Run real backend QA for upload, language detection, generation, reader session, and library."
            ),
            FloentlyReleaseGate(
                id = "create-service-wiring",
                area = FloentlyReleaseArea.Create,
                title = "Create Studio service wiring",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Create Studio has generation service routes, repository fallback, app container injection, and suite shell injection.",
                nextAction = "Run real backend QA for generation, saved projects, export, and share."
            ),
            FloentlyReleaseGate(
                id = "learn-service-wiring",
                area = FloentlyReleaseArea.Learn,
                title = "Learn service wiring",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Learn roleplay, cards, and progress services are injected while preserving the existing Learn UI/function structure.",
                nextAction = "Run real backend QA for roleplay generation, anti-repetition, cards scheduling, scoring, and durable progress."
            ),
            FloentlyReleaseGate(
                id = "store-submission-pack",
                area = FloentlyReleaseArea.Store,
                title = "Store submission pack",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Store listing draft, screenshot checklist, data safety prompts, release notes draft, and manual QA checklist are documented.",
                nextAction = "Fill final privacy URL, support email, data-safety answers, screenshots, and release notes after QA."
            ),
            FloentlyReleaseGate(
                id = "live-backend-qa",
                area = FloentlyReleaseArea.Backend,
                title = "Live backend verification",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Service boundaries are wired, but real backend behaviour still needs end-to-end verification with production-like data.",
                nextAction = "Test auth, access, Learn, Read, Create, progress, and fallback states against the live backend."
            ),
            FloentlyReleaseGate(
                id = "payments-live-checkout",
                area = FloentlyReleaseArea.Payments,
                title = "Payment provider checkout",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Billing routes and entitlement boundary are wired, but real checkout/provider flow is not verified yet.",
                nextAction = "Verify provider checkout URL creation, return/cancel flow, entitlement refresh, and separate product access."
            ),
            FloentlyReleaseGate(
                id = "local-release-signing",
                area = FloentlyReleaseArea.Store,
                title = "Local release signing",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Signing support exists and secrets are not tracked, but local release signing values are not configured yet.",
                nextAction = "Create local signing file or build-machine variables outside Git before Play upload."
            ),
            FloentlyReleaseGate(
                id = "privacy-data-safety-assets",
                area = FloentlyReleaseArea.Store,
                title = "Privacy, data safety, and screenshots",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Store text is drafted, but final privacy URL, data-safety answers, screenshots, and support details are unresolved.",
                nextAction = "Complete policy URL, account deletion path, AI/payment disclosures, device screenshots, and listing assets."
            ),
            FloentlyReleaseGate(
                id = "manual-device-qa",
                area = FloentlyReleaseArea.Qa,
                title = "Manual real-device QA",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Builds pass, but real-device QA across login, suite, Learn, Read, Create, checkout, offline, and sign-out is not complete.",
                nextAction = "Run the native manual QA checklist and record pass/fail notes before submission."
            )
        ),
        isLoading = false,
        errorMessage = null
    )
}
