import Foundation

struct ReadProjectProgress: Equatable, Sendable {
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

struct ReadContentProject: Identifiable, Equatable, Sendable {
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
        try write("--\(boundary)\r\n")
        try write("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName.replacingOccurrences(of: "\"", with: "_"))\"\r\n")
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
    private var progressSyncTail: Task<Void, Never>?
    private var progressSyncGeneration = 0
    private var progressSyncSequence = 0

    func refresh(accessToken: String) async {
        activity = .loading
        errorMessage = nil

        do {
            projects = try await client.listProjects(
                accessToken: accessToken
            )
            activity = .idle
        } catch {
            activity = .idle
            errorMessage = error.localizedDescription
        }
    }

    func hydrate(
        _ project: ReadContentProject,
        accessToken: String
    ) async throws -> ReadContentProject {
        if let text = project.rawText, !text.isEmpty {
            return project
        }
        return try await client.project(
            id: project.id,
            accessToken: accessToken
        )
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
        projects.removeAll { $0.id == project.id }
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

        if
            project.sourceType.lowercased() == "pdf"
            || url.pathExtension.lowercased() == "pdf"
        {
            try? await originalStore.savePDF(
                projectId: project.id,
                sourceURL: url
            )
        }

        upsert(project)
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
                    currentSegmentIndex: currentSegmentIndex,
                    currentCharacterOffset: currentCharacterOffset,
                    progressPercent: progressPercent,
                    voiceId: voiceId,
                    playbackRate: playbackRate,
                    accessToken: accessToken
                )
                guard
                    !Task.isCancelled,
                    generation == self.progressSyncGeneration,
                    sequence == self.progressSyncSequence,
                    let progress
                else {
                    return
                }
                if let index = self.projects.firstIndex(where: {
                    $0.id == projectId
                }) {
                    self.projects[index] =
                        self.projects[index]
                            .replacingProgress(progress)
                }
            } catch {
                // Best effort: local resume remains authoritative offline.
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
        projects = []
        activity = .idle
        errorMessage = nil
    }

    private func upsert(_ project: ReadContentProject) {
        projects.removeAll { $0.id == project.id }
        projects.insert(project, at: 0)
    }
}
