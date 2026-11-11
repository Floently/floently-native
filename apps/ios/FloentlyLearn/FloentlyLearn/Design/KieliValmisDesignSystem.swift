import SwiftUI

enum KieliValmisSpacing {
    static let xs: CGFloat = 4
    static let s: CGFloat = 8
    static let sm: CGFloat = 12
    static let m: CGFloat = 16
    static let ml: CGFloat = 20
    static let l: CGFloat = 24
    static let xl: CGFloat = 32
    static let xxl: CGFloat = 40
    static let xxxl: CGFloat = 48
    static let huge: CGFloat = 64
}

enum KieliValmisRadius {
    static let s: CGFloat = 10
    static let m: CGFloat = 14
    static let l: CGFloat = 16
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
}

enum KieliValmisColor {
    static let canvas = Color(red: 7.0 / 255.0, green: 6.0 / 255.0, blue: 10.0 / 255.0)
    static let canvasRaised = Color(red: 11.0 / 255.0, green: 8.0 / 255.0, blue: 16.0 / 255.0)
    static let surface1 = Color(red: 15.0 / 255.0, green: 12.0 / 255.0, blue: 20.0 / 255.0)
    static let surface2 = Color(red: 21.0 / 255.0, green: 16.0 / 255.0, blue: 29.0 / 255.0)
    static let surface3 = Color(red: 27.0 / 255.0, green: 20.0 / 255.0, blue: 39.0 / 255.0)
    static let brand = Color(red: 139.0 / 255.0, green: 92.0 / 255.0, blue: 1.0)
    static let brandBright = Color(red: 185.0 / 255.0, green: 152.0 / 255.0, blue: 1.0)
    static let brandDeep = Color(red: 90.0 / 255.0, green: 52.0 / 255.0, blue: 204.0 / 255.0)
    static let brandTint = Color(red: 36.0 / 255.0, green: 24.0 / 255.0, blue: 58.0 / 255.0)
    static let textPrimary = Color(red: 247.0 / 255.0, green: 243.0 / 255.0, blue: 1.0)
    static let textSecondary = Color(red: 185.0 / 255.0, green: 177.0 / 255.0, blue: 196.0 / 255.0)
    static let textTertiary = Color(red: 129.0 / 255.0, green: 120.0 / 255.0, blue: 141.0 / 255.0)
    static let borderSoft = Color(red: 36.0 / 255.0, green: 30.0 / 255.0, blue: 44.0 / 255.0)
    static let border = Color(red: 48.0 / 255.0, green: 38.0 / 255.0, blue: 58.0 / 255.0)
    static let success = Color(red: 103.0 / 255.0, green: 216.0 / 255.0, blue: 168.0 / 255.0)
    static let warning = Color(red: 243.0 / 255.0, green: 198.0 / 255.0, blue: 111.0 / 255.0)
    static let danger = Color(red: 1.0, green: 113.0 / 255.0, blue: 141.0 / 255.0)
}

enum KieliValmisCardStyle {
    case hero
    case standard
    case compact

    var radius: CGFloat {
        switch self {
        case .hero: return KieliValmisRadius.xxl
        case .standard, .compact: return KieliValmisRadius.xl
        }
    }

    var padding: CGFloat {
        switch self {
        case .hero: return KieliValmisSpacing.l
        case .standard: return KieliValmisSpacing.ml
        case .compact: return KieliValmisSpacing.m
        }
    }

    var minimumHeight: CGFloat {
        switch self {
        case .hero: return 184
        case .standard: return 132
        case .compact: return 84
        }
    }

    var fill: Color {
        switch self {
        case .hero: return KieliValmisColor.brandTint
        case .standard: return KieliValmisColor.surface2
        case .compact: return KieliValmisColor.surface1
        }
    }
}

struct KieliValmisCardSurface<Content: View>: View {
    let style: KieliValmisCardStyle
    private let content: Content

    init(style: KieliValmisCardStyle = .standard, @ViewBuilder content: () -> Content) {
        self.style = style
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.sm) {
            content
        }
        .frame(maxWidth: .infinity, minHeight: style.minimumHeight, alignment: .topLeading)
        .padding(style.padding)
        .background(
            RoundedRectangle(cornerRadius: style.radius, style: .continuous)
                .fill(style.fill)
                .overlay(
                    RoundedRectangle(cornerRadius: style.radius, style: .continuous)
                        .stroke(KieliValmisColor.borderSoft, lineWidth: 1)
                )
        )
    }
}

struct KieliValmisIconTile: View {
    let symbol: String

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: 20, weight: .semibold))
            .foregroundStyle(KieliValmisColor.brandBright)
            .frame(width: 44, height: 44)
            .background(
                RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                    .fill(KieliValmisColor.surface3)
            )
            .accessibilityHidden(true)
    }
}

struct KieliValmisPrimaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    RoundedRectangle(cornerRadius: KieliValmisRadius.l, style: .continuous)
                        .fill(KieliValmisColor.brand)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }
}

