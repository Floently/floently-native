import CryptoKit
import Foundation

struct ReadNativeTtsAsset: Equatable {
    let audioURL: URL
    let cacheKey: String
    let contentHash: String
    let duration: TimeInterval?
    let voiceId: String
    let provider: String?
    let model: String?
}

struct ReadNativeTtsRequest: Encodable {
    let text: String
    let language: String
    let voiceId: String
}

actor ReadNativeTtsClient {
    private let baseURL: URL
    private let session: URLSession

    init(
        baseURL: URL = URL(string: "https://flowreader-api.onrender.com")!,
        session: URLSession = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
    }

    func synthesize(
        text: String,
        language: String,
        voiceId: String,
        accessToken: String? = nil
    ) async throws -> ReadNativeTtsAsset {
        let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else {
            throw ReadNativeTtsError.emptyText
        }

        var request = URLRequest(
            url: baseURL.appending(path: "/api/tts/prerender")
        )
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = accessToken?.trimmingCharacters(in: .whitespacesAndNewlines),
           !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        request.httpBody = try JSONEncoder().encode(
            ReadNativeTtsRequest(
                text: value,
                language: language.isEmpty ? "auto" : language,
                voiceId: voiceId
            )
        )

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw ReadNativeTtsError.invalidResponse
        }

        let json = try JSONSerialization.jsonObject(with: data)
        guard let root = json as? [String: Any] else {
            throw ReadNativeTtsError.invalidResponse
        }

        if !(200..<300).contains(http.statusCode) {
            let message = Self.errorMessage(from: root)
                ?? "Read TTS failed with HTTP \(http.statusCode)."
            throw ReadNativeTtsError.server(message)
        }

        let source = (root["data"] as? [String: Any]) ?? root

        guard
            let rawURL = (source["audioUrl"] as? String)
                ?? (source["audio_url"] as? String),
            let audioURL = URL(string: rawURL)
        else {
            throw ReadNativeTtsError.missingAudioURL
        }

        let resolvedVoice =
            (source["voiceId"] as? String)
            ?? (source["voice_id"] as? String)
            ?? voiceId

        let hashInput = [
            value,
            language,
            resolvedVoice,
            "render-read-prerender"
        ].joined(separator: "\u{1f}")
        let digest = SHA256.hash(data: Data(hashInput.utf8))
            .map { String(format: "%02x", $0) }
            .joined()

        let cacheKey =
            (source["cacheKey"] as? String)
            ?? (source["cache_key"] as? String)
            ?? "read-tts:sha256:\(digest)"

        let explicitDuration: TimeInterval? = {
            if let ms = source["durationMs"] as? NSNumber {
                return max(0, ms.doubleValue / 1_000)
            }
            if let ms = source["duration_ms"] as? NSNumber {
                return max(0, ms.doubleValue / 1_000)
            }
            if let seconds = source["durationSeconds"] as? NSNumber {
                return max(0, seconds.doubleValue)
            }
            if let seconds = source["duration_seconds"] as? NSNumber {
                return max(0, seconds.doubleValue)
            }
            return nil
        }()

        return ReadNativeTtsAsset(
            audioURL: audioURL,
            cacheKey: cacheKey,
            contentHash: "sha256:\(digest)",
            duration: explicitDuration,
            voiceId: resolvedVoice,
            provider: source["provider"] as? String,
            model: source["model"] as? String
        )
    }

    private static func errorMessage(
        from root: [String: Any]
    ) -> String? {
        if let error = root["error"] as? [String: Any],
           let value = error["message"] as? String,
           !value.isEmpty {
            return value
        }
        if let detail = root["detail"] as? String, !detail.isEmpty {
            return detail
        }
        if let message = root["message"] as? String, !message.isEmpty {
            return message
        }
        return nil
    }
}

enum ReadNativeTtsError: LocalizedError {
    case emptyText
    case invalidResponse
    case missingAudioURL
    case server(String)

