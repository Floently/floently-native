import SwiftUI

public enum FloentlyProduct {
    case learn
    case read
    case create
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
        background: Color(red: 0.02, green: 0.03, blue: 0.10),
        surface: Color(red: 0.06, green: 0.08, blue: 0.18),
        elevated: Color(red: 0.10, green: 0.09, blue: 0.25),
        text: Color.white,
        muted: Color(red: 0.72, green: 0.70, blue: 0.86),
        accent: Color(red: 0.70, green: 0.45, blue: 1.00),
        accent2: Color(red: 0.30, green: 0.82, blue: 1.00),
        border: Color.white.opacity(0.12)
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
    private let palette: FloentlyPalette
    private let content: Content

    public init(product: FloentlyProduct, @ViewBuilder content: () -> Content) {
        self.palette = FloentlyPalette.palette(for: product)
        self.content = content()
    }

    public var body: some View {
        ZStack {
            LinearGradient(
                colors: [palette.background, palette.surface],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            content
                .padding(.horizontal, 20)
        }
    }
}

public struct FloentlyCard<Content: View>: View {
    private let palette: FloentlyPalette
    private let content: Content

    public init(product: FloentlyProduct, @ViewBuilder content: () -> Content) {
        self.palette = FloentlyPalette.palette(for: product)
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            content
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(palette.elevated.opacity(0.88))
                .overlay(
                    RoundedRectangle(cornerRadius: 28, style: .continuous)
                        .stroke(palette.border, lineWidth: 1)
                )
        )
        .shadow(color: palette.accent.opacity(0.18), radius: 24, x: 0, y: 18)
    }
}

public struct FloentlyPrimaryButton: View {
    private let title: String
    private let product: FloentlyProduct
    private let action: () -> Void

    public init(_ title: String, product: FloentlyProduct, action: @escaping () -> Void) {
        self.title = title
        self.product = product
        self.action = action
    }

    public var body: some View {
        let palette = FloentlyPalette.palette(for: product)
        Button(action: action) {
            Text(title)
                .font(.headline.weight(.semibold))
                .foregroundStyle(Color.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    LinearGradient(colors: [palette.accent, palette.accent2], startPoint: .leading, endPoint: .trailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
