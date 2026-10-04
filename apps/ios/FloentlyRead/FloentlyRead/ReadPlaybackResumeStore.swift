import Foundation

struct ReadPlaybackResumeSnapshot: Codable, Equatable {
    let documentId: String
    let revisionId: String
    let logicalTime: TimeInterval
    let playbackRate: Float
    let updatedAt: Date
    let sourceScalarOffset: Int?
    let sourceSegmentId: String?
    let sourceSegmentIndex: Int?
    let voiceId: String?
    let renditionId: String?
    let sourceAnchorQuote: String?
    let sourceAnchorPrefixContext: String?
    let sourceAnchorSuffixContext: String?
    let sourceAnchorCursorOffset: Int?

    init(
        documentId: String,
        revisionId: String,
        logicalTime: TimeInterval,
        playbackRate: Float,
        updatedAt: Date,
        sourceScalarOffset: Int? = nil,
        sourceSegmentId: String? = nil,
        sourceSegmentIndex: Int? = nil,
        voiceId: String? = nil,
        renditionId: String? = nil,
        sourceAnchorQuote: String? = nil,
        sourceAnchorPrefixContext: String? = nil,
        sourceAnchorSuffixContext: String? = nil,
        sourceAnchorCursorOffset: Int? = nil
    ) {
        self.documentId = documentId
        self.revisionId = revisionId
        self.logicalTime = logicalTime
        self.playbackRate = playbackRate
        self.updatedAt = updatedAt
        self.sourceScalarOffset = sourceScalarOffset
        self.sourceSegmentId = sourceSegmentId
        self.sourceSegmentIndex = sourceSegmentIndex
        self.voiceId = voiceId
        self.renditionId = renditionId
        self.sourceAnchorQuote = sourceAnchorQuote
        self.sourceAnchorPrefixContext =
            sourceAnchorPrefixContext
        self.sourceAnchorSuffixContext =
            sourceAnchorSuffixContext
        self.sourceAnchorCursorOffset =
            sourceAnchorCursorOffset
    }
}

@MainActor
final class ReadPlaybackResumeStore {
    private let defaults: UserDefaults
    private let prefix = "floently.read.resume."

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load(
        documentId: String,
        revisionId: String
    ) -> ReadPlaybackResumeSnapshot? {
        guard
            let data = defaults.data(
                forKey: key(
                    documentId: documentId,
                    revisionId: revisionId
                )
            ),
            let snapshot = try? JSONDecoder().decode(
                ReadPlaybackResumeSnapshot.self,
                from: data
            ),
            snapshot.documentId == documentId,
            snapshot.revisionId == revisionId,
            snapshot.logicalTime.isFinite,
            snapshot.logicalTime >= 0,
            snapshot.playbackRate.isFinite
        else {
            return nil
        }

        return snapshot
    }

    func loadLatest(
        documentId: String,
        excludingRevisionId: String
    ) -> ReadPlaybackResumeSnapshot? {
        let keyPrefix =
            prefix + documentId + "::"

        return defaults.dictionaryRepresentation()
            .compactMap { entry in
                guard
                    entry.key.hasPrefix(keyPrefix),
                    let data = entry.value as? Data,
                    let snapshot =
                        try? JSONDecoder().decode(
                            ReadPlaybackResumeSnapshot.self,
                            from: data
                        ),
                    snapshot.documentId == documentId,
                    snapshot.revisionId
                        != excludingRevisionId,
                    snapshot.logicalTime.isFinite,
                    snapshot.logicalTime >= 0,
                    snapshot.playbackRate.isFinite
                else {
                    return nil
                }

                return snapshot
            }
            .max {
                $0.updatedAt < $1.updatedAt
            }
    }

    func save(_ snapshot: ReadPlaybackResumeSnapshot) {
        guard
            snapshot.logicalTime.isFinite,
            snapshot.logicalTime >= 0,
            snapshot.playbackRate.isFinite
        else {
            return
        }

        // PlaybackSession may still write time/rate-only snapshots. Preserve
        // a previously recorded source anchor until a newer source-aware
        // writer explicitly replaces it.
        let existing = load(
            documentId: snapshot.documentId,
            revisionId: snapshot.revisionId
        )
        let resolved = ReadPlaybackResumeSnapshot(
            documentId: snapshot.documentId,
            revisionId: snapshot.revisionId,
            logicalTime: snapshot.logicalTime,
            playbackRate: snapshot.playbackRate,
            updatedAt: snapshot.updatedAt,
            sourceScalarOffset:
                snapshot.sourceScalarOffset
                ?? existing?.sourceScalarOffset,
            sourceSegmentId:
                snapshot.sourceSegmentId
                ?? existing?.sourceSegmentId,
            sourceSegmentIndex:
                snapshot.sourceSegmentIndex
                ?? existing?.sourceSegmentIndex,
            voiceId: snapshot.voiceId ?? existing?.voiceId,
            renditionId:
                snapshot.renditionId
                ?? existing?.renditionId,
            sourceAnchorQuote:
                snapshot.sourceAnchorQuote
                ?? existing?.sourceAnchorQuote,
            sourceAnchorPrefixContext:
                snapshot.sourceAnchorPrefixContext
                ?? existing?.sourceAnchorPrefixContext,
            sourceAnchorSuffixContext:
                snapshot.sourceAnchorSuffixContext
                ?? existing?.sourceAnchorSuffixContext,
            sourceAnchorCursorOffset:
                snapshot.sourceAnchorCursorOffset
                ?? existing?.sourceAnchorCursorOffset
        )

        guard let data = try? JSONEncoder().encode(resolved) else {
            return
        }

        defaults.set(
            data,
            forKey: key(
                documentId: resolved.documentId,
                revisionId: resolved.revisionId
            )
        )
    }

    func remove(
        documentId: String,
        revisionId: String
    ) {
        defaults.removeObject(
            forKey: key(
                documentId: documentId,
                revisionId: revisionId
            )
        )
    }

    private func key(
        documentId: String,
        revisionId: String
    ) -> String {
        prefix + documentId + "::" + revisionId
    }
}
