import Foundation

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
}
