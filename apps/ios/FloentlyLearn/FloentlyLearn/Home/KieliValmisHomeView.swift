import SwiftUI

private enum KieliValmisDestination: Hashable {
    case yki
    case professional
    case speaking
    case listening
    case reading
    case writing
    case review
    case profile
}

struct KieliValmisHomeView: View {
    @State private var path: [KieliValmisDestination] = []
    @State private var showPathPicker = false

    private let pathwayColumns = [
        GridItem(.adaptive(minimum: 156), spacing: KieliValmisSpacing.m)
    ]

    private let skillColumns = [
        GridItem(.adaptive(minimum: 144), spacing: KieliValmisSpacing.m)
    ]

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.xl) {
                    header
                    continueHero
                    pathways
                    skills
                    review
                }
                .padding(.horizontal, KieliValmisSpacing.ml)
                .padding(.top, KieliValmisSpacing.m)
                .padding(.bottom, KieliValmisSpacing.huge)
            }
            .background(KieliValmisColor.canvas.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        path.append(.profile)
                    } label: {
                        Image(systemName: "person.crop.circle")
                            .font(.system(size: 22, weight: .medium))
                            .foregroundStyle(KieliValmisColor.textPrimary)
                            .frame(width: 48, height: 48)
                    }
                    .accessibilityLabel("Profile")
                }
            }
            .navigationDestination(for: KieliValmisDestination.self) { destination in
                destinationView(destination)
            }
            .sheet(isPresented: $showPathPicker) {
                KieliValmisPathPicker { destination in
                    showPathPicker = false
                    path.append(destination)
                }
                .presentationDetents([.medium])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(KieliValmisRadius.xxl)
            }
        }
        .tint(KieliValmisColor.brandBright)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
            Text("KieliValmis")
                .font(.system(size: 34, weight: .heavy))
                .foregroundStyle(KieliValmisColor.textPrimary)

            Text("Finnish for life, work and YKI — built around what you should do next.")
                .font(.system(size: 16))
                .foregroundStyle(KieliValmisColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var continueHero: some View {
        KieliValmisCardSurface(style: .hero) {
            Text("CONTINUE")
                .font(.system(size: 11, weight: .semibold))
                .tracking(1)
                .foregroundStyle(KieliValmisColor.brandBright)

            Text("Start a learning session")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(KieliValmisColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            Text("Choose a pathway now. When your learning history is connected, this space will resume the most useful next activity.")
                .font(.system(size: 14))
                .foregroundStyle(KieliValmisColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            Spacer(minLength: KieliValmisSpacing.s)

            KieliValmisPrimaryButton(title: "Choose a path") {
                showPathPicker = true
            }
        }
    }

    private var pathways: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
            KieliValmisSectionHeader(title: "Your pathways")

            LazyVGrid(columns: pathwayColumns, spacing: KieliValmisSpacing.m) {
                KieliValmisPathwayCard(
                    symbol: "checkmark.seal",
                    title: "YKI preparation",
                    subtitle: "Reading, listening, writing and speaking practice."
                ) {
                    path.append(.yki)
                }

                KieliValmisPathwayCard(
                    symbol: "briefcase",
                    title: "Work in Finland",
                    subtitle: "Professional Finnish and workplace communication."
                ) {
                    path.append(.professional)
                }
            }
        }
    }

    private var skills: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
            KieliValmisSectionHeader(title: "Practice by skill")

            LazyVGrid(columns: skillColumns, spacing: KieliValmisSpacing.m) {
                KieliValmisSkillCard(
                    symbol: "waveform",
                    title: "Speaking",
                    subtitle: "Guided speaking and roleplay."
                ) {
                    path.append(.speaking)
                }

                KieliValmisSkillCard(
                    symbol: "headphones",
                    title: "Listening",
                    subtitle: "Understand Finnish in real situations."
                ) {
                    path.append(.listening)
                }

                KieliValmisSkillCard(
                    symbol: "text.book.closed",
                    title: "Reading",
                    subtitle: "Build comprehension with focused tasks."
                ) {
                    path.append(.reading)
                }

                KieliValmisSkillCard(
                    symbol: "square.and.pencil",
                    title: "Writing",
                    subtitle: "Practice useful and YKI-style writing."
                ) {
                    path.append(.writing)
                }
            }
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
            KieliValmisSectionHeader(title: "Review")
            KieliValmisReviewCard {
                path.append(.review)
            }
        }
    }

    @ViewBuilder
    private func destinationView(_ destination: KieliValmisDestination) -> some View {
        switch destination {
        case .yki:
            KieliValmisCapabilityView(
                title: "YKI preparation",
                subtitle: "Practice reading, listening, writing and speaking in one structured pathway.",
                symbol: "checkmark.seal"
            )
        case .professional:
            KieliValmisCapabilityView(
                title: "Work in Finland",
                subtitle: "Profession-specific language, workplace communication and real-world scenarios.",
                symbol: "briefcase"
            )
        case .speaking:
            KieliValmisCapabilityView(
                title: "Speaking",
                subtitle: "Guided speaking and roleplay sessions will appear here.",
                symbol: "waveform"
            )
        case .listening:
            KieliValmisCapabilityView(
                title: "Listening",
                subtitle: "Listening activities and comprehension practice will appear here.",
                symbol: "headphones"
            )
        case .reading:
            KieliValmisCapabilityView(
                title: "Reading",
                subtitle: "Focused Finnish reading activities will appear here.",
                symbol: "text.book.closed"
            )
        case .writing:
            KieliValmisCapabilityView(
                title: "Writing",
                subtitle: "Writing prompts, feedback and YKI writing practice will appear here.",
                symbol: "square.and.pencil"
            )
        case .review:
            KieliValmisCapabilityView(
                title: "Review",
                subtitle: "Your due review and mistake-focused practice will appear here once learning history is available.",
                symbol: "arrow.triangle.2.circlepath"
            )
        case .profile:
            KieliValmisCapabilityView(
                title: "Profile",
                subtitle: "Account, preferences and access settings will live here.",
                symbol: "person.crop.circle"
            )
        }
    }
}

private struct KieliValmisPathPicker: View {
    let onSelect: (KieliValmisDestination) -> Void

    var body: some View {
        ZStack {
            KieliValmisColor.surface1.ignoresSafeArea()

            VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
                Text("Choose a pathway")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text("Start with the area you want to work on now.")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)

                KieliValmisPathwayCard(
                    symbol: "checkmark.seal",
                    title: "YKI preparation",
                    subtitle: "Practice all four YKI skills."
                ) {
                    onSelect(.yki)
                }

                KieliValmisPathwayCard(
                    symbol: "briefcase",
                    title: "Work in Finland",
                    subtitle: "Professional and workplace Finnish."
                ) {
                    onSelect(.professional)
                }
            }
            .padding(KieliValmisSpacing.ml)
        }
    }
}

private struct KieliValmisCapabilityView: View {
    let title: String
    let subtitle: String
    let symbol: String

    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            VStack(alignment: .leading, spacing: KieliValmisSpacing.l) {
                KieliValmisIconTile(symbol: symbol)

                Text(title)
                    .font(.system(size: 34, weight: .heavy))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text(subtitle)
                    .font(.system(size: 16))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                Spacer()
            }
            .padding(.horizontal, KieliValmisSpacing.ml)
            .padding(.top, KieliValmisSpacing.xl)
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}
