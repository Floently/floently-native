import SwiftUI
import FloentlyShared

@main
struct FloentlyCreateApp: App {
    var body: some Scene {
        WindowGroup {
            CreateHomeView()
        }
    }
}

struct CreateHomeView: View {
    var body: some View {
        FloentlyScreen(product: .create) {
            VStack(alignment: .leading, spacing: 24) {
                Spacer()

                Text("Floently Create")
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)

                Text("Native creator foundation for future CapCut-level quality.")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.72))

                FloentlyCard(product: .create) {
                    Text("Create is planned natively")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.white)
                    Text("SwiftUI, UIKit where needed, AVFoundation, Metal, and later C++/Rust engine support.")
                        .foregroundStyle(.white.opacity(0.72))
                }

                FloentlyPrimaryButton("Join waitlist", product: .create) {}

                Spacer()
            }
        }
    }
}
