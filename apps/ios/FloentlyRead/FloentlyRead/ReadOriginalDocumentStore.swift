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
        guard sourceURL.pathExtension.lowercased() == "pdf" else {
            return
        }

        try ensureDirectory()

        let target = pdfURL(projectId: projectId)
        let temporary = directoryURL.appending(
            path: "\(storageKey(projectId))-\(UUID().uuidString).tmp"
        )

        defer {
            try? fileManager.removeItem(at: temporary)
        }

        try fileManager.copyItem(
            at: sourceURL,
            to: temporary
        )

        if fileManager.fileExists(atPath: target.path) {
            try fileManager.removeItem(at: target)
        }

        try fileManager.moveItem(
            at: temporary,
            to: target
        )
    }

    func pdfURL(
        for projectId: String
    ) -> URL? {
        let value = pdfURL(projectId: projectId)

        return fileManager.fileExists(atPath: value.path)
            ? value
            : nil
    }

    func delete(
        projectId: String
    ) {
        let value = pdfURL(projectId: projectId)
        try? fileManager.removeItem(at: value)
    }

    func clearAll() {
        try? fileManager.removeItem(at: directoryURL)
    }

    private func ensureDirectory() throws {
        try fileManager.createDirectory(
            at: directoryURL,
            withIntermediateDirectories: true
        )
    }

    private func pdfURL(
        projectId: String
    ) -> URL {
        directoryURL.appending(
            path: "\(storageKey(projectId)).pdf"
        )
    }

    private func storageKey(
        _ projectId: String
    ) -> String {
        SHA256.hash(data: Data(projectId.utf8))
            .map {
                String(format: "%02x", $0)
            }
            .joined()
    }
}
