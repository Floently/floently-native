import SwiftUI
import FloentlyShared

struct ReadBrowserView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var playbackSession: ReadPlaybackSession
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var documentLoader: ReadDocumentPlaybackLoader

    @StateObject private var controller = ReadBrowserController()

    let initialURL: URL?

    private let palette = FloentlyPalette.read

    var body: some View {
        VStack(spacing: 0) {
            browserToolbar

            if controller.isLoading {
                ProgressView(value: controller.estimatedProgress)
                    .progressViewStyle(.linear)
                    .tint(palette.accent)
            }

            if controller.currentURL == nil {
                browserStart
            } else {
                ZStack(alignment: .bottom) {
                    ReadWebView(controller: controller)
                        .ignoresSafeArea(edges: .bottom)

                    readStrip
                        .padding(.horizontal, 12)
                        .padding(.bottom, 10)
                }
            }
        }
        .background(palette.background)
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            if let initialURL, controller.currentURL == nil {
                controller.open(url: initialURL)
            }
        }
    }

    private var browserToolbar: some View {
        HStack(spacing: 6) {
            browserButton(
                systemName: "chevron.left",
                enabled: true,
                label: "Back"
            ) {
                if controller.canGoBack {
                    controller.goBack()
                } else {
                    dismiss()
                }
            }

            browserButton(
                systemName: "chevron.right",
                enabled: controller.canGoForward,
                label: "Forward"
            ) {
                controller.goForward()
            }

            TextField(
                "Search or enter website",
                text: $controller.addressText
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .keyboardType(.URL)
            .submitLabel(.go)
            .onSubmit {
                controller.open(controller.addressText)
            }
            .padding(.horizontal, 14)
            .frame(height: 44)
            .background(palette.elevated.opacity(0.92))
            .foregroundStyle(palette.text)
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
                .stroke(palette.border, lineWidth: 1)
            )
            .accessibilityLabel("Website address")

            browserButton(
                systemName:
                    controller.isLoading
                    ? "xmark"
                    : "arrow.clockwise",
                enabled: controller.currentURL != nil,
                label:
                    controller.isLoading
                    ? "Stop loading"
                    : "Reload"
            ) {
                if controller.isLoading {
                    controller.stopLoading()
                } else {
                    controller.reload()
                }
            }

            browserButton(
                systemName: "xmark.circle",
                enabled: true,
                label: "Close browser"
            ) {
                dismiss()
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .frame(minHeight: 56)
        .background(palette.surface)
    }

    private var browserStart: some View {
        VStack(spacing: 18) {
            Spacer()

            Image(systemName: "globe")
                .font(.system(size: 42, weight: .medium))
                .foregroundStyle(palette.accent)
                .accessibilityHidden(true)

            Text("Browse the real website")
                .font(.title2.weight(.semibold))
                .foregroundStyle(palette.text)

            Text(
                "Enter a course, article or website above. "
                + "Read keeps the original page interactive "
                + "and stays available while you navigate."
            )
            .font(.body)
            .multilineTextAlignment(.center)
            .foregroundStyle(palette.muted)
            .frame(maxWidth: 420)

            Spacer()
        }
        .padding(.horizontal, 28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(palette.background)
    }

    private var readStrip: some View {
        HStack(spacing: 8) {
            Button {
                controller.readPage()
            } label: {
                Label(
                    "Read page",
                    systemImage: "doc.text.magnifyingglass"
                )
                .font(.headline.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .frame(height: 48)
                .background(palette.accent)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .accessibilityHint(
                "Finds the main visible lesson or article "
                + "without replacing the website"
            )

            Button {
                controller.readSelection()
            } label: {
                Image(systemName: "selection.pin.in.out")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(palette.text)
                    .frame(width: 48, height: 48)
                    .background(palette.elevated)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Read selected text")

            Button {
                toggleNativeReading()
            } label: {
                Image(
                    systemName:
                        playbackSession.state == .playing
                        ? "pause.fill"
                        : "play.fill"
                )
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(palette.text)
                .frame(width: 48, height: 48)
                .background(palette.elevated)
                .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .disabled(
                activeReadingSource == nil
                || documentLoader.state == .preparing
            )
            .accessibilityLabel(
                playbackSession.state == .playing
                ? "Pause reading"
                : "Play reading"
            )
            .accessibilityHint(
                "Uses the persistent Floently Read player "
                + "while the original webpage stays visible"
            )

            Text(controller.readingStatus)
                .font(.footnote.weight(.medium))
                .foregroundStyle(
                    palette.text.opacity(0.82)
                )
                .lineLimit(2)
                .frame(
                    maxWidth: .infinity,
                    alignment: .leading
                )
        }
        .padding(8)
        .background(.ultraThinMaterial)
        .clipShape(
            RoundedRectangle(
                cornerRadius: 24,
                style: .continuous
            )
        )
        .overlay(
            RoundedRectangle(
                cornerRadius: 24,
                style: .continuous
            )
            .stroke(palette.border, lineWidth: 1)
        )
        .accessibilityElement(children: .contain)
    }

    private var activeReadingSource: ReadBrowserReadingSource? {
        guard let url = controller.currentURL else {
            return nil
        }

        let selection = controller.selectionText
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )

        if !selection.isEmpty {
            return ReadBrowserReadingSource(
                kind: .selection,
                url: url,
                title: readingTitle,
                language: controller.selectionLanguage,
                text: selection
            )
        }

        let page = controller.extractedText
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )

        guard !page.isEmpty else {
            return nil
        }

        return ReadBrowserReadingSource(
            kind: .page,
            url: url,
            title: readingTitle,
            language: controller.extractedLanguage,
            text: page
        )
    }

    private var readingTitle: String {
        let title = controller.pageTitle.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        if !title.isEmpty {
            return title
        }

        return controller.currentURL?.host
            ?? "Web reading"
    }

    private func toggleNativeReading() {
        guard let source = activeReadingSource else {
            return
        }

        do {
            let manifest =
                try ReadBrowserNativeReading.manifest(
                    for: source
                )

            if
                let current = playbackSession.document,
                current.id == manifest.documentId,
                current.revisionId == manifest.revisionId
            {
                playbackSession.togglePlayPause()
                controller.readingStatus =
                    playbackSession.state == .playing
                    ? "Reading the live page."
                    : "Reading paused."
                return
            }

            controller.readingStatus =
                "Preparing audio for the live page…"

            documentLoader.load(
                manifest: manifest,
                voiceId:
                    ReadBrowserNativeReading.defaultVoiceId(
                        for: source.language
                    ),
                sessionStore: sessionStore,
                playback: playbackSession,
                autoplay: true
            )
        } catch {
            controller.readingStatus =
                "Could not prepare this page: "
                + error.localizedDescription
        }
    }

    private func browserButton(
        systemName: String,
        enabled: Bool,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(
                    .system(
                        size: 17,
                        weight: .semibold
                    )
                )
                .foregroundStyle(
                    enabled
                    ? palette.text
                    : palette.muted.opacity(0.5)
                )
                .frame(width: 44, height: 44)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}
