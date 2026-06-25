import SwiftUI
import FloentlyShared

struct LearnAuthView: View {
    enum Mode {
        case signIn
        case createAccount
    }

    @State private var mode: Mode = .signIn
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var isBusy = false
    @State private var errorMessage: String?

    let onContinue: () -> Void

    var body: some View {
        FloentlyScreen(product: .learn) {
            VStack(alignment: .leading, spacing: 18) {
                Spacer()

                Text(mode == .signIn ? "Welcome back" : "Create your account")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)

                Text("Continue to Floently Learn with your existing Floently account.")
                    .foregroundStyle(.white.opacity(0.72))

                FloentlyCard(product: .learn) {
                    Picker("", selection: ) {
                        Text("Sign in").tag(Mode.signIn)
                        Text("Create").tag(Mode.createAccount)
                    }
                    .pickerStyle(.segmented)

                    if mode == .createAccount {
                        TextField("Name optional", text: )
                            .textContentType(.name)
                            .textFieldStyle(.roundedBorder)
                    }

                    TextField("Email", text: )
                        .keyboardType(.emailAddress)
                        .textContentType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .textFieldStyle(.roundedBorder)

                    SecureField("Password", text: )
                        .textContentType(mode == .signIn ? .password : .newPassword)
                        .textFieldStyle(.roundedBorder)

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }

                    FloentlyPrimaryButton(isBusy ? "Please wait..." : (mode == .signIn ? "Sign in" : "Create account"), product: .learn) {
                        submit()
                    }
                    .disabled(isBusy || email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.count < 6)
                }

                Button("Forgot password?") {
                    errorMessage = "Password reset screen is included in the auth contract and will be connected in the next UI flow bundle."
                }
                .foregroundStyle(.white.opacity(0.78))

                Spacer()
            }
        }
    }

    private func submit() {
        isBusy = true
        errorMessage = nil

        Task {
            try? await Task.sleep(nanoseconds: 250_000_000)
            await MainActor.run {
                isBusy = false
                onContinue()
            }
        }
    }
}
