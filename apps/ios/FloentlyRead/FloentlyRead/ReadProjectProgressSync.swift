import Foundation
import SwiftUI
import FloentlyShared

@MainActor
enum ReadRemoteProgressBridge {
    static func apply(
        project: ReadContentProject,
        manifest: ReadingManifestV1,
        voiceSettings: ReadVoiceSettings
    ) {
        guard let progress = project.progress else {
            return
        }

        let store = ReadPlaybackResumeStore()
        let local = store.load(
            documentId: manifest.documentId,
            revisionId: manifest.revisionId
        )
        let remoteDate = parseDate(progress.updatedAt)

        if let local {
            guard
                let remoteDate,
                remoteDate > local.updatedAt
            else {
                return
            }
        }

        let boundedPercent = min(
            100,
            max(0, progress.progressPercent)
        )
        let fallbackLogicalTime = manifest.estimatedSourceDuration
            * (boundedPercent / 100)
        let canonicalScalarOffset = {
            let offset = progress.currentCharacterOffset
            guard offset > 0 || boundedPercent <= 0 else {
                return nil
            }
            return min(
                manifest.textScalarLength,
                max(0, offset)
            )
        }()

        let preferredIndex = progress.currentSegmentIndex
        let scalarSegmentIndex: Int? = {
            guard let offset = canonicalScalarOffset else {
                return nil
            }

            if let index = manifest.segments.firstIndex(where: {
                offset >= $0.scalarStart
                    && offset < $0.scalarEnd
            }) {
                return index
            }

            if offset == manifest.textScalarLength {
                return manifest.segments.indices.last
            }

            return nil
        }()
        let segmentIndex: Int

        if let scalarSegmentIndex {
            segmentIndex = scalarSegmentIndex
        } else if manifest.segments.indices.contains(preferredIndex) {
            segmentIndex = preferredIndex
        } else {
            let cursorMs = Int64(fallbackLogicalTime * 1_000)
            segmentIndex = manifest.segments.firstIndex {
                cursorMs < $0.logicalEndMs
            } ?? manifest.segments.indices.last ?? 0
        }

        let segment = manifest.segments.indices.contains(segmentIndex)
            ? manifest.segments[segmentIndex]
            : nil
        let logicalTime: TimeInterval

        if let canonicalScalarOffset,
           let segment,
           canonicalScalarOffset >= segment.scalarStart,
           canonicalScalarOffset <= segment.scalarEnd
        {
            logicalTime = logicalTimeForScalarOffset(
                segment: segment,
                scalarOffset: canonicalScalarOffset
            )
        } else {
            logicalTime = fallbackLogicalTime
        }

        let rate = Float(
            min(
                3,
                max(
                    0.5,
                    progress.playbackRate
                        ?? Double(local?.playbackRate ?? 1)
                )
            )
        )

        store.save(
            ReadPlaybackResumeSnapshot(
                documentId: manifest.documentId,
                revisionId: manifest.revisionId,
                logicalTime: logicalTime,
                playbackRate: rate,
                updatedAt: remoteDate ?? Date(),
                sourceScalarOffset:
                    canonicalScalarOffset
                        ?? segment?.scalarStart,
                sourceSegmentId: segment?.id,
                sourceSegmentIndex: segment?.index,
                voiceId: progress.voiceId,
                renditionId: nil
            )
        )

        if let voiceId = progress.voiceId?
            .trimmingCharacters(in: .whitespacesAndNewlines),
           !voiceId.isEmpty
        {
            voiceSettings.select(
                voiceId: voiceId,
                for: manifest.language
            )
        }
    }

    static func payload(
        manifest: ReadingManifestV1,
        elapsedTime: TimeInterval,
        duration: TimeInterval,
        voiceId: String?,
        playbackRate: Float
    ) -> (
        currentSegmentIndex: Int,
        currentCharacterOffset: Int,
        progressPercent: Double,
        voiceId: String?,
        playbackRate: Double
    ) {
        let logicalDuration = max(
            duration,
            manifest.estimatedSourceDuration,
            0.001
        )
        let elapsed = min(
            max(0, elapsedTime),
            logicalDuration
        )
        let percent = min(
            100,
            max(0, elapsed / logicalDuration * 100)
        )

        let cursorMs = Int64(elapsed * 1_000)
        let index = manifest.segments.firstIndex {
            cursorMs < $0.logicalEndMs
        } ?? manifest.segments.indices.last ?? 0

        guard manifest.segments.indices.contains(index) else {
            return (
                0,
                0,
                percent,
                voiceId,
                Double(playbackRate)
            )
        }

        let segment = manifest.segments[index]
        let segmentDuration = max(
            1,
            segment.logicalEndMs - segment.logicalStartMs
        )
        let localMs = min(
            max(
                Int64(0),
                cursorMs - segment.logicalStartMs
            ),
            segmentDuration
        )
        let fraction = Double(localMs)
            / Double(segmentDuration)
        let scalarSpan = max(
            0,
            segment.scalarEnd - segment.scalarStart
        )
        let scalarOffset = segment.scalarStart
            + Int((Double(scalarSpan) * fraction).rounded())

        return (
            index,
            min(
                manifest.textScalarLength,
                max(0, scalarOffset)
            ),
            percent,
            voiceId,
            Double(playbackRate)
        )
    }

