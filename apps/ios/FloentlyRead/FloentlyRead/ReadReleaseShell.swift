import Foundation
import SwiftUI
import UniformTypeIdentifiers
import FloentlyShared

@MainActor
final class ReadAccessModel: ObservableObject {
    enum State: Equatable {
        case idle
        case checking
        case granted
        case grantedOffline
        case blocked
        case failed(String)
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var status: FloentlyAccessStatus?
    @Published private(set) var offlineVerifiedAt: Date?

    private let leaseStore =
        ReadAccessLeaseStore.shared

    func refresh(sessionStore: FloentlySessionStore) async {
        guard
            let session = sessionStore.session,
            !session.token.isEmpty
        else {
            status = nil
            offlineVerifiedAt = nil
            state = .idle
            return
        }

        let accountIdentity =
            readAccountIdentity(
                userId: session.user.id,
                email: session.user.email
            )

        state = .checking
        offlineVerifiedAt = nil

        do {
            let api = FloentlyAPIClient(
                baseURL: URL(
                    string:
                        "https://learn-api.floently.com"
                )!,
                tokenProvider: {
                    session.token
                }
            )
            let value =
                try await FloentlyAccessService(
                    api: api
                )
                .fetchStatus()
            let granted =
                value.readAccess
                || value.isInternalAllAccess

            status = value
            state = granted
                ? .granted
                : .blocked

            if let accountIdentity {
                if granted {
                    try? leaseStore.save(
                        status: value,
                        accountIdentity:
                            accountIdentity
                    )
                } else {
                    leaseStore.remove(
                        accountIdentity:
                            accountIdentity
                    )
                }
            }
        } catch let error as URLError {
            guard error.code != .cancelled else {
                return
            }

            if restoreOfflineLease(
                accountIdentity:
                    accountIdentity
            ) {
                return
            }

            state = .failed(
                error.localizedDescription
            )
        } catch let error as FloentlyAPIError {
            if
                isTransientAccessError(error),
                restoreOfflineLease(
                    accountIdentity:
                        accountIdentity
                )
            {
                return
            }

            state = .failed(
                error.message
            )
        } catch {
            state = .failed(
                error.localizedDescription
            )
        }
    }

    private func restoreOfflineLease(
        accountIdentity: String?
    ) -> Bool {
        guard
            let accountIdentity,
            let lease =
                leaseStore.validGrantedLease(
                    accountIdentity:
                        accountIdentity
                )
        else {
            return false
        }

        status = lease.status
        offlineVerifiedAt =
            lease.verifiedAt
        state = .grantedOffline
        return true
    }

    private func isTransientAccessError(
        _ error: FloentlyAPIError
    ) -> Bool {
        if error.retryable {
            return true
        }

        let code = error.code.uppercased()
        if
            code == "HTTP_408"
            || code == "HTTP_429"
        {
            return true
        }

        guard code.hasPrefix("HTTP_") else {
            return false
        }

        let rawStatus =
            code.dropFirst(5)
        guard let status = Int(rawStatus) else {
            return false
        }

        return (500...599).contains(status)
    }
}

private enum ReadAuthMode: String, CaseIterable, Identifiable {
    case signIn = "Sign in"
    case create = "Create account"

    var id: String { rawValue }
}

struct ReadReleaseGateView: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var accessModel: ReadAccessModel
    @EnvironmentObject private var playbackSession: ReadPlaybackSession
    @EnvironmentObject private var documentLoader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var projectStore: ReadProjectStore

    var body: some View {
        Group {
            if sessionStore.session == nil {
                ReadAuthView()
            } else {
                switch accessModel.state {
                case .idle, .checking:
                    ReadLaunchStateView(
                        title: "Opening Read",
                        message: "Checking your Floently access…"
                    )
                case .granted:
                    ReadMainShell()
                case .grantedOffline:
                    ReadMainShell()
                        .safeAreaInset(
                            edge: .top,
                            spacing: 0
                        ) {
                            ReadOfflineAccessLeaseBanner(
                                verifiedAt:
                                    accessModel
                                        .offlineVerifiedAt
                            )
                        }
                case .blocked:
                    ReadEntitlementView()
                case .failed(let message):
                    ReadAccessFailureView(message: message)
                }
            }
        }
        .background(
            ReadDurableProjectProgressSyncView()
        )
        .task(id: sessionStore.session?.token) {
            await accessModel.refresh(
                sessionStore: sessionStore
            )
        }
        .onChange(of: sessionStore.session?.token) { previous, current in
            guard previous != current, previous != nil else {
                return
            }

            documentLoader.cancel()
            playbackSession.clear()
            projectStore.reset()
        }
    }
}

private struct ReadOfflineAccessLeaseBanner: View {
    @EnvironmentObject private var sessionStore:
        FloentlySessionStore
    @EnvironmentObject private var accessModel:
        ReadAccessModel

    let verifiedAt: Date?

    private let palette = FloentlyPalette.read

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "wifi.slash")
                .foregroundStyle(palette.accent2)

            VStack(
                alignment: .leading,
                spacing: 2
            ) {
                Text("Offline mode")
                    .font(
                        .caption.weight(
                            .bold
                        )
                    )
                    .foregroundStyle(
                        palette.text
                    )

                Text(detail)
                    .font(.caption2)
                    .foregroundStyle(
                        palette.muted
                    )
                    .lineLimit(2)
            }

            Spacer(minLength: 8)

            Button("Recheck") {
                Task {
                    await accessModel.refresh(
                        sessionStore:
                            sessionStore
                    )
                }
            }
            .buttonStyle(.plain)
            .font(
                .caption.weight(
                    .semibold
                )
            )
            .foregroundStyle(
                palette.accent2
            )
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .background(
            palette.elevated.opacity(0.97)
        )
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(palette.border)
                .frame(height: 1)
        }
        .accessibilityElement(
            children: .combine
        )
    }

    private var detail: String {
        guard let verifiedAt else {
            return "Using recently verified Read access saved on this device."
        }

        return "Access last verified "
            + verifiedAt.formatted(
                date: .abbreviated,
                time: .shortened
            )
            + "."
    }
}

private struct ReadLaunchStateView: View {
    let title: String
    let message: String

    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(spacing: 18) {
                Spacer()
                ProgressView()
                    .controlSize(.large)
                    .tint(FloentlyPalette.read.accent)
                Text(title)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(FloentlyPalette.read.text)
                Text(message)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(FloentlyPalette.read.muted)
                Spacer()
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private struct ReadAuthView: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore

    @State private var mode: ReadAuthMode = .signIn
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var busy = false
    @State private var errorMessage: String?
    @State private var resetMessage: String?

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer(minLength: 48)

                    Text("Floently Read")
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundStyle(palette.accent2)

                    Text(
                        mode == .signIn
                        ? "Welcome back."
                        : "Build your listening library."
                    )
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.text)
                    .padding(.top, 38)

                    Text(
                        "Read websites and documents with one persistent, native listening session."
                    )
                    .font(.title3)
                    .foregroundStyle(palette.muted)
                    .padding(.top, 12)
                    .frame(maxWidth: 560, alignment: .leading)

                    FloentlyCard(product: .read) {
                        Picker("Authentication mode", selection: $mode) {
                            ForEach(ReadAuthMode.allCases) {
                                Text($0.rawValue).tag($0)
                            }
                        }
                        .pickerStyle(.segmented)

                        if mode == .create {
                            TextField("Name", text: $name)
                                .textContentType(.name)
                                .readFieldStyle()
                        }

                        TextField("Email", text: $email)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.emailAddress)
                            .textContentType(.username)
                            .readFieldStyle()

                        SecureField("Password", text: $password)
                            .textContentType(
                                mode == .create
                                ? .newPassword
                                : .password
                            )
                            .readFieldStyle()

                        if let errorMessage {
                            Label(
                                errorMessage,
                                systemImage: "exclamationmark.triangle.fill"
                            )
                            .font(.footnote)
                            .foregroundStyle(FloentlyDesignTokens.Colors.warning)
                            .accessibilityLabel(errorMessage)
                        }

                        if let resetMessage {
                            Text(resetMessage)
                                .font(.footnote)
                                .foregroundStyle(palette.muted)
                        }

                        FloentlyPrimaryButton(
                            busy
                            ? "Please wait…"
                            : mode.rawValue,
                            product: .read
                        ) {
                            authenticate()
                        }
                        .disabled(busy)

