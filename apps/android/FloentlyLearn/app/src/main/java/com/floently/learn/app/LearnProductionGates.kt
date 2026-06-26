package com.floently.learn.app

enum class LearnProductionGateStatus {
    NativeReady,
    ServiceNeeded,
    ReleaseBlocked
}

data class LearnProductionGate(
    val id: String,
    val title: String,
    val status: LearnProductionGateStatus,
    val summary: String,
    val nextAction: String
)

object PreviewLearnProductionGates {
    val items: List<LearnProductionGate> = listOf(
        LearnProductionGate(
            id = "content-parity",
            title = "Content parity",
            status = LearnProductionGateStatus.ServiceNeeded,
            summary = "Core Learn areas now have native screens, but production content still needs service wiring.",
            nextAction = "Connect real content repositories and finish parity review."
        ),
        LearnProductionGate(
            id = "durable-progress",
            title = "Durable progress",
            status = LearnProductionGateStatus.ServiceNeeded,
            summary = "Native progress boundaries exist for practice, roleplay, cards, and progress overview.",
            nextAction = "Connect server sync and conflict handling."
        ),
        LearnProductionGate(
            id = "access-and-devices",
            title = "Access and devices",
            status = LearnProductionGateStatus.ReleaseBlocked,
            summary = "Native Account has profile, access, device, and sign-out sections.",
            nextAction = "Connect plan status, device list, and device actions."
        ),
        LearnProductionGate(
            id = "roleplay-service",
            title = "Roleplay service",
            status = LearnProductionGateStatus.ReleaseBlocked,
            summary = "Roleplay has a native service boundary and anti-repetition state.",
            nextAction = "Connect dynamic roleplay generation and review."
        ),
        LearnProductionGate(
            id = "native-navigation",
            title = "Native navigation foundation",
            status = LearnProductionGateStatus.NativeReady,
            summary = "All core Learn dashboard destinations now route to native screens or native boundaries.",
            nextAction = "Keep polishing the native experience while services are wired."
        )
    )
}
