import Foundation

struct ReadProjectProgress: Codable, Equatable, Sendable {
    let projectId: String
    let currentSegmentIndex: Int
    let currentCharacterOffset: Int
    let progressPercent: Double
    let voiceId: String?
    let playbackRate: Double?
    let updatedAt: String

    static func decode(_ value: Any?) -> ReadProjectProgress? {
        guard let object = value as? [String: Any] else { return nil }

        func string(_ key: String) -> String {
            object[key] as? String ?? ""
        }
        func integer(_ key: String) -> Int {
            if let value = object[key] as? NSNumber {
                return value.intValue
            }
            return object[key] as? Int ?? 0
        }
        func double(_ key: String) -> Double? {
            if let value = object[key] as? NSNumber {
                return value.doubleValue
            }
            return object[key] as? Double
        }

        return ReadProjectProgress(
            projectId: string("projectId"),
            currentSegmentIndex: max(0, integer("currentSegmentIndex")),
            currentCharacterOffset: max(0, integer("currentCharacterOffset")),
            progressPercent: min(100, max(0, double("progressPercent") ?? 0)),
            voiceId: string("voiceId").nilIfBlank,
            playbackRate: double("playbackRate"),
            updatedAt: string("updatedAt")
        )
    }
}

struct ReadContentProject: Codable, Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let kind: String
    let status: String
    let sourceType: String
    let sourceURL: String?
    let language: String?
    let textHash: String
    let wordCount: Int
    let characterCount: Int
    let createdAt: String
    let updatedAt: String
    let lastOpenedAt: String?
    let progress: ReadProjectProgress?
    let rawText: String?

    var revisionId: String {
        if !textHash.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return textHash
        }
        if !updatedAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return updatedAt
        }
        return "project-\(id)"
    }

    var displaySource: String {
        switch sourceType.lowercased() {
        case "pdf":
            return "PDF"
        case "epub":
            return "EPUB"
        case "docx", "word":
            return "Document"
        case "web", "website", "url":
            return "Website"
        case "text", "txt", "markdown", "md":
            return "Text"
        default:
            return sourceType.isEmpty ? "Document" : sourceType.capitalized
        }
    }

    static func decode(_ value: Any) -> ReadContentProject? {
        guard let object = value as? [String: Any] else { return nil }

        func string(_ keys: [String], fallback: String = "") -> String {
            for key in keys {
                if let value = object[key] as? String {
                    return value
                }
            }
            return fallback
        }

        func integer(_ keys: [String]) -> Int {
            for key in keys {
                if let value = object[key] as? NSNumber {
                    return value.intValue
                }
                if let value = object[key] as? Int {
                    return value
                }
            }
            return 0
        }

        let id = string(["id"]).trimmingCharacters(in: .whitespacesAndNewlines)
        let title = string(["title"]).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty, !title.isEmpty else { return nil }

        return ReadContentProject(
            id: id,
            title: title,
            kind: string(["kind"], fallback: "document"),
            status: string(["status"], fallback: "ready"),
            sourceType: string(["sourceType", "source_type"], fallback: "text"),
            sourceURL: string(["sourceUrl", "source_url"]).nilIfBlank,
            language: string(["language"]).nilIfBlank,
            textHash: string(["textHash", "text_hash"]),
            wordCount: integer(["wordCount", "word_count"]),
            characterCount: integer(["characterCount", "character_count"]),
            createdAt: string(["createdAt", "created_at"]),
            updatedAt: string(["updatedAt", "updated_at"]),
            lastOpenedAt: string(["lastOpenedAt", "last_opened_at"]).nilIfBlank,
            progress: ReadProjectProgress.decode(object["progress"]),
            rawText: string(["rawText", "raw_text", "text", "content"]).nilIfBlank
        )
    }

    func replacingProgress(_ progress: ReadProjectProgress?) -> ReadContentProject {
        ReadContentProject(
            id: id,
            title: title,
            kind: kind,
            status: status,
            sourceType: sourceType,
            sourceURL: sourceURL,
            language: language,
            textHash: textHash,
            wordCount: wordCount,
            characterCount: characterCount,
            createdAt: createdAt,
            updatedAt: updatedAt,
            lastOpenedAt: lastOpenedAt,
            progress: progress,
            rawText: rawText
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}

enum ReadProjectClientError: LocalizedError {
    case invalidResponse
    case invalidProject
    case emptyFile
    case fileTooLarge
    case server(String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "The Read service returned an invalid response."
        case .invalidProject:
            return "The document did not contain readable content."
        case .emptyFile:
            return "Choose a readable document file."
        case .fileTooLarge:
            return "This file is too large for the first release importer. Choose a file smaller than 75 MB."
        case .server(let message):
            return message
        }
    }
}

