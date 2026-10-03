import Foundation

actor ReadProgressiveAudioCoordinator {
    private let tts: ReadNativeTtsClient
    private let cache: ReadNativeAudioCache

    init(
        tts: ReadNativeTtsClient = ReadNativeTtsClient(),
        cache: ReadNativeAudioCache
    ) {
        self.tts = tts
        self.cache = cache
    }

    func prepare(
        manifest: ReadingManifestV1,
        startingAt index: Int,
        voiceId: String,
        accessToken: String? = nil,
        horizon: TimeInterval = 120,
        maxSegments: Int = 4
    ) async throws -> [ReadPlayableSegment] {
        guard !manifest.segments.isEmpty else { return [] }

        let boundedStart = min(
            max(0, index),
            manifest.segments.count - 1
        )
        let targetHorizon = max(30, horizon)
        let limit = max(1, maxSegments)

        var selected: [ReadingManifestSegmentV1] = []
        var logicalBuffered: TimeInterval = 0

        for segment in manifest.segments.dropFirst(boundedStart) {
            if selected.count >= limit { break }
            selected.append(segment)
            logicalBuffered += TimeInterval(
                max(0, segment.logicalEndMs - segment.logicalStartMs)
            ) / 1_000
            if logicalBuffered >= targetHorizon { break }
        }

        var result: [ReadPlayableSegment] = []
        result.reserveCapacity(selected.count)

        for segment in selected {
            let lookupKey = ReadNativeTtsClient.lookupKey(
                text: segment.text,
                language: manifest.language,
                voiceId: voiceId
            )

            if let cached = try await cache.cachedAsset(
                lookupKey: lookupKey
            ) {
                result.append(
                    ReadPlayableSegment(
                        id: segment.id,
                        index: segment.index,
                        url: cached.localURL,
                        logicalStartTime: TimeInterval(
                            segment.logicalStartMs
                        ) / 1_000,
                        logicalEndTime: TimeInterval(
                            segment.logicalEndMs
                        ) / 1_000,
                        physicalDuration: cached.duration
                    )
                )
                continue
            }

            let asset = try await tts.synthesize(
                text: segment.text,
                language: manifest.language,
                voiceId: voiceId,
                accessToken: accessToken
            )
            let localURL = try await cache.localURL(for: asset)

            result.append(
                ReadPlayableSegment(
                    id: segment.id,
                    index: segment.index,
                    url: localURL,
                    logicalStartTime: TimeInterval(
                        segment.logicalStartMs
                    ) / 1_000,
                    logicalEndTime: TimeInterval(
                        segment.logicalEndMs
                    ) / 1_000,
                    physicalDuration: asset.duration
                )
            )
        }

        return result
    }

    func replaceProtectedSegments(
        _ segments: [ReadPlayableSegment]
    ) async {
        try? await cache.replaceProtectedURLs(
            segments.map(\.url)
        )
    }

    func playableDocument(
        manifest: ReadingManifestV1,
        segments: [ReadPlayableSegment],
        author: String? = nil
    ) -> ReadPlayableDocument {
        ReadPlayableDocument(
            id: manifest.documentId,
            revisionId: manifest.revisionId,
            title: manifest.title,
            author: author,
            estimatedDuration: manifest.estimatedSourceDuration,
            segments: segments.sorted { $0.index < $1.index }
        )
    }
}
