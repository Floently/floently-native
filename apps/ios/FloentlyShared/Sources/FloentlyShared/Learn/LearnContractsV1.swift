import Combine
import Foundation

public enum LearnPathwayV1: String, Codable, CaseIterable {
    case everyday
    case yki
    case professional
}

public enum LearnSkillV1: String, Codable, CaseIterable {
    case vocabulary
    case grammar
    case listening
    case speaking
    case reading
    case writing
}

public enum LearnEventTypeV1: String, Codable, CaseIterable {
    case cardShown = "card_shown"
    case cardAnswered = "card_answered"
    case cardCorrect = "card_correct"
    case cardIncorrect = "card_incorrect"
    case confidenceCaptured = "confidence_captured"
    case reviewScheduled = "review_scheduled"
    case reviewCompleted = "review_completed"
    case vocabularyMastered = "vocabulary_mastered"
    case grammarActivity = "grammar_activity"
    case readingTask = "reading_task"
    case listeningTask = "listening_task"
    case writingSubmitted = "writing_submitted"
    case writingFeedback = "writing_feedback"
    case speakingSession = "speaking_session"
    case roleplayTurn = "roleplay_turn"
    case roleplayCompleted = "roleplay_completed"
    case ykiTaskAttempt = "yki_task_attempt"
    case ykiSkillResult = "yki_skill_result"
    case professionalScenarioAttempt = "professional_scenario_attempt"
    case professionalMissionCompleted = "professional_mission_completed"
    case phraseCaptured = "phrase_captured"
    case phraseReviewed = "phrase_reviewed"
    case learnerCorrection = "learner_correction"
    case qualifiedActivity = "qualified_activity"
}

public enum LearnJSONValue: Codable, Equatable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case object([String: LearnJSONValue])
    case array([LearnJSONValue])
    case null

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([String: LearnJSONValue].self) {
            self = .object(value)
        } else if let value = try? container.decode([LearnJSONValue].self) {
            self = .array(value)
        } else {
            throw DecodingError.typeMismatch(
                LearnJSONValue.self,
                DecodingError.Context(
                    codingPath: decoder.codingPath,
                    debugDescription: "Unsupported JSON value in Learn contract."
                )
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch self {
        case .string(let value):
            try container.encode(value)
        case .number(let value):
            try container.encode(value)
        case .bool(let value):
            try container.encode(value)
        case .object(let value):
            try container.encode(value)
        case .array(let value):
            try container.encode(value)
        case .null:
            try container.encodeNil()
        }
    }
}

public struct ActivityDefinitionV1: Codable, Equatable, Identifiable {
    public let schemaVersion: Int
    public let activityId: String
    public let activityType: String
    public let pathway: LearnPathwayV1
    public let skill: LearnSkillV1
    public let contentRef: String
    public let responseContract: [String: LearnJSONValue]
    public let evaluationPolicy: [String: LearnJSONValue]?
    public let mediaRefs: [String]

    public var id: String { activityId }

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case activityId = "activity_id"
        case activityType = "activity_type"
        case pathway
        case skill
        case contentRef = "content_ref"
        case responseContract = "response_contract"
        case evaluationPolicy = "evaluation_policy"
        case mediaRefs = "media_refs"
    }

    public init(
        schemaVersion: Int = 1,
        activityId: String,
        activityType: String,
        pathway: LearnPathwayV1,
        skill: LearnSkillV1,
        contentRef: String,
        responseContract: [String: LearnJSONValue],
        evaluationPolicy: [String: LearnJSONValue]? = nil,
        mediaRefs: [String] = []
    ) {
        self.schemaVersion = schemaVersion
        self.activityId = activityId
        self.activityType = activityType
        self.pathway = pathway
        self.skill = skill
        self.contentRef = contentRef
        self.responseContract = responseContract
        self.evaluationPolicy = evaluationPolicy
        self.mediaRefs = mediaRefs
    }
}

public struct LearningSessionPlanV1: Codable, Equatable, Identifiable {
    public let schemaVersion: Int
    public let sessionId: String
    public let revision: Int
    public let pathway: LearnPathwayV1
    public let levelBand: String
    public let objective: String
    public let policyVersion: String
    public let activities: [ActivityDefinitionV1]
    public let resumeCursor: Int
    public let generatedAt: String
    public let expiresAt: String?

