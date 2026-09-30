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
        let resumeSeconds = playback.storedResumeTime(
            documentId: manifest.documentId,
            revisionId: manifest.revisionId
        )
        let resumeMilliseconds = Int64(
            max(0, resumeSeconds * 1_000)
        )
        let resumeIndex = manifest.segments.first(where: {
            resumeMilliseconds < $0.logicalEndMs
        })?.index ?? manifest.segments.last?.index ?? 0
        let effectiveStart = max(index, resumeIndex)

        state = .preparing

        task = Task { [weak self] in
            guard let self else { return }

            do {
                let segments = try await coordinator.prepare(
                    manifest: manifest,
                    startingAt: effectiveStart,
                    voiceId: voiceId,
                    accessToken: accessToken
                )

                guard !Task.isCancelled else { return }

                let document = await coordinator.playableDocument(
                    manifest: manifest,
                    segments: segments
                )

                guard !Task.isCancelled else { return }

                playback.load(
                    document,
                    autoplay: autoplay
                )
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
}
