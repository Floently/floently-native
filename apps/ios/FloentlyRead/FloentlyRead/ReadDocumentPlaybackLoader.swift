import Foundation
import FloentlyShared

struct ReadProgressiveRefillTelemetry: Equatable {
    var refillAttempts = 0
    var refillSuccesses = 0
    var refillFailures = 0
    var underruns = 0
    var lastFailure: String?
}

@MainActor
final class ReadDocumentPlaybackLoader: ObservableObject {
    enum State: Equatable {
        case idle
        case preparing
        case ready
        case failed(String)
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var refillTelemetry =
        ReadProgressiveRefillTelemetry()
    @Published private(set) var activeVoiceId: String?
    @Published private(set) var activeLanguage = "auto"

    private let coordinator: ReadProgressiveAudioCoordinator?
    private let resumeStore = ReadPlaybackResumeStore()
    private let refillLowWatermark: TimeInterval = 45

    private var task: Task<Void, Never>?
    private var refillTask: Task<Void, Never>?
    private var seekTask: Task<Void, Never>?
    private weak var boundPlayback: ReadPlaybackSession?
    private var preparedSegments: [Int: ReadPlayableSegment] = [:]
    private var activeManifest: ReadingManifestV1?
    private var activeAccessToken: String?
    private var lastUnderrunBoundaryIndex: Int?
    private var nextRefillAllowedAt = Date.distantPast

    init() {
        coordinator = try? ReadProgressiveAudioCoordinator(
            cache: ReadNativeAudioCache()
        )
    }

    func load(
        manifest: ReadingManifestV1,
        voiceId: String,
        sessionStore: FloentlySessionStore,
        playback: ReadPlaybackSession,
        autoplay: Bool = false,
        startingAt index: Int = 0
    ) {
        cancelTasks()
        preparedSegments.removeAll()
        activeManifest = manifest
        activeVoiceId = voiceId
        activeLanguage = manifest.language
        activeAccessToken = sessionStore.session?.token
        lastUnderrunBoundaryIndex = nil
        nextRefillAllowedAt = .distantPast
        refillTelemetry = ReadProgressiveRefillTelemetry()

        bindUnpreparedSeek(
            manifest: manifest,
            voiceId: voiceId,
            accessToken: activeAccessToken,
            playback: playback
        )

        guard let coordinator else {
            state = .failed(
                "Read audio cache could not be initialized."
            )
            return
        }

        let accessToken = activeAccessToken
        let effectiveIndex = resolvedStartingIndex(
            manifest: manifest,
            requestedIndex: index
        )
        state = .preparing

        task = Task { [weak self] in
            guard let self else { return }

            do {
                let segments = try await coordinator.prepare(
                    manifest: manifest,
                    startingAt: effectiveIndex,
                    voiceId: voiceId,
                    accessToken: accessToken,
                    maxSegments: 1
                )

                guard !Task.isCancelled else { return }

                for segment in segments {
                    preparedSegments[segment.index] = segment
                }

                let document = await coordinator.playableDocument(
                    manifest: manifest,
                    segments: preparedSegments.values.map { $0 }
                )

                guard !Task.isCancelled else { return }

                if
                    let current = playback.document,
                    current.id == document.id,
                    current.revisionId == document.revisionId
                {
                    playback.refresh(document)
                    if autoplay, playback.state != .playing {
                        playback.play()
                    }
                } else {
                    playback.load(
                        document,
                        autoplay: autoplay
                    )
                }

                state = .ready
                startRefillLoop(
                    manifest: manifest,
                    voiceId: voiceId,
                    accessToken: accessToken,
                    playback: playback
                )
            } catch is CancellationError {
                return
            } catch {
                state = .failed(error.localizedDescription)
            }
        }
    }

