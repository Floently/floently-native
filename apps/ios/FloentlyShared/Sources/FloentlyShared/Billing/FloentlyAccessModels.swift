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
        case billingTier = "billing_tier"
        case subscriptionStatus = "subscription_status"
        case ykiAccess = "yki_access"
        case professionalAccess = "professional_access"
        case combinedAccess = "combined_access"
        case readAccess = "read_access"
        case createAccess = "create_access"
        case isInternalAllAccess = "is_internal_all_access"
        case trialAlreadyUsed = "trial_already_used"
        case canStartTrial = "can_start_trial"
        case cancelAtPeriodEnd = "cancel_at_period_end"
        case hasPaymentIssue = "has_payment_issue"
        case paymentIssueMessage = "payment_issue_message"
    }

    public var hasLearnAccess: Bool {
        ykiAccess || professionalAccess || combinedAccess || isInternalAllAccess
    }
}
