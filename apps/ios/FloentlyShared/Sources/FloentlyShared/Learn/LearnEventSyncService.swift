import Foundation

public struct LearnerEvidenceListResponseV1: Codable, Equatable {
    public let evidence: [SkillEvidenceV1]
}

public struct LearnerEventRecordResponseV1: Codable, Equatable {
    public let event: LearnerEventV1
    public let inserted: Bool
}

public final class LearnEventSyncService {
    private let api: FloentlyAPIClient

    public init(api: FloentlyAPIClient) {
        self.api = api
    }

    public func record(_ event: LearnerEventV1) async throws -> LearnerEventRecordResponseV1 {
        try await api.post(
            "/api/v1/learning/events",
            body: event,
            as: LearnerEventRecordResponseV1.self
        )
    }

    public func listEvidence(
        pathway: LearnPathwayV1? = nil,
        skill: LearnSkillV1? = nil,
        since: String? = nil,
        limit: Int = 200
    ) async throws -> [SkillEvidenceV1] {
        var query: [URLQueryItem] = [
            URLQueryItem(
                name: "limit",
                value: String(min(max(limit, 1), 500))
            )
        ]
        if let pathway {
            query.append(URLQueryItem(name: "pathway", value: pathway.rawValue))
        }
        if let skill {
            query.append(URLQueryItem(name: "skill", value: skill.rawValue))
        }
        if let since {
            query.append(URLQueryItem(name: "since", value: since))
        }

        let response = try await api.get(
            "/api/v1/learning/evidence",
            queryItems: query,
            as: LearnerEvidenceListResponseV1.self
        )
        return response.evidence
    }
}