actor ReadContentProjectClient {
    private let baseURL: URL
    private let session: URLSession

    init(
        baseURL: URL = URL(string: "https://flowreader-api.onrender.com")!,
        session: URLSession = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
    }

    func listProjects(
        accessToken: String,
        limit: Int = 50,
        offset: Int = 0
    ) async throws -> [ReadContentProject] {
        var components = URLComponents(
            url: baseURL.appending(path: "/api/v1/documents"),
            resolvingAgainstBaseURL: false
        )
        components?.queryItems = [
            URLQueryItem(name: "limit", value: String(max(1, min(100, limit)))),
            URLQueryItem(name: "offset", value: String(max(0, offset)))
        ]

        guard let url = components?.url else {
            throw ReadProjectClientError.invalidResponse
        }

        let payload = try await requestJSON(
            url: url,
            method: "GET",
            accessToken: accessToken
        )
        let value = unwrappedValue(payload)

        if let values = value as? [Any] {
            return values.compactMap(ReadContentProject.decode)
        }

        guard let object = value as? [String: Any] else {
            return []
        }

        for key in ["projects", "documents", "items"] {
            if let values = object[key] as? [Any] {
                return values.compactMap(ReadContentProject.decode)
            }
        }

        return []
    }

    func project(
        id: String,
        accessToken: String
    ) async throws -> ReadContentProject {
        let url = baseURL.appending(
            path: "/api/v1/documents/\(id.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? id)"
        )
        let payload = try await requestJSON(
            url: url,
            method: "GET",
            accessToken: accessToken
        )
        let object = unwrappedObject(payload)
        let candidate =
            object["project"]
            ?? object["document"]
            ?? object

        guard
            let project = ReadContentProject.decode(candidate),
            let rawText = project.rawText,
            !rawText.isEmpty
        else {
            throw ReadProjectClientError.invalidProject
        }
        return project
    }

    func createTextProject(
        title: String?,
        text: String,
        accessToken: String
    ) async throws -> ReadContentProject {
        let normalized = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            throw ReadProjectClientError.invalidProject
        }

        var body: [String: Any] = [
            "text": normalized,
            "content": normalized,
            "language": "auto",
            "sourceType": "text",
            "source_type": "text"
        ]
        if let title = title?.nilIfBlank {
            body["title"] = title
        }

        let data = try JSONSerialization.data(withJSONObject: body)
        let payload = try await requestJSON(
            url: baseURL.appending(path: "/api/v1/documents/from-text"),
            method: "POST",
            accessToken: accessToken,
            body: data,
            contentType: "application/json"
        )
        let object = unwrappedObject(payload)
        let candidate =
            object["project"]
            ?? object["document"]
            ?? object

        guard let project = ReadContentProject.decode(candidate) else {
            throw ReadProjectClientError.invalidProject
        }
        return project
    }

    func createURLProject(
        title: String?,
        sourceURL: String,
        accessToken: String
    ) async throws -> ReadContentProject {
        let normalized = sourceURL.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        guard
            let url = URL(string: normalized),
            let scheme = url.scheme?.lowercased(),
            scheme == "https" || scheme == "http"
        else {
            throw ReadProjectClientError.server(
                "Enter a valid http or https URL."
            )
        }

        var body: [String: Any] = [
            "url": normalized
        ]
        if let title = title?.nilIfBlank {
            body["title"] = title
        }

        let data = try JSONSerialization.data(withJSONObject: body)
        let payload = try await requestJSON(
            url: baseURL.appending(path: "/api/v1/documents/from-url"),
            method: "POST",
            accessToken: accessToken,
            body: data,
            contentType: "application/json"
        )
        let object = unwrappedObject(payload)
        let candidate =
            object["project"]
            ?? object["document"]
            ?? object

        guard let project = ReadContentProject.decode(candidate) else {
            throw ReadProjectClientError.invalidProject
        }
        return project
    }

    func updateProgress(
        projectId: String,
        currentSegmentIndex: Int,
        currentCharacterOffset: Int,
        progressPercent: Double,
        voiceId: String?,
        playbackRate: Double?,
        accessToken: String
    ) async throws -> ReadProjectProgress? {
        let encoded = projectId.addingPercentEncoding(
            withAllowedCharacters: .urlPathAllowed
        ) ?? projectId
        var body: [String: Any] = [
            "currentSegmentIndex": max(0, currentSegmentIndex),
            "currentCharacterOffset": max(0, currentCharacterOffset),
            "progressPercent": min(100, max(0, progressPercent))
        ]
        if let voiceId = voiceId?.nilIfBlank {
            body["voiceId"] = voiceId
        }
        if let playbackRate, playbackRate.isFinite {
            body["playbackRate"] = min(3, max(0.5, playbackRate))
        }
        let data = try JSONSerialization.data(withJSONObject: body)
        let payload = try await requestJSON(
            url: baseURL.appending(path: "/api/v1/documents/\(encoded)/progress"),
            method: "PUT",
            accessToken: accessToken,
            body: data,
            contentType: "application/json"
        )
        let object = unwrappedObject(payload)
        return ReadProjectProgress.decode(object["progress"])
    }

    func deleteProject(
        id: String,
        accessToken: String
    ) async throws {
        let encoded = id.addingPercentEncoding(
            withAllowedCharacters: .urlPathAllowed
        ) ?? id
        _ = try await requestJSON(
            url: baseURL.appending(
                path: "/api/v1/documents/\(encoded)"
            ),
            method: "DELETE",
            accessToken: accessToken
        )
    }

    func uploadProject(
        fileURL: URL,
        title: String?,
        accessToken: String
    ) async throws -> ReadContentProject {
        let values = try fileURL.resourceValues(
            forKeys: [.fileSizeKey, .nameKey]
        )
        let fileSize = values.fileSize ?? 0
        guard fileSize > 0 else {
            throw ReadProjectClientError.emptyFile
        }
        guard fileSize <= 75 * 1024 * 1024 else {
            throw ReadProjectClientError.fileTooLarge
        }

        let boundary = "FloentlyRead-\(UUID().uuidString)"
        let temporary = FileManager.default.temporaryDirectory
            .appending(path: "read-upload-\(UUID().uuidString).multipart")
        FileManager.default.createFile(
            atPath: temporary.path,
            contents: nil
        )

        let handle = try FileHandle(forWritingTo: temporary)
        defer {
            try? handle.close()
            try? FileManager.default.removeItem(at: temporary)
        }

        func write(_ value: String) throws {
            if let data = value.data(using: .utf8) {
                try handle.write(contentsOf: data)
            }
        }

        let fileName = values.name ?? fileURL.lastPathComponent
        let safeFileName = fileName
            .replacingOccurrences(of: "\r", with: "_")
            .replacingOccurrences(of: "\n", with: "_")
            .replacingOccurrences(of: "\"", with: "_")
        try write("--\(boundary)\r\n")
        try write(
            "Content-Disposition: form-data; name=\"file\"; "
            + "filename=\"\(safeFileName)\"\r\n"
        )
        try write("Content-Type: application/octet-stream\r\n\r\n")

        let source = try FileHandle(forReadingFrom: fileURL)
        defer { try? source.close() }
        while true {
            let chunk = try source.read(upToCount: 512 * 1024) ?? Data()
            if chunk.isEmpty { break }
            try handle.write(contentsOf: chunk)
        }

        try write("\r\n")
        if let title = title?.nilIfBlank {
            try write("--\(boundary)\r\n")
            try write("Content-Disposition: form-data; name=\"title\"\r\n\r\n")
            try write(title)
            try write("\r\n")
        }
        try write("--\(boundary)--\r\n")
        try handle.synchronize()

        var request = URLRequest(
            url: baseURL.appending(path: "/api/v1/documents/upload")
        )
        request.httpMethod = "POST"
        request.setValue(
            "Bearer \(accessToken)",
            forHTTPHeaderField: "Authorization"
        )
        request.setValue(
            "multipart/form-data; boundary=\(boundary)",
            forHTTPHeaderField: "Content-Type"
        )
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.upload(
            for: request,
            fromFile: temporary
        )
        try validate(response: response, data: data)

        let payload = try JSONSerialization.jsonObject(with: data)
        let object = unwrappedObject(payload)
        let candidate =
            object["project"]
            ?? object["document"]
            ?? object
        guard let project = ReadContentProject.decode(candidate) else {
            throw ReadProjectClientError.invalidProject
        }
        return project
    }

    private func requestJSON(
        url: URL,
        method: String,
        accessToken: String,
        body: Data? = nil,
        contentType: String? = nil
    ) async throws -> Any {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(
            "Bearer \(accessToken)",
            forHTTPHeaderField: "Authorization"
        )
        if let body {
            request.httpBody = body
            request.setValue(
                contentType ?? "application/json",
                forHTTPHeaderField: "Content-Type"
            )
        }

        let (data, response) = try await session.data(for: request)
        try validate(response: response, data: data)
        guard !data.isEmpty else { return [:] }
        return try JSONSerialization.jsonObject(with: data)
    }

    private func validate(
        response: URLResponse,
        data: Data
    ) throws {
        guard let http = response as? HTTPURLResponse else {
            throw ReadProjectClientError.invalidResponse
        }
        guard (200..<300).contains(http.statusCode) else {
            let message = Self.errorMessage(data)
                ?? "Read service failed with HTTP \(http.statusCode)."
            throw ReadProjectClientError.server(message)
        }
    }

    private func unwrappedValue(
        _ payload: Any
    ) -> Any {
        guard let object = payload as? [String: Any] else {
            return payload
        }

        if let data = object["data"] {
            return data
        }

        return object
    }

    private func unwrappedObject(
        _ payload: Any
    ) -> [String: Any] {
        unwrappedValue(payload) as? [String: Any] ?? [:]
    }

    private static func errorMessage(
        _ data: Data
    ) -> String? {
        guard
            let payload = try? JSONSerialization.jsonObject(with: data),
            let object = payload as? [String: Any]
        else {
            return nil
        }

        for key in ["detail", "message"] {
            if let value = object[key] as? String, !value.isEmpty {
                return value
            }
        }

        if let error = object["error"] as? String, !error.isEmpty {
            return error
        }
        if
            let error = object["error"] as? [String: Any],
            let message = error["message"] as? String,
            !message.isEmpty
        {
            return message
        }
        return nil
    }
}

