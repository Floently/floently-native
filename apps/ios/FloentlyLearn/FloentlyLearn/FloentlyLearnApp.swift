import SwiftUI

@main
struct FloentlyLearnApp: App {
    @State private var isAuthenticated = false

    var body: some Scene {
        WindowGroup {
            Group {
                if isAuthenticated {
                    KieliValmisHomeView()
                } else {
                    LearnAuthView {
                        isAuthenticated = true
                    }
                }
            }
            .preferredColorScheme(.dark)
        }
    }
}
