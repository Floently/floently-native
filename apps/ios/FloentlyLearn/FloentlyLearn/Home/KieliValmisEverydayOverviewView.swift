import SwiftUI
import FloentlyShared

struct KieliValmisEverydayOverviewView: View {
    let service: LearnOverviewService
    let accessState: KieliValmisCapabilityAccessState
    let retryAccess: () -> Void

    @State private var levelBand = "B1_B2"
    @State private var contentType = "vocabulary_card"
    @State private var deck: LearnCardDeckResponse?
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let levelOptions: [(String, String)] = [
        ("A1_A2", "A1–A2"),
        ("B1_B2", "B1–B2"),
        ("C1_C2", "C1–C2")
    ]

    private let contentOptions: [(String, String)] = [
        ("vocabulary_card", "Vocabulary"),
        ("sentence_card", "Sentences"),
        ("grammar_card", "Grammar")
    ]

    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.xl) {
                    KieliValmisIconTile(symbol: "book.pages")

                    VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
                        Text("Everyday Finnish")
                            .font(.system(size: 34, weight: .heavy))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        Text("Build practical Finnish through carefully structured vocabulary, sentence and grammar practice.")
                            .font(.system(size: 16))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    accessGate

                    if case .available = accessState {
                        filters
                        content
                    }
                }
                .padding(.horizontal, KieliValmisSpacing.ml)
                .padding(.top, KieliValmisSpacing.xl)
                .padding(.bottom, KieliValmisSpacing.huge)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task(id: "\(levelBand)|\(contentType)") {
            guard case .available = accessState else { return }
            await load()
        }
    }

    @ViewBuilder
    private var accessGate: some View {
        switch accessState {
        case .available:
            KieliValmisStatusBanner(
                message: "Everyday Finnish is available for this account.",
                tone: .success
            )
        case .locked(let message):
            KieliValmisStatusBanner(
                message: message,
                tone: .warning
            )
        case .unknown:
            KieliValmisStatusBanner(
                message: "We cannot confirm your Everyday Finnish access right now.",
                tone: .info,
                actionTitle: "Retry",
                action: retryAccess
            )
        }
    }

    private var filters: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.l) {
            VStack(alignment: .leading, spacing: KieliValmisSpacing.sm) {
                Text("Level")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                HStack(spacing: KieliValmisSpacing.s) {
                    ForEach(levelOptions, id: \.0) { option in
                        filterButton(
                            title: option.1,
                            selected: levelBand == option.0
                        ) {
                            levelBand = option.0
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: KieliValmisSpacing.sm) {
                Text("Practice material")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: KieliValmisSpacing.s) {
                        ForEach(contentOptions, id: \.0) { option in
                            Button {
                                contentType = option.0
                            } label: {
                                Text(option.1)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(
                                        contentType == option.0
                                            ? KieliValmisColor.textPrimary
                                            : KieliValmisColor.textSecondary
                                    )
                                    .padding(.horizontal, KieliValmisSpacing.sm)
                                    .frame(height: 44)
                                    .background(
                                        RoundedRectangle(
                                            cornerRadius: KieliValmisRadius.m,
                                            style: .continuous
                                        )
                                        .fill(
                                            contentType == option.0
                                                ? KieliValmisColor.brandTint
                                                : KieliValmisColor.surface1
                                        )
                                        .overlay(
                                            RoundedRectangle(
                                                cornerRadius: KieliValmisRadius.m,
                                                style: .continuous
                                            )
                                            .stroke(
                                                contentType == option.0
                                                    ? KieliValmisColor.brand
                                                    : KieliValmisColor.borderSoft,
                                                lineWidth: 1
                                            )
                                        )
                                    )
                            }
                            .buttonStyle(.plain)
                            .frame(minHeight: 48)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if isLoading && deck == nil {
            KieliValmisCardSurface(style: .standard) {
                ProgressView()
                    .tint(KieliValmisColor.brandBright)

                Text("Loading learning material…")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
            }
        } else if let errorMessage, deck == nil {
            KieliValmisStatusBanner(
                message: errorMessage,
                tone: .danger,
                actionTitle: "Retry",
                action: {
                    Task { await load() }
                }
            )
        } else if let deck {
            KieliValmisCardSurface(style: .hero) {
                Text(currentContentLabel.uppercased())
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(1)
                    .foregroundStyle(KieliValmisColor.brandBright)

                Text("\(deck.cards.count) items available")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text("Explore Finnish material for the selected level and activity type.")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            if deck.cards.isEmpty {
                KieliValmisStatusBanner(
                    message: "No release-safe material is currently available for this selection.",
                    tone: .info
                )
            } else {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
                    KieliValmisSectionHeader(title: "Examples")

                    ForEach(Array(deck.cards.prefix(6))) { card in
                        KieliValmisCardSurface(style: .compact) {
                            HStack(spacing: KieliValmisSpacing.sm) {
                                KieliValmisIconTile(symbol: symbolForContentType(card.contentType))

                                VStack(alignment: .leading, spacing: KieliValmisSpacing.xs) {
                                    Text(card.frontText)
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundStyle(KieliValmisColor.textPrimary)
                                        .fixedSize(horizontal: false, vertical: true)

                                    Text(displayLevel(card.levelBand))
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundStyle(KieliValmisColor.textTertiary)
                                }

                                Spacer()
                            }
                        }
                    }
                }
            }

            if let errorMessage {
                KieliValmisStatusBanner(
                    message: errorMessage,
                    tone: .warning,
                    actionTitle: "Retry",
                    action: {
                        Task { await load() }
                    }
                )
            }
        }
    }

    private var currentContentLabel: String {
        contentOptions.first(where: { $0.0 == contentType })?.1 ?? "Learning material"
    }

    private func filterButton(
        title: String,
        selected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(
                    selected
                        ? KieliValmisColor.textPrimary
                        : KieliValmisColor.textSecondary
                )
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(
                    RoundedRectangle(
                        cornerRadius: KieliValmisRadius.m,
                        style: .continuous
                    )
                    .fill(
                        selected
                            ? KieliValmisColor.brandTint
                            : KieliValmisColor.surface1
                    )
                    .overlay(
                        RoundedRectangle(
                            cornerRadius: KieliValmisRadius.m,
                            style: .continuous
                        )
                        .stroke(
                            selected
                                ? KieliValmisColor.brand
                                : KieliValmisColor.borderSoft,
                            lineWidth: 1
                        )
                    )
                )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .frame(minHeight: 48)
    }

    private func symbolForContentType(_ type: String) -> String {
        switch type {
        case "grammar_card":
            return "textformat"
        case "sentence_card":
            return "text.bubble"
        default:
            return "character.book.closed"
        }
    }

    private func displayLevel(_ value: String) -> String {
        value.replacingOccurrences(of: "_", with: "–")
    }

    @MainActor
    private func load() async {
        isLoading = true
        errorMessage = nil

        do {
            deck = try await service.fetchEverydayDeck(
                contentType: contentType,
                levelBand: levelBand
            )
        } catch let error as FloentlyAPIError {
            errorMessage = error.message
        } catch {
            errorMessage = "Could not load Everyday Finnish material."
        }

        isLoading = false
    }
}
