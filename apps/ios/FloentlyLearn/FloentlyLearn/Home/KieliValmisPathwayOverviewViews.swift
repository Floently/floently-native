import SwiftUI
import FloentlyShared

struct KieliValmisYKIOverviewView: View {
    let service: LearnOverviewService
    let accessState: KieliValmisCapabilityAccessState
    let retryAccess: () -> Void

    @State private var levelBand = "B1-B2"
    @State private var overview: YKIPracticeOverview?
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let levelOptions = ["A1-A2", "B1-B2", "C1-C2"]

    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.xl) {
                    KieliValmisIconTile(symbol: "checkmark.seal")

                    VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
                        Text("YKI preparation")
                            .font(.system(size: 34, weight: .heavy))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        Text("Practice the four YKI skills using the existing certified practice bank.")
                            .font(.system(size: 16))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    accessGate

                    if case .available = accessState {
                        levelSelector
                        content
                    }
                }
                .padding(.horizontal, KieliValmisSpacing.ml)
                .padding(.top, KieliValmisSpacing.xl)
                .padding(.bottom, KieliValmisSpacing.huge)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task(id: levelBand) {
            guard case .available = accessState else { return }
            await load()
        }
    }

    @ViewBuilder
    private var accessGate: some View {
        switch accessState {
        case .available:
            KieliValmisStatusBanner(
                message: "YKI is available for this account.",
                tone: .success
            )
        case .locked(let message):
            KieliValmisStatusBanner(
                message: message,
                tone: .warning
            )
        case .unknown:
            KieliValmisStatusBanner(
                message: "We cannot confirm your YKI access right now.",
                tone: .info,
                actionTitle: "Retry",
                action: retryAccess
            )
        }
    }

    private var levelSelector: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.sm) {
            Text("Practice level")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(KieliValmisColor.textPrimary)

            HStack(spacing: KieliValmisSpacing.s) {
                ForEach(levelOptions, id: \.self) { level in
                    Button {
                        levelBand = level
                    } label: {
                        Text(level)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(
                                levelBand == level
                                    ? KieliValmisColor.textPrimary
                                    : KieliValmisColor.textSecondary
                            )
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(
                                RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                                    .fill(
                                        levelBand == level
                                            ? KieliValmisColor.brandTint
                                            : KieliValmisColor.surface1
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                                            .stroke(
                                                levelBand == level
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

    @ViewBuilder
    private var content: some View {
        if isLoading && overview == nil {
            KieliValmisCardSurface(style: .standard) {
                ProgressView()
                    .tint(KieliValmisColor.brandBright)
                Text("Loading YKI practice…")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
            }
        } else if let errorMessage, overview == nil {
            KieliValmisStatusBanner(
                message: errorMessage,
                tone: .danger,
                actionTitle: "Retry",
                action: {
                    Task { await load() }
                }
            )
        } else if let overview {
            KieliValmisCardSurface(style: .hero) {
                Text(overview.displayLevelBand)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.brandBright)

                Text("Certified practice bank")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text("\(overview.totalTasks) practice tasks · about \(overview.dailyPractice.minutes) min for a guided block")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
                KieliValmisSectionHeader(title: "Skills in this level")

                ForEach(
                    ["reading", "listening", "writing", "speaking"],
                    id: \.self
                ) { skill in
                    YKISkillAvailabilityRow(
                        skill: skill,
                        count: overview.countsBySkill[skill] ?? 0
                    )
                }
            }

            if !overview.nextTask.isEmpty {
                KieliValmisCardSurface(style: .compact) {
                    Text("Guided practice format")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(KieliValmisColor.textTertiary)

                    Text(overview.nextTask)
                        .font(.system(size: 16))
                        .foregroundStyle(KieliValmisColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
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

    @MainActor
    private func load() async {
        isLoading = true
        errorMessage = nil

        do {
            overview = try await service.fetchYKIOverview(
                levelBand: levelBand
            )
        } catch let error as FloentlyAPIError {
            errorMessage = error.message
        } catch {
            errorMessage = "Could not load YKI practice information."
        }

        isLoading = false
    }
}

private struct YKISkillAvailabilityRow: View {
    let skill: String
    let count: Int

    private var title: String {
        skill.prefix(1).uppercased() + skill.dropFirst()
    }

    private var symbol: String {
        switch skill {
        case "reading": return "text.book.closed"
        case "listening": return "headphones"
        case "writing": return "square.and.pencil"
        case "speaking": return "waveform"
        default: return "circle.grid.2x2"
        }
    }

    var body: some View {
        KieliValmisCardSurface(style: .compact) {
            HStack(spacing: KieliValmisSpacing.sm) {
                KieliValmisIconTile(symbol: symbol)

                VStack(alignment: .leading, spacing: KieliValmisSpacing.xs) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(KieliValmisColor.textPrimary)

                    Text("\(count) tasks available")
                        .font(.system(size: 14))
                        .foregroundStyle(KieliValmisColor.textSecondary)
                }

                Spacer()
            }
        }
    }
}

struct KieliValmisProfessionalOverviewView: View {
    let service: LearnOverviewService
    let accessState: KieliValmisCapabilityAccessState
    let retryAccess: () -> Void

    @State private var overview: ProfessionalOverview?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.xl) {
                    KieliValmisIconTile(symbol: "briefcase")

                    VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
                        Text("Work in Finland")
                            .font(.system(size: 34, weight: .heavy))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        Text("Professional Finnish for real workplace communication and role-specific situations.")
                            .font(.system(size: 16))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    accessGate

                    if case .available = accessState {
                        content
                    }
                }
                .padding(.horizontal, KieliValmisSpacing.ml)
                .padding(.top, KieliValmisSpacing.xl)
                .padding(.bottom, KieliValmisSpacing.huge)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            guard case .available = accessState else { return }
            if overview == nil {
                await load()
            }
        }
    }

    @ViewBuilder
    private var accessGate: some View {
        switch accessState {
        case .available:
            KieliValmisStatusBanner(
                message: "Professional Finnish is available for this account.",
                tone: .success
            )
        case .locked(let message):
            KieliValmisStatusBanner(
                message: message,
                tone: .warning
            )
        case .unknown:
            KieliValmisStatusBanner(
                message: "We cannot confirm your Professional Finnish access right now.",
                tone: .info,
                actionTitle: "Retry",
                action: retryAccess
            )
        }
    }

    @ViewBuilder
    private var content: some View {
        if isLoading && overview == nil {
            KieliValmisCardSurface(style: .standard) {
                ProgressView()
                    .tint(KieliValmisColor.brandBright)
                Text("Loading professional tracks…")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
            }
        } else if let errorMessage, overview == nil {
            KieliValmisStatusBanner(
                message: errorMessage,
                tone: .danger,
                actionTitle: "Retry",
                action: {
                    Task { await load() }
                }
            )
        } else if let overview {
            VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
                KieliValmisSectionHeader(title: "Work domains")

                ForEach(overview.tracks, id: \.domain) { track in
                    KieliValmisCardSurface(style: .standard) {
                        Text(track.title)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        if !track.coreTasks.isEmpty {
                            Text(
                                track.coreTasks
                                    .prefix(3)
                                    .map { $0.prefix(1).uppercased() + $0.dropFirst() }
                                    .joined(separator: " · ")
                            )
                            .font(.system(size: 14))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                        }

                        HStack(spacing: KieliValmisSpacing.s) {
                            Label(
                                "\(track.speakingScenarios.count) speaking",
                                systemImage: "waveform"
                            )
                            Label(
                                "\(track.writingTasks.count) writing",
                                systemImage: "square.and.pencil"
                            )
                        }
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(KieliValmisColor.textTertiary)
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

    @MainActor
    private func load() async {
        isLoading = true
        errorMessage = nil

        do {
            overview = try await service.fetchProfessionalOverview()
        } catch let error as FloentlyAPIError {
            errorMessage = error.message
        } catch {
            errorMessage = "Could not load professional Finnish tracks."
        }

        isLoading = false
    }
}
