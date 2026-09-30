import AVFoundation
import Combine
import Foundation
import MediaPlayer

struct ReadPlayableSegment: Identifiable, Equatable {
    let id: String
    let index: Int
    let url: URL
    let logicalStartTime: TimeInterval
    let logicalEndTime: TimeInterval

    var logicalDuration: TimeInterval {
        max(0, logicalEndTime - logicalStartTime)
    }
}

struct ReadPlayableDocument: Equatable {
    let id: String
    let revisionId: String
    let title: String
    let author: String?
    let estimatedDuration: TimeInterval
    let segments: [ReadPlayableSegment]
}

enum ReadPlaybackState: Equatable {
    case idle
    case preparing
    case ready
    case playing
    case paused
    case ended
    case failed(String)
}

@MainActor
final class ReadPlaybackSession: ObservableObject {
    @Published private(set) var document: ReadPlayableDocument?
    @Published private(set) var state: ReadPlaybackState = .idle
    @Published private(set) var elapsedTime: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var bufferedAhead: TimeInterval = 0
    @Published private(set) var activeSegmentIndex: Int?
    @Published var playbackRate: Float = 1.0 {
        didSet {
            playbackRate = min(2.0, max(0.5, playbackRate))
            if state == .playing {
                player.rate = playbackRate
            }
            publishNowPlaying()
        }
    }

    private let player = AVQueuePlayer()
    private var segmentByItemId: [ObjectIdentifier: ReadPlayableSegment] = [:]
    private var timeObserver: Any?
    private var itemEndObserver: NSObjectProtocol?
    private var remoteCommandTargets: [(MPRemoteCommand, Any)] = []

    init() {
        configureAudioSession()
        installTimeObserver()
        installItemEndObserver()
        installRemoteCommands()
    }

    func load(_ document: ReadPlayableDocument, autoplay: Bool = false) {
        self.document = document
        state = .preparing
        elapsedTime = 0
        duration = max(
            document.estimatedDuration,
            document.segments.last?.logicalEndTime ?? 0
        )
        rebuildQueue(startingAt: 0, localOffset: 0)
        state = .ready
        publishNowPlaying()

        if autoplay {
            play()
        }
    }

