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

struct ReadPlaybackSourceAnchor {
    let scalarOffset: Int
    let segmentId: String
    let segmentIndex: Int
    let quote: String
    let prefixContext: String
    let suffixContext: String
    let cursorOffset: Int
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

    func loadOrMigrate(
        manifest: ReadingManifestV1
    ) -> ReadPlaybackResumeSnapshot? {
        if
            let current = load(
                documentId:
                    manifest.documentId,
                revisionId:
                    manifest.revisionId
            )
        {
            return current
        }

        guard
            let previous = loadLatest(
                documentId:
                    manifest.documentId,
                excludingRevisionId:
                    manifest.revisionId
            ),
            let quote =
                previous.sourceAnchorQuote,
            !quote.isEmpty,
            let cursorOffset =
                previous
                    .sourceAnchorCursorOffset,
            cursorOffset >= 0
        else {
            return nil
        }

        let prefix =
            previous
                .sourceAnchorPrefixContext
            ?? ""
        let suffix =
            previous
                .sourceAnchorSuffixContext
            ?? ""
        var matches:
            [(
                segment:
                    ReadingManifestSegmentV1,
                scalarOffset: Int
            )] = []

        for segment in manifest.segments {
            let resolution:
                ReadSourceAnchorResolution?

            do {
                resolution =
                    try ReadCoreNative
                        .resolveSourceAnchor(
                            sourceText:
                                segment.text,
                            quote: quote,
                            prefixContext:
                                prefix,
                            suffixContext:
                                suffix
                        )
            } catch {
                resolution = nil
            }

            guard
                let resolution
            else {
                continue
            }

            let localCursor =
                resolution.scalarStart
                + cursorOffset
            guard
                localCursor >= 0,
                localCursor
                    <= segment.text
                        .unicodeScalars
                        .count
            else {
                continue
            }

            matches.append(
                (
                    segment,
                    min(
                        segment.scalarEnd,
                        segment.scalarStart
                            + localCursor
                    )
                )
            )

            if matches.count > 1 {
                return nil
            }
        }

        guard
            let match = matches.first
        else {
            return nil
        }

        let logicalTime =
            logicalTime(
                for:
                    match.scalarOffset,
                in: match.segment
            )
        let quoteLength =
            quote.unicodeScalars.count
        let localQuoteStart =
            max(
                0,
                match.scalarOffset
                    - cursorOffset
                    - match.segment
                        .scalarStart
            )
        let localQuoteEnd =
            min(
                match.segment.text
                    .unicodeScalars
                    .count,
                localQuoteStart
                    + quoteLength
            )
        let migratedPrefix =
            substring(
                match.segment.text,
                scalarStart:
                    max(
                        0,
                        localQuoteStart - 32
                    ),
                scalarEnd:
                    localQuoteStart
            )
        let migratedSuffix =
            substring(
                match.segment.text,
                scalarStart:
                    localQuoteEnd,
                scalarEnd:
                    min(
                        match.segment.text
                            .unicodeScalars
                            .count,
                        localQuoteEnd + 32
                    )
            )
        let migrated =
            ReadPlaybackResumeSnapshot(
                documentId:
                    manifest.documentId,
                revisionId:
                    manifest.revisionId,
                logicalTime:
                    logicalTime,
                playbackRate:
                    previous.playbackRate,
                updatedAt:
                    previous.updatedAt,
                sourceScalarOffset:
                    match.scalarOffset,
                sourceSegmentId:
                    match.segment.id,
                sourceSegmentIndex:
                    match.segment.index,
                voiceId:
                    previous.voiceId,
                renditionId:
                    previous.renditionId,
                sourceAnchorQuote:
                    quote,
                sourceAnchorPrefixContext:
                    migratedPrefix,
                sourceAnchorSuffixContext:
                    migratedSuffix,
                sourceAnchorCursorOffset:
                    cursorOffset
            )

        save(migrated)
        return migrated
    }

