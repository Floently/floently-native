import SwiftUI
import FloentlyShared

@main
struct FloentlyReadApp: App {
    var body: some Scene {
        WindowGroup {
            ReadHomeView()
        }
    }
}

struct ReadHomeView: View {
    var body: some View {
        FloentlyScreen(product: .read) {
            VStack(alignment: .leading, spacing: 24) {
                Spacer()

                Text("Floently Read")
                    .font(.system(size: 38, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)

                Text("Import, read, and listen without dead spinners.")
                    .font(.title3)
                    .foregroundStyle(.white.opacity(0.72))

                FloentlyCard(product: .read) {
                    Text("Import to first listen")
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.white)
                    Text("Native file import, visible upload progress, first-pages-first text, and audio-first playback will be built here.")
                        .foregroundStyle(.white.opacity(0.72))
                }

                FloentlyPrimaryButton("Import document", product: .read) {}

                Spacer()
            }
        }
    }
}
