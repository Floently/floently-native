package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus

enum class FloentlyBillingInterval {
    Monthly,
    Yearly
}

enum class FloentlyCheckoutStatus {
    NotStarted,
    Ready,
    ServicePending
}

data class FloentlyProductPlan(
    val product: FloentlyAccessProduct,
    val planId: String,
    val title: String,
    val subtitle: String,
    val interval: FloentlyBillingInterval,
    val displayPrice: String,
    val status: FloentlyAccessStatus,
    val checkoutStatus: FloentlyCheckoutStatus,
    val accessNote: String
)

data class FloentlyCheckoutIntent(
    val product: FloentlyAccessProduct,
    val planId: String,
    val status: FloentlyCheckoutStatus,
    val message: String
)

data class FloentlyBillingDashboardState(
    val plans: List<FloentlyProductPlan>,
    val latestCheckoutIntent: FloentlyCheckoutIntent?,
    val isLoading: Boolean,
    val errorMessage: String?
)
