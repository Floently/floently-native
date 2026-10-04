import SwiftUI

public enum FloentlyProduct {
    case learn
    case read
    case create
}

/// Native adapter for the frozen Iloadi UI 1.0.0 token contract.
///
/// Source of truth:
/// docs/design/ILOADI_DESIGN_TOKENS.md
public enum FloentlyDesignTokens {
    public static let version = "1.0.0"

    public enum Colors {
        public static let canvas = Color(
            red: 7.0 / 255.0,
            green: 6.0 / 255.0,
            blue: 10.0 / 255.0
        )
        public static let canvasRaised = Color(
            red: 11.0 / 255.0,
            green: 8.0 / 255.0,
            blue: 16.0 / 255.0
        )
        public static let surface1 = Color(
            red: 15.0 / 255.0,
            green: 12.0 / 255.0,
            blue: 20.0 / 255.0
        )
        public static let surface2 = Color(
            red: 21.0 / 255.0,
            green: 16.0 / 255.0,
            blue: 29.0 / 255.0
        )
        public static let surface3 = Color(
            red: 27.0 / 255.0,
            green: 20.0 / 255.0,
            blue: 39.0 / 255.0
        )
        public static let brand = Color(
            red: 139.0 / 255.0,
            green: 92.0 / 255.0,
            blue: 255.0 / 255.0
        )
        public static let brandBright = Color(
            red: 185.0 / 255.0,
            green: 152.0 / 255.0,
            blue: 255.0 / 255.0
        )
        public static let brandDeep = Color(
            red: 90.0 / 255.0,
            green: 52.0 / 255.0,
            blue: 204.0 / 255.0
        )
        public static let brandTint = Color(
            red: 36.0 / 255.0,
            green: 24.0 / 255.0,
            blue: 58.0 / 255.0
        )

        public static let textPrimary = Color(
            red: 247.0 / 255.0,
            green: 243.0 / 255.0,
            blue: 255.0 / 255.0
        )
        public static let textSecondary = Color(
            red: 185.0 / 255.0,
            green: 177.0 / 255.0,
            blue: 196.0 / 255.0
        )
        public static let textTertiary = Color(
            red: 129.0 / 255.0,
            green: 120.0 / 255.0,
            blue: 141.0 / 255.0
        )
        public static let textDisabled = Color(
            red: 94.0 / 255.0,
            green: 86.0 / 255.0,
            blue: 105.0 / 255.0
        )
        public static let textOnBrand = Color.white

        public static let borderSoft = Color(
            red: 36.0 / 255.0,
            green: 30.0 / 255.0,
            blue: 44.0 / 255.0
        )
        public static let border = Color(
            red: 48.0 / 255.0,
            green: 38.0 / 255.0,
            blue: 58.0 / 255.0
        )
        public static let success = Color(
            red: 103.0 / 255.0,
            green: 216.0 / 255.0,
            blue: 168.0 / 255.0
        )
        public static let warning = Color(
            red: 243.0 / 255.0,
            green: 198.0 / 255.0,
            blue: 111.0 / 255.0
        )
        public static let danger = Color(
            red: 255.0 / 255.0,
            green: 113.0 / 255.0,
            blue: 141.0 / 255.0
        )
        public static let info = Color(
            red: 108.0 / 255.0,
            green: 185.0 / 255.0,
            blue: 255.0 / 255.0
        )
    }

    public enum Space {
        public static let s1: CGFloat = 4
        public static let s2: CGFloat = 8
        public static let s3: CGFloat = 12
        public static let s4: CGFloat = 16
        public static let s5: CGFloat = 20
        public static let s6: CGFloat = 24
        public static let s8: CGFloat = 32
        public static let s10: CGFloat = 40
        public static let s12: CGFloat = 48
        public static let s16: CGFloat = 64
    }

    public enum Radius {
        public static let s: CGFloat = 10
        public static let m: CGFloat = 14
        public static let l: CGFloat = 16
        public static let xl: CGFloat = 24
        public static let xxl: CGFloat = 32
        public static let pill: CGFloat = 999
    }

    public enum Control {
        public static let primaryHeight: CGFloat = 52
        public static let compactHeight: CGFloat = 44
        public static let searchHeight: CGFloat = 48
        public static let iconTarget: CGFloat = 48
        public static let floatingAction: CGFloat = 56
        public static let primaryPlay: CGFloat = 64
    }

    public enum TypeScale {
        public static let display: CGFloat = 42
        public static let h1: CGFloat = 34
        public static let h2: CGFloat = 28
        public static let h3: CGFloat = 22
        public static let title: CGFloat = 18
        public static let body: CGFloat = 16
        public static let small: CGFloat = 14
        public static let caption: CGFloat = 12
        public static let micro: CGFloat = 11
        public static let readerDefault: CGFloat = 20
        public static let readerLarge: CGFloat = 22
        public static let readerCompact: CGFloat = 18
    }

    public enum Motion {
        public static let instant = 0.100
        public static let fast = 0.160
        public static let standard = 0.240
        public static let enter = 0.320
        public static let exit = 0.220
        public static let content = 0.360
    }
}

public struct FloentlyPalette {
    public let background: Color
    public let surface: Color
    public let elevated: Color
    public let text: Color
    public let muted: Color
    public let accent: Color
    public let accent2: Color
    public let border: Color