                        if mode == .signIn {
                            Button("Forgot password?") {
                                requestPasswordReset()
                            }
                            .buttonStyle(.plain)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(palette.accent2)
                            .disabled(busy)
                        }
                    }
                    .padding(.top, 32)

                    Text(
                        "Your website passwords stay inside the native browser surface. Floently only receives the readable text you explicitly ask Read to process."
                    )
                    .font(.caption)
                    .foregroundStyle(palette.muted)
                    .padding(.top, 18)

                    HStack(spacing: 18) {
                        Link(
                            "Privacy",
                            destination: URL(
                                string: "https://www.floently.com/learn/privacy"
                            )!
                        )
                        Link(
                            "Terms",
                            destination: URL(
                                string: "https://www.floently.com/learn/terms"
                            )!
                        )
                        Link(
                            "Support",
                            destination: URL(
                                string: "https://www.floently.com/learn/support"
                            )!
                        )
                    }
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(palette.accent2)
                    .padding(.top, 10)
                    .padding(.bottom, 34)
                }
                .frame(maxWidth: 620)
                .frame(maxWidth: .infinity, alignment: .center)
            }
        }
    }

    private func authenticate() {
        let normalizedEmail = email.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        guard
            normalizedEmail.contains("@"),
            password.count >= 6
        else {
            errorMessage =
                "Enter a valid email and a password with at least 6 characters."
            return
        }

        busy = true
        errorMessage = nil
        resetMessage = nil

        Task {
            defer { busy = false }

            do {
                let api = FloentlyAPIClient(
                    baseURL: URL(
                        string: "https://learn-api.floently.com"
                    )!
                )
                let auth = FloentlyAuthService(
                    api: api,
                    store: sessionStore
                )

                switch mode {
                case .signIn:
                    _ = try await auth.login(
                        email: normalizedEmail,
                        password: password
                    )
                case .create:
                    _ = try await auth.register(
                        email: normalizedEmail,
                        password: password,
                        name: name.trimmingCharacters(
                            in: .whitespacesAndNewlines
                        ).nilIfBlank
                    )
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func requestPasswordReset() {
        let normalizedEmail = email.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        guard normalizedEmail.contains("@") else {
            errorMessage = "Enter your email first."
            return
        }

        busy = true
        errorMessage = nil

        Task {
            defer { busy = false }
            do {
                let api = FloentlyAPIClient(
                    baseURL: URL(
                        string: "https://learn-api.floently.com"
                    )!
                )
                let auth = FloentlyAuthService(
                    api: api,
                    store: sessionStore
                )
                try await auth.requestPasswordReset(
                    email: normalizedEmail
                )
                resetMessage =
                    "Password reset instructions have been requested."
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct ReadEntitlementView: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var accessModel: ReadAccessModel

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(alignment: .leading, spacing: 24) {
                Spacer()

                Text("Read access")
                    .font(.system(size: 40, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.text)

                Text(
                    "Your Floently account is signed in, but Read is not currently enabled for this account."
                )
                .font(.title3)
                .foregroundStyle(palette.muted)

                FloentlyCard(product: .read) {
                    Label(
                        "Native reading browser",
                        systemImage: "globe"
                    )
                    Label(
                        "Document library and imports",
                        systemImage: "books.vertical"
                    )
                    Label(
                        "Persistent voices, speed and resume",
                        systemImage: "waveform"
                    )

                    Text(
                        "This first native release verifies existing Floently Read entitlement. Purchase and plan changes stay outside this build until the native store flow is approved."
                    )
                    .font(.caption)
                    .foregroundStyle(palette.muted)

                    FloentlyPrimaryButton(
                        "Refresh access",
                        product: .read
                    ) {
                        Task {
                            await accessModel.refresh(
                                sessionStore: sessionStore
                            )
                        }
                    }
                }

                ReadAccessAccountDeletionControl()

                Button("Sign out") {
                    sessionStore.clear()
                }
                .buttonStyle(.plain)
                .foregroundStyle(palette.muted)

                Spacer()
            }
            .frame(maxWidth: 620)
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }
}

private struct ReadAccessFailureView: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var accessModel: ReadAccessModel

    let message: String

    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(spacing: 18) {
                Spacer()
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 42))
                    .foregroundStyle(FloentlyPalette.read.accent)
                Text("Couldn’t verify Read access")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(FloentlyPalette.read.text)
                Text(message)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(FloentlyPalette.read.muted)
                    .frame(maxWidth: 420)
                FloentlyPrimaryButton(
                    "Try again",
                    product: .read
                ) {
                    Task {
                        await accessModel.refresh(
                            sessionStore: sessionStore
                        )
                    }
                }
                .frame(maxWidth: 320)
                ReadAccessAccountDeletionControl()
                    .frame(maxWidth: 320)

                Button("Sign out") {
                    sessionStore.clear()
                }
                .buttonStyle(.plain)
                .foregroundStyle(FloentlyPalette.read.muted)
                Spacer()
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private struct ReadAccessAccountDeletionControl: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore

    @State private var showingConfirmation = false
    @State private var deleting = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(role: .destructive) {
                showingConfirmation = true
            } label: {
                Label(
                    deleting
                    ? "Deleting account…"
                    : "Delete account",
                    systemImage: "trash"
                )
                .font(.subheadline.weight(.semibold))
            }
            .buttonStyle(.plain)
            .foregroundStyle(FloentlyDesignTokens.Colors.danger)
            .disabled(deleting)

            if let errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(FloentlyDesignTokens.Colors.warning)
            }
        }
        .confirmationDialog(
            "Delete your Floently account?",
            isPresented: $showingConfirmation,
            titleVisibility: .visible
        ) {
            Button(
                "Delete account permanently",
                role: .destructive
            ) {
                deleteAccount()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This starts permanent deletion of your Floently account and associated personal data where deletion is legally possible. Store subscriptions must be cancelled separately in the App Store or Google Play."
            )
        }
    }

    private var offlineAccountIdentity: String? {
        guard
            let user =
                sessionStore.session?.user
        else {
            return nil
        }

        return readAccountIdentity(
            userId: user.id,
            email: user.email
        )
    }

    @ViewBuilder
    private func settingsValueRow(
        label: String,
        value: String
    ) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(palette.text)
            Spacer()
            Text(value)
                .font(
                    .subheadline.weight(
                        .semibold
                    )
                )
                .foregroundStyle(palette.muted)
        }
    }

    private func refreshOfflineSummary() async {
        guard let offlineAccountIdentity else {
            offlineSummary = .empty
            offlineStorageError = nil
            return
        }

        offlineStorageBusy = true
        offlineStorageError = nil
        offlineSummary =
            await ReadOfflineAudioStore.shared
                .summary(
                    accountIdentity:
                        offlineAccountIdentity
                )
        offlineStorageBusy = false
    }

    private func clearOfflineDownloads() {
        guard playback.document == nil else {
            offlineStorageError =
                "Stop playback before removing all offline downloads."
            return
        }
        guard let offlineAccountIdentity else {
            offlineStorageError =
                "Sign in again before managing offline downloads."
            return
        }

        offlineStorageBusy = true
        offlineStorageError = nil

        Task {
            await ReadOfflineAudioStore.shared
                .clearAccount(
                    accountIdentity:
                        offlineAccountIdentity
                )
            offlineSummary =
                await ReadOfflineAudioStore.shared
                    .summary(
                        accountIdentity:
                            offlineAccountIdentity
                    )
            offlineStorageBusy = false
        }
    }

    private func deleteAccount() {
        guard
            let token = sessionStore.session?.token,
            !token.isEmpty
        else {
            errorMessage =
                "Your session expired. Sign in again before deleting your account."
            return
        }

        deleting = true
        errorMessage = nil

        Task {
            defer { deleting = false }

            do {
                let api = FloentlyAPIClient(
                    baseURL: URL(
                        string: "https://learn-api.floently.com"
                    )!,
                    tokenProvider: { token }
                )
                try await FloentlyAuthService(
                    api: api,
                    store: sessionStore
                ).deleteAccount(
                    deletionReason: "read_native_access_screen"
                )
                await ReadOriginalDocumentStore.shared
                    .clearAll()
                await ReadEpubSourceStore.shared
                    .clearAll()
                await ReadProjectSnapshotStore.shared
                    .clearAll()
                await ReadOfflineAudioStore.shared
                    .clearAll()
                ReadAccessLeaseStore.shared
                    .clearAll()
                await ReadProgressOutboxStore.shared
                    .clearAll()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

struct ReadMainShell: View {
    enum Tab: Hashable {
        case home
        case library
        case settings
    }

    @EnvironmentObject private var browserRouter: ReadBrowserRouter
    @EnvironmentObject private var playbackSession: ReadPlaybackSession
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var projectStore: ReadProjectStore

    @State private var tab: Tab = .home
    @State private var showingAddSheet = false
    @State private var showingFileImporter = false
    @State private var showingPasteSheet = false
    @State private var showingURLSheet = false
    @State private var activeProject: ReadContentProject?

    var body: some View {
        TabView(selection: $tab) {
            ReadHomeDashboard(
                openBrowser: {
                    browserRouter.openBrowser()
                },
                openLibrary: {
                    tab = .library
                },
                addSource: {
                    showingAddSheet = true
                },
                openProject: {
                    activeProject = $0
                }
            )
            .tag(Tab.home)
            .tabItem {
                Label("Read", systemImage: "house.fill")
            }

            ReadLibraryScreen(
                addSource: {
                    showingAddSheet = true
                },
                openProject: {
                    activeProject = $0
                }
            )
            .tag(Tab.library)
            .tabItem {
                Label("Library", systemImage: "books.vertical.fill")
            }

            ReadSettingsScreen()
                .tag(Tab.settings)
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
        }
        .tint(FloentlyPalette.read.accent)
        .task(id: sessionStore.session?.token) {
            guard let session = sessionStore.session else {
                return
            }
            projectStore.bindAccount(
                userId: session.user.id,
                email: session.user.email
            )
            await projectStore.refresh(
                accessToken: session.token
            )

            if let incoming = browserRouter.consumePendingIncomingURL() {
                browserRouter.openBrowser(incoming)
            }
        }
        .onChange(of: browserRouter.pendingIncomingURL) { _, incoming in
            guard incoming != nil else { return }
            if let resolved = browserRouter.consumePendingIncomingURL() {
                browserRouter.openBrowser(resolved)
            }
        }
        .sheet(isPresented: $showingAddSheet) {
            ReadAddSourceSheet(
                onFiles: {
                    showingAddSheet = false
                    showingFileImporter = true
                },
                onPaste: {
                    showingAddSheet = false
                    showingPasteSheet = true
                },
                onLink: {
                    showingAddSheet = false
                    showingURLSheet = true
                },
                onWebsite: {
                    showingAddSheet = false
                    browserRouter.openBrowser()
                }
            )
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingPasteSheet) {
            ReadPasteTextSheet { project in
                activeProject = project
            }
        }
        .sheet(isPresented: $showingURLSheet) {
            ReadURLImportSheet { project in
                activeProject = project
            }
        }
        .fileImporter(
            isPresented: $showingFileImporter,
            allowedContentTypes:
                Self.supportedFileImportTypes,
            allowsMultipleSelection: false
        ) { result in
            importFile(result)
        }
        .fullScreenCover(item: $activeProject) { project in
            ReadProjectReaderView(project: project)
        }
    }

    private static let supportedFileImportTypes: [UTType] = {
        var values: [UTType] = [
            .pdf,
            .plainText,
            .rtf,
            .html
        ]

        values.append(
            contentsOf: [
                "docx",
                "epub",
                "md",
                "markdown"
            ].compactMap {
                UTType(filenameExtension: $0)
            }
        )

        return values
    }()

    private func importFile(
        _ result: Result<[URL], Error>
    ) {
        guard
            let token = sessionStore.session?.token
        else {
            return
        }

        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }

            Task {
                do {
                    activeProject = try await projectStore.addFile(
                        url: url,
                        accessToken: token
                    )
                } catch {
                    projectStore.errorMessage =
                        error.localizedDescription
                }
            }

        case .failure(let error):
            projectStore.errorMessage = error.localizedDescription
        }
    }
}

private struct ReadHomeDashboard: View {
    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var projectStore: ReadProjectStore
    @EnvironmentObject private var sessionStore: FloentlySessionStore

    let openBrowser: () -> Void
    let openLibrary: () -> Void
    let addSource: () -> Void
    let openProject: (ReadContentProject) -> Void

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Read")
                                .font(.system(size: 34, weight: .bold, design: .rounded))
                                .foregroundStyle(palette.text)
                            Text(
                                "Listen without leaving the source."
                            )
                            .foregroundStyle(palette.muted)
                        }

                        Spacer()

                        if playback.document != nil {
                            Button("Add") {
                                addSource()
                            }
                            .buttonStyle(.plain)
                            .font(
                                .subheadline.weight(
                                    .semibold
                                )
                            )
                            .foregroundStyle(
                                FloentlyDesignTokens
                                    .Colors
                                    .brandBright
                            )
                            .frame(
                                minWidth:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget,
                                minHeight:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget
                            )
                            .accessibilityLabel(
                                "Add to Read"
                            )
                        }

                        Circle()
                            .fill(palette.elevated)
                            .frame(width: 44, height: 44)
                            .overlay {
                                Text(accountInitial)
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(palette.accent2)
                            }
                            .accessibilityLabel("Account")
                    }
                    .padding(.top, 18)

                    if let document = playback.document {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("CONTINUE")
                                .readSectionLabel()

                            Button {
                                if
                                    let activeProject =
                                        projectStore
                                            .projects
                                            .first(
                                                where: {
                                                    $0.id
                                                        == document.id
                                                }
                                            )
                                {
                                    openProject(
                                        activeProject
                                    )
                                } else {
                                    playback.play()
                                }
                            } label: {
                                VStack(
                                    alignment: .leading,
                                    spacing:
                                        FloentlyDesignTokens
                                            .Space
                                            .s4
                                ) {
                                    HStack {
                                        Image(
                                            systemName:
                                                "waveform.circle.fill"
                                        )
                                        .font(
                                            .system(
                                                size: 34
                                            )
                                        )
                                        .foregroundStyle(
                                            palette.accent
                                        )

                                        Spacer()

                                        Image(
                                            systemName:
                                                "chevron.right"
                                        )
                                        .font(
                                            .headline.weight(
                                                .semibold
                                            )
                                        )
                                        .foregroundStyle(
                                            palette.muted
                                        )
                                        .frame(
                                            width:
                                                FloentlyDesignTokens
                                                    .Control
                                                    .iconTarget,
                                            height:
                                                FloentlyDesignTokens
                                                    .Control
                                                    .iconTarget
                                        )
                                    }

                                    Text(
                                        document.title
                                    )
                                    .font(
                                        .title2.weight(
                                            .bold
                                        )
                                    )
                                    .foregroundStyle(
                                        palette.text
                                    )
                                    .lineLimit(2)

                                    ProgressView(
                                        value:
                                            playback.duration
                                                > 0
                                            ? playback
                                                .elapsedTime
                                                / playback
                                                    .duration
                                            : 0
                                    )
                                    .tint(
                                        palette.accent
                                    )
                                }
                                .padding(
                                    FloentlyDesignTokens
                                        .Space
                                        .s5
                                )
                                .frame(
                                    maxWidth: .infinity,
                                    minHeight: 172
                                )
                                .background(
                                    RoundedRectangle(
                                        cornerRadius:
                                            FloentlyDesignTokens
                                                .Radius
                                                .xl,
                                        style:
                                            .continuous
                                    )
                                    .fill(
                                        FloentlyDesignTokens
                                            .Colors
                                            .surface2
                                    )
                                )
                                .overlay {
                                    RoundedRectangle(
                                        cornerRadius:
                                            FloentlyDesignTokens
                                                .Radius
                                                .xl,
                                        style:
                                            .continuous
                                    )
                                    .stroke(
                                        FloentlyDesignTokens
                                            .Colors
                                            .borderSoft,
                                        lineWidth: 1
                                    )
                                }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(
                                "Continue reading "
                                    + document.title
                            )
                        }
                        .padding(.top, 30)
                    }

                    HStack {
                        Text("LIBRARY")
                            .readSectionLabel()
                        Spacer()
                        Button("See all", action: openLibrary)
                            .buttonStyle(.plain)
                            .foregroundStyle(palette.accent2)
                    }
                    .padding(.top, 32)

                    if projectStore.projects.isEmpty {
                        ReadEmptyLibraryCard(addSource: addSource)
                            .padding(.top, 12)
                    } else {
                        VStack(spacing: 0) {
                            ForEach(projectStore.projects.prefix(3)) { project in
                                ReadProjectRow(
                                    project: project,
                                    action: {
                                        openProject(project)
                                    }
                                )
                            }
                        }
                        .padding(.top, 8)
                    }

                    if case .importing(let message) = projectStore.activity {
                        ReadImportProgressView(message: message)
                            .padding(.top, 18)
                    }

                    if let error = projectStore.errorMessage {
                        ReadStatusBanner(
                            icon: "exclamationmark.triangle",
                            text: error,
                            actionTitle: "Retry"
                        ) {
                            if let token = sessionStore.session?.token {
                                Task {
                                    await projectStore.refresh(
                                        accessToken: token
                                    )
                                }
                            }
                        }
                        .padding(.top, 18)
                    }

                    if playback.document == nil {
                        HStack(spacing: 12) {
                            Button(action: addSource) {
                                Label(
                                    "Add to Read",
                                    systemImage: "plus"
                                )
                                .font(
                                    .headline.weight(
                                        .semibold
                                    )
                                )
                                .frame(
                                    maxWidth: .infinity
                                )
                                .frame(
                                    height:
                                        FloentlyDesignTokens
                                            .Control
                                            .primaryHeight
                                )
                                .background(
                                    FloentlyDesignTokens
                                        .Colors
                                        .brand
                                )
                                .foregroundStyle(
                                    FloentlyDesignTokens
                                        .Colors
                                        .textOnBrand
                                )
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius:
                                            FloentlyDesignTokens
                                                .Radius
                                                .l,
                                        style:
                                            .continuous
                                    )
                                )
                            }
                            .buttonStyle(.plain)

                            Button(
                                action: openBrowser
                            ) {
                                Image(
                                    systemName:
                                        "globe"
                                )
                                .font(.headline)
                                .frame(
                                    width:
                                        FloentlyDesignTokens
                                            .Control
                                            .primaryHeight,
                                    height:
                                        FloentlyDesignTokens
                                            .Control
                                            .primaryHeight
                                )
                                .background(
                                    FloentlyDesignTokens
                                        .Colors
                                        .surface2
                                )
                                .foregroundStyle(
                                    palette.text
                                )
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius:
                                            FloentlyDesignTokens
                                                .Radius
                                                .l,
                                        style:
                                            .continuous
                                    )
                                )
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(
                                "Open reading browser"
                            )
                        }
                        .padding(.top, 28)
                        .padding(.bottom, 30)
                    } else {
                        Spacer()
                            .frame(height: 18)
                    }
                }
                .frame(maxWidth: 720)
                .frame(maxWidth: .infinity, alignment: .center)
            }
        }
    }

    private var accountInitial: String {
        let candidate =
            sessionStore.session?.user.name?.first
            ?? sessionStore.session?.user.email.first
            ?? "F"
        return String(candidate).uppercased()
    }
}

