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
        case blocked
        case failed(String)
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var status: FloentlyAccessStatus?

    func refresh(sessionStore: FloentlySessionStore) async {
        guard let token = sessionStore.session?.token, !token.isEmpty else {
            status = nil
            state = .idle
            return
        }

        state = .checking

        do {
            let api = FloentlyAPIClient(
                baseURL: URL(string: "https://learn-api.floently.com")!,
                tokenProvider: { token }
            )
            let value = try await FloentlyAccessService(api: api)
                .fetchStatus()
            status = value
            state =
                value.readAccess || value.isInternalAllAccess
                ? .granted
                : .blocked
        } catch {
            state = .failed(error.localizedDescription)
        }
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
                case .blocked:
                    ReadEntitlementView()
                case .failed(let message):
                    ReadAccessFailureView(message: message)
                }
            }
        }
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
                            .foregroundStyle(.orange)
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
            guard let token = sessionStore.session?.token else {
                return
            }
            await projectStore.refresh(accessToken: token)

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
            allowedContentTypes: [
                .pdf,
                .plainText,
                .rtf,
                .data
            ],
            allowsMultipleSelection: false
        ) { result in
            importFile(result)
        }
        .fullScreenCover(item: $activeProject) { project in
            ReadProjectReaderView(project: project)
        }
    }

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
                                playback.play()
                            } label: {
                                VStack(alignment: .leading, spacing: 16) {
                                    HStack {
                                        Image(systemName: "waveform.circle.fill")
                                            .font(.system(size: 34))
                                            .foregroundStyle(palette.accent)
                                        Spacer()
                                        Image(systemName: "play.fill")
                                            .foregroundStyle(.white)
                                            .frame(width: 46, height: 46)
                                            .background(palette.accent)
                                            .clipShape(Circle())
                                    }

                                    Text(document.title)
                                        .font(.title2.weight(.bold))
                                        .foregroundStyle(palette.text)
                                        .lineLimit(2)

                                    ProgressView(
                                        value: playback.duration > 0
                                            ? playback.elapsedTime / playback.duration
                                            : 0
                                    )
                                    .tint(palette.accent)
                                }
                                .padding(20)
                                .frame(maxWidth: .infinity, minHeight: 172)
                                .background(
                                    RoundedRectangle(
                                        cornerRadius: 24,
                                        style: .continuous
                                    )
                                    .fill(palette.elevated.opacity(0.92))
                                )
                            }
                            .buttonStyle(.plain)
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

                    HStack(spacing: 12) {
                        Button(action: addSource) {
                            Label("Add to Read", systemImage: "plus")
                                .font(.headline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(palette.accent)
                                .foregroundStyle(.white)
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius: 18,
                                        style: .continuous
                                    )
                                )
                        }
                        .buttonStyle(.plain)

                        Button(action: openBrowser) {
                            Image(systemName: "globe")
                                .font(.headline)
                                .frame(width: 54, height: 54)
                                .background(palette.elevated)
                                .foregroundStyle(palette.text)
                                .clipShape(
                                    RoundedRectangle(
                                        cornerRadius: 18,
                                        style: .continuous
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Open reading browser")
                    }
                    .padding(.top, 28)
                    .padding(.bottom, 30)
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
        VStack(spacing: 14) {
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

private struct ReadLibraryScreen: View {
    @EnvironmentObject private var projectStore: ReadProjectStore
    @EnvironmentObject private var sessionStore: FloentlySessionStore

    @State private var searchText = ""
    @State private var pendingDelete: ReadContentProject?

    let addSource: () -> Void
    let openProject: (ReadContentProject) -> Void

    private let palette = FloentlyPalette.read

    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(spacing: 0) {
                HStack {
                    Text("Library")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(palette.text)

                    Spacer()

                    Button(action: addSource) {
                        Image(systemName: "plus")
                            .font(.headline.weight(.bold))
                            .frame(width: 48, height: 48)
                            .background(palette.elevated)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(palette.text)
                    .accessibilityLabel("Add to Read")
                }
                .padding(.top, 18)

                TextField("Search your library", text: $searchText)
                    .textInputAutocapitalization(.never)
                    .readFieldStyle()
                    .padding(.top, 18)

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
        guard !query.isEmpty else {
            return projectStore.projects
        }

        return projectStore.projects.filter {
            $0.title.localizedCaseInsensitiveContains(query)
                || $0.displaySource.localizedCaseInsensitiveContains(query)
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
                HStack(spacing: 14) {
                    RoundedRectangle(
                        cornerRadius: 14,
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
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.secondary.opacity(0.12))
                    .frame(width: 42, height: 42)
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
            VStack(spacing: 14) {
                TextField("Title optional", text: $title)
                    .readFieldStyle()

                TextEditor(text: $text)
                    .font(.body)
                    .padding(10)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.secondary.opacity(0.08))
                    .clipShape(
                        RoundedRectangle(
                            cornerRadius: 18,
                            style: .continuous
                        )
                    )
                    .overlay(
                        RoundedRectangle(
                            cornerRadius: 18,
                            style: .continuous
                        )
                        .stroke(Color.secondary.opacity(0.18))
                    )

                HStack {
                    Text("\(wordCount) words")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(.orange)
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
                    .foregroundStyle(.orange)
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
    @State private var errorMessage: String?
    @State private var preparing = false

    private let palette = FloentlyPalette.read

    var body: some View {
        ZStack {
            palette.background
                .ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.left")
                            .frame(width: 48, height: 48)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(palette.text)
                    .accessibilityLabel("Back")

                    Text(project.title)
                        .font(.headline)
                        .foregroundStyle(palette.text)
                        .lineLimit(1)

                    Spacer()

                    Button {
                        startListening()
                    } label: {
                        if preparing {
                            ProgressView()
                                .tint(.white)
                                .frame(width: 48, height: 48)
                        } else {
                            Image(systemName: "headphones")
                                .frame(width: 48, height: 48)
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.white)
                    .background(palette.accent)
                    .clipShape(Circle())
                    .disabled(manifest == nil || preparing)
                    .accessibilityLabel("Listen")
                }
                .padding(.horizontal, 12)
                .frame(height: 58)

                ProgressView(
                    value: playback.document?.id == project.id
                        && playback.duration > 0
                        ? playback.elapsedTime / playback.duration
                        : 0
                )
                .tint(palette.accent)
                .frame(height: 3)

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

                if let text = hydrated?.rawText {
                    ScrollView {
                        LazyVStack(
                            alignment: .leading,
                            spacing: 18
                        ) {
                            ForEach(
                                Array(paragraphs(text).enumerated()),
                                id: \.offset
                            ) { _, paragraph in
                                Text(paragraph)
                                    .font(
                                        .system(
                                            size: 20,
                                            weight: .regular,
                                            design: .rounded
                                        )
                                    )
                                    .lineSpacing(8)
                                    .foregroundStyle(palette.text)
                                    .textSelection(.enabled)
                            }
                        }
                        .frame(maxWidth: 680, alignment: .leading)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 24)
                        .padding(.top, 28)
                        .padding(.bottom, 120)
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
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if playback.document != nil {
                ReadPersistentPlayerView()
            }
        }
        .task {
            await prepareProject()
        }
    }

    private func prepareProject() async {
        guard let token = sessionStore.session?.token else {
            errorMessage = "Your session expired. Sign in again."
            return
        }

        preparing = true
        errorMessage = nil
        defer { preparing = false }

        do {
            let value = try await projectStore.hydrate(
                project,
                accessToken: token
            )
            hydrated = value

            guard let text = value.rawText, !text.isEmpty else {
                throw ReadProjectClientError.invalidProject
            }

            manifest = try ReadCoreNative.buildManifest(
                documentId: value.id,
                revisionId: value.revisionId,
                title: value.title,
                language: value.language ?? "auto",
                text: text
            )
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

        let voice = voiceSettings.voiceId(
            for: manifest.language
        )
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

                    Button(role: .destructive) {
                        sessionStore.clear()
                    } label: {
                        Text("Sign out")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                    .padding(.bottom, 30)
                }
                .frame(maxWidth: 720)
                .frame(maxWidth: .infinity, alignment: .center)
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
        .padding(14)
        .background(palette.elevated.opacity(0.9))
        .clipShape(
            RoundedRectangle(
                cornerRadius: 16,
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
                .foregroundStyle(.orange)
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
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(palette.elevated.opacity(0.9))
        .clipShape(
            RoundedRectangle(
                cornerRadius: 14,
                style: .continuous
            )
        )
    }
}

private extension View {
    func readFieldStyle() -> some View {
        self
            .padding(.horizontal, 14)
            .frame(minHeight: 52)
            .background(
                Color.white.opacity(0.07)
            )
            .foregroundStyle(Color.white)
            .clipShape(
                RoundedRectangle(
                    cornerRadius: 16,
                    style: .continuous
                )
            )
            .overlay(
                RoundedRectangle(
                    cornerRadius: 16,
                    style: .continuous
                )
                .stroke(Color.white.opacity(0.12))
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
