package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus
import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class FloentlyBillingService(private val api: FloentlyApiClient) {
    suspend fun dashboard(): FloentlyBillingDashboardState {
        val response = runCatching { api.get("/api/v1/billing/dashboard") }
            .getOrElse { api.get("/api/v1/subscription/status") }
        val plans = if (response.has("plans") || response.has("products")) {
            plansFromJson(response)
        } else {
            plansFromSubscriptionStatus(response)
        }
        val latestIntent = response.optJSONObject("latest_checkout_intent")?.let {
            checkoutIntentFromJson(
                json = it,
                fallbackProduct = FloentlyAccessProduct.Learn,
                action = FloentlyBillingAction.Dashboard,
                providerPath = "/api/v1/billing/dashboard"
            )
        }
        return FloentlyBillingDashboardState(
            plans = plans,
            latestCheckoutIntent = latestIntent,
            isLoading = false,
            errorMessage = null
        )
    }

    suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyCheckoutIntent {
        val primaryPath = "/api/v1/subscription/checkout"
        val fallbackPath = "/api/v1/billing/checkout"
        val primaryPayload = checkoutPayload(product)
        val fallbackPayload = JSONObject().put("product", product.apiName)
        val result = runCatching { primaryPath to api.post(primaryPath, primaryPayload) }
            .getOrElse { fallbackPath to api.post(fallbackPath, fallbackPayload) }
        return checkoutIntentFromJson(
            json = result.second,
            fallbackProduct = product,
            action = FloentlyBillingAction.Checkout,
            providerPath = result.first
        )
    }

    suspend fun startTrial(product: FloentlyAccessProduct = FloentlyAccessProduct.Learn): FloentlyCheckoutIntent {
        val path = "/api/v1/subscription/trial"
        val response = api.post(
            path,
            JSONObject()
                .put("product", product.apiName)
                .put("trial_days", 3)
        )
        return checkoutIntentFromJson(
            json = response,
            fallbackProduct = product,
            action = FloentlyBillingAction.Trial,
            providerPath = path
        )
    }

    suspend fun createPortalSession(): FloentlyCheckoutIntent {
        val path = "/api/v1/subscription/portal"
        val response = api.post(path, JSONObject())
        return checkoutIntentFromJson(
            json = response,
            fallbackProduct = FloentlyAccessProduct.Learn,
            action = FloentlyBillingAction.Portal,
            providerPath = path
        )
    }

    suspend fun cancelTrial(): FloentlyCheckoutIntent {
        val path = "/api/v1/subscription/cancel-trial"
        val response = api.post(path, JSONObject())
        return checkoutIntentFromJson(
            json = response,
            fallbackProduct = FloentlyAccessProduct.Learn,
            action = FloentlyBillingAction.CancelTrial,
            providerPath = path
        )
    }

    suspend fun reactivateSubscription(): FloentlyCheckoutIntent {
        val path = "/api/v1/subscription/reactivate"
        val response = api.post(path, JSONObject())
        return checkoutIntentFromJson(
            json = response,
            fallbackProduct = FloentlyAccessProduct.Learn,
            action = FloentlyBillingAction.Reactivate,
            providerPath = path
        )
    }

    private fun checkoutPayload(product: FloentlyAccessProduct): JSONObject {
        val pathway = when (product) {
            FloentlyAccessProduct.Learn -> "yki"
            FloentlyAccessProduct.Read -> "read"
            FloentlyAccessProduct.Create -> "create"
        }
        val plan = when (product) {
            FloentlyAccessProduct.Learn -> "yki_yearly"
            FloentlyAccessProduct.Read -> "read_monthly"
            FloentlyAccessProduct.Create -> "create_monthly"
        }
        return JSONObject()
            .put("product", product.apiName)
            .put("pathway", pathway)
            .put("plan", plan)
            .put("billing_period", if (product == FloentlyAccessProduct.Learn) "yearly" else "monthly")
            .put("professions", JSONArray())
            .put("trial_days", if (product == FloentlyAccessProduct.Learn) 3 else 0)
    }

    private fun plansFromJson(json: JSONObject): List<FloentlyProductPlan> {
        val array = json.optJSONArray("plans") ?: json.optJSONArray("products") ?: JSONArray()
        return List(array.length()) { index -> planFromJson(array.getJSONObject(index)) }
    }

    private fun plansFromSubscriptionStatus(json: JSONObject): List<FloentlyProductPlan> {
        val tier = firstNonBlank(json, "billing_tier", "billingTier", "tier", "plan_key", "planKey").ifBlank { "free" }
        val rawStatus = firstNonBlank(json, "subscription_status", "subscriptionStatus", "status")
        val hasLearnAccess = json.optBoolean("yki_access", json.optBoolean("ykiAccess")) ||
            json.optBoolean("professional_access", json.optBoolean("professionalAccess")) ||
            json.optBoolean("combined_access", json.optBoolean("combinedAccess")) ||
            json.optBoolean("is_internal_all_access", json.optBoolean("isInternalAllAccess"))
        val status = billingStatusFromApiName(rawStatus).let {
            if (it == FloentlyAccessStatus.None && hasLearnAccess) FloentlyAccessStatus.Active else it
        }
        val canStartTrial = json.optBoolean("can_start_trial", json.optBoolean("canStartTrial"))
        val cancelAtPeriodEnd = json.optBoolean("cancel_at_period_end", json.optBoolean("cancelAtPeriodEnd"))
        val learnNote = when {
            cancelAtPeriodEnd -> "Renewal cancellation is scheduled. Reactivation remains available from the provider action boundary."
            canStartTrial -> "A 3-day trial can be started through the provider action boundary."
            status == FloentlyAccessStatus.Active || status == FloentlyAccessStatus.Trialing -> "Learn access is active. Read and Create remain separate."
            else -> "No active Learn subscription. Checkout and trial actions are available through the provider boundary."
        }

        return listOf(
            FloentlyProductPlan(
                product = FloentlyAccessProduct.Learn,
                planId = tier,
                title = "Floently Learn",
                subtitle = "Finnish learning, YKI, roleplay, cards, professional Finnish, progress, and account.",
                interval = FloentlyBillingInterval.Yearly,
                displayPrice = if (tier == "free") "No active subscription" else tier.replace('_', ' '),
                status = status,
                checkoutStatus = FloentlyCheckoutStatus.Ready,
                accessNote = learnNote
            ),
            FloentlyProductPlan(
                product = FloentlyAccessProduct.Read,
                planId = "read-separate-access",
                title = "Floently Read",
                subtitle = "Separate document reading access.",
                interval = FloentlyBillingInterval.Monthly,
                displayPrice = "Separate Read plan",
                status = FloentlyAccessStatus.None,
                checkoutStatus = FloentlyCheckoutStatus.ServicePending,
                accessNote = "Read access stays separate from Learn and Create."
            ),
            FloentlyProductPlan(
                product = FloentlyAccessProduct.Create,
                planId = "create-separate-access",
                title = "Floently Create Studio",
                subtitle = "Separate creation tools access.",
                interval = FloentlyBillingInterval.Monthly,
                displayPrice = "Separate Create plan",
                status = FloentlyAccessStatus.None,
                checkoutStatus = FloentlyCheckoutStatus.ServicePending,
                accessNote = "Create access stays separate from Learn and Read."
            )
        )
    }

    private fun planFromJson(json: JSONObject): FloentlyProductPlan {
        val product = billingProductFromApiName(json.optString("product")) ?: FloentlyAccessProduct.Learn
        return FloentlyProductPlan(
            product = product,
            planId = json.optString("plan_id").ifBlank { json.optString("id") },
            title = json.optString("title").ifBlank { defaultTitle(product) },
            subtitle = json.optString("subtitle").ifBlank { "Separate ${defaultTitle(product)} access." },
            interval = if (json.optString("interval").equals("yearly", ignoreCase = true)) FloentlyBillingInterval.Yearly else FloentlyBillingInterval.Monthly,
            displayPrice = json.optString("display_price").ifBlank { "Separate ${defaultTitle(product)} plan" },
            status = billingStatusFromApiName(json.optString("status")),
            checkoutStatus = FloentlyCheckoutStatus.Ready,
            accessNote = json.optString("access_note").ifBlank { "Access is checked separately for ${defaultTitle(product)}." }
        )
    }

    private fun checkoutIntentFromJson(
        json: JSONObject,
        fallbackProduct: FloentlyAccessProduct? = null,
        action: FloentlyBillingAction,
        providerPath: String
    ): FloentlyCheckoutIntent {
        val nested = json.optJSONObject("checkout")
            ?: json.optJSONObject("session")
            ?: json.optJSONObject("portal")
            ?: json

        val product = billingProductFromApiName(firstNonBlank(nested, "product").ifBlank { firstNonBlank(json, "product") })
            ?: fallbackProduct
            ?: FloentlyAccessProduct.Learn

        val checkoutUrl = firstNonBlank(
            nested,
            "checkout_url",
            "checkoutUrl",
            "url",
            "portal_url",
            "portalUrl"
        ).ifBlank {
            firstNonBlank(json, "checkout_url", "checkoutUrl", "url", "portal_url", "portalUrl")
        }.takeIf { it.isNotBlank() }

        val planId = firstNonBlank(nested, "plan_id", "planId", "id", "plan").ifBlank {
            firstNonBlank(json, "plan_id", "planId", "id", "plan")
        }

        val message = firstNonBlank(nested, "message", "detail").ifBlank {
            firstNonBlank(json, "message", "detail")
        }.ifBlank {
            defaultActionMessage(action, product)
        }

        return FloentlyCheckoutIntent(
            product = product,
            planId = planId,
            status = if (checkoutUrl.isNullOrBlank()) FloentlyCheckoutStatus.ServicePending else FloentlyCheckoutStatus.Ready,
            message = message,
            checkoutUrl = checkoutUrl,
            action = action,
            providerPath = providerPath
        )
    }

    private fun firstNonBlank(json: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = json.optString(key).trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun defaultActionMessage(action: FloentlyBillingAction, product: FloentlyAccessProduct): String = when (action) {
        FloentlyBillingAction.Dashboard -> "Billing dashboard loaded for ${defaultTitle(product)}."
        FloentlyBillingAction.Checkout -> "Checkout boundary prepared for ${defaultTitle(product)}."
        FloentlyBillingAction.Trial -> "Trial boundary prepared for ${defaultTitle(product)}."
        FloentlyBillingAction.Portal -> "Subscription portal boundary prepared."
        FloentlyBillingAction.CancelTrial -> "Cancel trial or renewal boundary prepared."
        FloentlyBillingAction.Reactivate -> "Subscription reactivation boundary prepared."
    }

    private fun defaultTitle(product: FloentlyAccessProduct): String = when (product) {
        FloentlyAccessProduct.Learn -> "Floently Learn"
        FloentlyAccessProduct.Read -> "Floently Read"
        FloentlyAccessProduct.Create -> "Floently Create Studio"
    }
}
