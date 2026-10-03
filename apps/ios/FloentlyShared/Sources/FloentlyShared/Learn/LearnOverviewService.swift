import Foundation

public struct YKIDailyPracticeOverview: Codable, Equatable {
    public let status: String
    public let focus: String
    public let minutes: Int
}

public struct YKIPracticeOverview: Codable, Equatable {
    public let levelBand: String
    public let displayLevelBand: String
    public let bankKind: String
    public let totalTasks: Int
    public let recommendedSections: [String]
    public let nextFocus: String
    public let nextTask: String
    public let countsBySkill: [String: Int]
    public let dailyPractice: YKIDailyPracticeOverview

    enum CodingKeys: String, CodingKey {
        case levelBand = "level_band"
        case displayLevelBand = "display_level_band"
        case bankKind = "bank_kind"
        case totalTasks = "total_tasks"
        case recommendedSections
        case nextFocus
        case nextTask
        case countsBySkill
        case dailyPractice
    }
}

public struct ProfessionalWorkTrack: Codable, Equatable, Identifiable {
    public let domain: String
    public let title: String
    public let coreTasks: [String]
    public let keyLanguageTargets: [String]
    public let speakingScenarios: [String]
    public let writingTasks: [String]
    public let vocabularyClusters: [String]

    public var id: String { domain }

    enum CodingKeys: String, CodingKey {
        case domain
        case title
        case coreTasks = "core_tasks"
        case keyLanguageTargets = "key_language_targets"
        case speakingScenarios = "speaking_scenarios"
        case writingTasks = "writing_tasks"
        case vocabularyClusters = "vocabulary_clusters"
    }
}

public struct ProfessionalOverview: Codable, Equatable {
    public let tracks: [ProfessionalWorkTrack]

    // The current backend also exposes a static recommendedTrack/nextMission.
    // Native Learn intentionally does not model those as learner-specific truth.
}

public final class LearnOverviewService {
    private let api: FloentlyAPIClient

    public init(api: FloentlyAPIClient) {
        self.api = api
    }

    public func fetchYKIOverview(
        levelBand: String = "B1-B2"
    ) async throws -> YKIPracticeOverview {
        try await api.get(
            "/api/v1/yki-practice/overview",
            queryItems: [
                URLQueryItem(
                    name: "level_band",
                    value: levelBand
                )
            ],
            as: YKIPracticeOverview.self
        )
    }

    public func fetchProfessionalOverview() async throws -> ProfessionalOverview {
        try await api.get(
            "/api/v1/professional/overview",
            as: ProfessionalOverview.self
        )
    }
}


public struct LearnCardPreview: Codable, Equatable, Identifiable {
    public let id: String
    public let contentType: String
    public let levelBand: String
    public let frontText: String
    public let backPrompt: String
    public let tags: [String]
    public let profession: String?

    enum CodingKeys: String, CodingKey {
        case id
        case contentType = "content_type"
        case levelBand = "level_band"
        case frontText = "front_text"
        case backPrompt = "back_prompt"
        case tags
        case profession
    }
}

public struct LearnCardDeckResponse: Codable, Equatable {
    public let cards: [LearnCardPreview]
}

public extension LearnOverviewService {
    func fetchEverydayDeck(
        contentType: String = "vocabulary_card",
        levelBand: String = "B1_B2",
        uiLanguage: String = "en"
    ) async throws -> LearnCardDeckResponse {
        try await api.get(
            "/api/v1/cards/deck",
            queryItems: [
                URLQueryItem(name: "domain", value: "general"),
                URLQueryItem(name: "content_type", value: contentType),
                URLQueryItem(name: "level", value: levelBand),
                URLQueryItem(name: "ui_language", value: uiLanguage)
            ],
            as: LearnCardDeckResponse.self
        )
    }
}
