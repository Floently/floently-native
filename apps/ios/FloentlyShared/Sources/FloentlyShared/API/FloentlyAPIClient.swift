import Foundation

public struct FloentlyEmpty: Codable, Equatable {
    public init() {}
}

public struct FloentlyAPIError: Error, Equatable {
    public let code: String
    public let message: String
    public let retryable: Bool

    public init(code: String, message: String, retryable: Bool = false) {
        self.code = code
        self.message = message
        self.retryable = retryable
    }
}

public struct FloentlyEnvelope<T: Decodable>: Decodable {
    public let ok: Bool
    public let data: T?
    public let error: FloentlyEnvelopeError?
    public let meta: FloentlyEnvelopeMeta?
}

public struct FloentlyEnvelopeError: Decodable {
    public let code: String?
    public let message: String?
    public let retryable: Bool?
}

public struct FloentlyEnvelopeMeta: Decodable {
    public let requestId: String?
    public let timestamp: String?
    public let apiVersion: String?

    enum CodingKeys: String, CodingKey {
        case requestId = "request_id"
        case timestamp
        case apiVersion = "api_version"
    }
}

public final class FloentlyAPIClient {
    public let baseURL: URL
    private let session: URLSession
    private let tokenProvider: () -> String?

    public init(
        baseURL: URL = URL(string: "https://learn.floently.com")!,
        session: URLSession = .shared,
        tokenProvider: @escaping () -> String? = { nil }
    ) {
        self.baseURL = baseURL
        self.session = session
        self.tokenProvider = tokenProvider
    }

    public func get<T: Decodable>(_ path: String, as type: T.Type) async throws -> T {
        try await request(path: path, method: "GET", body: Optional<Data>.none, as: type)
    }

    public func post<RequestBody: Encodable, ResponseBody: Decodable>(
        _ path: String,
        body: RequestBody,
        as type: ResponseBody.Type
    ) async throws -> ResponseBody {
        let encoded = try JSONEncoder().encode(body)
        return try await request(path: path, method: "POST", body: encoded, as: type)
    }

    private func request<T: Decodable>(
        path: String,
        method: String,
        body: Data?,
        as type: T.Type
    ) async throws -> T {
        let url = try makeURL(path: path)
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        if let token = tokenProvider(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw FloentlyAPIError(code: "NO_HTTP_RESPONSE", message: "The server did not return a valid HTTP response.")
        }

        let decoder = JSONDecoder()

        if let envelope = try? decoder.decode(FloentlyEnvelope<T>.self, from: data) {
            if envelope.ok {
                if let payload = envelope.data {
                    return payload
                }
                if T.self == FloentlyEmpty.self {
                    return FloentlyEmpty() as! T
                }
            }

            throw FloentlyAPIError(
                code: envelope.error?.code ?? "API_ERROR",
                message: envelope.error?.message ?? "Request failed.",
                retryable: envelope.error?.retryable ?? false
            )
        }

        if (200..<300).contains(http.statusCode) {
            if T.self == FloentlyEmpty.self {
                return FloentlyEmpty() as! T
            }
            return try decoder.decode(T.self, from: data)
        }

        throw FloentlyAPIError(code: "HTTP_\(http.statusCode)", message: "Request failed with status \(http.statusCode).")
    }

    private func makeURL(path: String) throws -> URL {
        guard var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) else {
            throw FloentlyAPIError(code: "BAD_BASE_URL", message: "Invalid API base URL.")
        }

        let normalized = path.hasPrefix("/") ? path : "/\(path)"
        let basePath = components.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))

        if basePath.isEmpty {
            components.path = normalized
        } else {
            components.path = "/" + basePath + normalized
        }

        guard let url = components.url else {
            throw FloentlyAPIError(code: "BAD_URL", message: "Invalid API URL path.")
        }

        return url
    }
}
