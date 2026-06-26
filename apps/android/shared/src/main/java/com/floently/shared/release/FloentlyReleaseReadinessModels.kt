package com.floently.shared.release

enum class FloentlyReleaseArea {
    AppShell,
    Learn,
    Read,
    Create,
    Payments,
    Backend,
    Store,
    Qa
}

enum class FloentlyReleaseGateStatus {
    Verified,
    ReadyForWiring,
    Blocked
}

data class FloentlyReleaseGate(
    val id: String,
    val area: FloentlyReleaseArea,
    val title: String,
    val status: FloentlyReleaseGateStatus,
    val summary: String,
    val nextAction: String
)

data class FloentlyReleaseReadinessState(
    val gates: List<FloentlyReleaseGate>,
    val isLoading: Boolean,
    val errorMessage: String?
) {
    val verifiedCount: Int
        get() = gates.count { it.status == FloentlyReleaseGateStatus.Verified }

    val wiringCount: Int
        get() = gates.count { it.status == FloentlyReleaseGateStatus.ReadyForWiring }

    val blockerCount: Int
        get() = gates.count { it.status == FloentlyReleaseGateStatus.Blocked }

    val storeReady: Boolean
        get() = blockerCount == 0 && wiringCount == 0
}
