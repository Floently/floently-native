import Combine
import Foundation

public enum LearnPathwayV1: String, Codable, CaseIterable {
    case everyday
    case professional
    case yki
}

public enum LearnSkillV1: String, Codable, CaseIterable {
    case vocabulary
    case grammar
    case listening
    case speaking
    case reading
    case writing
}

public enum LearnRuntimeV1: String, Codable, CaseIterable {
    case cards
    case roleplay
    case reading
    case writing
    case listening
    case yki
    case professionalMission = "professional_mission"
    case grammar
}

public enum LearnTaskHealthV1: String, Codable {
    case available
    case degraded
    case unavailable
}

public enum LearnEventKindV1: String, Codable, CaseIterable {
    case taskStarted = "task_started"
    case taskCompleted = "task_completed"
    case taskSkipped = "task_skipped"
    case taskAbandoned = "task_abandoned"
    case answerSubmitted = "answer_submitted"
    case answerCorrected = "answer_corrected"
    case retryCompleted = "retry_completed"
    case writingSubmitted = "writing_submitted"
    case writingRetried = "writing_retried"
    case speakingSubmitted = "speaking_submitted"
    case readingCompleted = "reading_completed"
    case listeningCompleted = "listening_completed"
}

public enum LearnMetadataValue: Codable, Equatable {
    case string(String)
    case number(Double)
    case bool(Bool)

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else {
            throw DecodingError.typeMismatch(
                LearnMetadataValue.self,
                DecodingError.Context(
                    codingPath: decoder.codingPath,
                    debugDescription: "learning.v1 metadata accepts only string, number, or boolean values."
                )
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .bool(let value): try container.encode(value)
        }
    }
}

public struct LearnTaskModalityV1: Codable, Equatable {
    public let audio: Bool?
    public let microphone: Bool?
    public let keyboard: Bool?
    public let visual: Bool?

    public init(
        audio: Bool? = nil,
        microphone: Bool? = nil,
        keyboard: Bool? = nil,
        visual: Bool? = nil
    ) {
        self.audio = audio
        self.microphone = microphone
        self.keyboard = keyboard
        self.visual = visual
    }
}

public struct LearnTaskLaunchTargetV1: Codable, Equatable {
    public let route: String
    public let params: [String: String]?

    public init(route: String, params: [String: String]? = nil) {
        self.route = route
        self.params = params
    }
}

public struct TaskDescriptorV1: Codable, Equatable, Identifiable {
    public let schemaVersion: String
    public let taskId: String
    public let contentVersion: String
    public let runtime: LearnRuntimeV1
    public let pathway: LearnPathwayV1
    public let skills: [LearnSkillV1]
    public let levelBand: String
    public let estimatedMinutes: Int
    public let modality: LearnTaskModalityV1
    public let requiredEntitlements: [String]
    public let launch: LearnTaskLaunchTargetV1
    public let health: LearnTaskHealthV1
    public let featureFlag: String?
    public let profession: String?
    public let topic: String?
    public let contextId: String?
    public let prerequisites: [String]?
    public let tags: [String]?
    public let ykiMode: String?

    public var id: String { taskId }

    public init(
        schemaVersion: String = "learning.v1",
        taskId: String,
        contentVersion: String,
        runtime: LearnRuntimeV1,
        pathway: LearnPathwayV1,
        skills: [LearnSkillV1],
        levelBand: String,
        estimatedMinutes: Int,
        modality: LearnTaskModalityV1,
        requiredEntitlements: [String],
        launch: LearnTaskLaunchTargetV1,
        health: LearnTaskHealthV1,
        featureFlag: String? = nil,
        profession: String? = nil,
        topic: String? = nil,
        contextId: String? = nil,
        prerequisites: [String]? = nil,
        tags: [String]? = nil,
        ykiMode: String? = nil
    ) {
        self.schemaVersion = schemaVersion
        self.taskId = taskId
        self.contentVersion = contentVersion
        self.runtime = runtime
        self.pathway = pathway
        self.skills = skills
        self.levelBand = levelBand
        self.estimatedMinutes = estimatedMinutes
        self.modality = modality
        self.requiredEntitlements = requiredEntitlements
        self.launch = launch
        self.health = health
        self.featureFlag = featureFlag
        self.profession = profession
        self.topic = topic
        self.contextId = contextId
        self.prerequisites = prerequisites
        self.tags = tags
        self.ykiMode = ykiMode
    }
}

