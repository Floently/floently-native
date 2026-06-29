package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus

fun billingProductFromApiName(value: String?): FloentlyAccessProduct? = when (value?.trim()?.lowercase()) {
    FloentlyAccessProduct.Learn.apiName -> FloentlyAccessProduct.Learn
    FloentlyAccessProduct.Read.apiName -> FloentlyAccessProduct.Read
    FloentlyAccessProduct.Create.apiName -> FloentlyAccessProduct.Create
    else -> null
}

fun billingStatusFromApiName(value: String?): FloentlyAccessStatus = when (value?.trim()?.lowercase()) {
    "active" -> FloentlyAccessStatus.Active
    "trialing", "trial" -> FloentlyAccessStatus.Trialing
    "past_due", "pastdue" -> FloentlyAccessStatus.PastDue
    "expired", "canceled", "cancelled" -> FloentlyAccessStatus.Expired
    else -> FloentlyAccessStatus.None
}

enum class FloentlyBillingInterval {
    Monthly,
    Yearly
}

enum class FloentlyCheckoutStatus {
    NotStarted,
    Ready,
    ServicePending
}

enum class FloentlyBillingAction {
    Dashboard,
    Checkout,
    Trial,
    Portal,
    CancelTrial,
    Reactivate
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
    val message: String,
    val checkoutUrl: String? = null,
    val action: FloentlyBillingAction = FloentlyBillingAction.Checkout,
    val providerPath: String? = null
)

data class FloentlyBillingDashboardState(
    val plans: List<FloentlyProductPlan>,
    val latestCheckoutIntent: FloentlyCheckoutIntent?,
    val isLoading: Boolean,
    val errorMessage: String?
)