    func changeVoice(
        to voiceId: String,
        playback: ReadPlaybackSession
    ) {
        guard
            let coordinator,
            let manifest = activeManifest,
            !manifest.segments.isEmpty
        else {
            return
        }

        let newVoice = voiceId.trimmingCharacters(
            in: .whitespacesAndNewlines
        )
        guard !newVoice.isEmpty else { return }
        guard newVoice != activeVoiceId else { return }

        let cursor = playback.elapsedTime
        let shouldResume =
            playback.state == .playing
            || playback.state == .preparing
        let previousVoice = activeVoiceId

        cancelTasks()
        preparedSegments.removeAll()
        lastUnderrunBoundaryIndex = nil
        nextRefillAllowedAt = .distantPast
        activeVoiceId = newVoice
        state = .preparing

        playback.beginAudioReplacement(
            at: cursor,
            resumeAfterReady: shouldResume
        )

        bindUnpreparedSeek(
            manifest: manifest,
            voiceId: newVoice,
            accessToken: activeAccessToken,
            playback: playback
        )

        let cursorMs = Int64(max(0, cursor) * 1_000)
        let targetIndex = manifest.segments.firstIndex {
            cursorMs < $0.logicalEndMs
        } ?? manifest.segments.indices.last ?? 0

        task = Task { [weak self, weak playback] in
            guard let self, let playback else { return }

            do {
                let segments = try await coordinator.prepare(
                    manifest: manifest,
                    startingAt: targetIndex,
                    voiceId: newVoice,
                    accessToken: activeAccessToken,
                    maxSegments: 1
                )

                guard !Task.isCancelled else { return }

                for segment in segments {
                    preparedSegments[segment.index] = segment
                }

                let document = await coordinator.playableDocument(
                    manifest: manifest,
                    segments: preparedSegments.values.map { $0 }
                )

                guard !Task.isCancelled else { return }

                playback.refresh(document)
                state = .ready

                startRefillLoop(
                    manifest: manifest,
                    voiceId: newVoice,
                    accessToken: activeAccessToken,
                    playback: playback
                )
            } catch is CancellationError {
                return
            } catch {
                guard
                    let previousVoice,
                    previousVoice != newVoice
                else {
                    state = .failed(error.localizedDescription)
                    return
                }

                activeVoiceId = previousVoice
                preparedSegments.removeAll()

                do {
                    let fallback = try await coordinator.prepare(
                        manifest: manifest,
                        startingAt: targetIndex,
                        voiceId: previousVoice,
                        accessToken: activeAccessToken,
                        maxSegments: 1
                    )

                    guard !Task.isCancelled else { return }

                    for segment in fallback {
                        preparedSegments[segment.index] = segment
                    }

                    let document = await coordinator.playableDocument(
                        manifest: manifest,
                        segments: preparedSegments.values.map { $0 }
                    )

                    guard !Task.isCancelled else { return }

                    playback.refresh(document)
                    state = .ready

                    bindUnpreparedSeek(
                        manifest: manifest,
                        voiceId: previousVoice,
                        accessToken: activeAccessToken,
                        playback: playback
                    )
                    startRefillLoop(
                        manifest: manifest,
                        voiceId: previousVoice,
                        accessToken: activeAccessToken,
                        playback: playback
                    )
                } catch {
                    state = .failed(error.localizedDescription)
                }
            }
        }
    }

    func cancel() {
        cancelTasks()
        preparedSegments.removeAll()
        activeManifest = nil
        activeVoiceId = nil
        activeLanguage = "auto"
        activeAccessToken = nil
        lastUnderrunBoundaryIndex = nil
        nextRefillAllowedAt = .distantPast
        boundPlayback?.onUnpreparedSeek = nil
        boundPlayback = nil

        if state == .preparing {
            state = .idle
        }
    }

    private func bindUnpreparedSeek(
        manifest: ReadingManifestV1,
        voiceId: String,
        accessToken: String?,
        playback: ReadPlaybackSession
    ) {
        if boundPlayback !== playback {
            boundPlayback?.onUnpreparedSeek = nil
        }
        boundPlayback = playback

        playback.onUnpreparedSeek = {
            [weak self, weak playback] logicalTime in
            guard let self, let playback else { return }

            self.loadWindowForUnpreparedSeek(
                logicalTime: logicalTime,
                manifest: manifest,
                voiceId: voiceId,
                accessToken: accessToken,
                playback: playback
            )
        }
    }

    private func loadWindowForUnpreparedSeek(
        logicalTime: TimeInterval,
        manifest: ReadingManifestV1,
        voiceId: String,
        accessToken: String?,
        playback: ReadPlaybackSession
    ) {
        guard let coordinator, !manifest.segments.isEmpty else {
            return
        }

        seekTask?.cancel()
        refillTask?.cancel()

        let cursorMs = Int64(
            max(0, logicalTime) * 1_000
        )
        let targetIndex = manifest.segments.firstIndex {
            cursorMs < $0.logicalEndMs
        } ?? manifest.segments.indices.last ?? 0

        state = .preparing
        refillTelemetry.refillAttempts += 1

        seekTask = Task { [weak self, weak playback] in
            guard let self, let playback else { return }

            do {
                let segments = try await coordinator.prepare(
                    manifest: manifest,
                    startingAt: targetIndex,
                    voiceId: voiceId,
                    accessToken: accessToken,
                    maxSegments: 1
                )

                guard !Task.isCancelled else { return }

                preparedSegments.removeAll()
                for segment in segments {
                    preparedSegments[segment.index] = segment
                }

                let document = await coordinator.playableDocument(
                    manifest: manifest,
                    segments: preparedSegments.values.map { $0 }
                )

                guard !Task.isCancelled else { return }

                playback.refresh(document)
                refillTelemetry.refillSuccesses += 1
                refillTelemetry.lastFailure = nil
                state = .ready
                lastUnderrunBoundaryIndex = nil
                nextRefillAllowedAt = .distantPast

                startRefillLoop(
                    manifest: manifest,
                    voiceId: voiceId,
                    accessToken: accessToken,
                    playback: playback
                )
            } catch is CancellationError {
                return
            } catch {
                refillTelemetry.refillFailures += 1
                refillTelemetry.lastFailure =
                    error.localizedDescription
                state = .failed(error.localizedDescription)
            }
        }
    }

