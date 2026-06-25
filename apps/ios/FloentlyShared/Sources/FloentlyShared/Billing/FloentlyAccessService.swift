import Foundation

public final class FloentlyAccessService {
    private let api: FloentlyAPIClient

    public init(api: FloentlyAPIClient) {
        self.api = api
    }

    public func fetchStatus() async throws -> FloentlyAccessStatus {
        try await api.get("/api/v1/subscription/status", as: FloentlyAccessStatus.self)
    }
}