    public static let learn = FloentlyPalette(
        background: Color(red: 0.03, green: 0.05, blue: 0.12),
        surface: Color(red: 0.07, green: 0.10, blue: 0.22),
        elevated: Color(red: 0.10, green: 0.14, blue: 0.30),
        text: Color.white,
        muted: Color(red: 0.70, green: 0.75, blue: 0.86),
        accent: Color(red: 0.35, green: 0.78, blue: 0.98),
        accent2: Color(red: 0.58, green: 0.45, blue: 1.00),
        border: Color.white.opacity(0.12)
    )

    public static let read = FloentlyPalette(
        background: FloentlyDesignTokens.Colors.canvas,
        surface: FloentlyDesignTokens.Colors.surface1,
        elevated: FloentlyDesignTokens.Colors.surface2,
        text: FloentlyDesignTokens.Colors.textPrimary,
        muted: FloentlyDesignTokens.Colors.textSecondary,
        accent: FloentlyDesignTokens.Colors.brand,
        accent2: FloentlyDesignTokens.Colors.brandBright,
        border: FloentlyDesignTokens.Colors.border
    )

    public static let create = FloentlyPalette(
        background: Color(red: 0.04, green: 0.03, blue: 0.09),
        surface: Color(red: 0.11, green: 0.07, blue: 0.18),
        elevated: Color(red: 0.18, green: 0.10, blue: 0.28),
        text: Color.white,
        muted: Color(red: 0.78, green: 0.70, blue: 0.86),
        accent: Color(red: 1.00, green: 0.42, blue: 0.72),
        accent2: Color(red: 0.68, green: 0.43, blue: 1.00),
        border: Color.white.opacity(0.12)
    )

    public static func palette(for product: FloentlyProduct) -> FloentlyPalette {
        switch product {
        case .learn: return .learn
        case .read: return .read
        case .create: return .create
        }
    }
}

public struct FloentlyScreen<Content: View>: View {
    private let product: FloentlyProduct
    private let palette: FloentlyPalette
    private let content: Content

    public init(product: FloentlyProduct, @ViewBuilder content: () -> Content) {
        self.product = product
        self.palette = FloentlyPalette.palette(for: product)
        self.content = content()
    }

    public var body: some View {
        ZStack {
            LinearGradient(
                colors: backgroundColors,
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            content
                .padding(
                    .horizontal,
                    horizontalPadding
                )
        }
    }

    private var backgroundColors: [Color] {
        switch product {
        case .read:
            return [
                FloentlyDesignTokens.Colors.canvas,
                FloentlyDesignTokens.Colors.canvasRaised
            ]
        case .learn, .create:
            return [
                palette.background,
                palette.surface
            ]
        }
    }

    private var horizontalPadding: CGFloat {
        switch product {
        case .read:
            return FloentlyDesignTokens.Space.s5
        case .learn, .create:
            return 20
        }
    }
}

public struct FloentlyCard<Content: View>: View {
    private let product: FloentlyProduct
    private let palette: FloentlyPalette
    private let content: Content

    public init(product: FloentlyProduct, @ViewBuilder content: () -> Content) {
        self.product = product
        self.palette = FloentlyPalette.palette(for: product)
        self.content = content()
    }

    public var body: some View {
        VStack(
            alignment: .leading,
            spacing: FloentlyDesignTokens.Space.s3
        ) {
            content
        }
        .padding(FloentlyDesignTokens.Space.s5)
        .background(cardBackground)
        .shadow(
            color: shadowColor,
            radius: shadowRadius,
            x: 0,
            y: shadowY
        )
    }

    @ViewBuilder
    private var cardBackground: some View {
        switch product {
        case .read:
            RoundedRectangle(
                cornerRadius: FloentlyDesignTokens.Radius.xl,
                style: .continuous
            )
            .fill(FloentlyDesignTokens.Colors.surface2)
            .overlay {
                RoundedRectangle(
                    cornerRadius: FloentlyDesignTokens.Radius.xl,
                    style: .continuous
                )
                .stroke(
                    FloentlyDesignTokens.Colors.borderSoft,
                    lineWidth: 1
                )
            }
        case .learn, .create:
            RoundedRectangle(
                cornerRadius: 28,
                style: .continuous
            )
            .fill(palette.elevated.opacity(0.88))
            .overlay {
                RoundedRectangle(
                    cornerRadius: 28,
                    style: .continuous
                )
                .stroke(
                    palette.border,
                    lineWidth: 1
                )
            }
        }
    }

    private var shadowColor: Color {
        switch product {
        case .read:
            return .black.opacity(0.20)
        case .learn, .create:
            return palette.accent.opacity(0.18)
        }
    }

    private var shadowRadius: CGFloat {
        switch product {
        case .read: return 16
        case .learn, .create: return 24
        }
    }

    private var shadowY: CGFloat {
        switch product {
        case .read: return 8
        case .learn, .create: return 18
        }
    }
}

public struct FloentlyPrimaryButton: View {
    private let title: String
    private let product: FloentlyProduct
    private let action: () -> Void

    public init(
        _ title: String,
        product: FloentlyProduct,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.product = product
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            switch product {
            case .read:
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(
                        FloentlyDesignTokens.Colors.textOnBrand
                    )
                    .frame(maxWidth: .infinity)
                    .frame(
                        height:
                            FloentlyDesignTokens
                                .Control
                                .primaryHeight
                    )
                    .background(
                        FloentlyDesignTokens.Colors.brand
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
            case .learn, .create:
                let palette = FloentlyPalette.palette(
                    for: product
                )
                Text(title)
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(
                            colors: [
                                palette.accent,
                                palette.accent2
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(
                        RoundedRectangle(
                            cornerRadius: 20,
                            style: .continuous
                        )
                    )
            }
        }
        .buttonStyle(.plain)
    }
}