@MainActor
final class ReadProjectStore: ObservableObject {
    enum Activity: Equatable {
        case idle
        case loading
        case importing(String)
    }

    @Published private(set) var projects: [ReadContentProject] = []
    @Published private(set) var activity: Activity = .idle
    @Published var errorMessage: String?

    private let client = ReadContentProjectClient()
    private let originalStore = ReadOriginalDocumentStore.shared
    private let snapshotStore = ReadProjectSnapshotStore.shared
    private let progressOutbox = ReadProgressOutboxStore.shared
    private var snapshotAccountIdentity: String?
    private var progressSyncTail: Task<Void, Never>?
    private var progressSyncGeneration = 0
    private var progressSyncSequence = 0

    func bindAccount(
        userId: String,
        email: String
    ) {
        snapshotAccountIdentity =
            readAccountIdentity(
                userId: userId,
                email: email
            )
    }

    func refresh(accessToken: String) async {
        activity = .loading
        errorMessage = nil

        let cached: [ReadContentProject]
        if let snapshotAccountIdentity {
            cached = await snapshotStore.load(
                accountIdentity: snapshotAccountIdentity
            )
        } else {
            cached = []
        }
        if projects.isEmpty && !cached.isEmpty {
            projects = cached
        }

        do {
            let remote = try await client.listProjects(
                accessToken: accessToken
            )
            projects = mergeRemoteProjects(
                remote,
                cached: cached
            )
            await flushPendingProgress(
                accessToken: accessToken,
                remoteProjects: remote
            )
            await persistSnapshotIfBound()
            activity = .idle
        } catch {
            activity = .idle
            if projects.isEmpty {
                errorMessage = error.localizedDescription
            } else {
                errorMessage =
                    "You’re offline. Showing saved library content from this device."
            }
        }
    }

