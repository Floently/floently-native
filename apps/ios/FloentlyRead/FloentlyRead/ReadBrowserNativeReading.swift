import CryptoKit
import Foundation

struct ReadBrowserReadingSource {
    enum Kind: String {
        case page
        case selection
    }

    let kind: Kind
    let url: URL
    let title: String
    let language: String
    let text: String
}

enum ReadBrowserNativeReading {
    static func manifest(
        for source: ReadBrowserReadingSource
    ) throws -> ReadingManifestV1 {
        let canonicalLanguage = normalizedLanguage(
            source.language
        )

        return try ReadCoreNative.buildManifest(
            documentId: documentId(for: source),
            revisionId: revisionId(for: source),
            title: source.title,
            language: canonicalLanguage,
            text: source.text
        )
    }

    static func defaultVoiceId(
        for language: String
    ) -> String {
        let primary = normalizedLanguage(language)
            .lowercased()
            .split(separator: "-")
            .first
            .map(String.init)
            ?? ""

        if primary == "fi" {
            return "azure:fi-FI-SelmaNeural"
        }

        return "google:en-US-Neural2-C"
    }

    private static func documentId(
        for source: ReadBrowserReadingSource
    ) -> String {
        let value = [
            "web",
            source.kind.rawValue,
            source.url.absoluteString
        ].joined(separator: "\u{1f}")

        return "web:\(source.kind.rawValue):\(sha256(value))"
    }

    private static func revisionId(
        for source: ReadBrowserReadingSource
    ) -> String {
        "sha256:\(sha256(source.text))"
    }

    private static func normalizedLanguage(
        _ value: String
    ) -> String {
        let trimmed = value.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        return trimmed.isEmpty ? "auto" : trimmed
    }

    private static func sha256(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}
