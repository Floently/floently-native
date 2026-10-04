import CryptoKit
import Foundation

actor ReadOriginalDocumentStore {
    static let shared = ReadOriginalDocumentStore()

    private let fileManager: FileManager
    private let directoryURL: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first ?? fileManager.temporaryDirectory

        self.directoryURL = base.appending(
            path: "FloentlyReadOriginalDocuments",
            directoryHint: .isDirectory
        )
    }

    func savePDF(
        projectId: String,
        sourceURL: URL
    ) throws {
        guard
            sourceURL.pathExtension
                .lowercased() == "pdf"
        else {
            return
        }

        try saveOriginal(
            projectId: projectId,
            sourceURL: sourceURL,
            fileExtension: "pdf"
        )
    }

    func saveEPUB(
        projectId: String,
        sourceURL: URL
    ) throws {
        guard
            sourceURL.pathExtension
                .lowercased() == "epub"
        else {
            return
        }

        try saveOriginal(
            projectId: projectId,
            sourceURL: sourceURL,
            fileExtension: "epub"
        )
    }

    func pdfURL(
        for projectId: String
    ) -> URL? {
        existingOriginalURL(
            projectId: projectId,
            fileExtension: "pdf"
        )
    }

    func epubURL(
        for projectId: String
    ) -> URL? {
        existingOriginalURL(
            projectId: projectId,
            fileExtension: "epub"
        )
    }

    func delete(
        projectId: String
    ) {
        for fileExtension in [
            "pdf",
            "epub"
        ] {
            try? fileManager.removeItem(
                at: originalURL(
                    projectId: projectId,
                    fileExtension:
                        fileExtension
                )
            )
        }
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: directoryURL
        )
    }

    private func saveOriginal(
        projectId: String,
        sourceURL: URL,
        fileExtension: String
    ) throws {
        try ensureDirectory()

        let target = originalURL(
            projectId: projectId,
            fileExtension: fileExtension
        )
        let temporary =
            directoryURL.appending(
                path:
                    storageKey(projectId)
                    + "-"
                    + UUID().uuidString
                    + ".tmp"
            )

        defer {
            try? fileManager.removeItem(
                at: temporary
            )
        }

        try fileManager.copyItem(
            at: sourceURL,
            to: temporary
        )

        if fileManager.fileExists(
            atPath: target.path
        ) {
            try fileManager.removeItem(
                at: target
            )
        }

        try fileManager.moveItem(
            at: temporary,
            to: target
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

    private func existingOriginalURL(
        projectId: String,
        fileExtension: String
    ) -> URL? {
        let value = originalURL(
            projectId: projectId,
            fileExtension: fileExtension
        )

        guard
            fileManager.fileExists(
                atPath: value.path
            ),
            (
                try? value
                    .resourceValues(
                        forKeys:
                            [.fileSizeKey]
                    )
                    .fileSize
            ) ?? 0 > 0
        else {
            return nil
        }

        return value
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

    private func originalURL(
        projectId: String,
        fileExtension: String
    ) -> URL {
        directoryURL.appending(
            path:
                storageKey(projectId)
                + "."
                + fileExtension
        )
    }

    private func storageKey(
        _ projectId: String
    ) -> String {
        SHA256.hash(
            data: Data(projectId.utf8)
        )
        .map {
            String(format: "%02x", $0)
        }
        .joined()
    }
}
