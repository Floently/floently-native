import CryptoKit
import Foundation

struct ReadSourceHighlight:
    Codable,
    Equatable,
    Identifiable,
    Sendable
{
    let id: String
    let projectId: String
    let revisionId: String
    let sourceScalarStart: Int
    let sourceScalarLength: Int
    let quote: String
    let prefixContext: String
    let suffixContext: String
    let note: String?
    let createdAt: Date
}

actor ReadSourceHighlightStore {
    static let shared =
        ReadSourceHighlightStore()

    private struct Envelope: Codable {
        let schemaVersion: Int
        var highlights:
            [ReadSourceHighlight]
    }

    private let fileManager: FileManager
    private let root: URL

    init(
        fileManager: FileManager = .default
    ) {
        self.fileManager = fileManager

        let base = fileManager.urls(
            for:
                .applicationSupportDirectory,
            in: .userDomainMask
        ).first
            ?? fileManager
                .temporaryDirectory

        root = base.appending(
            path:
                "FloentlyReadSourceHighlights",
            directoryHint: .isDirectory
        )
    }

    func highlights(
        accountIdentity: String,
        projectId: String,
        revisionId: String,
        sourceText: String? = nil
    ) -> [ReadSourceHighlight] {
        var values = load(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )

        if
            let sourceText,
            !sourceText.isEmpty
        {
            let migration =
                migrateHighlights(
                    values,
                    toRevisionId:
                        revisionId,
                    sourceText:
                        sourceText
                )

            if migration.changed {
                try? persist(
                    migration.values,
                    accountIdentity:
                        accountIdentity,
                    projectId:
                        projectId
                )
            }

            values = migration.values
        }

        return values
            .filter {
                $0.revisionId
                    == revisionId
            }
            .sorted {
                if
                    $0.sourceScalarStart
                        == $1.sourceScalarStart
                {
                    return $0.createdAt
                        < $1.createdAt
                }

                return $0.sourceScalarStart
                    < $1.sourceScalarStart
            }
    }

    @discardableResult
    func add(
        accountIdentity: String,
        projectId: String,
        revisionId: String,
        sourceText: String,
        sourceScalarStart: Int,
        sourceScalarLength: Int
    ) throws -> ReadSourceHighlight {
        let scalarCount =
            ReadScalarOffsets
                .scalarCount(sourceText)
        let start =
            min(
                max(
                    0,
                    sourceScalarStart
                ),
                scalarCount
            )
        let end =
            min(
                scalarCount,
                start
                    + max(
                        0,
                        sourceScalarLength
                    )
            )
        guard end > start else {
            throw ReadSourceHighlightError
                .invalidRange
        }

        let quote =
            substring(
                sourceText,
                scalarStart: start,
                scalarEnd: end
            )
        let prefix =
            substring(
                sourceText,
                scalarStart:
                    max(0, start - 32),
                scalarEnd: start
            )
        let suffix =
            substring(
                sourceText,
                scalarStart: end,
                scalarEnd:
                    min(
                        scalarCount,
                        end + 32
                    )
            )

        var values = load(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
        values.removeAll {
            $0.revisionId == revisionId
                && $0.sourceScalarStart
                    == start
                && $0.sourceScalarLength
                    == end - start
        }

        let value =
            ReadSourceHighlight(
                id: UUID().uuidString,
                projectId: projectId,
                revisionId: revisionId,
                sourceScalarStart:
                    start,
                sourceScalarLength:
                    end - start,
                quote: quote,
                prefixContext: prefix,
                suffixContext: suffix,
                note: nil,
                createdAt: Date()
            )
        values.append(value)

        try persist(
            values,
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )

        return value
    }

    func updateNote(
        accountIdentity: String,
        projectId: String,
        id: String,
        note: String?
    ) throws {
        var values = load(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
        guard
            let index =
                values.firstIndex(
                    where: {
                        $0.id == id
                    }
                )
        else {
            return
        }

        let trimmed =
            note?
                .trimmingCharacters(
                    in:
                        .whitespacesAndNewlines
                )
        let normalized =
            trimmed?.isEmpty == false
            ? trimmed
            : nil

        let current =
            values[index]
        values[index] =
            ReadSourceHighlight(
                id: current.id,
                projectId:
                    current.projectId,
                revisionId:
                    current.revisionId,
                sourceScalarStart:
                    current
                        .sourceScalarStart,
                sourceScalarLength:
                    current
                        .sourceScalarLength,
                quote: current.quote,
                prefixContext:
                    current.prefixContext,
                suffixContext:
                    current.suffixContext,
                note: normalized,
                createdAt:
                    current.createdAt
            )

        try persist(
            values,
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
    }

    func remove(
        accountIdentity: String,
        projectId: String,
        id: String
    ) throws {
        var values = load(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
        values.removeAll {
            $0.id == id
        }

        try persist(
            values,
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
    }

    func removeProject(
        accountIdentity: String,
        projectId: String
    ) {
        try? fileManager.removeItem(
            at: projectURL(
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

    private func migrateHighlights(
        _ highlights: [ReadSourceHighlight],
        toRevisionId revisionId: String,
        sourceText: String
    ) -> (
        values: [ReadSourceHighlight],
        changed: Bool
    ) {
        var current =
            highlights.filter {
                $0.revisionId == revisionId
            }
        var retained:
            [ReadSourceHighlight] = []
        var changed = false
        let scalarCount =
            ReadScalarOffsets
                .scalarCount(sourceText)

        for value in highlights
        where value.revisionId != revisionId {
            let resolution:
                ReadSourceAnchorResolution?

            do {
                resolution =
                    try ReadCoreNative
                        .resolveSourceAnchor(
                            sourceText:
                                sourceText,
                            quote:
                                value.quote,
                            prefixContext:
                                value.prefixContext,
                            suffixContext:
                                value.suffixContext
                        )
            } catch {
                resolution = nil
            }

            guard
                let resolution
            else {
                retained.append(value)
                continue
            }

            let start =
                resolution.scalarStart
            let length =
                resolution.scalarLength
            guard
                length > 0,
                start + length
                    <= scalarCount
            else {
                retained.append(value)
                continue
            }

            let end = start + length
            let prefix =
                substring(
                    sourceText,
                    scalarStart:
                        max(0, start - 32),
                    scalarEnd: start
                )
            let suffix =
                substring(
                    sourceText,
                    scalarStart: end,
                    scalarEnd:
                        min(
                            scalarCount,
                            end + 32
                        )
                )

            if
                let existingIndex =
                    current.firstIndex(
                        where: {
                            $0.sourceScalarStart
                                == start
                            && $0.sourceScalarLength
                                == length
                            && $0.quote
                                == value.quote
                        }
                    )
            {
                let existing =
                    current[existingIndex]
                let mergedNote =
                    existing.note
                    ?? value.note

                if
                    mergedNote
                        != existing.note
                {
                    current[existingIndex] =
                        ReadSourceHighlight(
                            id: existing.id,
                            projectId:
                                existing.projectId,
                            revisionId:
                                existing.revisionId,
                            sourceScalarStart:
                                existing
                                    .sourceScalarStart,
                            sourceScalarLength:
                                existing
                                    .sourceScalarLength,
                            quote:
                                existing.quote,
                            prefixContext:
                                existing
                                    .prefixContext,
                            suffixContext:
                                existing
                                    .suffixContext,
                            note:
                                mergedNote,
                            createdAt:
                                existing.createdAt
                        )
                }

                changed = true
                continue
            }

            current.append(
                ReadSourceHighlight(
                    id: value.id,
                    projectId:
                        value.projectId,
                    revisionId:
                        revisionId,
                    sourceScalarStart:
                        start,
                    sourceScalarLength:
                        length,
                    quote: value.quote,
                    prefixContext:
                        prefix,
                    suffixContext:
                        suffix,
                    note: value.note,
                    createdAt:
                        value.createdAt
                )
            )
            changed = true
        }

        return (
            retained + current,
            changed
        )
    }

    private func load(
        accountIdentity: String,
        projectId: String
    ) -> [ReadSourceHighlight] {
        let url = projectURL(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )
        guard
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
            return []
        }

        return envelope.highlights
    }

    private func persist(
        _ highlights:
            [ReadSourceHighlight],
        accountIdentity: String,
        projectId: String
    ) throws {
        let directory =
            accountDirectory(
                accountIdentity
            )

        if highlights.isEmpty {
            try? fileManager.removeItem(
                at: projectURL(
                    accountIdentity:
                        accountIdentity,
                    projectId:
                        projectId
                )
            )
            return
        }

        try fileManager.createDirectory(
            at: directory,
            withIntermediateDirectories:
                true
        )

        var values =
            URLResourceValues()
        values.isExcludedFromBackup = true
        var mutableDirectory =
            directory
        try? mutableDirectory
            .setResourceValues(values)

        let envelope = Envelope(
            schemaVersion: 1,
            highlights: highlights
        )
        let url = projectURL(
            accountIdentity:
                accountIdentity,
            projectId: projectId
        )

        try JSONEncoder()
            .encode(envelope)
            .write(
                to: url,
                options: .atomic
            )

        try? fileManager
            .setAttributes(
                [
                    .protectionKey:
                        FileProtectionType
                            .completeUntilFirstUserAuthentication
                ],
                ofItemAtPath:
                    url.path
            )
    }

    private func accountDirectory(
        _ accountIdentity: String
    ) -> URL {
        root.appending(
            path:
                storageKey(
                    accountIdentity
                ),
            directoryHint: .isDirectory
        )
    }

    private func projectURL(
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

    private func substring(
        _ text: String,
        scalarStart: Int,
        scalarEnd: Int
    ) -> String {
        let start =
            ReadScalarOffsets
                .stringIndex(
                    in: text,
                    scalarOffset:
                        scalarStart
                )
        let end =
            ReadScalarOffsets
                .stringIndex(
                    in: text,
                    scalarOffset:
                        scalarEnd
                )

        return String(
            text[start..<end]
        )
    }

    private func storageKey(
        _ value: String
    ) -> String {
        SHA256.hash(
            data:
                Data(value.utf8)
        )
        .map {
            String(
                format: "%02x",
                $0
            )
        }
        .joined()
    }
}

enum ReadSourceHighlightError:
    LocalizedError
{
    case invalidRange

    var errorDescription: String? {
        "The selected source range could not be highlighted."
    }
}
