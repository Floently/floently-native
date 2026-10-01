import Foundation

enum ReadingManifestSynthesisStatus: String, Codable, Equatable {
    case pending
    case generating
    case ready
    case failed
}

struct ReadingManifestSynthesisV1: Codable, Equatable {
    let status: ReadingManifestSynthesisStatus
    let requestKey: String?
    let voiceId: String?
    let provider: String?
    let model: String?
    let errorCode: String?
    let errorMessage: String?
}

struct ReadingManifestAudioV1: Codable, Equatable {
    let uri: String
    let durationMs: Int64
    let contentHash: String?
    let cacheKey: String?
    let voiceId: String?
    let provider: String?
    let model: String?
    let format: String?

    var url: URL? {
        URL(string: uri)
    }
}

struct ReadingManifestTimingV1: Codable, Equatable {
    let scalarStart: Int
    let scalarEnd: Int
    let startMs: Int64
    let endMs: Int64
}

struct ReadingManifestSegmentV1: Codable, Equatable, Identifiable {
    let id: String
    let index: Int
    let text: String
    let scalarStart: Int
    let scalarEnd: Int
    let wordStart: Int
    let wordEnd: Int
    let wordCount: Int
    let estimatedSourceDurationMs: Int64
    let logicalStartMs: Int64
    let logicalEndMs: Int64
    let synthesis: ReadingManifestSynthesisV1?
    let audio: ReadingManifestAudioV1?
    let timings: [ReadingManifestTimingV1]?

    var isAudioReady: Bool {
        guard synthesis?.status == .ready else { return false }
        return audio?.url != nil
    }
}

struct ReadingManifestV1: Codable, Equatable {
    let schemaVersion: Int
    let documentId: String
    let revisionId: String
    let title: String
    let language: String
    let wordCount: Int
    let textScalarLength: Int
    let estimatedSourceDurationMs: Int64
    let segments: [ReadingManifestSegmentV1]

    var estimatedSourceDuration: TimeInterval {
        TimeInterval(estimatedSourceDurationMs) / 1_000
    }

    /// Returns only the contiguous ready prefix. Playback must never skip a
    /// pending segment just because a later segment happens to be cached.
    var readyAudioPrefix: [ReadingManifestSegmentV1] {
        Array(segments.prefix { $0.isAudioReady })
    }

    static func decode(data: Data) throws -> ReadingManifestV1 {
        let manifest = try JSONDecoder().decode(ReadingManifestV1.self, from: data)
        try manifest.validate()
        return manifest
    }

    func validate() throws {
        guard schemaVersion == 1 else {
            throw ReadingManifestValidationError.unsupportedSchemaVersion(schemaVersion)
        }
        guard wordCount >= 0, textScalarLength >= 0, estimatedSourceDurationMs >= 0 else {
            throw ReadingManifestValidationError.negativeDocumentValue
        }

        var expectedIndex = 0
        var expectedWordStart = 0
        var expectedLogicalStart: Int64 = 0
        var previousScalarEnd = 0
        var totalWords = 0

        for segment in segments {
            guard segment.index == expectedIndex else {
                throw ReadingManifestValidationError.nonContiguousSegmentIndex(
                    expected: expectedIndex,
                    actual: segment.index
                )
            }

            guard
                segment.scalarStart >= previousScalarEnd,
                segment.scalarStart >= 0,
                segment.scalarEnd > segment.scalarStart,
                segment.scalarEnd <= textScalarLength
            else {
                throw ReadingManifestValidationError.invalidScalarRange(segment.index)
            }

            guard
                segment.wordStart == expectedWordStart,
                segment.wordEnd >= segment.wordStart,
                segment.wordEnd - segment.wordStart == segment.wordCount
            else {
                throw ReadingManifestValidationError.invalidWordRange(segment.index)
            }

            guard
                segment.logicalStartMs == expectedLogicalStart,
                segment.logicalEndMs >= segment.logicalStartMs,
                segment.estimatedSourceDurationMs >= 0
            else {
                throw ReadingManifestValidationError.invalidLogicalRange(segment.index)
            }

            if segment.synthesis?.status == .ready {
                guard segment.audio?.url != nil else {
                    throw ReadingManifestValidationError.readySegmentMissingAudio(segment.index)
                }
            }

            if let audio = segment.audio, audio.durationMs < 0 {
                throw ReadingManifestValidationError.invalidAudioDuration(segment.index)
            }

            var priorTimingEnd: Int64 = 0
            for timing in segment.timings ?? [] {
                guard
                    timing.scalarStart >= segment.scalarStart,
                    timing.scalarEnd > timing.scalarStart,
                    timing.scalarEnd <= segment.scalarEnd,
                    timing.startMs >= priorTimingEnd,
                    timing.endMs >= timing.startMs
                else {
                    throw ReadingManifestValidationError.invalidTiming(segment.index)
                }
                priorTimingEnd = timing.endMs
            }

            expectedIndex += 1
            expectedWordStart = segment.wordEnd
            expectedLogicalStart = segment.logicalEndMs
            previousScalarEnd = segment.scalarEnd
            totalWords += segment.wordCount
        }

        guard totalWords == wordCount else {
            throw ReadingManifestValidationError.wordCountMismatch(
                expected: wordCount,
                actual: totalWords
            )
        }

        guard expectedLogicalStart == estimatedSourceDurationMs else {
            throw ReadingManifestValidationError.durationMismatch(
                expected: estimatedSourceDurationMs,
                actual: expectedLogicalStart
            )
        }
    }
}

