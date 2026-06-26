package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus

interface FloentlyBillingRepository {
    suspend fun dashboard(): FloentlyBillingDashboardState
    suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyBillingDashboardState
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
            message = "Checkout boundary prepared for ${plan.title}."
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
}
