package com.floently.shared.access

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONObject

class FloentlyAccessService(private val api: FloentlyApiClient) {
    suspend fun getAccess(product: FloentlyAccessProduct): FloentlyProductAccess {
        val nativePath = "/api/native/access/${product.apiName}"
        val legacyPath = "/api/v1/access/${product.apiName}"

        val response = runCatching { api.get(nativePath) }
            .recoverCatching { api.get(legacyPath) }
            .recoverCatching {
                if (product == FloentlyAccessProduct.Learn) {
                    api.get("/api/v1/subscription/status")
                } else {
                    throw it
                }
            }
            .getOrThrow()

        return if (product == FloentlyAccessProduct.Learn && looksLikeSubscriptionStatus(response)) {
            productAccessFromJson(product = product, json = learnAccessFromSubscriptionStatus(response))
        } else {
            productAccessFromJson(product = product, json = response)
        }
    }

    private fun looksLikeSubscriptionStatus(json: JSONObject): Boolean {
        return json.has("subscription_status") ||
            json.has("subscriptionStatus") ||
            json.has("yki_access") ||
            json.has("ykiAccess") ||
            json.has("professional_access") ||
            json.has("professionalAccess") ||
            json.has("combined_access") ||
            json.has("combinedAccess") ||
            json.has("is_internal_all_access") ||
            json.has("isInternalAllAccess")
    }

    private fun learnAccessFromSubscriptionStatus(json: JSONObject): JSONObject {
        val hasLearnAccess =
            json.optBoolean("yki_access", json.optBoolean("ykiAccess")) ||
                json.optBoolean("professional_access", json.optBoolean("professionalAccess")) ||
                json.optBoolean("combined_access", json.optBoolean("combinedAccess")) ||
                json.optBoolean("is_internal_all_access", json.optBoolean("isInternalAllAccess"))

        val rawStatus = firstNonBlank(json, "subscription_status", "subscriptionStatus", "status")
        val normalizedStatus = when (rawStatus.trim().lowercase()) {
            "active" -> "active"
            "trialing", "trial" -> "trialing"
            "past_due", "pastdue" -> "past_due"
            "expired", "canceled", "cancelled" -> "expired"
            else -> if (hasLearnAccess) "active" else "none"
        }

        val allowed = hasLearnAccess || normalizedStatus == "active" || normalizedStatus == "trialing"
        val access = JSONObject()
            .put("allowed", allowed)
            .put("status", normalizedStatus)

        if (!allowed) {
            access.put("reason", "Floently Learn access is required.")
        }

        return JSONObject().put("access", access)
    }

    private fun firstNonBlank(json: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = json.optString(key).trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }
}
