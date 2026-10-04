import CryptoKit
import Foundation

struct ReadPendingProgressWrite:
    Codable,
    Equatable,
    Sendable
{
    let nonce: String
    let projectId: String
    let currentSegmentIndex: Int
    let currentCharacterOffset: Int
    let progressPercent: Double
    let voiceId: String?
    let playbackRate: Double?
    let baseServerUpdatedAt: String?
    let createdAt: Date
}

actor ReadProgressOutboxStore {
    static let shared =
        ReadProgressOutboxStore()

    private struct Envelope: Codable {
        let schemaVersion: Int
        let write: ReadPendingProgressWrite
    }

    private let fileManager: FileManager
    private let root: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first ?? fileManager.temporaryDirectory

        root = base.appending(
            path: "FloentlyReadProgressOutbox",
            directoryHint: .isDirectory
        )
    }

    func save(
        accountIdentity: String,
        write: ReadPendingProgressWrite
    ) throws {
        let directory = accountDirectory(
            accountIdentity
        )
        try ensureDirectory(directory)

        let envelope = Envelope(
            schemaVersion: 1,
            write: write
        )
        let url = writeURL(
            accountIdentity:
                accountIdentity,
            projectId: write.projectId
        )

        try JSONEncoder()
            .encode(envelope)
            .write(
                to: url,
                options: .atomic
            )

        try? protectFile(url)
    }

    func entries(
        accountIdentity: String
    ) -> [ReadPendingProgressWrite] {
        let directory = accountDirectory(
            accountIdentity
        )
        guard
            let urls =
                try? fileManager
                    .contentsOfDirectory(
                        at: directory,
                        includingPropertiesForKeys:
                            nil,
                        options:
                            [.skipsHiddenFiles]
                    )
        else {
            return []
        }

        return urls.compactMap { url in
            guard
                url.pathExtension
                    .lowercased()
                    == "json",
                let data =
                    try? Data(
                        contentsOf: url
                    ),
                let envelope =
                    try? JSONDecoder()
                        .decode(
                            Envelope.self,
                            from: data
                        ),
                envelope.schemaVersion == 1
            else {
                return nil
            }

            return envelope.write
        }
        .sorted {
            $0.createdAt < $1.createdAt
        }
    }

    func currentWrite(
        accountIdentity: String,
        projectId: String
    ) -> ReadPendingProgressWrite? {
        let url = writeURL(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )

        guard
            let data = try? Data(
                contentsOf: url
            ),
            let envelope =
                try? JSONDecoder().decode(
                    Envelope.self,
                    from: data
                ),
            envelope.schemaVersion == 1
        else {
            return nil
        }

        return envelope.write
    }

    func removeIfMatches(
        accountIdentity: String,
        projectId: String,
        nonce: String
    ) {
        let url = writeURL(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
        guard
            let data = try? Data(
                contentsOf: url
            ),
            let envelope =
                try? JSONDecoder().decode(
                    Envelope.self,
                    from: data
                ),
            envelope.schemaVersion == 1,
            envelope.write.nonce == nonce
        else {
            return
        }

        try? fileManager.removeItem(
            at: url
        )
    }

    func removeProject(
        accountIdentity: String,
        projectId: String
    ) {
        try? fileManager.removeItem(
            at: writeURL(
                accountIdentity:
                    accountIdentity,
                projectId: projectId
            )
        )
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: root
        )
    }

    private func ensureDirectory(
        _ directory: URL
    ) throws {
        try fileManager.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )

        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableRoot = root
        try? mutableRoot.setResourceValues(
            values
        )
    }

    private func protectFile(
        _ url: URL
    ) throws {
        try fileManager.setAttributes(
            [
                .protectionKey:
                    FileProtectionType
                        .completeUntilFirstUserAuthentication
            ],
            ofItemAtPath: url.path
        )
    }

    private func accountDirectory(
        _ accountIdentity: String
    ) -> URL {
        root.appending(
            path: storageKey(
                accountIdentity
            ),
            directoryHint: .isDirectory
        )
    }

    private func writeURL(
        accountIdentity: String,
        projectId: String
    ) -> URL {
        accountDirectory(
            accountIdentity
        )
        .appending(
            path:
                storageKey(projectId)
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
