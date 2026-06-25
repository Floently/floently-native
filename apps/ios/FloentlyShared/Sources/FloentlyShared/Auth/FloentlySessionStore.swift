import Foundation
import Security

public final class FloentlySessionStore: ObservableObject {
    @Published public private(set) var session: FloentlyAuthSession?

    private let service = "com.vitusidi.floently.native.auth"
    private let account = "floently_session"

    public init() {
        self.session = load()
    }

    public func save(_ session: FloentlyAuthSession) {
        self.session = session

        guard let data = try? JSONEncoder().encode(session) else {
            return
        }

        deleteKeychainItem()

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data
        ]

        SecItemAdd(query as CFDictionary, nil)
    }

    public func clear() {
        session = nil
        deleteKeychainItem()
    }

    private func load() -> FloentlyAuthSession? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        guard status == errSecSuccess, let data = item as? Data else {
            return nil
        }

        return try? JSONDecoder().decode(FloentlyAuthSession.self, from: data)
    }

    private func deleteKeychainItem() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        SecItemDelete(query as CFDictionary)
    }
}