private struct ReadEmptyLibraryCard: View {
    let addSource: () -> Void

    private let palette = FloentlyPalette.read

    var body: some View {
        VStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s3
        ) {
            Image(systemName: "books.vertical")
                .font(.system(size: 34))
                .foregroundStyle(palette.accent)
            Text("Your library is ready")
                .font(.title3.weight(.bold))
                .foregroundStyle(palette.text)
            Text(
                "Add a document, paste text, or open a live website."
            )
            .multilineTextAlignment(.center)
            .foregroundStyle(palette.muted)

            Button("Add your first source", action: addSource)
                .buttonStyle(.plain)
                .font(.headline.weight(.semibold))
                .foregroundStyle(palette.accent2)
        }
        .padding(24)
        .frame(maxWidth: .infinity, minHeight: 220)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(palette.elevated.opacity(0.7))
        )
    }
}

private enum ReadLibraryFilter:
    String,
    CaseIterable,
    Identifiable
{
    case all = "All"
    case pdf = "PDF"
    case epub = "EPUB"
    case web = "Web"
    case text = "Text"
    case other = "Other"

    var id: String { rawValue }

    func matches(
        _ project: ReadContentProject
    ) -> Bool {
        let type =
            project.sourceType
                .lowercased()

        switch self {
        case .all:
            return true
        case .pdf:
            return type == "pdf"
        case .epub:
            return type == "epub"
        case .web:
            return [
                "web",
                "website",
                "url"
            ].contains(type)
        case .text:
            return [
                "text",
                "txt",
                "markdown",
                "md"
            ].contains(type)
        case .other:
            return !ReadLibraryFilter
                .allCases
                .dropFirst()
                .dropLast()
                .contains {
                    $0.matches(project)
                }
        }
    }
}

private struct ReadLibraryScreen: View {
    @EnvironmentObject private var projectStore: ReadProjectStore
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var loader: ReadDocumentPlaybackLoader

    @State private var searchText = ""
    @State private var searchVisible = false
    @State private var selectedFilter:
        ReadLibraryFilter = .all
    @State private var pendingDelete: ReadContentProject?

