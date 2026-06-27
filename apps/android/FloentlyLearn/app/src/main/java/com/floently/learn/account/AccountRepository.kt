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
            planName = "Learn access",
            renewalText = "Plan details are being prepared.",
            accessNote = "Learn access is active."
        ),
        devices = listOf(
            LearnDeviceSnapshot(
                id = "current-device",
                name = "This Android device",
                status = LearnDeviceStatus.Current,
                lastSeenText = "Now",
                revokeAvailable = false
            ),
            LearnDeviceSnapshot(
                id = "saved-device-local",
                name = "Saved device",
                status = LearnDeviceStatus.Registered,
                lastSeenText = "Recently used",
                revokeAvailable = false
            )
        ),
        maxDevices = 2,
        isLoading = false,
        errorMessage = null
    )
}