    public var id: String { sessionId }

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case sessionId = "session_id"
        case revision
        case pathway
        case levelBand = "level_band"
        case objective
        case policyVersion = "policy_version"
        case activities
        case resumeCursor = "resume_cursor"
        case generatedAt = "generated_at"
        case expiresAt = "expires_at"
    }

    public init(
        schemaVersion: Int = 1,
        sessionId: String,
        revision: Int,
        pathway: LearnPathwayV1,
        levelBand: String,
        objective: String,
        policyVersion: String,
        activities: [ActivityDefinitionV1],
        resumeCursor: Int,
        generatedAt: String,
        expiresAt: String? = nil
    ) {
        self.schemaVersion = schemaVersion
        self.sessionId = sessionId
        self.revision = revision
        self.pathway = pathway
        self.levelBand = levelBand
        self.objective = objective
        self.policyVersion = policyVersion
        self.activities = activities
        self.resumeCursor = resumeCursor
        self.generatedAt = generatedAt
        self.expiresAt = expiresAt
    }
}

public struct LearningEventV1: Codable, Equatable, Identifiable {
    public let schemaVersion: Int
    public let eventId: String
    public let learnerId: String
    public let sessionId: String
    public let activityId: String?
    public let eventType: LearnEventTypeV1
    public let pathway: LearnPathwayV1
    public let skill: LearnSkillV1?
    public let occurredAt: String
    public let idempotencyKey: String
    public let evidence: [String: LearnJSONValue]

    public var id: String { eventId }

    enum CodingKeys: String, CodingKey {
        case schemaVersion = "schema_version"
        case eventId = "event_id"
        case learnerId = "learner_id"
        case sessionId = "session_id"
        case activityId = "activity_id"
        case eventType = "event_type"
        case pathway
        case skill
        case occurredAt = "occurred_at"
        case idempotencyKey = "idempotency_key"
        case evidence
    }

    public init(
        schemaVersion: Int = 1,
        eventId: String = UUID().uuidString,
        learnerId: String,
        sessionId: String,
        activityId: String?,
        eventType: LearnEventTypeV1,
        pathway: LearnPathwayV1,
        skill: LearnSkillV1?,
        occurredAt: String = ISO8601DateFormatter().string(from: Date()),
        idempotencyKey: String = UUID().uuidString,
        evidence: [String: LearnJSONValue]
    ) {
        self.schemaVersion = schemaVersion
        self.eventId = eventId
        self.learnerId = learnerId
        self.sessionId = sessionId
        self.activityId = activityId
        self.eventType = eventType
        self.pathway = pathway
        self.skill = skill
        self.occurredAt = occurredAt
        self.idempotencyKey = idempotencyKey
        self.evidence = evidence
    }
}

@MainActor
public final class LearningSessionStateV1: ObservableObject {
    @Published public private(set) var plan: LearningSessionPlanV1?
    @Published public private(set) var currentActivityIndex: Int = 0

    public init() {}

    public var currentActivity: ActivityDefinitionV1? {
        guard let plan, plan.activities.indices.contains(currentActivityIndex) else {
            return nil
        }
        return plan.activities[currentActivityIndex]
    }

    public var isComplete: Bool {
        guard let plan else { return false }
        return !plan.activities.isEmpty && currentActivityIndex >= plan.activities.count
    }

    public func load(_ plan: LearningSessionPlanV1) {
        self.plan = plan
        currentActivityIndex = min(
            max(plan.resumeCursor, 0),
            plan.activities.count
        )
    }

    public func moveToActivity(index: Int) {
        guard let plan else { return }
        currentActivityIndex = min(max(index, 0), plan.activities.count)
    }

    public func advance() {
        guard let plan else { return }
        currentActivityIndex = min(currentActivityIndex + 1, plan.activities.count)
    }

    public func clear() {
        plan = nil
        currentActivityIndex = 0
    }
}

@MainActor
public final class LearningEventOutboxV1: ObservableObject {
    @Published public private(set) var pending: [LearningEventV1] = []

    public init() {}

    public func enqueue(_ event: LearningEventV1) {
        guard !pending.contains(where: { $0.idempotencyKey == event.idempotencyKey }) else {
            return
        }
        pending.append(event)
    }

    public func acknowledge(idempotencyKey: String) {
        pending.removeAll { $0.idempotencyKey == idempotencyKey }
    }

    public func replacePending(_ events: [LearningEventV1]) {
        var seen = Set<String>()
        pending = events.filter { seen.insert($0.idempotencyKey).inserted }
    }

    public func clear() {
        pending.removeAll()
    }
}