    private func startRefillLoop(
        manifest: ReadingManifestV1,
        voiceId: String,
        accessToken: String?,
        playback: ReadPlaybackSession
    ) {
        refillTask?.cancel()

        refillTask = Task { [weak self, weak playback] in
            guard let self, let playback else { return }

            // The first playable segment is already published. Keep forward
            // refill on the existing periodic scheduler until ordinary queue
            // expansion is append-only and acoustically qualified.
            while !Task.isCancelled {
                do {
                    try await Task.sleep(
                        nanoseconds: 1_000_000_000
                    )
                } catch {
                    return
                }

                guard
                    let current = playback.document,
                    current.id == manifest.documentId,
                    current.revisionId == manifest.revisionId
                else {
                    return
                }

                if playback.state == .ended {
                    return
                }

                await refillIfNeeded(
                    manifest: manifest,
                    voiceId: voiceId,
                    accessToken: accessToken,
                    playback: playback
                )
            }
        }
    }

    private func refillIfNeeded(
        manifest: ReadingManifestV1,
        voiceId: String,
        accessToken: String?,
        playback: ReadPlaybackSession
    ) async {
        guard
            let coordinator,
            !manifest.segments.isEmpty,
            Date() >= nextRefillAllowedAt
        else {
            return
        }

        let highestPrepared = preparedSegments.keys.max() ?? -1
        let nextIndex = highestPrepared + 1

        guard nextIndex < manifest.segments.count else {
            return
        }

        let prefixExhausted =
            playback.state == .preparing
            && playback.elapsedTime + 0.001 < playback.duration

        if prefixExhausted,
           lastUnderrunBoundaryIndex != highestPrepared {
            lastUnderrunBoundaryIndex = highestPrepared
            refillTelemetry.underruns += 1
        }

        guard
            prefixExhausted
            || playback.bufferedAhead <= refillLowWatermark
        else {
            return
        }

        refillTelemetry.refillAttempts += 1

        do {
            let segments = try await coordinator.prepare(
                manifest: manifest,
                startingAt: nextIndex,
                voiceId: voiceId,
                accessToken: accessToken
            )

            guard !Task.isCancelled else { return }

            for segment in segments {
                preparedSegments[segment.index] = segment
            }

            let document = await coordinator.playableDocument(
                manifest: manifest,
                segments: preparedSegments.values.map { $0 }
            )

            guard !Task.isCancelled else { return }

            playback.refresh(document)
            refillTelemetry.refillSuccesses += 1
            refillTelemetry.lastFailure = nil
            nextRefillAllowedAt = .distantPast
        } catch is CancellationError {
            return
        } catch {
            refillTelemetry.refillFailures += 1
            refillTelemetry.lastFailure =
                error.localizedDescription
            nextRefillAllowedAt = Date()
                .addingTimeInterval(5)
        }
    }

    private func cancelTasks() {
        task?.cancel()
        task = nil
        refillTask?.cancel()
        refillTask = nil
        seekTask?.cancel()
        seekTask = nil
    }

    private func resolvedStartingIndex(
        manifest: ReadingManifestV1,
        requestedIndex: Int
    ) -> Int {
        guard
            requestedIndex == 0,
            let snapshot = resumeStore.load(
                documentId: manifest.documentId,
                revisionId: manifest.revisionId
            ),
            !manifest.segments.isEmpty
        else {
            return requestedIndex
        }

        let cursorMs = Int64(
            max(0, snapshot.logicalTime) * 1_000
        )

        return manifest.segments.firstIndex {
            cursorMs < $0.logicalEndMs
        } ?? manifest.segments.indices.last ?? 0
    }
}
