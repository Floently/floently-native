import Foundation
import FloentlyShared

@MainActor
final class ReadDocumentPlaybackLoader: ObservableObject {
    enum State: Equatable {
        case idle
        case preparing
        case ready
        case failed(String)
    }

    @Published private(set) var state: State = .idle

    private let coordinator: ReadProgressiveAudioCoordinator?
    private let resumeStore = ReadPlaybackResumeStore()
    private var task: Task<Void, Never>?

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
        task?.cancel()

        guard let coordinator else {
            state = .failed(
                "Read audio cache could not be initialized."
            )
            return
        }

        let accessToken = sessionStore.session?.token
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
                    accessToken: accessToken
                )

                guard !Task.isCancelled else { return }

                let document = await coordinator.playableDocument(
                    manifest: manifest,
                    segments: segments
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
            } catch is CancellationError {
                return
            } catch {
                state = .failed(error.localizedDescription)
            }
        }
    }

    func cancel() {
        task?.cancel()
        task = nil
        if state == .preparing {
            state = .idle
        }
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