    func sourceAnchor(
        manifest: ReadingManifestV1,
        logicalTime: TimeInterval
    ) -> ReadPlaybackSourceAnchor? {
        guard
            !manifest.segments.isEmpty
        else {
            return nil
        }

        let cursorMs =
            Int64(
                max(
                    0,
                    logicalTime
                ) * 1_000
            )
        guard
            let segment =
                manifest.segments.first(
                    where: {
                        cursorMs
                            < $0.logicalEndMs
                    }
                )
                ?? manifest.segments.last
        else {
            return nil
        }

        let scalarSpan =
            max(
                0,
                segment.scalarEnd
                    - segment.scalarStart
            )
        let logicalSpan =
            max(
                Int64(1),
                segment.logicalEndMs
                    - segment.logicalStartMs
            )
        let localMs =
            min(
                logicalSpan,
                max(
                    Int64(0),
                    cursorMs
                        - segment
                            .logicalStartMs
                )
            )
        let fraction =
            Double(localMs)
            / Double(logicalSpan)
        let localScalar =
            min(
                scalarSpan,
                max(
                    0,
                    Int(
                        (
                            Double(
                                scalarSpan
                            )
                            * fraction
                        )
                        .rounded()
                    )
                )
            )
        let scalarOffset =
            min(
                segment.scalarEnd,
                segment.scalarStart
                    + localScalar
            )
        let segmentScalarCount =
            segment.text
                .unicodeScalars
                .count

        guard
            segmentScalarCount > 0
        else {
            return nil
        }

        let localCursor =
            min(
                segmentScalarCount,
                max(
                    0,
                    scalarOffset
                        - segment
                            .scalarStart
                )
            )
        var quoteStart =
            max(
                0,
                localCursor - 24
            )
        var quoteEnd =
            min(
                segmentScalarCount,
                quoteStart + 64
            )
        quoteStart =
            max(
                0,
                quoteEnd - 64
            )
        quoteEnd =
            min(
                segmentScalarCount,
                quoteStart + 64
            )

        guard quoteEnd > quoteStart else {
            return nil
        }

        let prefixStart =
            max(
                0,
                quoteStart - 32
            )
        let suffixEnd =
            min(
                segmentScalarCount,
                quoteEnd + 32
            )

        return ReadPlaybackSourceAnchor(
            scalarOffset:
                scalarOffset,
            segmentId:
                segment.id,
            segmentIndex:
                segment.index,
            quote:
                substring(
                    segment.text,
                    scalarStart:
                        quoteStart,
                    scalarEnd:
                        quoteEnd
                ),
            prefixContext:
                substring(
                    segment.text,
                    scalarStart:
                        prefixStart,
                    scalarEnd:
                        quoteStart
                ),
            suffixContext:
                substring(
                    segment.text,
                    scalarStart:
                        quoteEnd,
                    scalarEnd:
                        suffixEnd
                ),
            cursorOffset:
                localCursor
                    - quoteStart
        )
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

    private func logicalTime(
        for scalarOffset: Int,
        in segment: ReadingManifestSegmentV1
    ) -> TimeInterval {
        let scalarSpan =
            max(
                1,
                segment.scalarEnd
                    - segment.scalarStart
            )
        let localScalar =
            min(
                scalarSpan,
                max(
                    0,
                    scalarOffset
                        - segment.scalarStart
                )
            )
        let fraction =
            Double(localScalar)
            / Double(scalarSpan)
        let logicalSpan =
            max(
                Int64(0),
                segment.logicalEndMs
                    - segment.logicalStartMs
            )
        let logicalMs =
            Double(
                segment.logicalStartMs
            )
            + Double(logicalSpan)
                * fraction

        return max(
            0,
            logicalMs / 1_000
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

    private func key(
        documentId: String,
        revisionId: String
    ) -> String {
        prefix + documentId + "::" + revisionId
    }
}