    func clear() {
        player.pause()
        player.removeAllItems()
        segmentByItemId.removeAll()
        document = nil
        state = .idle
        elapsedTime = 0
        duration = 0
        bufferedAhead = 0
        activeSegmentIndex = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    func play() {
        guard document != nil else { return }
        activateAudioSession()

        if state == .ended {
            seek(to: 0, resumeAfterSeek: false)
        }

        player.playImmediately(atRate: playbackRate)
        state = .playing
        publishNowPlaying()
    }

    func pause() {
        player.pause()
        if document != nil {
            state = .paused
        }
        publishNowPlaying()
    }

    func togglePlayPause() {
        state == .playing ? pause() : play()
    }

    func seek(to logicalTime: TimeInterval, resumeAfterSeek: Bool? = nil) {
        guard let document, !document.segments.isEmpty else { return }

        let boundedTarget = min(max(0, logicalTime), max(duration, 0))
        guard let target = segmentAndOffset(for: boundedTarget, in: document) else { return }

        let shouldResume = resumeAfterSeek ?? (state == .playing)
        player.pause()
        rebuildQueue(startingAt: target.segment.index, localOffset: target.offset)
        elapsedTime = boundedTarget
        state = shouldResume ? .playing : .paused

        if shouldResume {
            activateAudioSession()
            player.playImmediately(atRate: playbackRate)
        }
        publishNowPlaying()
    }

    func seekBy(_ delta: TimeInterval) {
        seek(to: elapsedTime + delta)
    }

    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(
                .playback,
                mode: .spokenAudio,
                options: [.allowAirPlay, .allowBluetoothA2DP]
            )
        } catch {
            state = .failed("Audio session configuration failed: \(error.localizedDescription)")
        }
    }

    private func activateAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            state = .failed("Audio session activation failed: \(error.localizedDescription)")
        }
    }

    private func rebuildQueue(startingAt index: Int, localOffset: TimeInterval) {
        guard let document else { return }

        player.pause()
        player.removeAllItems()
        segmentByItemId.removeAll()

        for segment in document.segments where segment.index >= index {
            let item = AVPlayerItem(url: segment.url)
            segmentByItemId[ObjectIdentifier(item)] = segment
            player.insert(item, after: nil)
        }

        guard localOffset > 0 else {
            updateCurrentLogicalTime()
            return
        }

        let target = CMTime(seconds: localOffset, preferredTimescale: 600)
        player.seek(to: target, toleranceBefore: .zero, toleranceAfter: .zero)
        updateCurrentLogicalTime()
    }

    private func installTimeObserver() {
        let interval = CMTime(seconds: 0.1, preferredTimescale: 600)
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: interval,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                self?.updateCurrentLogicalTime()
            }
        }
    }

    private func installItemEndObserver() {
        itemEndObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                self.updateCurrentLogicalTime()

                if self.player.items().isEmpty {
                    self.elapsedTime = self.duration
                    self.state = .ended
                    self.publishNowPlaying()
                }
            }
        }
    }

    private func updateCurrentLogicalTime() {
        guard
            let currentItem = player.currentItem,
            let segment = segmentByItemId[ObjectIdentifier(currentItem)]
        else {
            if state == .ended {
                elapsedTime = duration
            }
            bufferedAhead = 0
            activeSegmentIndex = nil
            return
        }

        activeSegmentIndex = segment.index
        let local = max(0, currentItem.currentTime().seconds.isFinite
            ? currentItem.currentTime().seconds
            : 0)
        elapsedTime = min(duration, segment.logicalStartTime + local)

        var ahead: TimeInterval = 0
        for item in player.items() {
            guard let queuedSegment = segmentByItemId[ObjectIdentifier(item)] else { continue }
            if queuedSegment.index == segment.index {
                ahead += max(0, queuedSegment.logicalDuration - local)
            } else if queuedSegment.index > segment.index {
                ahead += queuedSegment.logicalDuration
            }
        }
        bufferedAhead = ahead

        if state != .paused && player.rate > 0 {
            state = .playing
        }
        publishNowPlaying()
    }

    private func segmentAndOffset(
        for logicalTime: TimeInterval,
        in document: ReadPlayableDocument
    ) -> (segment: ReadPlayableSegment, offset: TimeInterval)? {
        let bounded = max(0, logicalTime)

        if let segment = document.segments.first(where: {
            bounded < $0.logicalEndTime
        }) {
            return (
                segment,
                min(segment.logicalDuration, max(0, bounded - segment.logicalStartTime))
            )
        }

        guard let last = document.segments.last else { return nil }
        return (last, last.logicalDuration)
    }

    private func installRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.isEnabled = true
        remoteCommandTargets.append((
            center.playCommand,
            center.playCommand.addTarget { [weak self] _ in
                Task { @MainActor in self?.play() }
                return .success
            }
        ))

        center.pauseCommand.isEnabled = true
        remoteCommandTargets.append((
            center.pauseCommand,
            center.pauseCommand.addTarget { [weak self] _ in
                Task { @MainActor in self?.pause() }
                return .success
            }
        ))

        center.changePlaybackPositionCommand.isEnabled = true
        remoteCommandTargets.append((
            center.changePlaybackPositionCommand,
            center.changePlaybackPositionCommand.addTarget { [weak self] event in
                guard let event = event as? MPChangePlaybackPositionCommandEvent else {
                    return .commandFailed
                }
                Task { @MainActor in
                    self?.seek(to: event.positionTime)
                }
                return .success
            }
        ))

        center.skipForwardCommand.isEnabled = true
        center.skipForwardCommand.preferredIntervals = [15]
        remoteCommandTargets.append((
            center.skipForwardCommand,
            center.skipForwardCommand.addTarget { [weak self] _ in
                Task { @MainActor in self?.seekBy(15) }
                return .success
            }
        ))

        center.skipBackwardCommand.isEnabled = true
        center.skipBackwardCommand.preferredIntervals = [15]
        remoteCommandTargets.append((
            center.skipBackwardCommand,
            center.skipBackwardCommand.addTarget { [weak self] _ in
                Task { @MainActor in self?.seekBy(-15) }
                return .success
            }
        ))
    }

    private func publishNowPlaying() {
        guard let document else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: document.title,
            MPMediaItemPropertyPlaybackDuration: max(0, duration),
            MPNowPlayingInfoPropertyElapsedPlaybackTime: min(max(0, elapsedTime), max(0, duration)),
            MPNowPlayingInfoPropertyPlaybackRate: state == .playing ? playbackRate : 0,
            MPNowPlayingInfoPropertyDefaultPlaybackRate: playbackRate
        ]

        if let author = document.author, !author.isEmpty {
            info[MPMediaItemPropertyArtist] = author
        } else {
            info[MPMediaItemPropertyArtist] = "Floently Read"
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}
