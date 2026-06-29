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
        errorMessage = null,
        billingActions = listOf(
            LearnBillingActionSnapshot(
                title = "Current plan",
                body = "Floently Learn access is checked separately from Read and Create.",
                statusText = "Access active"
            ),
            LearnBillingActionSnapshot(
                title = "Start 3-day trial",
                body = "Trial entry belongs to the billing surface. Android live purchases must use Google Play Billing when enabled.",
                statusText = "Store billing boundary"
            ),
            LearnBillingActionSnapshot(
                title = "Manage subscription",
                body = "Mobile-origin subscriptions are managed through the store account. Web portal and web checkout remain web-only.",
                statusText = "Provider wiring pending"
            ),
            LearnBillingActionSnapshot(
                title = "Start checkout",
                body = "Checkout preparation is available through the shared billing boundary and must preserve separate Learn, Read, and Create access.",
                statusText = "Checkout boundary ready"
            )
        ),
        productAccessNotes = listOf(
            "Learn: Finnish learning, YKI, roleplay, cards, professional Finnish, progress, and account.",
            "Read: separate document reading access.",
            "Create Studio: separate creation tools access.",
            "No bundle is implied unless a bundle plan is explicitly added later."
        )
    )
}
