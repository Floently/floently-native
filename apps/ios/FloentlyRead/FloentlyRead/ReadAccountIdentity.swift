import Foundation

func readAccountIdentity(
    userId: String,
    email: String
) -> String? {
    let id = userId.trimmingCharacters(
        in: .whitespacesAndNewlines
    )
    if !id.isEmpty {
        return "id:" + id
    }

    let normalizedEmail = email
        .trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        .lowercased()

    return normalizedEmail.isEmpty
        ? nil
        : "email:" + normalizedEmail
}