    func hydrate(
        _ project: ReadContentProject,
        accessToken: String
    ) async throws -> ReadContentProject {
        if let text = project.rawText, !text.isEmpty {
            return project
        }

        let hydrated = try await client.project(
            id: project.id,
            accessToken: accessToken
        )
        upsert(hydrated)
        await persistSnapshotIfBound()
        return hydrated
    }

    func addText(
        title: String?,
        text: String,
        accessToken: String
    ) async throws -> ReadContentProject {
        errorMessage = nil
        activity = .importing("Saving text")
        defer { activity = .idle }

        let project = try await client.createTextProject(
            title: title,
            text: text,
            accessToken: accessToken
        )
        upsert(project)
        await persistSnapshotIfBound()
        return project
    }

    func addURL(
        title: String?,
        sourceURL: String,
        accessToken: String
    ) async throws -> ReadContentProject {
        errorMessage = nil
        activity = .importing("Importing link")
        defer { activity = .idle }

        let project = try await client.createURLProject(
            title: title,
            sourceURL: sourceURL,
            accessToken: accessToken
        )
        upsert(project)
        await persistSnapshotIfBound()
        return project
    }

    func delete(
        _ project: ReadContentProject,
        accessToken: String
    ) async throws {
        errorMessage = nil
        try await client.deleteProject(
            id: project.id,
            accessToken: accessToken
        )
        await originalStore.delete(
            projectId: project.id
        )
        await ReadEpubSourceStore.shared
            .delete(
                projectId: project.id
            )
        if let snapshotAccountIdentity {
            await ReadOfflineAudioStore.shared
                .removeDocument(
                    accountIdentity:
                        snapshotAccountIdentity,
                    documentId: project.id
                )
            await progressOutbox.removeProject(
                accountIdentity:
                    snapshotAccountIdentity,
                projectId: project.id
            )
            await ReadSourceHighlightStore
                .shared
                .removeProject(
                    accountIdentity:
                        snapshotAccountIdentity,
                    projectId: project.id
                )
        }
        projects.removeAll { $0.id == project.id }
        await persistSnapshotIfBound()
    }

