package com.floently.learn.account

enum class LearnPlanStatus {
    Active,
    Pending,
    Missing
}

enum class LearnDeviceStatus {
    Current,
    Registered
}

data class LearnPlanSnapshot(
    val product: String,
    val status: LearnPlanStatus,
    val planName: String,
    val renewalText: String,
    val accessNote: String
)

data class LearnDeviceSnapshot(
    val id: String,
    val name: String,
    val status: LearnDeviceStatus,
    val lastSeenText: String,
    val revokeAvailable: Boolean
)

data class AccountDashboardState(
    val email: String,
    val plan: LearnPlanSnapshot,
    val devices: List<LearnDeviceSnapshot>,
    val maxDevices: Int,
    val isLoading: Boolean,
    val errorMessage: String?
) {
    val usedDeviceSlots: Int
        get() = devices.size
}
