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
        get() =
            ykiAccess
                || professionalAccess
                || combinedAccess
                || isInternalAllAccess
}

fun accessStatusFromJson(
    json: JSONObject
): FloentlyAccessStatus {
    fun string(vararg keys: String): String? {
        keys.forEach { key ->
            if (json.has(key) && !json.isNull(key)) {
                json.optString(key)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { return it }
            }
        }
        return null
    }

    fun boolean(vararg keys: String): Boolean? {
        keys.forEach { key ->
            if (
                json.has(key)
                && !json.isNull(key)
            ) {
                return when (
                    val value = json.opt(key)
                ) {
                    is Boolean -> value
                    is Number -> value.toInt() != 0
                    is String -> when (
                        value.trim().lowercase()
                    ) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> null
                    }
                    else -> null
                }
            }
        }
        return null
    }

    val tier = string(
        "billing_tier",
        "billingTier",
        "tier"
    )
    val internalAccess =
        boolean(
            "is_internal_all_access",
            "isInternalAllAccess",
            "internal_all_access",
            "internalAllAccess"
        )
            ?: (tier?.lowercase() == "internal_all_access")

    val yki =
        internalAccess
            || boolean(
                "yki_access",
                "ykiAccess"
            ) == true
    val professional =
        internalAccess
            || boolean(
                "professional_access",
                "professionalAccess"
            ) == true

    val explicitRead = boolean(
        "read_access",
        "readAccess",
        "reader_access",
        "readerAccess"
    )

    return FloentlyAccessStatus(
        billingTier = tier,
        subscriptionStatus = string(
            "subscription_status",
            "subscriptionStatus"
        ),
        ykiAccess = yki,
        professionalAccess = professional,
        combinedAccess =
            boolean(
                "combined_access",
                "combinedAccess"
            )
                ?: (yki && professional),
        readAccess =
            internalAccess
                || explicitRead == true
                || tierHasReadAccess(tier),
        createAccess =
            internalAccess
                || boolean(
                    "create_access",
                    "createAccess"
                ) == true,
        isInternalAllAccess = internalAccess,
        trialAlreadyUsed =
            boolean(
                "trial_already_used",
                "trialAlreadyUsed"
            )
                ?: false,
        canStartTrial =
            boolean(
                "can_start_trial",
                "canStartTrial"
            )
                ?: false,
        cancelAtPeriodEnd =
            boolean(
                "cancel_at_period_end",
                "cancelAtPeriodEnd"
            )
                ?: false,
        hasPaymentIssue =
            boolean(
                "has_payment_issue",
                "hasPaymentIssue"
            )
                ?: false,
        paymentIssueMessage = string(
            "payment_issue_message",
            "paymentIssueMessage"
        )
    )
}

private fun tierHasReadAccess(
    tier: String?
): Boolean {
    val normalized = tier
        ?.trim()
        ?.lowercase()
        .orEmpty()

    return normalized == "reader"
        || normalized == "read"
        || normalized == "read_premium"
        || normalized == "reader_premium"
        || normalized.startsWith("read_")
        || normalized.startsWith("reader_")
        || normalized.contains("floently_read")
}
