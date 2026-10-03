import Foundation
import SwiftUI
import FloentlyShared

@MainActor
final class LearnAppModel: ObservableObject {
    enum Phase: Equatable {
        case bootstrapping
        case signedOut
        case signedIn
    }

    @Published private(set) var phase: Phase = .bootstrapping
    @Published private(set) var accessStatus: FloentlyAccessStatus?
    @Published private(set) var isAuthenticating = false
    @Published private(set) var isRefreshingAccess = false
    @Published var authError: String?
    @Published var connectionNotice: String?
    @Published var accessNotice: String?

    let sessionStore: FloentlySessionStore
    let learningSession: LearningSessionStateV1
    let eventOutbox: LearningEventOutboxV1
    let overviewService: LearnOverviewService

    private let api: FloentlyAPIClient
    private let authService: FloentlyAuthService
    private let accessService: FloentlyAccessService

    init() {
        let store = FloentlySessionStore()
        self.sessionStore = store
        self.learningSession = LearningSessionStateV1()
        self.eventOutbox = LearningEventOutboxV1()

        let client = FloentlyAPIClient(
            tokenProvider: {
                store.session?.token
            }
        )
        self.api = client
        self.authService = FloentlyAuthService(api: client, store: store)
        self.accessService = FloentlyAccessService(api: client)
        self.overviewService = LearnOverviewService(api: client)
    }

    var user: FloentlyUser? {
        sessionStore.session?.user
    }

    func bootstrap() async {
        guard phase == .bootstrapping else { return }

        guard sessionStore.session != nil else {
            phase = .signedOut
            return
        }

        do {
            _ = try await authService.restoreSession()
            phase = .signedIn
            connectionNotice = nil
            await refreshAccess()
        } catch let error as FloentlyAPIError {
            if isAuthenticationFailure(error) {
                sessionStore.clear()
                phase = .signedOut
                authError = nil
            } else {
                // The secure local session remains available for offline/retry.
                phase = .signedIn
                connectionNotice = "We could not verify your session right now. Some online features may be unavailable until you reconnect."
            }
        } catch {
            phase = .signedIn
            connectionNotice = "We could not verify your session right now. Some online features may be unavailable until you reconnect."
        }
    }

    func signIn(email: String, password: String) async {
        guard !isAuthenticating else { return }
        isAuthenticating = true
        authError = nil
        connectionNotice = nil
        defer { isAuthenticating = false }

        do {
            _ = try await authService.login(
                email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                password: password
            )
            phase = .signedIn
            await refreshAccess()
        } catch {
            authError = userFacingAuthMessage(error)
        }
    }

    func register(email: String, password: String, name: String?) async {
        guard !isAuthenticating else { return }
        isAuthenticating = true
        authError = nil
        connectionNotice = nil
        defer { isAuthenticating = false }

        do {
            _ = try await authService.register(
                email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                password: password,
                name: name?.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            phase = .signedIn
            await refreshAccess()
        } catch {
            authError = userFacingAuthMessage(error)
        }
    }

    func requestPasswordReset(email: String) async throws {
        try await authService.requestPasswordReset(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    func refreshAccess() async {
        guard phase == .signedIn, !isRefreshingAccess else { return }
        isRefreshingAccess = true
        accessNotice = nil
        defer { isRefreshingAccess = false }

        do {
            accessStatus = try await accessService.fetchStatus()
            connectionNotice = nil
        } catch let error as FloentlyAPIError {
            if isAuthenticationFailure(error) {
                sessionStore.clear()
                accessStatus = nil
                phase = .signedOut
                authError = "Your session has expired. Sign in again to continue."
            } else {
                accessStatus = nil
                accessNotice = "Your access status is temporarily unavailable."
            }
        } catch {
            accessStatus = nil
            accessNotice = "Your access status is temporarily unavailable."
        }
    }

    func logout() async {
        await authService.logout()
        learningSession.clear()
        eventOutbox.clear()
        accessStatus = nil
        authError = nil
        accessNotice = nil
        connectionNotice = nil
        phase = .signedOut
    }

    private func isAuthenticationFailure(_ error: FloentlyAPIError) -> Bool {
        error.code == "HTTP_401"
            || error.code == "HTTP_403"
            || error.code == "UNAUTHENTICATED"
            || error.code == "INVALID_SESSION"
    }

    private func userFacingAuthMessage(_ error: Error) -> String {
        guard let apiError = error as? FloentlyAPIError else {
            return "Could not connect to KieliValmis. Check your connection and try again."
        }

        switch apiError.code {
        case "INVALID_CREDENTIALS", "AUTH_INVALID_CREDENTIALS", "HTTP_401":
            return "The email or password is incorrect."
        case "EMAIL_ALREADY_EXISTS", "ACCOUNT_EXISTS", "CONFLICT":
            return "An account already exists for this email."
        case "RATE_LIMITED", "HTTP_429":
            return "Too many attempts. Try again shortly."
        case "HTTP_503", "SERVICE_UNAVAILABLE":
            return "KieliValmis is temporarily unavailable. Try again shortly."
        default:
            return apiError.message.isEmpty
                ? "Could not complete the request. Try again."
                : apiError.message
        }
    }
}
