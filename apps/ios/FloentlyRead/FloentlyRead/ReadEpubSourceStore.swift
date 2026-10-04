import CryptoKit
import Foundation

actor ReadEpubSourceStore {
    static let shared = ReadEpubSourceStore()

    private let fileManager: FileManager
    private let root: URL

    init(
        fileManager: FileManager = .default
    ) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for: .applicationSupportDirectory,
            in: .userDomainMask
        ).first ?? fileManager.temporaryDirectory

        root = base.appending(
            path: "FloentlyReadEpubSourcesV1",
            directoryHint: .isDirectory
        )
    }

    func package(
        projectId: String,
        revisionId: String,
        sourceURL: URL
    ) throws -> ReadLocalEpubPackage {
        let directory = packageDirectory(
            projectId: projectId,
            revisionId: revisionId
        )

        if
            let cached = try? loadCached(
                from: directory
            )
        {
            return cached
        }

        try ensureRoot()
        try? fileManager.removeItem(
            at: directory
        )

        let metadata =
            try ReadCoreNative.extractEPUB(
                inputURL: sourceURL,
                outputDirectory: directory
            )

        guard validate(
            metadata,
            in: directory
        ) else {
            try? fileManager.removeItem(
                at: directory
            )
            throw ReadCoreNativeError
                .epubExtractionFailed
        }

        let marker =
            directory.appending(
                path: "floently-package.json"
            )
        try JSONEncoder()
            .encode(metadata)
            .write(
                to: marker,
                options: .atomic
            )
        try? protectRecursively(
            directory
        )

        return ReadLocalEpubPackage(
            metadata: metadata,
            rootDirectory: directory
        )
    }

    func delete(
        projectId: String
    ) {
        try? fileManager.removeItem(
            at: projectDirectory(
                projectId
            )
        )
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: root
        )
    }

    private func loadCached(
        from directory: URL
    ) throws -> ReadLocalEpubPackage {
        let marker =
            directory.appending(
                path: "floently-package.json"
            )
        let metadata =
            try JSONDecoder().decode(
                ReadEpubPackage.self,
                from: Data(
                    contentsOf: marker
                )
            )

        guard
            metadata.schemaVersion == 1,
            validate(
                metadata,
                in: directory
            )
        else {
            throw ReadCoreNativeError
                .epubExtractionFailed
        }

        return ReadLocalEpubPackage(
            metadata: metadata,
            rootDirectory: directory
        )
    }

    private func validate(
        _ package: ReadEpubPackage,
        in directory: URL
    ) -> Bool {
        guard !package.spine.isEmpty else {
            return false
        }

        return package.spine.allSatisfy {
            item in
            guard
                !item.path.hasPrefix("/"),
                !item.path
                    .split(separator: "/")
                    .contains("..")
            else {
                return false
            }

            return fileManager.fileExists(
                atPath:
                    directory
                        .appending(
                            path: item.path
                        )
                        .path
            )
        }
    }

    private func ensureRoot() throws {
        try fileManager.createDirectory(
            at: root,
            withIntermediateDirectories: true
        )

        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableRoot = root
        try? mutableRoot
            .setResourceValues(values)
    }

    private func protectRecursively(
        _ directory: URL
    ) throws {
        guard
            let enumerator =
                fileManager.enumerator(
                    at: directory,
                    includingPropertiesForKeys:
                        nil
                )
        else {
            return
        }

        try? fileManager.setAttributes(
            [
                .protectionKey:
                    FileProtectionType
                        .completeUntilFirstUserAuthentication
            ],
            ofItemAtPath:
                directory.path
        )

        for case let url as URL in enumerator {
            try? fileManager.setAttributes(
                [
                    .protectionKey:
                        FileProtectionType
                            .completeUntilFirstUserAuthentication
                ],
                ofItemAtPath: url.path
            )
        }
    }

    private func projectDirectory(
        _ projectId: String
    ) -> URL {
        root.appending(
            path: storageKey(projectId),
            directoryHint: .isDirectory
        )
    }

    private func packageDirectory(
        projectId: String,
        revisionId: String
    ) -> URL {
        projectDirectory(projectId)
            .appending(
                path: storageKey(revisionId),
                directoryHint: .isDirectory
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