public struct PracticeSelectionReasonV1: Codable, Equatable {
    public let code: String
    public let message: String
    public let evidenceMode: String
}

public struct PracticeSessionTaskV1: Codable, Equatable {
    public let order: Int
    public let task: TaskDescriptorV1
    public let reasons: [PracticeSelectionReasonV1]
}

public struct PracticeSessionManifestV1: Codable, Equatable, Identifiable {
    public let schemaVersion: String
    public let sessionId: String
    public let learnerId: String
    public let createdAt: String
    public let scope: String
    public let targetMinutes: Int
    public let tasks: [PracticeSessionTaskV1]
    public let composerVersion: String

    public var id: String { sessionId }
}

public struct LearnerEventV1: Codable, Equatable, Identifiable {
    public let schemaVersion: String
    public let eventId: String
    public let learnerId: String
    public let occurredAt: String
    public let eventKind: LearnEventKindV1
    public let taskId: String
    public let contentVersion: String
    public let attemptId: String?
    public let pathway: LearnPathwayV1
    public let runtime: LearnRuntimeV1
    public let skills: [LearnSkillV1]
    public let levelBand: String
    public let profession: String?
    public let contextId: String?
    public let score: Double?
    public let maxScore: Double?
    public let metadata: [String: LearnMetadataValue]?

    public var id: String { eventId }

    public init(
        schemaVersion: String = "learning.v1",
        eventId: String = UUID().uuidString,
        learnerId: String,
        occurredAt: String = ISO8601DateFormatter().string(from: Date()),
        eventKind: LearnEventKindV1,
        taskId: String,
        contentVersion: String,
        attemptId: String? = nil,
        pathway: LearnPathwayV1,
        runtime: LearnRuntimeV1,
        skills: [LearnSkillV1],
        levelBand: String,
        profession: String? = nil,
        contextId: String? = nil,
        score: Double? = nil,
        maxScore: Double? = nil,
        metadata: [String: LearnMetadataValue]? = nil
    ) {
        self.schemaVersion = schemaVersion
        self.eventId = eventId
        self.learnerId = learnerId
        self.occurredAt = occurredAt
        self.eventKind = eventKind
        self.taskId = taskId
        self.contentVersion = contentVersion
        self.attemptId = attemptId
        self.pathway = pathway
        self.runtime = runtime
        self.skills = skills
        self.levelBand = levelBand
        self.profession = profession
        self.contextId = contextId
        self.score = score
        self.maxScore = maxScore
        self.metadata = metadata
    }
}

@MainActor
public final class LearningSessionStateV1: ObservableObject {
    @Published public private(set) var manifest: PracticeSessionManifestV1?
    @Published public private(set) var currentTaskIndex: Int = 0

    public init() {}

    public var currentTask: PracticeSessionTaskV1? {
        guard let manifest, manifest.tasks.indices.contains(currentTaskIndex) else {
            return nil
        }
        return manifest.tasks[currentTaskIndex]
    }

    public var isComplete: Bool {
        guard let manifest else { return false }
        return !manifest.tasks.isEmpty && currentTaskIndex >= manifest.tasks.count
    }

    public func load(_ manifest: PracticeSessionManifestV1) {
        self.manifest = manifest
        currentTaskIndex = 0
    }

    public func moveToTask(index: Int) {
        guard let manifest else { return }
        currentTaskIndex = min(max(index, 0), manifest.tasks.count)
    }

    public func advance() {
        guard let manifest else { return }
        currentTaskIndex = min(currentTaskIndex + 1, manifest.tasks.count)
    }

    public func clear() {
        manifest = nil
        currentTaskIndex = 0
    }
}

@MainActor
public final class LearningEventOutboxV1: ObservableObject {
    @Published public private(set) var pending: [LearnerEventV1] = []

    public init() {}

    public func enqueue(_ event: LearnerEventV1) {
        guard !pending.contains(where: { $0.eventId == event.eventId }) else {
            return
        }
        pending.append(event)
    }

    public func acknowledge(eventId: String) {
        pending.removeAll { $0.eventId == eventId }
    }

    public func replacePending(_ events: [LearnerEventV1]) {
        var seen = Set<String>()
        pending = events.filter { seen.insert($0.eventId).inserted }
    }

    public func clear() {
        pending.removeAll()
    }
}
