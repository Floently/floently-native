package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus

interface FloentlyBillingRepository {
    suspend fun dashboard(): FloentlyBillingDashboardState
    suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyBillingDashboardState
    suspend fun startTrial(product: FloentlyAccessProduct = FloentlyAccessProduct.Learn): FloentlyBillingDashboardState
    suspend fun managePortal(): FloentlyBillingDashboardState
    suspend fun cancelTrial(): FloentlyBillingDashboardState
    suspend fun reactivateSubscription(): FloentlyBillingDashboardState
}

class ServiceFloentlyBillingRepository(
    private val service: FloentlyBillingService,
    private val fallback: FloentlyBillingRepository = PreviewFloentlyBillingRepository()
) : FloentlyBillingRepository {
    override suspend fun dashboard(): FloentlyBillingDashboardState {
        return runCatching { service.dashboard() }.getOrElse { error ->
            fallback.dashboard().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Billing service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyBillingDashboardState {
        return runCatching {
            val intent = service.prepareCheckout(product)
            dashboard().copy(latestCheckoutIntent = intent, errorMessage = null)
        }.getOrElse { error ->
            fallback.prepareCheckout(product).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Checkout service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun startTrial(product: FloentlyAccessProduct): FloentlyBillingDashboardState {
        return runCatching {
            val intent = service.startTrial(product)
            dashboard().copy(latestCheckoutIntent = intent, errorMessage = null)
        }.getOrElse { error ->
            fallback.startTrial(product).copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Trial service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun managePortal(): FloentlyBillingDashboardState {
        return runCatching {
            val intent = service.createPortalSession()
            dashboard().copy(latestCheckoutIntent = intent, errorMessage = null)
        }.getOrElse { error ->
            fallback.managePortal().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Subscription portal is not available from the existing backend yet."
            )
        }
    }

    override suspend fun cancelTrial(): FloentlyBillingDashboardState {
        return runCatching {
            val intent = service.cancelTrial()
            dashboard().copy(latestCheckoutIntent = intent, errorMessage = null)
        }.getOrElse { error ->
            fallback.cancelTrial().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Cancel trial service is not available from the existing backend yet."
            )
        }
    }

    override suspend fun reactivateSubscription(): FloentlyBillingDashboardState {
        return runCatching {
            val intent = service.reactivateSubscription()
            dashboard().copy(latestCheckoutIntent = intent, errorMessage = null)
        }.getOrElse { error ->
            fallback.reactivateSubscription().copy(
                errorMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: "Reactivation service is not available from the existing backend yet."
            )
        }
    }
}

class PreviewFloentlyBillingRepository : FloentlyBillingRepository {
    private var latestIntent: FloentlyCheckoutIntent? = null

    override suspend fun dashboard(): FloentlyBillingDashboardState = FloentlyBillingDashboardState(
        plans = previewPlans(),
        latestCheckoutIntent = latestIntent,
        isLoading = false,
        errorMessage = null
    )

    override suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyBillingDashboardState {
        val plan = previewPlans().first { it.product == product }
        latestIntent = FloentlyCheckoutIntent(
            product = product,
            planId = plan.planId,
            status = FloentlyCheckoutStatus.ServicePending,
            message = "Checkout boundary prepared for ${plan.title}.",
            action = FloentlyBillingAction.Checkout,
            providerPath = "/api/v1/subscription/checkout"
        )
        return dashboard()
    }

    override suspend fun startTrial(product: FloentlyAccessProduct): FloentlyBillingDashboardState {
        latestIntent = FloentlyCheckoutIntent(
            product = product,
            planId = "trial-preview",
            status = FloentlyCheckoutStatus.ServicePending,
            message = "Trial boundary prepared for ${defaultTitle(product)}.",
            action = FloentlyBillingAction.Trial,
            providerPath = "/api/v1/subscription/trial"
        )
        return dashboard()
    }

    override suspend fun managePortal(): FloentlyBillingDashboardState {
        latestIntent = FloentlyCheckoutIntent(
            product = FloentlyAccessProduct.Learn,
            planId = "portal-preview",
            status = FloentlyCheckoutStatus.ServicePending,
            message = "Subscription portal boundary prepared.",
            action = FloentlyBillingAction.Portal,
            providerPath = "/api/v1/subscription/portal"
        )
        return dashboard()
    }

    override suspend fun cancelTrial(): FloentlyBillingDashboardState {
        latestIntent = FloentlyCheckoutIntent(
            product = FloentlyAccessProduct.Learn,
            planId = "cancel-trial-preview",
            status = FloentlyCheckoutStatus.ServicePending,
            message = "Cancel trial or renewal boundary prepared.",
            action = FloentlyBillingAction.CancelTrial,
            providerPath = "/api/v1/subscription/cancel-trial"
        )
        return dashboard()
    }

    override suspend fun reactivateSubscription(): FloentlyBillingDashboardState {
        latestIntent = FloentlyCheckoutIntent(
            product = FloentlyAccessProduct.Learn,
            planId = "reactivate-preview",
            status = FloentlyCheckoutStatus.ServicePending,
            message = "Subscription reactivation boundary prepared.",
            action = FloentlyBillingAction.Reactivate,
            providerPath = "/api/v1/subscription/reactivate"
        )
        return dashboard()
    }

    private fun previewPlans(): List<FloentlyProductPlan> = listOf(
        FloentlyProductPlan(
            product = FloentlyAccessProduct.Learn,
            planId = "learn-monthly-preview",
            title = "Floently Learn",
            subtitle = "Finnish learning, YKI, roleplay, cards, and progress.",
            interval = FloentlyBillingInterval.Monthly,
            displayPrice = "Separate Learn plan",
            status = FloentlyAccessStatus.Active,
            checkoutStatus = FloentlyCheckoutStatus.Ready,
            accessNote = "Learn access stays separate from Read and Create."
        ),
        FloentlyProductPlan(
            product = FloentlyAccessProduct.Read,
            planId = "read-monthly-preview",
            title = "Floently Read",
            subtitle = "Upload, generate, and read documents natively.",
            interval = FloentlyBillingInterval.Monthly,
            displayPrice = "Separate Read plan",
            status = FloentlyAccessStatus.None,
            checkoutStatus = FloentlyCheckoutStatus.Ready,
            accessNote = "Read access stays separate from Learn and Create."
        ),
        FloentlyProductPlan(
            product = FloentlyAccessProduct.Create,
            planId = "create-monthly-preview",
            title = "Floently Create Studio",
            subtitle = "Direct creation tools, projects, and exports.",
            interval = FloentlyBillingInterval.Monthly,
            displayPrice = "Separate Create plan",
            status = FloentlyAccessStatus.None,
            checkoutStatus = FloentlyCheckoutStatus.Ready,
            accessNote = "Create access stays separate from Learn and Read."
        )
    )

    private fun defaultTitle(product: FloentlyAccessProduct): String = when (product) {
        FloentlyAccessProduct.Learn -> "Floently Learn"
        FloentlyAccessProduct.Read -> "Floently Read"
        FloentlyAccessProduct.Create -> "Floently Create Studio"
    }
}