    let addSource: () -> Void
    let openProject: (ReadContentProject) -> Void

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(spacing: 0) {
                HStack {
                    Text("Library")
                        .font(
                            .system(
                                size:
                                    FloentlyDesignTokens
                                        .TypeScale
                                        .h1,
                                weight: .bold
                            )
                        )
                        .foregroundStyle(
                            palette.text
                        )

                    Spacer()

                    Button {
                        withAnimation(
                            .easeInOut(
                                duration:
                                    FloentlyDesignTokens
                                        .Motion
                                        .fast
                            )
                        ) {
                            searchVisible.toggle()
                            if !searchVisible {
                                searchText = ""
                            }
                        }
                    } label: {
                        Image(
                            systemName:
                                searchVisible
                                ? "xmark"
                                : "magnifyingglass"
                        )
                        .frame(
                            width:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget,
                            height:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget
                        )
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(
                        palette.text
                    )
                    .accessibilityLabel(
                        searchVisible
                        ? "Close library search"
                        : "Search library"
                    )

                    Button(action: addSource) {
                        Image(systemName: "plus")
                            .font(
                                .headline.weight(
                                    .bold
                                )
                            )
                            .frame(
                                width:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget,
                                height:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget
                            )
                            .background(
                                FloentlyDesignTokens
                                    .Colors
                                    .surface2
                            )
                            .clipShape(
                                RoundedRectangle(
                                    cornerRadius:
                                        FloentlyDesignTokens
                                            .Radius
                                            .m,
                                    style:
                                        .continuous
                                )
                            )
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(
                        palette.text
                    )
                    .accessibilityLabel(
                        "Add to Read"
                    )
                }
                .padding(.top, 18)

                if searchVisible {
                    TextField(
                        "Search your library",
                        text: $searchText
                    )
                    .textInputAutocapitalization(
                        .never
                    )
                    .readFieldStyle()
                    .padding(.top, 12)
                }

                ScrollView(
                    .horizontal,
                    showsIndicators: false
                ) {
                    HStack(
                        spacing:
                            FloentlyDesignTokens
                                .Space
                                .s2
                    ) {
                        ForEach(
                            ReadLibraryFilter
                                .allCases
                        ) { filter in
                            Button {
                                selectedFilter =
                                    filter
                            } label: {
                                Text(
                                    filter.rawValue
                                )
                                .font(
                                    .caption.weight(
                                        .semibold
                                    )
                                )
                                .foregroundStyle(
                                    selectedFilter
                                        == filter
                                    ? FloentlyDesignTokens
                                        .Colors
                                        .brandBright
                                    : palette.muted
                                )
                                .padding(
                                    .horizontal,
                                    FloentlyDesignTokens
                                        .Space
                                        .s3
                                )
                                .frame(
                                    minHeight:
                                        FloentlyDesignTokens
                                            .Control
                                            .compactHeight
                                )
                                .background(
                                    selectedFilter
                                        == filter
                                    ? FloentlyDesignTokens
                                        .Colors
                                        .brandTint
                                    : FloentlyDesignTokens
                                        .Colors
                                        .surface1
                                )
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius:
                                            FloentlyDesignTokens
                                                .Radius
                                                .m,
                                        style:
                                            .continuous
                                    )
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.top, 12)

                if case .importing(let message) = projectStore.activity {
                    ReadImportProgressView(message: message)
                        .padding(.top, 14)
                }

                if case .loading = projectStore.activity,
                   projectStore.projects.isEmpty {
                    Spacer()
                    ProgressView("Loading library…")
                        .tint(palette.accent)
                        .foregroundStyle(palette.muted)
                    Spacer()
                } else if filteredProjects.isEmpty {
                    Spacer()
                    ReadEmptyLibraryCard(addSource: addSource)
                        .frame(maxWidth: 420)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(filteredProjects) { project in
                                ReadProjectRow(
                                    project: project,
                                    action: {
                                        openProject(project)
                                    },
                                    onDelete: {
                                        pendingDelete = project
                                    }
                                )
                            }
                        }
                        .padding(.top, 14)
                        .padding(.bottom, 30)
                    }
                    .refreshable {
                        if let token = sessionStore.session?.token {
                            await projectStore.refresh(
                                accessToken: token
                            )
                        }
                    }
                }
            }
            .frame(maxWidth: 760)
            .frame(maxWidth: .infinity)
        }
        .confirmationDialog(
            "Remove this reading?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { presented in
                    if !presented {
                        pendingDelete = nil
                    }
                }
            ),
            titleVisibility: .visible,
            presenting: pendingDelete
        ) { project in
            Button("Remove from Library", role: .destructive) {
                delete(project)
            }
            Button("Cancel", role: .cancel) {
                pendingDelete = nil
            }
        } message: { project in
            Text(
                "“\(project.title)” will be removed from your synced Read library."
            )
        }
    }

    private func delete(_ project: ReadContentProject) {
        guard let token = sessionStore.session?.token else {
            projectStore.errorMessage = "Sign in again to change your library."
            return
        }

        pendingDelete = nil

        Task {
            do {
                if playback.document?.id == project.id {
                    loader.cancel()
                    playback.clear()
                }

                try await projectStore.delete(
                    project,
                    accessToken: token
                )
            } catch {
                projectStore.errorMessage =
                    error.localizedDescription
            }
        }
    }

    private var filteredProjects: [ReadContentProject] {
        let query = searchText.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        return projectStore.projects.filter {
            project in
            selectedFilter.matches(
                project
            )
            && (
                query.isEmpty
                || project.title
                    .localizedCaseInsensitiveContains(
                        query
                    )
                || project.displaySource
                    .localizedCaseInsensitiveContains(
                        query
                    )
            )
        }
    }
}

private struct ReadProjectRow: View {
    let project: ReadContentProject
    let action: () -> Void
    var onDelete: (() -> Void)? = nil

    private let palette = FloentlyPalette.read

    var body: some View {
        HStack(spacing: 4) {
            Button(action: action) {
                HStack(
                    spacing:
                        FloentlyDesignTokens
                            .Space
                            .s3
                ) {
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens
                                .Radius
                                .m,
                        style: .continuous
                    )
                    .fill(palette.elevated)
                    .frame(width: 48, height: 62)
                    .overlay {
                        Image(systemName: projectIcon)
                            .foregroundStyle(palette.accent2)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text(project.title)
                            .font(.headline)
                            .foregroundStyle(palette.text)
                            .lineLimit(2)

                        HStack(spacing: 7) {
                            Text(project.displaySource)
                            if project.wordCount > 0 {
                                Text("•")
                                Text(
                                    "\(project.wordCount.formatted()) words"
                                )
                            }
                        }
                        .font(.caption)
                        .foregroundStyle(palette.muted)
                        .lineLimit(1)
                    }

                    Spacer(minLength: 8)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.muted)
                }
                .padding(.vertical, 11)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity)

            if let onDelete {
                Menu {
                    Button(
                        "Remove from Library",
                        systemImage: "trash",
                        role: .destructive,
                        action: onDelete
                    )
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.headline)
                        .foregroundStyle(palette.muted)
                        .frame(width: 44, height: 52)
                        .contentShape(Rectangle())
                }
                .accessibilityLabel(
                    "More actions for \(project.title)"
                )
            }
        }
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(palette.border)
                .frame(height: 1)
                .padding(.leading, 62)
        }
        .accessibilityElement(children: .contain)
    }

    private var projectIcon: String {
        switch project.sourceType.lowercased() {
        case "pdf":
            return "doc.richtext"
        case "web", "website", "url":
            return "globe"
        case "text", "txt", "markdown", "md":
            return "text.alignleft"
        default:
            return "doc.text"
        }
    }
}

private struct ReadAddSourceSheet: View {
    @Environment(\.dismiss) private var dismiss

    let onFiles: () -> Void
    let onPaste: () -> Void
    let onLink: () -> Void
    let onWebsite: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Add to Read")
                .font(.title2.weight(.bold))
                .padding(.top, 8)
            Text(
                "Choose a source. Read keeps one listening session across documents and live websites."
            )
            .foregroundStyle(.secondary)
            .padding(.bottom, 8)

            ReadSourceRow(
                icon: "folder",
                title: "Files",
                subtitle: "PDF, document, TXT and Markdown"
            ) {
                dismiss()
                onFiles()
            }

            ReadSourceRow(
                icon: "doc.on.clipboard",
                title: "Paste text",
                subtitle: "Turn notes or copied text into a saved project"
            ) {
                dismiss()
                onPaste()
            }

            ReadSourceRow(
                icon: "link",
                title: "Import link",
                subtitle: "Save an article or public page into your library"
            ) {
                dismiss()
                onLink()
            }

            ReadSourceRow(
                icon: "globe",
                title: "Live website",
                subtitle: "Keep the original page visible and interactive"
            ) {
                dismiss()
                onWebsite()
            }

            Spacer(minLength: 12)
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
    }
}

