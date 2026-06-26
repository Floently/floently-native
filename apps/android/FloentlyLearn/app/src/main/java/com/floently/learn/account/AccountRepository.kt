package com.floently.learn.account

interface AccountRepository {
    suspend fun dashboard(email: String): AccountDashboardState
}

class PreviewAccountRepository : AccountRepository {
    override suspend fun dashboard(email: String): AccountDashboardState = AccountDashboardState(
        email = email,
        plan = LearnPlanSnapshot(
            product = "Floently Learn",
            status = LearnPlanStatus.Active,
            planName = "Preview Learn access",
            renewalText = "Plan service is not connected yet.",
            accessNote = "Learn access is checked before the signed-in shell opens."
        ),
        devices = listOf(
            LearnDeviceSnapshot(
                id = "current-device",
                name = "This Android device",
                status = LearnDeviceStatus.Current,
                lastSeenText = "Current session",
                revokeAvailable = false
            ),
            LearnDeviceSnapshot(
                id = "saved-device-preview",
                name = "Saved device preview",
                status = LearnDeviceStatus.Registered,
                lastSeenText = "Device service pending",
                revokeAvailable = false
            )
        ),
        maxDevices = 2,
        isLoading = false,
        errorMessage = null
    )
}