    func addFile(
        url: URL,
        accessToken: String
    ) async throws -> ReadContentProject {
        errorMessage = nil
        activity = .importing("Uploading and extracting")
        defer { activity = .idle }

        let granted = url.startAccessingSecurityScopedResource()
        defer {
            if granted {
                url.stopAccessingSecurityScopedResource()
            }
        }

        let project = try await client.uploadProject(
            fileURL: url,
            title: nil,
            accessToken: accessToken
        )

        let sourceType =
            project.sourceType.lowercased()
        let fileExtension =
            url.pathExtension.lowercased()

        if
            sourceType == "pdf"
            || fileExtension == "pdf"
        {
            try? await originalStore.savePDF(
                projectId: project.id,
                sourceURL: url
            )
        } else if
            sourceType == "epub"
            || fileExtension == "epub"
        {
            try? await originalStore.saveEPUB(
                projectId: project.id,
                sourceURL: url
            )
        }

        upsert(project)
        await persistSnapshotIfBound()
        return project
    }

    func syncProgress(
        projectId: String,
        currentSegmentIndex: Int,
        currentCharacterOffset: Int,
        progressPercent: Double,
        voiceId: String?,
        playbackRate: Double?,
        accessToken: String
    ) async {
        let generation = progressSyncGeneration
        progressSyncSequence &+= 1
        let sequence = progressSyncSequence
        let accountIdentity = snapshotAccountIdentity
        let baseServerUpdatedAt =
            projects.first {
                $0.id == projectId
            }?.progress?.updatedAt
        let boundedPlaybackRate =
            playbackRate.flatMap {
                $0.isFinite
                ? min(
                    3,
                    max(0.5, $0)
                )
                : nil
            }
        let pending = ReadPendingProgressWrite(
            nonce: UUID().uuidString,
            projectId: projectId,
            currentSegmentIndex:
                max(0, currentSegmentIndex),
            currentCharacterOffset:
                max(0, currentCharacterOffset),
            progressPercent:
                min(100, max(0, progressPercent)),
            voiceId: voiceId,
            playbackRate:
                boundedPlaybackRate,
            baseServerUpdatedAt:
                baseServerUpdatedAt,
            createdAt: Date()
        )

        if let accountIdentity {
            try? await progressOutbox.save(
                accountIdentity:
                    accountIdentity,
                write: pending
            )
        }

        let previous = progressSyncTail
        let task = Task { [weak self] in
            if let previous {
                await previous.value
            }

            guard
                !Task.isCancelled,
                let self,
                generation == self.progressSyncGeneration,
                sequence == self.progressSyncSequence
            else {
                return
            }

            do {
                let progress = try await self.client.updateProgress(
                    projectId: projectId,
                    currentSegmentIndex:
                        pending.currentSegmentIndex,
                    currentCharacterOffset:
                        pending.currentCharacterOffset,
                    progressPercent:
                        pending.progressPercent,
                    voiceId: pending.voiceId,
                    playbackRate:
                        pending.playbackRate,
                    accessToken: accessToken
                )

                if let accountIdentity {
                    await self.progressOutbox
                        .removeIfMatches(
                            accountIdentity:
                                accountIdentity,
                            projectId: projectId,
                            nonce: pending.nonce
                        )
                }

                guard
                    !Task.isCancelled,
                    generation == self.progressSyncGeneration,
                    sequence == self.progressSyncSequence
                else {
                    return
                }

                if
                    let progress,
                    let index =
                        self.projects
                            .firstIndex(where: {
                                $0.id == projectId
                            })
                {
                    self.projects[index] =
                        self.projects[index]
                            .replacingProgress(
                                progress
                            )
                }
            } catch is CancellationError {
                return
            } catch {
                // The write-ahead outbox retains the newest cursor for
                // reconnect. Local resume remains authoritative offline.
            }
        }

        progressSyncTail = task
        await task.value
        // Chaining writes prevents an older cursor from completing after
        // a newer one and moving cloud progress backwards. Waiting writes are
        // coalesced so only the newest queued cursor reaches the server.
    }

