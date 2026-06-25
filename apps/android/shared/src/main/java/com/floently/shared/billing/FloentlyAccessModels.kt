package com.floently.shared.billing

import org.json.JSONObject

data class FloentlyAccessStatus(
    val billingTier: String?,
    val subscriptionStatus: String?,
    val ykiAccess: Boolean,
    val professionalAccess: Boolean,
    val combinedAccess: Boolean,
    val readAccess: Boolean,
    val createAccess: Boolean,
    val isInternalAllAccess: Boolean,
    val trialAlreadyUsed: Boolean,
    val canStartTrial: Boolean,
    val cancelAtPeriodEnd: Boolean,
    val hasPaymentIssue: Boolean,
    val paymentIssueMessage: String?
) {
    val hasLearnAccess: Boolean
        get() = ykiAccess || professionalAccess || combinedAccess || isInternalAllAccess
}

fun accessStatusFromJson(json: JSONObject): FloentlyAccessStatus {
    return FloentlyAccessStatus(
        billingTier = json.optString("billing_tier", json.optString("billingTier")).takeIf { it.isNotBlank() },
        subscriptionStatus = json.optString("subscription_status", json.optString("subscriptionStatus")).takeIf { it.isNotBlank() },
        ykiAccess = json.optBoolean("yki_access", json.optBoolean("ykiAccess")),
        professionalAccess = json.optBoolean("professional_access", json.optBoolean("professionalAccess")),
        combinedAccess = json.optBoolean("combined_access", json.optBoolean("combinedAccess")),
        readAccess = json.optBoolean("read_access", json.optBoolean("readAccess")),
        createAccess = json.optBoolean("create_access", json.optBoolean("createAccess")),
        isInternalAllAccess = json.optBoolean("is_internal_all_access", json.optBoolean("isInternalAllAccess")),
        trialAlreadyUsed = json.optBoolean("trial_already_used", json.optBoolean("trialAlreadyUsed")),
        canStartTrial = json.optBoolean("can_start_trial", json.optBoolean("canStartTrial")),
        cancelAtPeriodEnd = json.optBoolean("cancel_at_period_end", json.optBoolean("cancelAtPeriodEnd")),
        hasPaymentIssue = json.optBoolean("has_payment_issue", json.optBoolean("hasPaymentIssue")),
        paymentIssueMessage = json.optString("payment_issue_message", json.optString("paymentIssueMessage")).takeIf { it.isNotBlank() }
    )
}
