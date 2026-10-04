import Foundation

public struct FloentlyAccessStatus: Codable, Equatable {
    public let billingTier: String?
    public let subscriptionStatus: String?
    public let ykiAccess: Bool
    public let professionalAccess: Bool
    public let combinedAccess: Bool
    public let readAccess: Bool
    public let createAccess: Bool
    public let isInternalAllAccess: Bool
    public let trialAlreadyUsed: Bool
    public let canStartTrial: Bool
    public let cancelAtPeriodEnd: Bool
    public let hasPaymentIssue: Bool
    public let paymentIssueMessage: String?

    enum CodingKeys: String, CodingKey {
        case tier
        case billingTier = "billing_tier"
        case billingTierCamel = "billingTier"
        case subscriptionStatus = "subscription_status"
        case subscriptionStatusCamel = "subscriptionStatus"
        case ykiAccess = "yki_access"
        case ykiAccessCamel = "ykiAccess"
        case professionalAccess = "professional_access"
        case professionalAccessCamel = "professionalAccess"
        case combinedAccess = "combined_access"
        case combinedAccessCamel = "combinedAccess"
        case readAccess = "read_access"
        case readAccessCamel = "readAccess"
        case readerAccess = "reader_access"
        case readerAccessCamel = "readerAccess"
        case createAccess = "create_access"
        case createAccessCamel = "createAccess"
        case isInternalAllAccess = "is_internal_all_access"
        case isInternalAllAccessCamel = "isInternalAllAccess"
        case trialAlreadyUsed = "trial_already_used"
        case trialAlreadyUsedCamel = "trialAlreadyUsed"
        case canStartTrial = "can_start_trial"
        case canStartTrialCamel = "canStartTrial"
        case cancelAtPeriodEnd = "cancel_at_period_end"
        case cancelAtPeriodEndCamel = "cancelAtPeriodEnd"
        case hasPaymentIssue = "has_payment_issue"
        case hasPaymentIssueCamel = "hasPaymentIssue"
        case paymentIssueMessage = "payment_issue_message"
        case paymentIssueMessageCamel = "paymentIssueMessage"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(
            keyedBy: CodingKeys.self
        )

        func bool(
            _ primary: CodingKeys,
            _ alternate: CodingKeys? = nil
        ) -> Bool? {
            if let value = try? container.decodeIfPresent(
                Bool.self,
                forKey: primary
            ) {
                return value
            }

            if let alternate,
               let value = try? container.decodeIfPresent(
                    Bool.self,
                    forKey: alternate
               ) {
                return value
            }

            return nil
        }

        func string(
            _ primary: CodingKeys,
            _ alternate: CodingKeys? = nil
        ) -> String? {
            if let value = try? container.decodeIfPresent(
                String.self,
                forKey: primary
            ), !value.isEmpty {
                return value
            }

            if let alternate,
               let value = try? container.decodeIfPresent(
                    String.self,
                    forKey: alternate
               ), !value.isEmpty {
                return value
            }

            return nil
        }

        let tier =
            string(.billingTier, .billingTierCamel)
            ?? string(.tier)
        let internalAccess =
            bool(
                .isInternalAllAccess,
                .isInternalAllAccessCamel
            )
            ?? (tier?.lowercased() == "internal_all_access")

        let resolvedYki =
            bool(.ykiAccess, .ykiAccessCamel)
            ?? false
        let resolvedProfessional =
            bool(
                .professionalAccess,
                .professionalAccessCamel
            )
            ?? false

        billingTier = tier
        subscriptionStatus = string(
            .subscriptionStatus,
            .subscriptionStatusCamel
        )
        isInternalAllAccess = internalAccess

        ykiAccess =
            internalAccess
            || resolvedYki
        professionalAccess =
            internalAccess
            || resolvedProfessional
        combinedAccess =
            bool(
                .combinedAccess,
                .combinedAccessCamel
            )
            ?? (ykiAccess && professionalAccess)

        let explicitRead =
            bool(.readAccess, .readAccessCamel)
            ?? bool(.readerAccess, .readerAccessCamel)

        readAccess =
            internalAccess
            || explicitRead == true
            || Self.tierHasReadAccess(tier)

        createAccess =
            internalAccess
            || bool(
                .createAccess,
                .createAccessCamel
            ) == true

        trialAlreadyUsed =
            bool(
                .trialAlreadyUsed,
                .trialAlreadyUsedCamel
            )
            ?? false
        canStartTrial =
            bool(.canStartTrial, .canStartTrialCamel)
            ?? false
        cancelAtPeriodEnd =
            bool(
                .cancelAtPeriodEnd,
                .cancelAtPeriodEndCamel
            )
            ?? false
        hasPaymentIssue =
            bool(
                .hasPaymentIssue,
                .hasPaymentIssueCamel
            )
            ?? false
        paymentIssueMessage = string(
            .paymentIssueMessage,
            .paymentIssueMessageCamel
        )
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(
            keyedBy: CodingKeys.self
        )
        try container.encodeIfPresent(
            billingTier,
            forKey: .billingTier
        )
        try container.encodeIfPresent(
            subscriptionStatus,
            forKey: .subscriptionStatus
        )
        try container.encode(ykiAccess, forKey: .ykiAccess)
        try container.encode(
            professionalAccess,
            forKey: .professionalAccess
        )
        try container.encode(
            combinedAccess,
            forKey: .combinedAccess
        )
        try container.encode(
            readAccess,
            forKey: .readAccess
        )
        try container.encode(
            createAccess,
            forKey: .createAccess
        )
        try container.encode(
            isInternalAllAccess,
            forKey: .isInternalAllAccess
        )
        try container.encode(
            trialAlreadyUsed,
            forKey: .trialAlreadyUsed
        )
        try container.encode(
            canStartTrial,
            forKey: .canStartTrial
        )
        try container.encode(
            cancelAtPeriodEnd,
            forKey: .cancelAtPeriodEnd
        )
        try container.encode(
            hasPaymentIssue,
            forKey: .hasPaymentIssue
        )
        try container.encodeIfPresent(
            paymentIssueMessage,
            forKey: .paymentIssueMessage
        )
    }

    public var hasLearnAccess: Bool {
        ykiAccess
            || professionalAccess
            || combinedAccess
            || isInternalAllAccess
    }

    private static func tierHasReadAccess(
        _ tier: String?
    ) -> Bool {
        let normalized = tier?
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            .lowercased()
            ?? ""

        return normalized == "reader"
            || normalized == "read"
            || normalized == "read_premium"
            || normalized == "reader_premium"
            || normalized.hasPrefix("read_")
            || normalized.hasPrefix("reader_")
            || normalized.contains("floently_read")
    }
}