    func reset() {
        progressSyncGeneration &+= 1
        progressSyncSequence &+= 1
        progressSyncTail?.cancel()
        progressSyncTail = nil
        snapshotAccountIdentity = nil
        projects = []
        activity = .idle
        errorMessage = nil
    }

    private func flushPendingProgress(
        accessToken: String,
        remoteProjects: [ReadContentProject]
    ) async {
        guard let snapshotAccountIdentity else {
            return
        }

        if let progressSyncTail {
            await progressSyncTail.value
        }

        let remoteById = Dictionary(
            uniqueKeysWithValues:
                remoteProjects.map {
                    ($0.id, $0)
                }
        )
        let writes = await progressOutbox.entries(
            accountIdentity:
                snapshotAccountIdentity
        )

        for write in writes {
            guard
                let current =
                    await progressOutbox
                        .currentWrite(
                            accountIdentity:
                                snapshotAccountIdentity,
                            projectId:
                                write.projectId
                        ),
                current.nonce == write.nonce
            else {
                continue
            }

            guard
                let remote =
                    remoteById[write.projectId]
            else {
                await progressOutbox
                    .removeIfMatches(
                        accountIdentity:
                            snapshotAccountIdentity,
                        projectId:
                            write.projectId,
                        nonce: write.nonce
                    )
                continue
            }

            let remoteUpdatedAt =
                remote.progress?
                    .updatedAt
                    .nilIfBlank
            let baseUpdatedAt =
                write.baseServerUpdatedAt?
                    .nilIfBlank

            if
                let remoteUpdatedAt,
                remoteUpdatedAt != baseUpdatedAt
            {
                // The server advanced since this offline cursor was based
                // on it. Remote wins to prevent another device's newer
                // progress from being overwritten by a stale replay.
                await progressOutbox
                    .removeIfMatches(
                        accountIdentity:
                            snapshotAccountIdentity,
                        projectId:
                            write.projectId,
                        nonce: write.nonce
                    )
                continue
            }

            do {
                let progress =
                    try await client.updateProgress(
                        projectId:
                            write.projectId,
                        currentSegmentIndex:
                            write.currentSegmentIndex,
                        currentCharacterOffset:
                            write.currentCharacterOffset,
                        progressPercent:
                            write.progressPercent,
                        voiceId:
                            write.voiceId,
                        playbackRate:
                            write.playbackRate,
                        accessToken:
                            accessToken
                    )

                await progressOutbox
                    .removeIfMatches(
                        accountIdentity:
                            snapshotAccountIdentity,
                        projectId:
                            write.projectId,
                        nonce: write.nonce
                    )

                if
                    let progress,
                    let index =
                        projects.firstIndex(
                            where: {
                                $0.id
                                    == write.projectId
                            }
                        )
                {
                    projects[index] =
                        projects[index]
                            .replacingProgress(
                                progress
                            )
                }
            } catch is CancellationError {
                return
            } catch {
                // Keep the pending write for a later online refresh.
                continue
            }
        }
    }

