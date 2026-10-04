import CryptoKit
import FloentlyShared
import Foundation

struct ReadAccessLeaseRecord {
    let status: FloentlyAccessStatus
    let verifiedAt: Date
}

final class ReadAccessLeaseStore {
    static let shared = ReadAccessLeaseStore()

    private struct Lease: Codable {
        let schemaVersion: Int
        let verifiedAt: Date
        let status: FloentlyAccessStatus
    }

    private let fileManager: FileManager
    private let directoryURL: URL
    private let maximumAge: TimeInterval =
        7 * 24 * 60 * 60

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first ?? fileManager.temporaryDirectory

        directoryURL = base.appending(
            path: "FloentlyReadAccessLeases",
            directoryHint: .isDirectory
        )
    }

    func save(
        status: FloentlyAccessStatus,
        accountIdentity: String,
        verifiedAt: Date = Date()
    ) throws {
        guard isGranted(status) else {
            remove(
                accountIdentity: accountIdentity
            )
            return
        }

        try ensureDirectory()

        let lease = Lease(
            schemaVersion: 1,
            verifiedAt: verifiedAt,
            status: status
        )
        let url = leaseURL(
            accountIdentity: accountIdentity
        )

        try JSONEncoder()
            .encode(lease)
            .write(
                to: url,
                options: .atomic
            )

        try? fileManager.setAttributes(
            [
                .protectionKey:
                    FileProtectionType
                        .completeUntilFirstUserAuthentication
            ],
            ofItemAtPath: url.path
        )
    }

    func validGrantedLease(
        accountIdentity: String,
        now: Date = Date()
    ) -> ReadAccessLeaseRecord? {
        let url = leaseURL(
            accountIdentity: accountIdentity
        )
        guard
            let data = try? Data(
                contentsOf: url
            ),
            let lease = try? JSONDecoder()
                .decode(
                    Lease.self,
                    from: data
                ),
            lease.schemaVersion == 1,
            isGranted(lease.status)
        else {
            return nil
        }

        let age = now.timeIntervalSince(
            lease.verifiedAt
        )
        guard
            age >= -5 * 60,
            age <= maximumAge
        else {
            try? fileManager.removeItem(
                at: url
            )
            return nil
        }

        return ReadAccessLeaseRecord(
            status: lease.status,
            verifiedAt: lease.verifiedAt
        )
    }

    func remove(
        accountIdentity: String
    ) {
        try? fileManager.removeItem(
            at: leaseURL(
                accountIdentity: accountIdentity
            )
        )
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: directoryURL
        )
    }

    private func isGranted(
        _ status: FloentlyAccessStatus
    ) -> Bool {
        status.readAccess
            || status.isInternalAllAccess
    }

    private func ensureDirectory() throws {
        try fileManager.createDirectory(
            at: directoryURL,
            withIntermediateDirectories: true
        )

        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var directory = directoryURL
        try? directory.setResourceValues(
            values
        )
    }

    private func leaseURL(
        accountIdentity: String
    ) -> URL {
        directoryURL.appending(
            path:
                storageKey(accountIdentity)
                + ".json"
        )
    }

    private func storageKey(
        _ value: String
    ) -> String {
        SHA256.hash(
            data: Data(value.utf8)
        )
        .map {
            String(format: "%02x", $0)
        }
        .joined()
    }
}