private struct ReadSourceRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s3
            ) {
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .m,
                    style: .continuous
                )
                    .fill(
                        FloentlyDesignTokens
                            .Colors
                            .surface2
                    )
                    .frame(
                        width:
                            FloentlyDesignTokens
                                .Control
                                .compactHeight,
                        height:
                            FloentlyDesignTokens
                                .Control
                                .compactHeight
                    )
                    .overlay {
                        Image(systemName: icon)
                    }

                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.headline)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.secondary)
            }
            .frame(minHeight: 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct ReadPasteTextSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var projectStore: ReadProjectStore

    @State private var title = ""
    @State private var text = ""
    @State private var busy = false
    @State private var errorMessage: String?

    let onCreated: (ReadContentProject) -> Void

    var body: some View {
        NavigationStack {
            VStack(
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s4
            ) {
                TextField("Title optional", text: $title)
                    .readFieldStyle()

                TextEditor(text: $text)
                    .font(.body)
                    .padding(
                        FloentlyDesignTokens
                            .Space
                            .s4
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(
                        FloentlyDesignTokens
                            .Colors
                            .surface1
                    )
                    .clipShape(
                        RoundedRectangle(
                            cornerRadius:
                                FloentlyDesignTokens
                                    .Radius
                                    .m,
                            style: .continuous
                        )
                    )
                    .overlay(
                        RoundedRectangle(
                            cornerRadius:
                                FloentlyDesignTokens
                                    .Radius
                                    .m,
                            style: .continuous
                        )
                        .stroke(
                            FloentlyDesignTokens
                                .Colors
                                .border
                        )
                    )

                HStack {
                    Text("\(wordCount) words")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(FloentlyDesignTokens.Colors.warning)
                            .lineLimit(2)
                    }
                }

                FloentlyPrimaryButton(
                    busy ? "Adding…" : "Add to Read",
                    product: .read
                ) {
                    create()
                }
                .disabled(
                    busy
                    || text.trimmingCharacters(
                        in: .whitespacesAndNewlines
                    ).isEmpty
                )
            }
            .padding(20)
            .navigationTitle("Paste text")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    private var wordCount: Int {
        text.split(whereSeparator: { $0.isWhitespace }).count
    }

    private func create() {
        guard let token = sessionStore.session?.token else {
            errorMessage = "Sign in again to save this text."
            return
        }

        busy = true
        errorMessage = nil

        Task {
            defer { busy = false }
            do {
                let project = try await projectStore.addText(
                    title: title,
                    text: text,
                    accessToken: token
                )
                dismiss()
                onCreated(project)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct ReadURLImportSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var projectStore: ReadProjectStore

    @State private var title = ""
    @State private var sourceURL = ""
    @State private var busy = false
    @State private var errorMessage: String?

    let onCreated: (ReadContentProject) -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(
                    "Save a public article or page as a reading. For signed-in or interactive sites, use Live website instead."
                )
                .foregroundStyle(.secondary)

                TextField("Title optional", text: $title)
                    .readFieldStyle()

                TextField(
                    "https://example.com/article",
                    text: $sourceURL
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
                .textContentType(.URL)
                .readFieldStyle()

                if let errorMessage {
                    Label(
                        errorMessage,
                        systemImage: "exclamationmark.triangle.fill"
                    )
                    .font(.footnote)
                    .foregroundStyle(FloentlyDesignTokens.Colors.warning)
                }

                FloentlyPrimaryButton(
                    busy ? "Importing…" : "Save and open",
                    product: .read
                ) {
                    create()
                }
                .disabled(
                    busy
                    || sourceURL.trimmingCharacters(
                        in: .whitespacesAndNewlines
                    ).isEmpty
                )

                Spacer()
            }
            .padding(20)
            .navigationTitle("Import link")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func create() {
        guard let token = sessionStore.session?.token else {
            errorMessage = "Sign in again to save this link."
            return
        }

        busy = true
        errorMessage = nil

        Task {
            defer { busy = false }

            do {
                let project = try await projectStore.addURL(
                    title: title,
                    sourceURL: sourceURL,
                    accessToken: token
                )
                dismiss()
                onCreated(project)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct ReadReaderParagraphAnchor:
    Identifiable
{
    let index: Int
    let text: String
    let sourceScalarStart: Int

    var id: Int { index }
}

private struct ReadReaderSearchMatch {
    let paragraphIndex: Int
    let sourceScalarOffset: Int
    let scalarLength: Int
}

private struct ReadProjectReaderView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var projectStore: ReadProjectStore
    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var loader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var voiceSettings: ReadVoiceSettings

    let project: ReadContentProject

    @State private var hydrated: ReadContentProject?
    @State private var manifest: ReadingManifestV1?
    @State private var originalPDFURL: URL?
    @State private var epubPackage: ReadLocalEpubPackage?
    @State private var epubPackageError: String?
    @State private var errorMessage: String?
    @State private var preparing = false
    @State private var offlineAvailable = false
    @State private var offlineBusy = false
    @State private var offlineError: String?
    @State private var showingAppearance = false
    @State private var isSearching = false
    @State private var searchQuery = ""
    @State private var searchMatchIndex = 0
    @State private var readerScrollPosition: Int? = 0
    @State private var searchReturnPosition: Int?
    @FocusState private var searchFocused: Bool
    @StateObject private var appearance =
        ReadReaderAppearanceSettings()

    private let palette = FloentlyPalette.read

    var body: some View {
        ZStack {
            palette.background
                .ignoresSafeArea()

            VStack(spacing: 0) {
                if isSearching {
                    searchToolbar
                } else {
                HStack(
                    spacing:
                        FloentlyDesignTokens
                            .Space
                            .s2
                ) {
                    Button {
                        dismiss()
                    } label: {
                        Image(
                            systemName:
                                "chevron.left"
                        )
                        .frame(
                            width:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget,
                            height:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget
                        )
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(palette.text)
                    .accessibilityLabel("Back")

                    Text(project.title)
                        .font(
                            .system(
                                size:
                                    FloentlyDesignTokens
                                        .TypeScale
                                        .title,
                                weight: .semibold
                            )
                        )
                        .foregroundStyle(palette.text)
                        .lineLimit(1)

                    Spacer(minLength: 4)

                    Menu {
                        Button {
                            startListening()
                        } label: {
                            Label(
                                preparing
                                ? "Preparing audio…"
                                : "Listen",
                                systemImage:
                                    preparing
                                    ? "hourglass"
                                    : "headphones"
                            )
                        }
                        .disabled(
                            manifest == nil
                            || preparing
                        )

                        Button {
                            showingAppearance = true
                        } label: {
                            Label(
                                "Appearance",
                                systemImage:
                                    "textformat.size"
                            )
                        }

                        Button {
                            beginSearch()
                        } label: {
                            Label(
                                "Search in document",
                                systemImage:
                                    "magnifyingglass"
                            )
                        }
                        .disabled(
                            originalPDFURL != nil
                            || epubPackage != nil
                            || searchableParagraphAnchors
                                .isEmpty
                        )

                        Button {
                            Task {
                                await toggleOffline()
                            }
                        } label: {
                            Label(
                                offlineBusy
                                ? "Updating offline copy…"
                                : (
                                    offlineAvailable
                                    ? "Remove offline download"
                                    : "Save for offline listening"
                                ),
                                systemImage:
                                    offlineAvailable
                                    ? "trash"
                                    : "arrow.down.circle"
                            )
                        }
                        .disabled(
                            manifest == nil
                            || resolvedVoiceId == nil
                            || offlineBusy
                        )
                    } label: {
                        Image(systemName: "ellipsis")
                            .font(
                                .system(
                                    size: 18,
                                    weight: .semibold
                                )
                            )
                            .foregroundStyle(palette.text)
                            .frame(
                                width:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget,
                                height:
                                    FloentlyDesignTokens
                                        .Control
                                        .iconTarget
                            )
                    }
                    .accessibilityLabel(
                        "Reader options"
                    )
                }
                .padding(
                    .horizontal,
                    FloentlyDesignTokens
                        .Space
                        .s1
                )
                .frame(height: 56)
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .overlay(alignment: .bottom) {
                    Rectangle()
                        .fill(
                            FloentlyDesignTokens
                                .Colors
                                .borderSoft
                        )
                        .frame(height: 1)
                }
                }

                ProgressView(
                    value:
                        playback.document?.id
                            == project.id
                        && playback.duration > 0
                        ? playback.elapsedTime
                            / playback.duration
                        : 0
                )
                .tint(
                    FloentlyDesignTokens
                        .Colors
                        .brand
                )
                .frame(height: 2)

                if let errorMessage {
                    ReadStatusBanner(
                        icon: "exclamationmark.triangle",
                        text: errorMessage,
                        actionTitle: "Retry"
                    ) {
                        Task {
                            await prepareProject()
                        }
                    }
                    .padding(18)
                }

                if let offlineError {
                    ReadStatusBanner(
                        icon: "arrow.down.circle",
                        text: offlineError
                    )
                    .padding(.horizontal, 18)
                    .padding(.bottom, 10)
                }

                if let epubPackageError {
                    ReadStatusBanner(
                        icon: "book.closed",
                        text: epubPackageError
                    )
                    .padding(.horizontal, 18)
                    .padding(.bottom, 10)
                }

                if let originalPDFURL {
                    VStack(spacing: 0) {
                        HStack(spacing: 8) {
                            Image(systemName: "doc.richtext")
                                .foregroundStyle(palette.accent2)
                            Text(
                                "Original PDF pages · native reading layer ready"
                            )
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(palette.muted)
                            Spacer()
                        }
                        .padding(.horizontal, 18)
                        .padding(.vertical, 8)
                        .background(palette.elevated.opacity(0.72))

                        ReadOriginalPDFView(
                            url: originalPDFURL
                        )
                    }
                } else if let epubPackage {
                    VStack(spacing: 0) {
                        HStack(
                            spacing:
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                        ) {
                            Image(
                                systemName:
                                    "book.closed"
                            )
                            .foregroundStyle(
                                FloentlyDesignTokens
                                    .Colors
                                    .brandBright
                            )

                            Text(
                                "Original EPUB chapters · native reading layer ready"
                            )
                            .font(
                                .caption.weight(
                                    .semibold
                                )
                            )
                            .foregroundStyle(
                                palette.muted
                            )

                            Spacer()
                        }
                        .padding(
                            .horizontal,
                            FloentlyDesignTokens
                                .Space
                                .s4
                        )
                        .padding(
                            .vertical,
                            FloentlyDesignTokens
                                .Space
                                .s2
                        )
                        .background(
                            FloentlyDesignTokens
                                .Colors
                                .surface1
                        )

                        ReadOriginalEpubView(
                            package:
                                epubPackage
                        )
                    }
                } else if let text = hydrated?.rawText {
                    VStack(spacing: 0) {
                        if
                            hydrated?
                                .sourceType
                                .lowercased()
                                == "pdf"
                        {
                            HStack(spacing: 8) {
                                Image(
                                    systemName:
                                        "exclamationmark.circle"
                                )
                                .foregroundStyle(
                                    palette.accent2
                                )
                                Text(
                                    "The original PDF is not stored on this device. Showing the semantic reading layer."
                                )
                                .font(
                                    .caption.weight(
                                        .semibold
                                    )
                                )
                                .foregroundStyle(
                                    palette.muted
                                )
                                Spacer()
                            }
                            .padding(
                                .horizontal,
                                FloentlyDesignTokens
                                    .Space
                                    .s4
                            )
                            .padding(
                                .vertical,
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                            )
                            .background(
                                FloentlyDesignTokens
                                    .Colors
                                    .surface1
                            )
                        } else if
                            hydrated?
                                .sourceType
                                .lowercased()
                                == "epub",
                            epubPackage == nil
                        {
                            HStack(spacing: 8) {
                                Image(
                                    systemName:
                                        "exclamationmark.circle"
                                )
                                .foregroundStyle(
                                    palette.accent2
                                )
                                Text(
                                    "The original EPUB is not stored on this device. Showing the semantic reading layer."
                                )
                                .font(
                                    .caption.weight(
                                        .semibold
                                    )
                                )
                                .foregroundStyle(
                                    palette.muted
                                )
                                Spacer()
                            }
                            .padding(
                                .horizontal,
                                FloentlyDesignTokens
                                    .Space
                                    .s4
                            )
                            .padding(
                                .vertical,
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                            )
                            .background(
                                FloentlyDesignTokens
                                    .Colors
                                    .surface1
                            )
                        }

                        ScrollView {
                            LazyVStack(
                                alignment: .leading,
                                spacing: 18
                            ) {
                                ForEach(
                                    searchableParagraphAnchors
                                ) { paragraph in
                                    Text(paragraph.text)
                                        .font(
                                            .system(
                                                size:
                                                    appearance
                                                        .fontSize,
                                                weight: .regular
                                            )
                                        )
                                        .lineSpacing(
                                            appearance
                                                .additionalLineSpacing
                                        )
                                        .foregroundStyle(palette.text)
                                        .background(
                                            searchHighlightsParagraph(
                                                paragraph.index
                                            )
                                            ? FloentlyDesignTokens
                                                .Colors
                                                .brandTint
                                            : Color.clear
                                        )
                                        .textSelection(.enabled)
                                        .id(
                                            paragraph.index
                                        )
                                }
                            }
                            .scrollTargetLayout()
                            .frame(
                                maxWidth: 680,
                                alignment: .leading
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 24)
                            .padding(.top, 28)
                            .padding(.bottom, 120)
                        }
                        .scrollPosition(
                            id:
                                $readerScrollPosition,
                            anchor: .top
                        )
                    }
                } else {
                    Spacer()
                    ProgressView("Preparing readable content…")
                        .tint(palette.accent)
                        .foregroundStyle(palette.muted)
                    Spacer()
                }
            }
        }
        .task {
            await prepareProject()
        }
        .task(id: offlineAvailabilityKey) {
            await refreshOfflineAvailability()
        }
        .sheet(
            isPresented:
                $showingAppearance
        ) {
            ReadReaderAppearanceSheet(
                settings: appearance
            )
            .presentationDetents(
                [.medium]
            )
            .presentationDragIndicator(
                .visible
            )
        }
    }

    private var searchableParagraphAnchors:
        [ReadReaderParagraphAnchor]
    {
        guard
            let text =
                hydrated?.rawText,
            !text.isEmpty
        else {
            return []
        }

        return readerParagraphAnchors(
            text
        )
    }

    private var searchMatches:
        [ReadReaderSearchMatch]
    {
        let query = searchQuery
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )
        guard !query.isEmpty else {
            return []
        }

        return searchableParagraphAnchors
            .flatMap {
                searchMatches(
                    query,
                    in: $0
                )
            }
    }

    private var searchToolbar: some View {
        HStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s1
        ) {
            Button {
                endSearch()
            } label: {
                Image(systemName: "xmark")
                    .frame(
                        width:
                            FloentlyDesignTokens
                                .Control
                                .iconTarget,
                        height:
                            FloentlyDesignTokens
                                .Control
                                .iconTarget
                    )
            }
            .buttonStyle(.plain)
            .foregroundStyle(palette.text)
            .accessibilityLabel("Close search")

            TextField(
                "Search in document",
                text: $searchQuery
            )
            .focused($searchFocused)
            .textInputAutocapitalization(
                .never
            )
            .autocorrectionDisabled()
            .padding(
                .horizontal,
                FloentlyDesignTokens
                    .Space
                    .s3
            )
            .frame(height: 48)
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface2
            )
            .clipShape(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .m,
                    style: .continuous
                )
            )
            .onChange(
                of: searchQuery
            ) { _, _ in
                searchMatchIndex = 0
                jumpToCurrentSearchMatch()
            }

            Text(searchResultLabel)
                .font(
                    .caption.monospacedDigit()
                )
                .foregroundStyle(
                    palette.muted
                )
                .frame(minWidth: 42)

            searchNavigationButton(
                systemName:
                    "chevron.up",
                label:
                    "Previous search result",
                enabled:
                    !searchMatches.isEmpty
            ) {
                moveSearchResult(
                    delta: -1
                )
            }

            searchNavigationButton(
                systemName:
                    "chevron.down",
                label:
                    "Next search result",
                enabled:
                    !searchMatches.isEmpty
            ) {
                moveSearchResult(
                    delta: 1
                )
            }
        }
        .padding(
            .horizontal,
            FloentlyDesignTokens
                .Space
                .s1
        )
        .frame(height: 56)
        .background(
            FloentlyDesignTokens
                .Colors
                .surface1
        )
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(
                    FloentlyDesignTokens
                        .Colors
                        .borderSoft
                )
                .frame(height: 1)
        }
    }

    private var searchResultLabel: String {
        guard !searchMatches.isEmpty else {
            return "0/0"
        }

        let bounded =
            min(
                searchMatchIndex,
                searchMatches.count - 1
            )

        return "\(bounded + 1)/\(searchMatches.count)"
    }

    private func searchNavigationButton(
        systemName: String,
        label: String,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(
                    .system(
                        size: 15,
                        weight: .semibold
                    )
                )
                .frame(
                    width:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget,
                    height:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget
                )
        }
        .buttonStyle(.plain)
        .foregroundStyle(
            enabled
            ? palette.text
            : palette.muted.opacity(0.45)
        )
        .disabled(!enabled)
        .accessibilityLabel(label)
    }

    private func beginSearch() {
        searchReturnPosition =
            readerScrollPosition
        isSearching = true

        DispatchQueue.main.async {
            searchFocused = true
        }
    }

    private func endSearch() {
        searchFocused = false
        isSearching = false
        searchQuery = ""
        searchMatchIndex = 0

        if let searchReturnPosition {
            readerScrollPosition =
                searchReturnPosition
        }
    }

    private func moveSearchResult(
        delta: Int
    ) {
        let matches = searchMatches
        guard !matches.isEmpty else {
            return
        }

        let count = matches.count
        searchMatchIndex =
            (searchMatchIndex
                + delta
                + count)
            % count

        jumpToCurrentSearchMatch()
    }

    private func jumpToCurrentSearchMatch() {
        let matches = searchMatches
        guard !matches.isEmpty else {
            return
        }

        searchMatchIndex = min(
            max(0, searchMatchIndex),
            matches.count - 1
        )
        readerScrollPosition =
            matches[searchMatchIndex]
                .paragraphIndex
    }

    private func searchHighlightsParagraph(
        _ index: Int
    ) -> Bool {
        guard
            isSearching,
            !searchQuery
                .trimmingCharacters(
                    in:
                        .whitespacesAndNewlines
                )
                .isEmpty
        else {
            return false
        }

        return searchMatches
            .contains {
                $0.paragraphIndex == index
            }
    }

    private func readerParagraphAnchors(
        _ rawText: String
    ) -> [ReadReaderParagraphAnchor] {
        let source =
            rawText as NSString
        let separator =
            try? NSRegularExpression(
                pattern:
                    #"(?:\r\n|\r|\n)[\t ]*(?:\r\n|\r|\n)+"#
            )
        let separatorMatches =
            separator?.matches(
                in: rawText,
                range: NSRange(
                    location: 0,
                    length: source.length
                )
            )
            ?? []

        var result:
            [ReadReaderParagraphAnchor] = []
        var cursor = 0

        func appendChunk(
            _ range: NSRange
        ) {
            guard range.length > 0 else {
                return
            }

            let chunk =
                source.substring(
                    with: range
                )
            let trimmed =
                chunk.trimmingCharacters(
                    in:
                        .whitespacesAndNewlines
                )
            guard !trimmed.isEmpty else {
                return
            }

            let localRange =
                (chunk as NSString)
                    .range(of: trimmed)
            guard
                localRange.location
                    != NSNotFound
            else {
                return
            }

            let sourceUTF16Offset =
                range.location
                + localRange.location
            guard
                let scalarStart =
                    ReadScalarOffsets
                        .scalarOffset(
                            in: rawText,
                            utf16Offset:
                                sourceUTF16Offset
                        )
            else {
                return
            }

            result.append(
                ReadReaderParagraphAnchor(
                    index: result.count,
                    text: trimmed,
                    sourceScalarStart:
                        scalarStart
                )
            )
        }

        for match in separatorMatches {
            appendChunk(
                NSRange(
                    location: cursor,
                    length:
                        max(
                            0,
                            match.range.location
                                - cursor
                        )
                )
            )
            cursor =
                match.range.location
                + match.range.length
        }

        appendChunk(
            NSRange(
                location: cursor,
                length:
                    max(
                        0,
                        source.length
                            - cursor
                    )
            )
        )

        if
            result.isEmpty,
            !rawText
                .trimmingCharacters(
                    in:
                        .whitespacesAndNewlines
                )
                .isEmpty
        {
            let trimmed =
                rawText
                    .trimmingCharacters(
                        in:
                            .whitespacesAndNewlines
                    )
            let localRange =
                source.range(of: trimmed)
            if
                localRange.location
                    != NSNotFound,
                let scalarStart =
                    ReadScalarOffsets
                        .scalarOffset(
                            in: rawText,
                            utf16Offset:
                                localRange.location
                        )
            {
                result.append(
                    ReadReaderParagraphAnchor(
                        index: 0,
                        text: trimmed,
                        sourceScalarStart:
                            scalarStart
                    )
                )
            }
        }

        return result
    }

    private func searchMatches(
        _ query: String,
        in paragraph:
            ReadReaderParagraphAnchor
    ) -> [ReadReaderSearchMatch] {
        let source =
            paragraph.text as NSString
        let needle =
            query as NSString
        guard needle.length > 0 else {
            return []
        }

        var matches:
            [ReadReaderSearchMatch] = []
        var location = 0

        while location < source.length {
            let range = source.range(
                of: query,
                options: [
                    .caseInsensitive,
                    .diacriticInsensitive
                ],
                range: NSRange(
                    location: location,
                    length:
                        source.length
                        - location
                )
            )

            if range.location
                == NSNotFound
            {
                break
            }

            let endUTF16 =
                range.location
                + range.length
            if
                let relativeStart =
                    ReadScalarOffsets
                        .scalarOffset(
                            in: paragraph.text,
                            utf16Offset:
                                range.location
                        ),
                let relativeEnd =
                    ReadScalarOffsets
                        .scalarOffset(
                            in: paragraph.text,
                            utf16Offset:
                                endUTF16
                        )
            {
                matches.append(
                    ReadReaderSearchMatch(
                        paragraphIndex:
                            paragraph.index,
                        sourceScalarOffset:
                            paragraph
                                .sourceScalarStart
                            + relativeStart,
                        scalarLength:
                            max(
                                0,
                                relativeEnd
                                    - relativeStart
                            )
                    )
                )
            }

            location =
                range.location
                + max(
                    range.length,
                    1
                )
        }

        return matches
    }

    private var resolvedVoiceId: String? {
        guard let manifest else {
            return nil
        }

        if
            loader.activeManifest?.documentId
                == manifest.documentId,
            loader.activeManifest?.revisionId
                == manifest.revisionId,
            let activeVoiceId =
                loader.activeVoiceId
        {
            return activeVoiceId
        }

        return hydrated?.progress?.voiceId
            ?? project.progress?.voiceId
            ?? voiceSettings.voiceId(
                for: manifest.language
            )
    }

    private var offlineAvailabilityKey: String {
        [
            manifest?.revisionId ?? "",
            resolvedVoiceId ?? "",
            sessionStore.session?.user.id
                ?? sessionStore.session?.user.email
                ?? ""
        ].joined(separator: "::")
    }

    private func refreshOfflineAvailability() async {
        guard
            let manifest,
            let voiceId = resolvedVoiceId
        else {
            offlineAvailable = false
            return
        }

        offlineAvailable =
            await loader.isAvailableOffline(
                manifest: manifest,
                voiceId: voiceId,
                sessionStore: sessionStore
            )
    }

    private func toggleOffline() async {
        guard
            let manifest,
            let voiceId = resolvedVoiceId,
            !offlineBusy
        else {
            return
        }

        offlineBusy = true
        offlineError = nil
        defer {
            offlineBusy = false
        }

        if offlineAvailable {
            if
                playback.document?.id
                    == manifest.documentId,
                playback.document?.revisionId
                    == manifest.revisionId,
                loader.activeVoiceId == voiceId
            {
                offlineError =
                    "This offline voice is currently in use. Open another reading or stop the current session before removing it."
                return
            }

            await loader.removeOffline(
                manifest: manifest,
                voiceId: voiceId,
                sessionStore: sessionStore
            )
            offlineAvailable = false
            return
        }

        do {
            try await loader.saveOffline(
                manifest: manifest,
                voiceId: voiceId,
                sessionStore: sessionStore
            )
            offlineAvailable =
                await loader.isAvailableOffline(
                    manifest: manifest,
                    voiceId: voiceId,
                    sessionStore: sessionStore
                )
        } catch is CancellationError {
            return
        } catch {
            offlineError =
                error.localizedDescription
        }
    }

    private func prepareProject() async {
        guard let token = sessionStore.session?.token else {
            errorMessage = "Your session expired. Sign in again."
            return
        }

        preparing = true
        errorMessage = nil
        originalPDFURL = nil
        epubPackage = nil
        epubPackageError = nil
        defer { preparing = false }

        do {
            let value = try await projectStore.hydrate(
                project,
                accessToken: token
            )
            hydrated = value

            originalPDFURL =
                await ReadOriginalDocumentStore.shared
                    .pdfURL(for: value.id)

            if
                originalPDFURL == nil,
                value.sourceType
                    .lowercased()
                    == "epub",
                let epubURL =
                    await ReadOriginalDocumentStore
                        .shared
                        .epubURL(
                            for: value.id
                        )
            {
                do {
                    epubPackage =
                        try await ReadEpubSourceStore
                            .shared
                            .package(
                                projectId:
                                    value.id,
                                revisionId:
                                    value.revisionId,
                                sourceURL:
                                    epubURL
                            )
                } catch is CancellationError {
                    return
                } catch {
                    epubPackageError =
                        "The original EPUB could not be prepared on this device. The semantic reading layer remains available."
                }
            }

            guard let text = value.rawText, !text.isEmpty else {
                throw ReadProjectClientError.invalidProject
            }

            let builtManifest = try ReadCoreNative.buildManifest(
                documentId: value.id,
                revisionId: value.revisionId,
                title: value.title,
                language: value.language ?? "auto",
                text: text
            )
            ReadRemoteProgressBridge.apply(
                project: value,
                manifest: builtManifest,
                voiceSettings: voiceSettings
            )
            manifest = builtManifest
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func startListening() {
        guard
            let manifest,
            !preparing
        else {
            return
        }

        guard let voice = resolvedVoiceId else {
            return
        }
        loader.load(
            manifest: manifest,
            voiceId: voice,
            sessionStore: sessionStore,
            playback: playback,
            autoplay: true
        )
    }

    private func paragraphs(
        _ text: String
    ) -> [String] {
        let normalized = text.replacingOccurrences(
            of: "\r\n",
            with: "\n"
        )

        let chunks = normalized.components(
            separatedBy: "\n\n"
        )
        .map {
            $0.trimmingCharacters(
                in: .whitespacesAndNewlines
            )
        }
        .filter { !$0.isEmpty }

        if !chunks.isEmpty {
            return chunks
        }

        return [normalized]
    }
}

private struct ReadSettingsScreen: View {
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var accessModel: ReadAccessModel
    @EnvironmentObject private var playback: ReadPlaybackSession

    @State private var showDeleteConfirmation = false
    @State private var deletingAccount = false
    @State private var deleteError: String?
    @State private var offlineSummary: ReadOfflineAudioSummary?
    @State private var offlineStorageBusy = false
    @State private var offlineStorageError: String?
    @State private var showClearDownloadsConfirmation = false

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Text("Settings")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(palette.text)
                        .padding(.top, 18)

                    FloentlyCard(product: .read) {
                        Text("Account")
                            .font(.headline)
                            .foregroundStyle(palette.text)

                        Text(
                            sessionStore.session?.user.name
                            ?? sessionStore.session?.user.email
                            ?? "Floently account"
                        )
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(palette.text)

                        if let email = sessionStore.session?.user.email,
                           email != sessionStore.session?.user.name {
                            Text(email)
                                .foregroundStyle(palette.muted)
                        }

                        HStack {
                            Text("Read access")
                            Spacer()
                            Text(accessLabel)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(palette.accent2)
                        }
                        .foregroundStyle(palette.text)
                    }

                    FloentlyCard(product: .read) {
                        Text("Playback")
                            .font(.headline)
                            .foregroundStyle(palette.text)

                        HStack {
                            Text("Speed")
                                .foregroundStyle(palette.text)
                            Spacer()
                            Text(
                                String(
                                    format: "%.2g×",
                                    Double(playback.playbackRate)
                                )
                            )
                            .foregroundStyle(palette.muted)
                        }

                        Text(
                            "Voice and speed remain document-level playback settings and do not reset between segments."
                        )
                        .font(.caption)
                        .foregroundStyle(palette.muted)
                    }

                    FloentlyCard(product: .read) {
                        HStack {
                            Text("Offline downloads")
                                .font(.headline)
                                .foregroundStyle(palette.text)

                            Spacer()

                            Button {
                                Task {
                                    await refreshOfflineSummary()
                                }
                            } label: {
                                if offlineStorageBusy {
                                    ProgressView()
                                        .tint(
                                            FloentlyDesignTokens
                                                .Colors
                                                .brandBright
                                        )
                                        .frame(
                                            width: 32,
                                            height: 32
                                        )
                                } else {
                                    Image(
                                        systemName:
                                            "arrow.clockwise"
                                    )
                                    .frame(
                                        width: 32,
                                        height: 32
                                    )
                                }
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(
                                FloentlyDesignTokens
                                    .Colors
                                    .brandBright
                            )
                            .disabled(
                                offlineStorageBusy
                            )
                            .accessibilityLabel(
                                "Refresh offline storage"
                            )
                        }

                        if let offlineSummary {
                            settingsValueRow(
                                label: "Saved documents",
                                value:
                                    "\(offlineSummary.documentCount)"
                            )
                            settingsValueRow(
                                label: "Audio bundles",
                                value:
                                    "\(offlineSummary.bundleCount)"
                            )
                            settingsValueRow(
                                label: "Storage",
                                value:
                                    ByteCountFormatter.string(
                                        fromByteCount:
                                            offlineSummary.bytes,
                                        countStyle: .file
                                    )
                            )

                            Text(
                                "Downloads are stored privately on this device for this Floently account."
                            )
                            .font(.caption)
                            .foregroundStyle(palette.muted)

                            Button(role: .destructive) {
                                if playback.document != nil {
                                    offlineStorageError =
                                        "Stop playback before removing all offline downloads."
                                } else {
                                    showClearDownloadsConfirmation = true
                                }
                            } label: {
                                Text(
                                    offlineSummary.bundleCount == 0
                                    ? "No offline downloads"
                                    : "Remove all downloads"
                                )
                                .font(
                                    .subheadline.weight(
                                        .semibold
                                    )
                                )
                                .frame(
                                    maxWidth: .infinity
                                )
                                .frame(height: 48)
                            }
                            .buttonStyle(.bordered)
                            .tint(
                                FloentlyDesignTokens
                                    .Colors
                                    .danger
                            )
                            .disabled(
                                offlineStorageBusy
                                || offlineSummary.bundleCount == 0
                            )
                        } else {
                            HStack(
                                spacing:
                                    FloentlyDesignTokens
                                        .Space
                                        .s2
                            ) {
                                ProgressView()
                                    .tint(
                                        FloentlyDesignTokens
                                            .Colors
                                            .brand
                                    )
                                Text(
                                    "Checking saved audio…"
                                )
                                .foregroundStyle(
                                    palette.muted
                                )
                            }
                        }

                        if let offlineStorageError {
                            Text(offlineStorageError)
                                .font(.caption)
                                .foregroundStyle(
                                    FloentlyDesignTokens
                                        .Colors
                                        .warning
                                )
                        }
                    }

                    FloentlyCard(product: .read) {
                        Text("Privacy & support")
                            .font(.headline)
                            .foregroundStyle(palette.text)

                        Link(
                            "Privacy Policy",
                            destination: URL(
                                string: "https://www.floently.com/learn/privacy"
                            )!
                        )
                        Link(
                            "Terms of Use",
                            destination: URL(
                                string: "https://www.floently.com/learn/terms"
                            )!
                        )
                        Link(
                            "Support",
                            destination: URL(
                                string: "https://www.floently.com/learn/support"
                            )!
                        )
                        Link(
                            "Account deletion information",
                            destination: URL(
                                string: "https://www.floently.com/learn/delete-account"
                            )!
                        )
                    }
                    .foregroundStyle(palette.accent2)

                    if let deleteError {
                        ReadStatusBanner(
                            icon: "exclamationmark.triangle",
                            text: deleteError
                        )
                    }

                    Button {
                        playback.clear()
                        sessionStore.clear()
                    } label: {
                        Text("Sign out")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                    }
                    .buttonStyle(.bordered)
                    .tint(palette.accent2)

                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        Text(
                            deletingAccount
                            ? "Deleting account…"
                            : "Delete Account"
                        )
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
                    .disabled(deletingAccount)
                    .padding(.bottom, 30)
                }
                .frame(maxWidth: 720)
                .frame(maxWidth: .infinity, alignment: .center)
            }
        }
        .task(id: offlineAccountIdentity) {
            await refreshOfflineSummary()
        }
        .confirmationDialog(
            "Remove all offline downloads?",
            isPresented:
                $showClearDownloadsConfirmation,
            titleVisibility: .visible
        ) {
            Button(
                "Remove downloads",
                role: .destructive
            ) {
                clearOfflineDownloads()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This removes saved audio for this account from this device. Your Library items remain available."
            )
        }
        .confirmationDialog(
            "Delete your Floently account?",
            isPresented: $showDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button("Delete Account", role: .destructive) {
                deleteAccount()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This permanently deletes your Floently account and associated personal data where deletion is legally possible. Store subscriptions must still be cancelled in the App Store or Google Play."
            )
        }
    }

    private func deleteAccount() {
        guard
            let token = sessionStore.session?.token,
            !token.isEmpty
        else {
            deleteError = "Your session expired. Sign in again."
            return
        }

        deletingAccount = true
        deleteError = nil

        Task {
            defer {
                deletingAccount = false
            }

            do {
                let api = FloentlyAPIClient(
                    baseURL: URL(
                        string: "https://learn-api.floently.com"
                    )!,
                    tokenProvider: { token }
                )
                let auth = FloentlyAuthService(
                    api: api,
                    store: sessionStore
                )
                try await auth.deleteAccount(
                    deletionReason: "in_app_settings"
                )
                await ReadOriginalDocumentStore.shared
                    .clearAll()
                await ReadEpubSourceStore.shared
                    .clearAll()
                await ReadProjectSnapshotStore.shared
                    .clearAll()
                await ReadOfflineAudioStore.shared
                    .clearAll()
                ReadAccessLeaseStore.shared
                    .clearAll()
                await ReadProgressOutboxStore.shared
                    .clearAll()
                playback.clear()
            } catch {
                deleteError = error.localizedDescription
            }
        }
    }

    private var accessLabel: String {
        if accessModel.status?.isInternalAllAccess == true {
            return "Internal"
        }
        if accessModel.status?.readAccess == true {
            return accessModel.status?.billingTier?
                .replacingOccurrences(of: "_", with: " ")
                .capitalized
                ?? "Active"
        }
        return "Unavailable"
    }
}

private struct ReadReaderAppearanceSheet: View {
    @ObservedObject var settings:
        ReadReaderAppearanceSettings

    @Environment(\.dismiss)
    private var dismiss

    private let palette = FloentlyPalette.read

    var body: some View {
        VStack(
            alignment: .leading,
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s6
        ) {
            HStack {
                Text("Reader appearance")
                    .font(
                        .system(
                            size:
                                FloentlyDesignTokens
                                    .TypeScale
                                    .h3,
                            weight: .bold
                        )
                    )
                    .foregroundStyle(
                        palette.text
                    )

                Spacer()

                Button("Done") {
                    dismiss()
                }
                .buttonStyle(.plain)
                .foregroundStyle(
                    FloentlyDesignTokens
                        .Colors
                        .brandBright
                )
            }

            appearancePicker(
                title: "Text size",
                values:
                    ReadReaderTextSize
                        .allCases,
                selection:
                    $settings.textSize
            )

            appearancePicker(
                title: "Line spacing",
                values:
                    ReadReaderLineRhythm
                        .allCases,
                selection:
                    $settings.lineRhythm
            )

            VStack(
                alignment: .leading,
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s2
            ) {
                Text("Preview")
                    .font(
                        .caption.weight(
                            .semibold
                        )
                    )
                    .foregroundStyle(
                        palette.muted
                    )

                Text(
                    "A calm reading surface keeps the words clear and the controls out of the way."
                )
                .font(
                    .system(
                        size:
                            settings
                                .fontSize,
                        weight: .regular
                    )
                )
                .lineSpacing(
                    settings
                        .additionalLineSpacing
                )
                .foregroundStyle(
                    palette.text
                )
                .frame(
                    maxWidth: .infinity,
                    minHeight: 104,
                    alignment: .topLeading
                )
                .padding(
                    FloentlyDesignTokens
                        .Space
                        .s4
                )
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens
                                .Radius
                                .m,
                        style: .continuous
                    )
                )
            }

            Spacer(minLength: 0)
        }
        .padding(
            FloentlyDesignTokens
                .Space
                .s6
        )
        .background(
            FloentlyDesignTokens
                .Colors
                .surface2
        )
    }

    @ViewBuilder
    private func appearancePicker<
        Value
    >(
        title: String,
        values: [Value],
        selection: Binding<Value>
    ) -> some View
    where
        Value: Hashable
            & Identifiable,
        Value.ID == String
    {
        VStack(
            alignment: .leading,
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s2
        ) {
            Text(title)
                .font(
                    .subheadline.weight(
                        .semibold
                    )
                )
                .foregroundStyle(
                    palette.text
                )

            Picker(
                title,
                selection: selection
            ) {
                ForEach(values) {
                    value in
                    Text(
                        appearanceLabel(
                            value
                        )
                    )
                    .tag(value)
                }
            }
            .pickerStyle(.segmented)
            .frame(minHeight: 56)
        }
    }

    private func appearanceLabel<
        Value
    >(
        _ value: Value
    ) -> String {
        if
            let textSize =
                value
                    as? ReadReaderTextSize
        {
            return textSize.label
        }

        if
            let lineRhythm =
                value
                    as? ReadReaderLineRhythm
        {
            return lineRhythm.label
        }

        return ""
    }
}