struct KieliValmisSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.system(size: 22, weight: .bold))
            .foregroundStyle(KieliValmisColor.textPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct KieliValmisPathwayCard: View {
    let symbol: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            KieliValmisCardSurface(style: .standard) {
                KieliValmisIconTile(symbol: symbol)

                Spacer(minLength: KieliValmisSpacing.s)

                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textPrimary)
                    .multilineTextAlignment(.leading)

                Text(subtitle)
                    .font(.system(size: 14, weight: .regular))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(3)
            }
            .frame(minHeight: 160)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct KieliValmisSkillCard: View {
    let symbol: String
    let title: String
    let subtitle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            KieliValmisCardSurface(style: .standard) {
                HStack(alignment: .top, spacing: KieliValmisSpacing.sm) {
                    KieliValmisIconTile(symbol: symbol)
                    Spacer(minLength: 0)
                }

                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text(subtitle)
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .lineLimit(2)
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

struct KieliValmisReviewCard: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            KieliValmisCardSurface(style: .compact) {
                HStack(spacing: KieliValmisSpacing.sm) {
                    KieliValmisIconTile(symbol: "arrow.triangle.2.circlepath")

                    VStack(alignment: .leading, spacing: KieliValmisSpacing.xs) {
                        Text("Review")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        Text("Your due reviews will appear here after your learning history is connected.")
                            .font(.system(size: 14))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Spacer(minLength: KieliValmisSpacing.s)

                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(KieliValmisColor.textTertiary)
                        .frame(width: 48, height: 48)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}


struct KieliValmisFieldShell<Content: View>: View {
    let label: String
    let isFocused: Bool
    private let content: Content

    init(
        label: String,
        isFocused: Bool,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        self.isFocused = isFocused
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(KieliValmisColor.textPrimary)

            HStack(spacing: KieliValmisSpacing.s) {
                content
            }
            .padding(.horizontal, KieliValmisSpacing.m)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                    .fill(isFocused ? KieliValmisColor.brandTint.opacity(0.66) : KieliValmisColor.surface1)
                    .overlay(
                        RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                            .stroke(
                                isFocused ? KieliValmisColor.brand : KieliValmisColor.border,
                                lineWidth: 1
                            )
                    )
            )
        }
    }
}

struct KieliValmisSegmentedControl<Option: Hashable>: View {
    let options: [(Option, String)]
    @Binding var selection: Option

    var body: some View {
        HStack(spacing: KieliValmisSpacing.xs) {
            ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                Button {
                    withAnimation(.easeInOut(duration: 0.18)) {
                        selection = option.0
                    }
                } label: {
                    Text(option.1)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(
                            selection == option.0
                                ? KieliValmisColor.textPrimary
                                : KieliValmisColor.textSecondary
                        )
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(
                                    selection == option.0
                                        ? KieliValmisColor.surface3
                                        : Color.clear
                                )
                        )
                }
                .buttonStyle(.plain)
                .frame(minHeight: 48)
            }
        }
        .padding(KieliValmisSpacing.xs)
        .background(
            RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                .fill(KieliValmisColor.surface1)
                .overlay(
                    RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                        .stroke(KieliValmisColor.borderSoft, lineWidth: 1)
                )
        )
    }
}


enum KieliValmisStatusTone {
    case info
    case warning
    case danger
    case success

    var accent: Color {
        switch self {
        case .info: return KieliValmisColor.brandBright
        case .warning: return KieliValmisColor.warning
        case .danger: return KieliValmisColor.danger
        case .success: return KieliValmisColor.success
        }
    }

    var symbol: String {
        switch self {
        case .info: return "info.circle.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .danger: return "exclamationmark.circle.fill"
        case .success: return "checkmark.circle.fill"
        }
    }
}

struct KieliValmisStatusBanner: View {
    let message: String
    let tone: KieliValmisStatusTone
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .center, spacing: KieliValmisSpacing.sm) {
            Image(systemName: tone.symbol)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(tone.accent)
                .frame(width: 24, height: 24)
                .accessibilityHidden(true)

            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(KieliValmisColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            Spacer(minLength: KieliValmisSpacing.s)

            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textPrimary)
                    .frame(minHeight: 48)
            }
        }
        .padding(.horizontal, KieliValmisSpacing.sm)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                .fill(KieliValmisColor.surface1)
                .overlay(
                    RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                        .stroke(KieliValmisColor.borderSoft, lineWidth: 1)
                )
        )
        .accessibilityElement(children: .combine)
    }
}

struct KieliValmisSecondaryButton: View {
    let title: String
    var destructive: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(
                    destructive ? KieliValmisColor.danger : KieliValmisColor.textPrimary
                )
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    RoundedRectangle(cornerRadius: KieliValmisRadius.l, style: .continuous)
                        .fill(KieliValmisColor.surface1)
                        .overlay(
                            RoundedRectangle(cornerRadius: KieliValmisRadius.l, style: .continuous)
                                .stroke(
                                    destructive ? KieliValmisColor.danger.opacity(0.55) : KieliValmisColor.border,
                                    lineWidth: 1
                                )
                        )
                )
        }
        .buttonStyle(.plain)
    }
}
