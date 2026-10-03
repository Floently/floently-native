import Foundation

public struct FloentlyAccessStatus: Codable, Equatable {
    public let billingTier: String?
    public let subscriptionStatus: String?
    public let pathway: String?
    public let ykiAccess: Bool
    public let professionalAccess: Bool
    public let combinedAccess: Bool
    public let readAccess: Bool
    public let createAccess: Bool
    public let isInternalAllAccess: Bool
    public let isActive: Bool
    public let trialAlreadyUsed: Bool
    public let canStartTrial: Bool
    public let cancelAtPeriodEnd: Bool
    public let hasPaymentIssue: Bool
    public let paymentIssueMessage: String?
    public let accessibleProfessions: [String]
    public let selectedProfessions: [String]

    enum CodingKeys: String, CodingKey {
        case billingTier = "billing_tier"
        case subscriptionStatus = "subscription_status"
        case pathway
        case ykiAccess = "yki_access"
        case professionalAccess = "professional_access"
        case combinedAccess = "combined_access"
        case readAccess = "read_access"
        case createAccess = "create_access"
        case isInternalAllAccess = "is_internal_all_access"
        case isActive = "is_active"
        case trialAlreadyUsed = "trial_already_used"
        case canStartTrial = "can_start_trial"
        case cancelAtPeriodEnd = "cancel_at_period_end"
        case hasPaymentIssue = "has_payment_issue"
        case paymentIssueMessage = "payment_issue_message"
        case accessibleProfessions = "accessible_professions"
        case selectedProfessions = "selected_professions"
    }

    public init(
        billingTier: String?,
        subscriptionStatus: String?,
        pathway: String?,
        ykiAccess: Bool,
        professionalAccess: Bool,
        combinedAccess: Bool,
        readAccess: Bool,
        createAccess: Bool,
        isInternalAllAccess: Bool,
        isActive: Bool,
        trialAlreadyUsed: Bool,
        canStartTrial: Bool,
        cancelAtPeriodEnd: Bool,
        hasPaymentIssue: Bool,
        paymentIssueMessage: String?,
        accessibleProfessions: [String],
        selectedProfessions: [String]
    ) {
        self.billingTier = billingTier
        self.subscriptionStatus = subscriptionStatus
        self.pathway = pathway
        self.ykiAccess = ykiAccess
        self.professionalAccess = professionalAccess
        self.combinedAccess = combinedAccess
        self.readAccess = readAccess
        self.createAccess = createAccess
        self.isInternalAllAccess = isInternalAllAccess
        self.isActive = isActive
        self.trialAlreadyUsed = trialAlreadyUsed
        self.canStartTrial = canStartTrial
        self.cancelAtPeriodEnd = cancelAtPeriodEnd
        self.hasPaymentIssue = hasPaymentIssue
        self.paymentIssueMessage = paymentIssueMessage
        self.accessibleProfessions = accessibleProfessions
        self.selectedProfessions = selectedProfessions
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)

        billingTier = try c.decodeIfPresent(String.self, forKey: .billingTier)
        subscriptionStatus = try c.decodeIfPresent(String.self, forKey: .subscriptionStatus)
        pathway = try c.decodeIfPresent(String.self, forKey: .pathway)

        ykiAccess = try c.decodeIfPresent(Bool.self, forKey: .ykiAccess) ?? false
        professionalAccess = try c.decodeIfPresent(Bool.self, forKey: .professionalAccess) ?? false

        let explicitCombined = try c.decodeIfPresent(Bool.self, forKey: .combinedAccess)
        let normalizedPathway = (pathway ?? "").lowercased()
        combinedAccess = explicitCombined
            ?? (normalizedPathway == "combined" || (ykiAccess && professionalAccess))

        readAccess = try c.decodeIfPresent(Bool.self, forKey: .readAccess) ?? false
        createAccess = try c.decodeIfPresent(Bool.self, forKey: .createAccess) ?? false
        isInternalAllAccess = try c.decodeIfPresent(Bool.self, forKey: .isInternalAllAccess) ?? false
        isActive = try c.decodeIfPresent(Bool.self, forKey: .isActive)
            ?? isInternalAllAccess
            ?? false
        trialAlreadyUsed = try c.decodeIfPresent(Bool.self, forKey: .trialAlreadyUsed) ?? false
        canStartTrial = try c.decodeIfPresent(Bool.self, forKey: .canStartTrial) ?? false
        cancelAtPeriodEnd = try c.decodeIfPresent(Bool.self, forKey: .cancelAtPeriodEnd) ?? false
        hasPaymentIssue = try c.decodeIfPresent(Bool.self, forKey: .hasPaymentIssue) ?? false
        paymentIssueMessage = try c.decodeIfPresent(String.self, forKey: .paymentIssueMessage)
        accessibleProfessions = try c.decodeIfPresent([String].self, forKey: .accessibleProfessions) ?? []
        selectedProfessions = try c.decodeIfPresent([String].self, forKey: .selectedProfessions) ?? []
    }

    public var hasLearnAccess: Bool {
        ykiAccess || professionalAccess || combinedAccess || isInternalAllAccess
    }
}