private struct ReadImportProgressView: View {
    let message: String

    private let palette = FloentlyPalette.read

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                ProgressView()
                    .tint(palette.accent)
                Text(message)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(palette.text)
                Spacer()
            }

            ProgressView()
                .progressViewStyle(.linear)
                .tint(palette.accent)

            Text(
                "Read will open the document as soon as readable content is available. Audio can continue preparing afterward."
            )
            .font(.caption)
            .foregroundStyle(palette.muted)
        }
        .padding(
            FloentlyDesignTokens
                .Space
                .s4
        )
        .background(
            FloentlyDesignTokens
                .Colors
                .surface1
        )
        .clipShape(
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens
                        .Radius
                        .l,
                style: .continuous
            )
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(message). Import in progress.")
    }
}

private struct ReadStatusBanner: View {
    let icon: String
    let text: String
    let actionTitle: String?
    let action: (() -> Void)?

    private let palette = FloentlyPalette.read

    init(
        icon: String,
        text: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.text = text
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(FloentlyDesignTokens.Colors.warning)
            Text(text)
                .font(.footnote)
                .foregroundStyle(palette.text)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.plain)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(palette.accent2)
            }
        }
        .padding(
            .horizontal,
            FloentlyDesignTokens
                .Space
                .s3
        )
        .padding(.vertical, 10)
        .background(
            FloentlyDesignTokens
                .Colors
                .surface1
        )
        .clipShape(
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens
                        .Radius
                        .m,
                style: .continuous
            )
        )
        .overlay {
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens
                        .Radius
                        .m,
                style: .continuous
            )
            .stroke(
                FloentlyDesignTokens
                    .Colors
                    .borderSoft,
                lineWidth: 1
            )
        }
    }
}

private extension View {
    func readFieldStyle() -> some View {
        self
            .padding(
                .horizontal,
                FloentlyDesignTokens
                    .Space
                    .s4
            )
            .frame(
                minHeight:
                    FloentlyDesignTokens
                        .Control
                        .primaryHeight
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface1
            )
            .foregroundStyle(
                FloentlyDesignTokens
                    .Colors
                    .textPrimary
            )
            .clipShape(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .m,
                    style: .continuous
                )
            )
            .overlay(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .m,
                    style: .continuous
                )
                .stroke(
                    FloentlyDesignTokens
                        .Colors
                        .border
                )
            )
    }

    func readSectionLabel() -> some View {
        self
            .font(.caption.weight(.bold))
            .tracking(1.1)
            .foregroundStyle(FloentlyPalette.read.muted)
    }
}

private extension String {
    var nilIfBlank: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}
