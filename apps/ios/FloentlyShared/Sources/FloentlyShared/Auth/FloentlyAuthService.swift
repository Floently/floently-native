import Foundation

public final class FloentlyAuthService {
    private let api: FloentlyAPIClient
    private let store: FloentlySessionStore

    public init(api: FloentlyAPIClient, store: FloentlySessionStore) {
        self.api = api
        self.store = store
    }

    public func login(email: String, password: String) async throws -> FloentlyAuthSession {
        let session = try await api.post(
            "/api/v1/auth/login/password",
            body: PasswordLoginRequest(email: email, password: password),
            as: FloentlyAuthSession.self
        )
        store.save(session)
        return session
    }

    public func register(email: String, password: String, name: String?) async throws -> FloentlyAuthSession {
        let session = try await api.post(
            "/api/v1/auth/register/password",
            body: PasswordRegisterRequest(email: email, password: password, name: name, captchaToken: nil),
            as: FloentlyAuthSession.self
        )
        store.save(session)
        return session
    }

    public func requestPasswordReset(email: String) async throws {
        _ = try await api.post(
            "/api/v1/auth/password-reset/request",
            body: PasswordResetRequest(email: email),
            as: FloentlyEmpty.self
        )
    }

    public func confirmPasswordReset(token: String, password: String) async throws {
        _ = try await api.post(
            "/api/v1/auth/password-reset/confirm",
            body: PasswordResetConfirmRequest(token: token, password: password),
            as: FloentlyEmpty.self
        )
    }

    public func restoreSession() async throws -> FloentlyAuthSession {
        let session = try await api.get("/api/v1/auth/session", as: FloentlyAuthSession.self)
        store.save(session)
        return session
    }

    public func logout() async {
        _ = try? await api.post(
            "/api/v1/auth/logout",
            body: LogoutRequest(refreshToken: store.session?.refreshToken),
            as: FloentlyEmpty.self
        )
        store.clear()
    }
}
