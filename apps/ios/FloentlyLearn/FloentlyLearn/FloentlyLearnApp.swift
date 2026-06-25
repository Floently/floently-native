import SwiftUI
import FloentlyShared

@main
struct FloentlyLearnApp: App {
    var body: some Scene {
        WindowGroup {
            LearnHomeView()
        }
    }
}

struct LearnHomeView: View {
    var body: some View {
        FloentlyScreen(product: .learn) {
            VStack(alignment: .leading, spacing: 24) {
                Spacer()

                Text("Floently Learn")
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)

                Text("YKI, professional Finnish, speaking practice, and progress — rebuilt natively without touching the old app.")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.72))

                FloentlyCard(product: .learn) {
                    Text("Native Learn foundation")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.white)
                    Text("This app will consume YKI and card-bank content from the existing backend API. The card bank remains server-owned.")
                        .foregroundStyle(.white.opacity(0.72))
                }

                FloentlyPrimaryButton("Continue", product: .learn) {}

                Spacer()
            }
        }
    }
}
