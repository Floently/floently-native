import CryptoKit
import Foundation

struct ReadOfflineAudioAsset: Equatable {
    let localURL: URL
    let duration: TimeInterval?
    let renditionId: String?
    let timingMapId: String?
}

struct ReadOfflineAudioSummary: Equatable {
    let documentCount: Int
    let bundleCount: Int
    let bytes: Int64

    static let empty = ReadOfflineAudioSummary(
        documentCount: 0,
        bundleCount: 0,
        bytes: 0
    )
}

actor ReadOfflineAudioStore {
    static let shared = ReadOfflineAudioStore()

    private static let currentSchemaVersion = 2
    private static let supportedSchemaVersions:
        Set<Int> = [1, 2]

    private struct SegmentMetadata: Codable {
        let id: String
        let index: Int
        let fileName: String
        let duration: TimeInterval?
        let sha256: String
        let renditionId: String?
        let timingMapId: String?
    }

    private struct BundleMetadata: Codable {
        let schemaVersion: Int
        let documentId: String
        let revisionId: String
        let voiceId: String
        let segments: [SegmentMetadata]
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
            path: "FloentlyReadOfflineAudioV1",
            directoryHint: .isDirectory
        )
    }

    func asset(
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String,
        segment: ReadingManifestSegmentV1
    ) throws -> ReadOfflineAudioAsset? {
        let directory = bundleDirectory(
            accountIdentity: accountIdentity,
            documentId: manifest.documentId,
            revisionId: manifest.revisionId,
            voiceId: voiceId
        )
        guard
            let metadata = try loadMetadata(
                from: directory
            ),
            metadata.documentId == manifest.documentId,
            metadata.revisionId == manifest.revisionId,
            metadata.voiceId == voiceId,
            let item = metadata.segments.first(where: {
                $0.index == segment.index
                    && $0.id == segment.id
            })
        else {
            return nil
        }

        if
            metadata.schemaVersion >= 2,
            (
                item.renditionId?.isEmpty != false
                || item.timingMapId?.isEmpty != false
            )
        {
            return nil
        }

        let file = directory.appending(
            path: item.fileName
        )
        guard fileManager.fileExists(
            atPath: file.path
        ) else {
            return nil
        }

        guard try checksum(file) == item.sha256 else {
            try? fileManager.removeItem(at: directory)
            return nil
        }

        return ReadOfflineAudioAsset(
            localURL: file,
            duration: item.duration,
            renditionId:
                item.renditionId,
            timingMapId:
                item.timingMapId
        )
    }

    func isComplete(
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String
    ) throws -> Bool {
        let directory = bundleDirectory(
            accountIdentity: accountIdentity,
            documentId: manifest.documentId,
            revisionId: manifest.revisionId,
            voiceId: voiceId
        )
        guard
            let metadata = try loadMetadata(
                from: directory
            ),
            metadata.documentId == manifest.documentId,
            metadata.revisionId == manifest.revisionId,
            metadata.voiceId == voiceId,
            metadata.segments.count == manifest.segments.count
        else {
            return false
        }

        let byIndex = Dictionary(
            uniqueKeysWithValues:
                metadata.segments.map {
                    ($0.index, $0)
                }
        )

        return manifest.segments.allSatisfy { segment in
            guard
                let item = byIndex[segment.index],
                item.id == segment.id
            else {
                return false
            }

            if
                metadata.schemaVersion >= 2,
                (
                    item.renditionId?.isEmpty != false
                    || item.timingMapId?.isEmpty != false
                )
            {
                return false
            }

            return fileManager.fileExists(
                atPath:
                    directory
                        .appending(path: item.fileName)
                        .path
            )
        }
    }

    func install(
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String,
        segments: [ReadPlayableSegment]
    ) throws {
        guard
            !manifest.segments.isEmpty,
            segments.count == manifest.segments.count,
            segments.allSatisfy({
                $0.renditionId?.isEmpty == false
                    && $0.timingMapId?.isEmpty == false
            })
        else {
            throw ReadOfflineAudioStoreError.incompleteBundle
        }

        let segmentByIndex = Dictionary(
            uniqueKeysWithValues:
                segments.map {
                    ($0.index, $0)
                }
        )

        guard manifest.segments.allSatisfy({
            segmentByIndex[$0.index]?.id == $0.id
        }) else {
            throw ReadOfflineAudioStoreError.incompleteBundle
        }

        let target = bundleDirectory(
            accountIdentity: accountIdentity,
            documentId: manifest.documentId,
            revisionId: manifest.revisionId,
            voiceId: voiceId
        )
        let parent = target.deletingLastPathComponent()
        try fileManager.createDirectory(
            at: parent,
            withIntermediateDirectories: true
        )

        let staging = parent.appending(
            path:
                target.lastPathComponent
                + ".staging-"
                + UUID().uuidString,
            directoryHint: .isDirectory
        )
        try? fileManager.removeItem(at: staging)
        try fileManager.createDirectory(
            at: staging,
            withIntermediateDirectories: true
        )

        var written: [SegmentMetadata] = []
        written.reserveCapacity(
            manifest.segments.count
        )

        do {
            for sourceSegment in manifest.segments {
                guard
                    let prepared =
                        segmentByIndex[sourceSegment.index],
                    prepared.url.isFileURL
                else {
                    throw ReadOfflineAudioStoreError.invalidAudioSource
                }

                let sourceExtension =
                    prepared.url.pathExtension
                        .trimmingCharacters(
                            in: .whitespacesAndNewlines
                        )
                let extensionName =
                    sourceExtension.isEmpty
                    ? "audio"
                    : sourceExtension
                let fileName = String(
                    format:
                        "segment-%06d.%@",
                    sourceSegment.index,
                    extensionName
                )
                let destination = staging.appending(
                    path: fileName
                )

                try fileManager.copyItem(
                    at: prepared.url,
                    to: destination
                )
                try protectFile(destination)

                written.append(
                    SegmentMetadata(
                        id: sourceSegment.id,
                        index: sourceSegment.index,
                        fileName: fileName,
                        duration:
                            prepared.physicalDuration,
                        sha256: try checksum(
                            destination
                        ),
                        renditionId:
                            prepared.renditionId,
                        timingMapId:
                            prepared.timingMapId
                    )
                )
            }

            let metadata = BundleMetadata(
                schemaVersion:
                    Self.currentSchemaVersion,
                documentId: manifest.documentId,
                revisionId: manifest.revisionId,
                voiceId: voiceId,
                segments: written
            )
            let metadataURL = staging.appending(
                path: "bundle.json"
            )
            try JSONEncoder()
                .encode(metadata)
                .write(
                    to: metadataURL,
                    options: .atomic
                )
            try protectFile(metadataURL)

            try ensureRootPolicy()

            if fileManager.fileExists(
                atPath: target.path
            ) {
                try fileManager.removeItem(
                    at: target
                )
            }

            try fileManager.moveItem(
                at: staging,
                to: target
            )
        } catch {
            try? fileManager.removeItem(
                at: staging
            )
            throw error
        }
    }

    func remove(
        accountIdentity: String,
        documentId: String,
        revisionId: String,
        voiceId: String
    ) {
        let target = bundleDirectory(
            accountIdentity: accountIdentity,
            documentId: documentId,
            revisionId: revisionId,
            voiceId: voiceId
        )
        try? fileManager.removeItem(
            at: target
        )
    }

    func removeDocument(
        accountIdentity: String,
        documentId: String
    ) {
        let directory = root
            .appending(
                path: storageKey(
                    accountIdentity
                ),
                directoryHint: .isDirectory
            )
            .appending(
                path: storageKey(documentId),
                directoryHint: .isDirectory
            )

        try? fileManager.removeItem(
            at: directory
        )
    }

    func summary(
        accountIdentity: String
    ) -> ReadOfflineAudioSummary {
        let account = accountDirectory(
            accountIdentity
        )
        guard fileManager.fileExists(
            atPath: account.path
        ) else {
            return .empty
        }

        let documentDirectories =
            (
                try? fileManager
                    .contentsOfDirectory(
                        at: account,
                        includingPropertiesForKeys:
                            [.isDirectoryKey],
                        options:
                            [.skipsHiddenFiles]
                    )
            )?
            .filter { url in
                (
                    try? url.resourceValues(
                        forKeys:
                            [.isDirectoryKey]
                    ).isDirectory
                ) == true
            }
            ?? []

        var documentCount = 0
        var bundleCount = 0
        var bytes: Int64 = 0

        for document in documentDirectories {
            guard
                let enumerator =
                    fileManager.enumerator(
                        at: document,
                        includingPropertiesForKeys: [
                            .isRegularFileKey,
                            .fileSizeKey
                        ],
                        options:
                            [.skipsHiddenFiles]
                    )
            else {
                continue
            }

            var documentHasBundle = false

            for case let url as URL in enumerator {
                guard
                    let values =
                        try? url.resourceValues(
                            forKeys: [
                                .isRegularFileKey,
                                .fileSizeKey
                            ]
                        ),
                    values.isRegularFile
                        == true
                else {
                    continue
                }

                bytes += Int64(
                    values.fileSize ?? 0
                )

                if
                    url.lastPathComponent
                        == "bundle.json"
                {
                    bundleCount += 1
                    documentHasBundle = true
                }
            }

            if documentHasBundle {
                documentCount += 1
            }
        }

        return ReadOfflineAudioSummary(
            documentCount:
                documentCount,
            bundleCount: bundleCount,
            bytes: bytes
        )
    }

    func clearAccount(
        accountIdentity: String
    ) {
        try? fileManager.removeItem(
            at: accountDirectory(
                accountIdentity
            )
        )
    }

    func clearAll() {
        try? fileManager.removeItem(
            at: root
        )
    }

    private func loadMetadata(
        from directory: URL
    ) throws -> BundleMetadata? {
        let url = directory.appending(
            path: "bundle.json"
        )
        guard fileManager.fileExists(
            atPath: url.path
        ) else {
            return nil
        }

        let metadata = try JSONDecoder().decode(
            BundleMetadata.self,
            from: Data(contentsOf: url)
        )

        return Self.supportedSchemaVersions
            .contains(
                metadata.schemaVersion
            )
            ? metadata
            : nil
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

    private func bundleDirectory(
        accountIdentity: String,
        documentId: String,
        revisionId: String,
        voiceId: String
    ) -> URL {
        accountDirectory(
            accountIdentity
        )
            .appending(
                path: storageKey(documentId),
                directoryHint: .isDirectory
            )
            .appending(
                path: storageKey(revisionId),
                directoryHint: .isDirectory
            )
            .appending(
                path: storageKey(voiceId),
                directoryHint: .isDirectory
            )
    }

    private func ensureRootPolicy() throws {
        try fileManager.createDirectory(
            at: root,
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

    private func checksum(
        _ url: URL
    ) throws -> String {
        let handle = try FileHandle(
            forReadingFrom: url
        )
        defer {
            try? handle.close()
        }

        var digest = SHA256()
        while true {
            let data =
                try handle.read(
                    upToCount: 256 * 1024
                )
                ?? Data()
            if data.isEmpty {
                break
            }
            digest.update(data: data)
        }

        return digest.finalize()
            .map {
                String(format: "%02x", $0)
            }
            .joined()
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

enum ReadOfflineAudioStoreError: LocalizedError {
    case incompleteBundle
    case invalidAudioSource
    case unavailable
    case signedOut

    var errorDescription: String? {
        switch self {
        case .incompleteBundle:
            return "Read could not finish the offline audio bundle."
        case .invalidAudioSource:
            return "Read received an invalid local audio source while saving offline."
        case .unavailable:
            return "Offline audio storage is unavailable on this device."
        case .signedOut:
            return "Sign in again before saving this document offline."
        }
    }
}
