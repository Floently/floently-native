package com.floently.shared.access

import org.json.JSONObject

enum class FloentlyAccessProduct(val apiName: String) {
    Learn("learn"),
    Read("read"),
    Create("create")
}

enum class FloentlyAccessStatus {
    Active,
    Trialing,
    PastDue,
    Expired,
    None
}

data class FloentlyEntitlement(
    val product: FloentlyAccessProduct,
    val status: FloentlyAccessStatus,
    val planId: String?,
    val expiresAtIso: String?
)

data class FloentlyProductAccess(
    val product: FloentlyAccessProduct,
    val isAllowed: Boolean,
    val entitlement: FloentlyEntitlement?,
    val reason: String?
)

fun productAccessFromJson(product: FloentlyAccessProduct, json: JSONObject): FloentlyProductAccess {
    val accessJson = json.optJSONObject("access") ?: json
    val entitlementJson = accessJson.optJSONObject("entitlement")
    val status = accessStatusFromApiValue(entitlementJson?.optString("status") ?: accessJson.optString("status"))
    val entitlement = entitlementJson?.let {
        FloentlyEntitlement(
            product = product,
            status = status,
            planId = it.optString("plan_id").takeIf { value -> value.isNotBlank() },
            expiresAtIso = it.optString("expires_at").takeIf { value -> value.isNotBlank() }
        )
    }

    return FloentlyProductAccess(
        product = product,
        isAllowed = accessJson.optBoolean("allowed", status == FloentlyAccessStatus.Active || status == FloentlyAccessStatus.Trialing),
        entitlement = entitlement,
        reason = accessJson.optString("reason").takeIf { it.isNotBlank() }
    )
}

private fun accessStatusFromApiValue(value: String?): FloentlyAccessStatus {
    return when (value?.trim()?.lowercase()) {
        "active" -> FloentlyAccessStatus.Active
        "trialing", "trial" -> FloentlyAccessStatus.Trialing
        "past_due", "pastdue" -> FloentlyAccessStatus.PastDue
        "expired", "canceled", "cancelled" -> FloentlyAccessStatus.Expired
        else -> FloentlyAccessStatus.None
    }
}