    var errorDescription: String? {
        switch self {
        case .emptyText:
            return "No readable text was available for synthesis."
        case .invalidResponse:
            return "Read TTS returned an invalid response."
        case .missingAudioURL:
            return "Read TTS returned no playable audio URL."
        case .server(let message):
            return message
        }
    }
}

actor ReadNativeAudioCache {
    private let root: URL
    private let session: URLSession
    private let fileManager: FileManager
    private let maximumBytes: Int64
    private var protectedPaths: Set<String> = []

    init(
        fileManager: FileManager = .default,
        session: URLSession = .shared,
        maximumBytes: Int64 = 256 * 1024 * 1024
    ) throws {
        let cacheRoot = try fileManager.url(
            for: .cachesDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        root = cacheRoot.appending(
            path: "FloentlyReadAudioV1",
            directoryHint: .isDirectory
        )
        self.session = session
        self.fileManager = fileManager
        self.maximumBytes = max(
            16 * 1024 * 1024,
            maximumBytes
        )
        try fileManager.createDirectory(
            at: root,
            withIntermediateDirectories: true
        )
    }

    func localURL(
        for asset: ReadNativeTtsAsset
    ) async throws -> URL {
        let extensionName =
            asset.audioURL.pathExtension.isEmpty
            ? "audio"
            : asset.audioURL.pathExtension
        let fileName = Self.safeFileName(asset.cacheKey)
            + "."
            + extensionName
        let destination = root.appending(path: fileName)

        if fileManager.fileExists(atPath: destination.path) {
            try? fileManager.setAttributes(
                [.modificationDate: Date()],
                ofItemAtPath: destination.path
            )
            try prune(
                additionallyProtecting: [destination.path]
            )
            return destination
        }

        let (temporaryURL, response) = try await session.download(
            from: asset.audioURL
        )

        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw ReadNativeTtsError.invalidResponse
        }

        try? fileManager.removeItem(at: destination)
        try fileManager.moveItem(
            at: temporaryURL,
            to: destination
        )

        try prune(
            additionallyProtecting: [destination.path]
        )
        return destination
    }

    func replaceProtectedURLs(
        _ urls: [URL]
    ) throws {
        protectedPaths = Set(
            urls.map { $0.standardizedFileURL.path }
        )
        try prune()
    }

    private func prune(
        additionallyProtecting extraPaths: Set<String> = []
    ) throws {
        let keys: Set<URLResourceKey> = [
            .isRegularFileKey,
            .fileSizeKey,
            .contentModificationDateKey
        ]
        let urls = try fileManager.contentsOfDirectory(
            at: root,
            includingPropertiesForKeys: Array(keys),
            options: [.skipsHiddenFiles]
        )

        let entries: [(url: URL, size: Int64, modified: Date)] =
            urls.compactMap { url in
                guard
                    let values = try? url.resourceValues(
                        forKeys: keys
                    ),
                    values.isRegularFile == true
                else {
                    return nil
                }

                return (
                    url: url,
                    size: Int64(max(0, values.fileSize ?? 0)),
                    modified:
                        values.contentModificationDate
                        ?? .distantPast
                )
            }

        var totalBytes = entries.reduce(Int64(0)) {
            $0 + $1.size
        }
        guard totalBytes > maximumBytes else { return }

        let protected = protectedPaths.union(
            extraPaths.map {
                URL(fileURLWithPath: $0)
                    .standardizedFileURL.path
            }
        )

        for entry in entries.sorted(
            by: { $0.modified < $1.modified }
        ) {
            if totalBytes <= maximumBytes {
                break
            }

            let path = entry.url.standardizedFileURL.path
            guard !protected.contains(path) else {
                continue
            }

            do {
                try fileManager.removeItem(at: entry.url)
                totalBytes -= entry.size
            } catch {
                // Cache pressure must never interrupt active playback.
                continue
            }
        }
    }

    private static func safeFileName(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
