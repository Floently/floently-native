import SwiftUI

@main
struct FloentlyLearnApp: App {
    @StateObject private var appModel = LearnAppModel()

    var body: some Scene {
        WindowGroup {
            Group {
                switch appModel.phase {
                case .bootstrapping:
                    KieliValmisBootstrapView()
                case .signedOut:
                    LearnAuthView(appModel: appModel)
                case .signedIn:
                    KieliValmisHomeView()
                        .environmentObject(appModel)
                }
            }
            .preferredColorScheme(.dark)
            .task {
                await appModel.bootstrap()
            }
        }
    }
}

private struct KieliValmisBootstrapView: View {
    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            VStack(spacing: KieliValmisSpacing.m) {
                ProgressView()
                    .tint(KieliValmisColor.brandBright)

                Text("KieliValmis")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(KieliValmisColor.textPrimary)

                Text("Checking your session…")
                    .font(.system(size: 14))
                    .foregroundStyle(KieliValmisColor.textSecondary)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("KieliValmis. Checking your session.")
    }
}
