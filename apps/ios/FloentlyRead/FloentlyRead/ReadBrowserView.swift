import SwiftUI
import FloentlyShared

struct ReadBrowserView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var playbackSession: ReadPlaybackSession
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var documentLoader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var voiceSettings: ReadVoiceSettings

    @StateObject private var controller = ReadBrowserController()
    @State private var visualDocumentId: String?
    @State private var visualRevisionId: String?

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
                        .id(controller.webViewGeneration)
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
        .onChange(of: controller.currentURL) { previous, current in
            if
                let previous,
                previous != current
            {
                clearReadingVisualAssociation()
            }
        }
        .onChange(of: playbackSession.state) { _, state in
            let active =
                state == .playing
                && playbackMatchesVisualDocument
            controller.setReadingVisual(
                active: active,
                pulse: active
            )
        }
        .onDisappear {
            clearReadingVisualAssociation()
            controller.detachBrowser()
        }
        .onChange(
            of: playbackSession.activeSegmentIndex
        ) { previous, current in
            guard
                previous != current,
                current != nil,
                playbackSession.state == .playing,
                playbackMatchesVisualDocument
            else {
                return
            }

            controller.setReadingVisual(
                active: true,
                pulse: true
            )
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
            .padding(
                .horizontal,
                FloentlyDesignTokens.Space.s4
            )
            .frame(height: 52)
            .background(
                FloentlyDesignTokens.Colors.surface1
            )
            .foregroundStyle(palette.text)
            .clipShape(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens.Radius.m,
                    style: .continuous
                )
            )
            .overlay(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens.Radius.m,
                    style: .continuous
                )
                .stroke(
                    FloentlyDesignTokens.Colors.border,
                    lineWidth: 1
                )
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
        .padding(
            .horizontal,
            FloentlyDesignTokens.Space.s1
        )
        .frame(height: 52)
        .background(
            FloentlyDesignTokens.Colors.surface1
        )
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(
                    FloentlyDesignTokens.Colors.borderSoft
                )
                .frame(height: 1)
        }
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
        .padding(
            .horizontal,
            FloentlyDesignTokens.Space.s6
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(palette.background)
    }

    private var readStrip: some View {
        HStack(spacing: 8) {
            Button {
                clearReadingVisualAssociation()
                controller.readPage {
                    toggleNativeReading(
                        ensurePlaying: true
                    )
                }
            } label: {
                Label(
                    "Read page",
                    systemImage: "doc.text.magnifyingglass"
                )
                .font(.headline.weight(.semibold))
                .foregroundStyle(
                    FloentlyDesignTokens.Colors.textOnBrand
                )
                .padding(
                    .horizontal,
                    FloentlyDesignTokens.Space.s4
                )
                .frame(height: 48)
                .background(
                    FloentlyDesignTokens.Colors.brand
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens.Radius.l,
                        style: .continuous
                    )
                )
            }
            .buttonStyle(.plain)
            .accessibilityHint(
                "Finds the main visible lesson or article "
                + "without replacing the website"
            )

            Button {
                clearReadingVisualAssociation()
                controller.readSelection {
                    toggleNativeReading(
                        ensurePlaying: true
                    )
                }
            } label: {
                Image(systemName: "selection.pin.in.out")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(palette.text)
                    .frame(width: 48, height: 48)
                    .background(
                        FloentlyDesignTokens.Colors.surface1
                    )
                    .clipShape(
                        RoundedRectangle(
                            cornerRadius:
                                FloentlyDesignTokens.Radius.m,
                            style: .continuous
                        )
                    )
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
                .background(
                    FloentlyDesignTokens.Colors.surface1
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens.Radius.m,
                        style: .continuous
                    )
                )
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
        .padding(
            .horizontal,
            FloentlyDesignTokens.Space.s1
        )
        .padding(.vertical, 2)
        .frame(minHeight: 52)
        .background(
            FloentlyDesignTokens.Colors.surface2
        )
        .clipShape(
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens.Radius.l,
                style: .continuous
            )
        )
        .overlay(
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens.Radius.l,
                style: .continuous
            )
            .stroke(
                FloentlyDesignTokens.Colors.borderSoft,
                lineWidth: 1
            )
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

    private var playbackMatchesVisualDocument: Bool {
        guard
            let document = playbackSession.document,
            let visualDocumentId,
            let visualRevisionId
        else {
            return false
        }

        return document.id == visualDocumentId
            && document.revisionId == visualRevisionId
    }

    private func clearReadingVisualAssociation() {
        visualDocumentId = nil
        visualRevisionId = nil
        controller.setReadingVisual(
            active: false,
            pulse: false
        )
    }

    private func toggleNativeReading(
        ensurePlaying: Bool = false
    ) {
        guard let source = activeReadingSource else {
            return
        }

        do {
            let manifest =
                try ReadBrowserNativeReading.manifest(
                    for: source
                )

            visualDocumentId = manifest.documentId
            visualRevisionId = manifest.revisionId

            if
                let current = playbackSession.document,
                current.id == manifest.documentId,
                current.revisionId == manifest.revisionId
            {
                if ensurePlaying {
                    if playbackSession.state != .playing {
                        playbackSession.play()
                    }
                } else {
                    playbackSession.togglePlayPause()
                }
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
                voiceId: voiceSettings.voiceId(
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
                .frame(
                    width:
                        FloentlyDesignTokens.Control.iconTarget,
                    height:
                        FloentlyDesignTokens.Control.iconTarget
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}
