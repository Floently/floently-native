import SwiftUI
import FloentlyShared

struct LearnAuthView: View {
    enum Mode: Hashable {
        case signIn
        case createAccount
    }

    private enum Field: Hashable {
        case name
        case email
        case password
    }

    @ObservedObject var appModel: LearnAppModel

    @State private var mode: Mode = .signIn
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var showPasswordReset = false
    @FocusState private var focusedField: Field?

    private var normalizedEmail: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canSubmit: Bool {
        !appModel.isAuthenticating
            && normalizedEmail.contains("@")
            && password.count >= 6
    }

    var body: some View {
        ZStack {
            KieliValmisColor.canvas.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: KieliValmisSpacing.xl) {
                    brandHeader

                    VStack(alignment: .leading, spacing: KieliValmisSpacing.s) {
                        Text(mode == .signIn ? "Welcome back" : "Create your account")
                            .font(.system(size: 34, weight: .heavy))
                            .foregroundStyle(KieliValmisColor.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)

                        Text("Use your Floently account for KieliValmis learning, YKI and professional Finnish.")
                            .font(.system(size: 16))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    KieliValmisSegmentedControl(
                        options: [
                            (.signIn, "Sign in"),
                            (.createAccount, "Create account")
                        ],
                        selection: $mode
                    )
                    .onChange(of: mode) {
                        appModel.authError = nil
                        if mode == .createAccount {
                            focusedField = name.isEmpty ? .name : .email
                        }
                    }

                    form

                    if let authError = appModel.authError {
                        HStack(alignment: .top, spacing: KieliValmisSpacing.s) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundStyle(KieliValmisColor.danger)
                                .accessibilityHidden(true)

                            Text(authError)
                                .font(.system(size: 14))
                                .foregroundStyle(KieliValmisColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel("Sign in error. \(authError)")
                    }

                    KieliValmisPrimaryButton(
                        title: appModel.isAuthenticating
                            ? "Please wait…"
                            : (mode == .signIn ? "Sign in" : "Create account")
                    ) {
                        submit()
                    }
                    .disabled(!canSubmit)
                    .opacity(canSubmit ? 1 : 0.55)

                    if mode == .signIn {
                        Button("Forgot password?") {
                            showPasswordReset = true
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(KieliValmisColor.textSecondary)
                        .frame(minHeight: 48)
                        .accessibilityHint("Opens password reset")
                    }

                    Spacer(minLength: KieliValmisSpacing.xl)
                }
                .frame(maxWidth: 560, alignment: .leading)
                .padding(.horizontal, KieliValmisSpacing.ml)
                .padding(.top, KieliValmisSpacing.xl)
                .padding(.bottom, KieliValmisSpacing.xl)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .sheet(isPresented: $showPasswordReset) {
            PasswordResetSheet(
                initialEmail: normalizedEmail,
                requestReset: { address in
                    try await appModel.requestPasswordReset(email: address)
                }
            )
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(KieliValmisRadius.xxl)
        }
    }

    private var brandHeader: some View {
        HStack(spacing: KieliValmisSpacing.sm) {
            Image(systemName: "text.bubble.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(KieliValmisColor.brandBright)
                .frame(width: 44, height: 44)
                .background(
                    RoundedRectangle(cornerRadius: KieliValmisRadius.m, style: .continuous)
                        .fill(KieliValmisColor.brandTint)
                )
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text("KieliValmis")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(KieliValmisColor.textPrimary)
                Text("Finnish that moves with you")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(KieliValmisColor.textTertiary)
            }
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: KieliValmisSpacing.m) {
            if mode == .createAccount {
                KieliValmisFieldShell(
                    label: "Name (optional)",
                    isFocused: focusedField == .name
                ) {
                    TextField("Your name", text: $name)
                        .textContentType(.name)
                        .foregroundStyle(KieliValmisColor.textPrimary)
                        .focused($focusedField, equals: .name)
                        .submitLabel(.next)
                        .onSubmit {
                            focusedField = .email
                        }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            KieliValmisFieldShell(
                label: "Email",
                isFocused: focusedField == .email
            ) {
                TextField("name@example.com", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .foregroundStyle(KieliValmisColor.textPrimary)
                    .focused($focusedField, equals: .email)
                    .submitLabel(.next)
                    .onSubmit {
                        focusedField = .password
                    }
            }

            KieliValmisFieldShell(
                label: "Password",
                isFocused: focusedField == .password
            ) {
                Group {
                    if passwordVisible {
                        TextField("Password", text: $password)
                            .textContentType(mode == .signIn ? .password : .newPassword)
                    } else {
                        SecureField("Password", text: $password)
                            .textContentType(mode == .signIn ? .password : .newPassword)
                    }
                }
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .foregroundStyle(KieliValmisColor.textPrimary)
                .focused($focusedField, equals: .password)
                .submitLabel(.go)
                .onSubmit {
                    if canSubmit {
                        submit()
                    }
                }

                Button {
                    passwordVisible.toggle()
                } label: {
                    Image(systemName: passwordVisible ? "eye.slash" : "eye")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(KieliValmisColor.textSecondary)
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(passwordVisible ? "Hide password" : "Show password")
            }
        }
        .animation(.easeInOut(duration: 0.2), value: mode)
    }

    private func submit() {
        guard canSubmit else { return }
        focusedField = nil

        Task {
            switch mode {
            case .signIn:
                await appModel.signIn(
                    email: normalizedEmail,
                    password: password
                )
            case .createAccount:
                await appModel.register(
                    email: normalizedEmail,
                    password: password,
                    name: name.isEmpty ? nil : name
                )
            }
        }
    }
}

private struct PasswordResetSheet: View {
    let initialEmail: String
    let requestReset: (String) async throws -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var email: String
    @State private var isBusy = false
    @State private var errorMessage: String?
    @State private var didRequest = false
    @FocusState private var emailFocused: Bool

    init(
        initialEmail: String,
        requestReset: @escaping (String) async throws -> Void
    ) {
        self.initialEmail = initialEmail
        self.requestReset = requestReset
        _email = State(initialValue: initialEmail)
    }

    var body: some View {
        ZStack {
            KieliValmisColor.surface1.ignoresSafeArea()

            VStack(alignment: .leading, spacing: KieliValmisSpacing.l) {
                HStack {
                    Text("Reset password")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(KieliValmisColor.textPrimary)

                    Spacer()

                    Button("Close") {
                        dismiss()
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(KieliValmisColor.textSecondary)
                    .frame(minHeight: 48)
                }

                if didRequest {
                    VStack(alignment: .leading, spacing: KieliValmisSpacing.sm) {
                        Image(systemName: "envelope.badge")
                            .font(.system(size: 28, weight: .semibold))
                            .foregroundStyle(KieliValmisColor.success)

                        Text("Reset requested")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(KieliValmisColor.textPrimary)

                        Text("If this address is eligible for password reset, follow the instructions sent by the account service.")
                            .font(.system(size: 14))
                            .foregroundStyle(KieliValmisColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                } else {
                    Text("Enter the email address for your Floently account.")
                        .font(.system(size: 14))
                        .foregroundStyle(KieliValmisColor.textSecondary)

                    KieliValmisFieldShell(
                        label: "Email",
                        isFocused: emailFocused
                    ) {
                        TextField("name@example.com", text: $email)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .foregroundStyle(KieliValmisColor.textPrimary)
                            .focused($emailFocused)
                            .submitLabel(.send)
                            .onSubmit {
                                send()
                            }
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 14))
                            .foregroundStyle(KieliValmisColor.danger)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    KieliValmisPrimaryButton(
                        title: isBusy ? "Please wait…" : "Send reset link"
                    ) {
                        send()
                    }
                    .disabled(isBusy || !email.contains("@"))
                    .opacity(isBusy || !email.contains("@") ? 0.55 : 1)
                }

                Spacer()
            }
            .padding(KieliValmisSpacing.ml)
        }
    }

    private func send() {
        guard !isBusy, email.contains("@") else { return }
        isBusy = true
        errorMessage = nil
        emailFocused = false

        Task {
            do {
                try await requestReset(
                    email.trimmingCharacters(in: .whitespacesAndNewlines)
                )
                didRequest = true
            } catch let error as FloentlyAPIError {
                errorMessage = error.message
            } catch {
                errorMessage = "Could not request a password reset. Try again."
            }
            isBusy = false
        }
    }
}
