import CryptoKit
import Foundation

actor ReadProjectSnapshotStore {
    static let shared = ReadProjectSnapshotStore()

    private struct Snapshot: Codable {
        let schemaVersion: Int
        let projects: [ReadContentProject]
    }

    private let fileManager: FileManager
    private let directoryURL: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first ?? fileManager.temporaryDirectory

        self.directoryURL = base.appending(
            path: "FloentlyReadProjectSnapshots",
            directoryHint: .isDirectory
        )
    }

    func load(
        accessToken: String
    ) -> [ReadContentProject] {
        let url = snapshotURL(
            accessToken: accessToken
        )

        guard
            let data = try? Data(contentsOf: url),
            let snapshot = try? JSONDecoder().decode(
                Snapshot.self,
                from: data
            ),
            snapshot.schemaVersion == 1
        else {
            return []
        }

        return snapshot.projects
    }

    func save(
        projects: [ReadContentProject],
        accessToken: String
    ) throws {
        try ensureDirectory()

        let snapshot = Snapshot(
            schemaVersion: 1,
            projects: projects
        )
        let data = try JSONEncoder().encode(snapshot)
        let target = snapshotURL(
            accessToken: accessToken
        )

        try data.write(
            to: target,
            options: .atomic
        )

        try? fileManager.setAttributes(
            [
                .protectionKey:
                    FileProtectionType
                        .completeUntilFirstUserAuthentication
            ],
            ofItemAtPath: target.path
        )
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: directoryURL
        )
    }

    private func ensureDirectory() throws {
        try fileManager.createDirectory(
            at: directoryURL,
            withIntermediateDirectories: true
        )

        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var directory = directoryURL
        try? directory.setResourceValues(values)
    }

    private func snapshotURL(
        accessToken: String
    ) -> URL {
        directoryURL.appending(
            path:
                storageKey(accessToken) +
                ".json"
        )
    }

    private func storageKey(
        _ accessToken: String
    ) -> String {
        SHA256.hash(
            data: Data(accessToken.utf8)
        )
        .map {
            String(format: "%02x", $0)
        }
        .joined()
    }
}
