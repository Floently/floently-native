import Combine
import Foundation
import FloentlyShared

struct ReadVoiceOption: Identifiable, Equatable {
    let id: String
    let name: String
    let language: String
    let locale: String
    let gender: String?
    let accent: String?
}

@MainActor
final class ReadVoiceSettings: ObservableObject {
    @Published private(set) var voices: [ReadVoiceOption] = []
    @Published private(set) var isLoading = false
    @Published private(set) var loadError: String?

    private let defaults: UserDefaults
    private let storageKey = "floently.read.voice.by-language.v1"
    private var selectedByLanguage: [String: String]

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if
            let data = defaults.data(forKey: storageKey),
            let decoded = try? JSONDecoder().decode(
                [String: String].self,
                from: data
            )
        {
            selectedByLanguage = decoded
        } else {
            selectedByLanguage = [:]
        }
    }

    func voiceId(for language: String) -> String {
        let key = languageKey(language)
        return selectedByLanguage[key]
            ?? Self.defaultVoiceId(for: language)
    }

    func select(
        voiceId: String,
        for language: String
    ) {
        let value = voiceId.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        guard !value.isEmpty else { return }

        selectedByLanguage[languageKey(language)] = value
        persist()
        objectWillChange.send()
    }

    func availableVoices(
        for language: String
    ) -> [ReadVoiceOption] {
        let primary = languageKey(language)

        let matching = voices.filter { voice in
            let voicePrimary = languageKey(
                voice.language.isEmpty
                ? voice.locale
                : voice.language
            )
            return voicePrimary == primary
        }

        return matching.isEmpty ? voices : matching
    }

    func refresh(
        sessionStore: FloentlySessionStore
    ) async {
        guard !isLoading else { return }
        isLoading = true
        loadError = nil

        defer {
            isLoading = false
        }

        do {
            voices = try await ReadVoiceCatalogClient()
                .listVoices(
                    accessToken: sessionStore.session?.token
                )
        } catch {
            loadError = error.localizedDescription
        }
    }

    static func defaultVoiceId(
        for language: String
    ) -> String {
        let primary = language
            .trimmingCharacters(in: .whitespacesAndNewlines)
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

    private func languageKey(
        _ language: String
    ) -> String {
        let primary = language
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .split(separator: "-")
            .first
            .map(String.init)

        return primary?.isEmpty == false
            ? primary!
            : "auto"
    }

    private func persist() {
        guard
            let data = try? JSONEncoder().encode(
                selectedByLanguage
            )
        else {
            return
        }

        defaults.set(data, forKey: storageKey)
    }
}

private struct ReadVoiceCatalogClient {
    private let baseURL = URL(
        string: "https://flowreader-api.onrender.com"
    )!

    func listVoices(
        accessToken: String?
    ) async throws -> [ReadVoiceOption] {
        var request = URLRequest(
            url: baseURL.appending(
                path: "/api/voices/unified"
            )
        )
        request.httpMethod = "GET"
        request.setValue(
            "application/json",
            forHTTPHeaderField: "Accept"
        )

        if
            let token = accessToken?
                .trimmingCharacters(
                    in: .whitespacesAndNewlines
                ),
            !token.isEmpty
        {
            request.setValue(
                "Bearer \(token)",
                forHTTPHeaderField: "Authorization"
            )
        }

        let (data, response) =
            try await URLSession.shared.data(
                for: request
            )

        guard
            let http = response as? HTTPURLResponse,
            (200..<300).contains(http.statusCode)
        else {
            throw ReadVoiceCatalogError.invalidResponse
        }

        guard
            let root = try JSONSerialization
                .jsonObject(with: data)
                as? [String: Any]
        else {
            throw ReadVoiceCatalogError.invalidResponse
        }

        let rawVoices =
            (root["voices"] as? [[String: Any]])
            ?? (root["available"] as? [[String: Any]])
            ?? []

        let parsed = rawVoices.compactMap {
            value -> ReadVoiceOption? in

            let id = (value["id"] as? String)?
                .trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
                ?? ""

            guard !id.isEmpty else { return nil }

            let name =
                ((value["name"] as? String)
                    ?? (value["voiceName"] as? String)
                    ?? id)
                .trimmingCharacters(
                    in: .whitespacesAndNewlines
                )

            return ReadVoiceOption(
                id: id,
                name: name,
                language:
                    (value["language"] as? String)
                    ?? "",
                locale:
                    (value["locale"] as? String)
                    ?? "",
                gender:
                    value["gender"] as? String,
                accent:
                    value["accent"] as? String
            )
        }

        if parsed.isEmpty {
            return [
                ReadVoiceOption(
                    id: "google:en-US-Neural2-C",
                    name: "English Neural",
                    language: "en",
                    locale: "en-US",
                    gender: nil,
                    accent: nil
                ),
                ReadVoiceOption(
                    id: "azure:fi-FI-SelmaNeural",
                    name: "Selma",
                    language: "fi",
                    locale: "fi-FI",
                    gender: nil,
                    accent: nil
                )
            ]
        }

        return parsed
    }
}

private enum ReadVoiceCatalogError:
    LocalizedError
{
    case invalidResponse

    var errorDescription: String? {
        "Read voice catalog returned an invalid response."
    }
}