    private static func logicalTimeForScalarOffset(
        segment: ReadingManifestSegmentV1,
        scalarOffset: Int
    ) -> TimeInterval {
        let scalarSpan = max(
            1,
            segment.scalarEnd - segment.scalarStart
        )
        let localScalar = min(
            scalarSpan,
            max(
                0,
                scalarOffset - segment.scalarStart
            )
        )
        let fraction = Double(localScalar)
            / Double(scalarSpan)
        let logicalSpanMs = max(
            0,
            segment.logicalEndMs - segment.logicalStartMs
        )
        let logicalMs = Double(segment.logicalStartMs)
            + Double(logicalSpanMs) * fraction
        return max(0, logicalMs / 1_000)
    }

    private static func parseDate(
        _ value: String
    ) -> Date? {
        let primary = ISO8601DateFormatter()
        primary.formatOptions = [
            .withInternetDateTime,
            .withFractionalSeconds
        ]

        if let date = primary.date(from: value) {
            return date
        }

        let fallback = ISO8601DateFormatter()
        fallback.formatOptions = [.withInternetDateTime]
        return fallback.date(from: value)
    }
}

struct ReadProjectProgressSyncModifier: ViewModifier {
    let project: ReadContentProject
    let manifest: ReadingManifestV1?

    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var loader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @EnvironmentObject private var projectStore: ReadProjectStore
    @EnvironmentObject private var voiceSettings: ReadVoiceSettings

    @State private var lastSyncedBucket = -1

    func body(content: Content) -> some View {
        content
            .task(id: remoteApplyKey) {
                guard let manifest else { return }
                ReadRemoteProgressBridge.apply(
                    project: project,
                    manifest: manifest,
                    voiceSettings: voiceSettings
                )
            }
            .onChange(of: playback.elapsedTime) { _, value in
                syncIfNeeded(elapsedTime: value)
            }
            .onChange(of: playback.state) { _, state in
                switch state {
                case .paused, .ended:
                    syncNow()
                default:
                    break
                }
            }
            .onDisappear {
                syncNow()
            }
    }

    private var remoteApplyKey: String {
        [
            project.id,
            project.revisionId,
            project.progress?.updatedAt ?? "",
            manifest?.revisionId ?? ""
        ].joined(separator: "::")
    }

    private func syncIfNeeded(
        elapsedTime: TimeInterval
    ) {
        guard elapsedTime > 0 else { return }

        let bucket = Int(elapsedTime / 10)
        guard bucket != lastSyncedBucket else {
            return
        }

        lastSyncedBucket = bucket
        syncNow()
    }

    private func syncNow() {
        guard
            let manifest,
            playback.document?.id == project.id,
            playback.document?.revisionId == project.revisionId,
            let accessToken = sessionStore.session?.token,
            !accessToken.isEmpty
        else {
            return
        }

        let payload = ReadRemoteProgressBridge.payload(
            manifest: manifest,
            elapsedTime: playback.elapsedTime,
            duration: playback.duration,
            voiceId: loader.activeVoiceId,
            playbackRate: playback.playbackRate
        )

        Task {
            await projectStore.syncProgress(
                projectId: project.id,
                currentSegmentIndex:
                    payload.currentSegmentIndex,
                currentCharacterOffset:
                    payload.currentCharacterOffset,
                progressPercent:
                    payload.progressPercent,
                voiceId: payload.voiceId,
                playbackRate: payload.playbackRate,
                accessToken: accessToken
            )
        }
    }
}

extension View {
    func readProjectProgressSync(
        project: ReadContentProject,
        manifest: ReadingManifestV1?
    ) -> some View {
        modifier(
            ReadProjectProgressSyncModifier(
                project: project,
                manifest: manifest
            )
        )
    }
}
