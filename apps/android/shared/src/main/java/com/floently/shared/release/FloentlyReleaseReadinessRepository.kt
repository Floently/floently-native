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
                summary = "Learn, Read, and Create are separated inside the native suite.",
                nextAction = "Keep shell stable while wiring services."
            ),
            FloentlyReleaseGate(
                id = "release-build",
                area = FloentlyReleaseArea.Store,
                title = "Debug and release builds",
                status = FloentlyReleaseGateStatus.Verified,
                summary = "Debug and unsigned release builds compile successfully.",
                nextAction = "Add signing outside Git before store upload."
            ),
            FloentlyReleaseGate(
                id = "read-services",
                area = FloentlyReleaseArea.Read,
                title = "Read service wiring",
                status = FloentlyReleaseGateStatus.ReadyForWiring,
                summary = "Native Read UI is ready for upload, detection, generation, reader, and library services.",
                nextAction = "Connect real Read backend endpoints and native file picker."
            ),
            FloentlyReleaseGate(
                id = "create-services",
                area = FloentlyReleaseArea.Create,
                title = "Create service wiring",
                status = FloentlyReleaseGateStatus.ReadyForWiring,
                summary = "Create Studio UI is ready for direct functions, result, project, and export services.",
                nextAction = "Connect Create backend generation and saved project endpoints."
            ),
            FloentlyReleaseGate(
                id = "learn-services",
                area = FloentlyReleaseArea.Learn,
                title = "Learn service wiring",
                status = FloentlyReleaseGateStatus.ReadyForWiring,
                summary = "Learn native screens are ready for real content, roleplay generation, scoring, and progress sync.",
                nextAction = "Connect OpenAI-backed roleplay, scoring, cards scheduling, and durable progress."
            ),
            FloentlyReleaseGate(
                id = "payments",
                area = FloentlyReleaseArea.Payments,
                title = "Payments and entitlements",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Plan boundaries are ready, but real provider checkout and entitlement refresh are not connected.",
                nextAction = "Wire checkout provider and product-specific entitlement refresh."
            ),
            FloentlyReleaseGate(
                id = "signing",
                area = FloentlyReleaseArea.Store,
                title = "Play signing and store assets",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Release builds exist, but signing secrets and store listing assets are not configured.",
                nextAction = "Configure signing outside Git and prepare privacy, screenshots, listing, and data safety."
            ),
            FloentlyReleaseGate(
                id = "manual-qa",
                area = FloentlyReleaseArea.Qa,
                title = "Manual device QA",
                status = FloentlyReleaseGateStatus.Blocked,
                summary = "Builds pass, but real-device test coverage is not complete.",
                nextAction = "Run login, product access, Read, Create, Learn, checkout, offline, and sign-out tests on device."
            )
        ),
        isLoading = false,
        errorMessage = null
    )
}