enum ReadingManifestValidationError: LocalizedError, Equatable {
    case unsupportedSchemaVersion(Int)
    case negativeDocumentValue
    case nonContiguousSegmentIndex(expected: Int, actual: Int)
    case invalidScalarRange(Int)
    case invalidWordRange(Int)
    case invalidLogicalRange(Int)
    case readySegmentMissingAudio(Int)
    case invalidAudioDuration(Int)
    case invalidTiming(Int)
    case wordCountMismatch(expected: Int, actual: Int)
    case durationMismatch(expected: Int64, actual: Int64)

    var errorDescription: String? {
        switch self {
        case .unsupportedSchemaVersion(let version):
            return "Unsupported ReadingManifest schema version: \(version)"
        case .negativeDocumentValue:
            return "ReadingManifest contains a negative document value."
        case .nonContiguousSegmentIndex(let expected, let actual):
            return "ReadingManifest segment index drift: expected \(expected), got \(actual)."
        case .invalidScalarRange(let index):
            return "ReadingManifest segment \(index) has an invalid canonical scalar range."
        case .invalidWordRange(let index):
            return "ReadingManifest segment \(index) has an invalid word range."
        case .invalidLogicalRange(let index):
            return "ReadingManifest segment \(index) has an invalid logical time range."
        case .readySegmentMissingAudio(let index):
            return "ReadingManifest segment \(index) is ready but has no valid audio URI."
        case .invalidAudioDuration(let index):
            return "ReadingManifest segment \(index) has an invalid audio duration."
        case .invalidTiming(let index):
            return "ReadingManifest segment \(index) has invalid timing metadata."
        case .wordCountMismatch(let expected, let actual):
            return "ReadingManifest word count mismatch: expected \(expected), got \(actual)."
        case .durationMismatch(let expected, let actual):
            return "ReadingManifest duration mismatch: expected \(expected) ms, got \(actual) ms."
        }
    }
}


extension ReadingManifestV1 {
    func playableDocument(
        author: String? = nil,
        localURLForSegment: (ReadingManifestSegmentV1) -> URL?
    ) -> ReadPlayableDocument {
        let playable = readyAudioPrefix.compactMap { segment -> ReadPlayableSegment? in
            guard let url = localURLForSegment(segment) else { return nil }

            return ReadPlayableSegment(
                id: segment.id,
                index: segment.index,
                url: url,
                logicalStartTime: TimeInterval(segment.logicalStartMs) / 1_000,
                logicalEndTime: TimeInterval(segment.logicalEndMs) / 1_000,
                physicalDuration: segment.audio.map {
                    TimeInterval($0.durationMs) / 1_000
                }
            )
        }

        return ReadPlayableDocument(
            id: documentId,
            revisionId: revisionId,
            title: title,
            author: author,
            estimatedDuration: estimatedSourceDuration,
            segments: playable
        )
    }
}
