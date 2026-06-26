package com.floently.shared.billing

import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessStatus
import com.floently.shared.api.FloentlyApiClient
import org.json.JSONArray
import org.json.JSONObject

class FloentlyBillingService(private val api: FloentlyApiClient) {
    suspend fun dashboard(): FloentlyBillingDashboardState {
        val response = api.get("/api/v1/billing/dashboard")
        val plans = plansFromJson(response)
        val latestIntent = response.optJSONObject("latest_checkout_intent")?.let { checkoutIntentFromJson(it) }
        return FloentlyBillingDashboardState(
            plans = plans,
            latestCheckoutIntent = latestIntent,
            isLoading = false,
            errorMessage = null
        )
    }

    suspend fun prepareCheckout(product: FloentlyAccessProduct): FloentlyCheckoutIntent {
        val response = api.post(
            "/api/v1/billing/checkout",
            JSONObject().put("product", product.apiName)
        )
        return checkoutIntentFromJson(response, product)
    }

    private fun plansFromJson(json: JSONObject): List<FloentlyProductPlan> {
        val array = json.optJSONArray("plans") ?: json.optJSONArray("products") ?: JSONArray()
        return List(array.length()) { index -> planFromJson(array.getJSONObject(index)) }
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

    private fun checkoutIntentFromJson(json: JSONObject, fallbackProduct: FloentlyAccessProduct? = null): FloentlyCheckoutIntent {
        val product = billingProductFromApiName(json.optString("product")) ?: fallbackProduct ?: FloentlyAccessProduct.Learn
        val checkoutUrl = json.optString("checkout_url").takeIf { it.isNotBlank() }
            ?: json.optString("url").takeIf { it.isNotBlank() }
        return FloentlyCheckoutIntent(
            product = product,
            planId = json.optString("plan_id").ifBlank { json.optString("id") },
            status = if (checkoutUrl.isNullOrBlank()) FloentlyCheckoutStatus.ServicePending else FloentlyCheckoutStatus.Ready,
            message = json.optString("message").ifBlank { "Checkout is ready for ${defaultTitle(product)}." },
            checkoutUrl = checkoutUrl
        )
    }

    private fun defaultTitle(product: FloentlyAccessProduct): String = when (product) {
        FloentlyAccessProduct.Learn -> "Floently Learn"
        FloentlyAccessProduct.Read -> "Floently Read"
        FloentlyAccessProduct.Create -> "Floently Create Studio"
    }
}