    private func persistSnapshotIfBound() async {
        guard let snapshotAccountIdentity else {
            return
        }

        try? await snapshotStore.save(
            projects: projects,
            accountIdentity: snapshotAccountIdentity
        )
    }

    private func mergeRemoteProjects(
        _ remote: [ReadContentProject],
        cached: [ReadContentProject]
    ) -> [ReadContentProject] {
        let cachedById = Dictionary(
            uniqueKeysWithValues:
                cached.map { ($0.id, $0) }
        )

        return remote.map { value in
            guard
                let local = cachedById[value.id],
                local.revisionId == value.revisionId
            else {
                return value
            }

            return ReadContentProject(
                id: value.id,
                title: value.title,
                kind: value.kind,
                status: value.status,
                sourceType: value.sourceType,
                sourceURL: value.sourceURL,
                language: value.language,
                textHash: value.textHash,
                wordCount: value.wordCount,
                characterCount: value.characterCount,
                createdAt: value.createdAt,
                updatedAt: value.updatedAt,
                lastOpenedAt: value.lastOpenedAt,
                progress: value.progress ?? local.progress,
                rawText: value.rawText ?? local.rawText
            )
        }
    }

    private func upsert(_ project: ReadContentProject) {
        projects.removeAll { $0.id == project.id }
        projects.insert(project, at: 0)
    }
}
