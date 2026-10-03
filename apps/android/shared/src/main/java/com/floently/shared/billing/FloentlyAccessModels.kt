package com.floently.shared.billing

import org.json.JSONObject

data class FloentlyAccessStatus(
    val billingTier: String?,
    val subscriptionStatus: String?,
    val pathway: String?,
    val ykiAccess: Boolean,
    val professionalAccess: Boolean,
    val combinedAccess: Boolean,
    val readAccess: Boolean,
    val createAccess: Boolean,
    val isInternalAllAccess: Boolean,
    val isActive: Boolean,
    val trialAlreadyUsed: Boolean,
    val canStartTrial: Boolean,
    val cancelAtPeriodEnd: Boolean,
    val hasPaymentIssue: Boolean,
    val paymentIssueMessage: String?,
    val accessibleProfessions: List<String>,
    val selectedProfessions: List<String>
) {
    val hasLearnAccess: Boolean
        get() = ykiAccess || professionalAccess || combinedAccess || isInternalAllAccess
}

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index)
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.let(::add)
        }
    }
}

fun accessStatusFromJson(json: JSONObject): FloentlyAccessStatus {
    val ykiAccess = json.optBoolean("yki_access", json.optBoolean("ykiAccess"))
    val professionalAccess = json.optBoolean(
        "professional_access",
        json.optBoolean("professionalAccess")
    )
    val pathway = json.optString("pathway")
        .takeIf { it.isNotBlank() }

    val explicitCombined = when {
        json.has("combined_access") -> json.optBoolean("combined_access")
        json.has("combinedAccess") -> json.optBoolean("combinedAccess")
        else -> null
    }

    return FloentlyAccessStatus(
        billingTier = json.optString(
            "billing_tier",
            json.optString("billingTier")
        ).takeIf { it.isNotBlank() },
        subscriptionStatus = json.optString(
            "subscription_status",
            json.optString("subscriptionStatus")
        ).takeIf { it.isNotBlank() },
        pathway = pathway,
        ykiAccess = ykiAccess,
        professionalAccess = professionalAccess,
        combinedAccess = explicitCombined
            ?: (pathway.equals("combined", ignoreCase = true) || (ykiAccess && professionalAccess)),
        readAccess = json.optBoolean("read_access", json.optBoolean("readAccess")),
        createAccess = json.optBoolean("create_access", json.optBoolean("createAccess")),
        isInternalAllAccess = json.optBoolean(
            "is_internal_all_access",
            json.optBoolean("isInternalAllAccess")
        ),
        isActive = json.optBoolean("is_active", json.optBoolean("isActive")),
        trialAlreadyUsed = json.optBoolean(
            "trial_already_used",
            json.optBoolean("trialAlreadyUsed")
        ),
        canStartTrial = json.optBoolean(
            "can_start_trial",
            json.optBoolean("canStartTrial")
        ),
        cancelAtPeriodEnd = json.optBoolean(
            "cancel_at_period_end",
            json.optBoolean("cancelAtPeriodEnd")
        ),
        hasPaymentIssue = json.optBoolean(
            "has_payment_issue",
            json.optBoolean("hasPaymentIssue")
        ),
        paymentIssueMessage = json.optString(
            "payment_issue_message",
            json.optString("paymentIssueMessage")
        ).takeIf { it.isNotBlank() },
        accessibleProfessions = json.stringList("accessible_professions")
            .ifEmpty { json.stringList("accessibleProfessions") },
        selectedProfessions = json.stringList("selected_professions")
            .ifEmpty { json.stringList("selectedProfessions") }
    )
}
