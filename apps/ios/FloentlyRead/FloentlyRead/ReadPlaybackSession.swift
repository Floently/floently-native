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
    let physicalDuration: TimeInterval?

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
            playbackRate = min(3.0, max(0.5, playbackRate))
            if state == .playing {
                player.rate = playbackRate
            }
            persistResumeIfNeeded()
            publishNowPlaying()
        }
    }

    private let player = AVQueuePlayer()
    private let resumeStore = ReadPlaybackResumeStore()
    private var segmentByItemId: [ObjectIdentifier: ReadPlayableSegment] = [:]
    private var timeObserver: Any?
    private var itemEndObserver: NSObjectProtocol?
    private var audioSessionObservers: [NSObjectProtocol] = []
    private var remoteCommandTargets: [(MPRemoteCommand, Any)] = []
    private var shouldResumeAfterInterruption = false
    private var lastResumePersistTime: TimeInterval = 0

    init() {
        configureAudioSession()
        installTimeObserver()
        installItemEndObserver()
        installAudioSessionObservers()
        installRemoteCommands()
    }

    func load(_ document: ReadPlayableDocument, autoplay: Bool = false) {
        self.document = document
        state = .preparing
        duration = max(
            document.estimatedDuration,
            document.segments.last?.logicalEndTime ?? 0
        )

        let saved = resumeStore.load(
            documentId: document.id,
            revisionId: document.revisionId
        )
        let requestedCursor = min(
            max(0, saved?.logicalTime ?? 0),
            max(duration, 0)
        )
        elapsedTime = requestedCursor

        if let saved {
            playbackRate = min(3.0, max(0.5, saved.playbackRate))
        }

        if let target = segmentAndOffset(
            for: requestedCursor,
            in: document
        ) {
            rebuildQueue(
                startingAt: target.segment.index,
                localOffset: target.offset
            )
        } else {
            rebuildQueue(startingAt: 0, localOffset: 0)
        }

        state = .ready
        publishNowPlaying()

        if autoplay {
            play()
        }
    }

    func refresh(_ updated: ReadPlayableDocument) {
        guard let current = document else {
            load(updated)
            return
        }

        guard
            current.id == updated.id,
            current.revisionId == updated.revisionId
        else {
            persistResume(force: true)
            load(updated)
            return
        }

        let resume = state == .playing
        let cursor = elapsedTime
        let currentRate = playbackRate
        document = updated
        duration = max(
            updated.estimatedDuration,
            updated.segments.last?.logicalEndTime ?? 0
        )

        guard !updated.segments.isEmpty else {
            player.pause()
            player.removeAllItems()
            segmentByItemId.removeAll()
            bufferedAhead = 0
            activeSegmentIndex = nil
            state = .preparing
            persistResume(force: true)
            publishNowPlaying()
            return
        }

        guard let target = segmentAndOffset(for: cursor, in: updated) else {
            return
        }

        rebuildQueue(
            startingAt: target.segment.index,
            localOffset: target.offset
        )
        elapsedTime = min(cursor, duration)
        playbackRate = currentRate
        state = resume ? .playing : .paused

        if resume {
            activateAudioSession()
            player.playImmediately(atRate: playbackRate)
        }
        persistResume(force: true)
        publishNowPlaying()
    }

    func clear() {
        persistResume(force: true)
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
        persistResume(force: true)
        publishNowPlaying()
    }

    func pause() {
        player.pause()
        if document != nil {
            state = .paused
        }
        persistResume(force: true)
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
        persistResume(force: true)
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

        guard
            let currentItem = player.currentItem,
            let segment = segmentByItemId[ObjectIdentifier(currentItem)]
        else {
            updateCurrentLogicalTime()
            return
        }

        let physicalOffset = physicalOffset(
            forLogicalOffset: localOffset,
            in: segment,
            item: currentItem
        )
        let target = CMTime(seconds: physicalOffset, preferredTimescale: 600)
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
                    let readyEnd = self.document?.segments.last?.logicalEndTime ?? 0
                    if readyEnd + 0.001 < self.duration {
                        self.elapsedTime = min(readyEnd, self.duration)
                        self.bufferedAhead = 0
                        self.activeSegmentIndex = nil
                        self.state = .preparing
                        self.persistResume(force: true)
                    } else {
                        self.elapsedTime = self.duration
                        self.state = .ended
                        if let document = self.document {
                            self.resumeStore.remove(
                                documentId: document.id,
                                revisionId: document.revisionId
                            )
                        }
                    }
                    self.publishNowPlaying()
                }
            }
        }
    }

    private func installAudioSessionObservers() {
        let center = NotificationCenter.default

        audioSessionObservers.append(
            center.addObserver(
                forName: AVAudioSession.interruptionNotification,
                object: AVAudioSession.sharedInstance(),
                queue: .main
            ) { [weak self] notification in
                let rawType =
                    notification.userInfo?[AVAudioSessionInterruptionTypeKey]
                    as? UInt
                let rawOptions =
                    notification.userInfo?[AVAudioSessionInterruptionOptionKey]
                    as? UInt
                    ?? 0

                Task { @MainActor in
                    self?.handleInterruption(
                        rawType: rawType,
                        rawOptions: rawOptions
                    )
                }
            }
        )

        audioSessionObservers.append(
            center.addObserver(
                forName: AVAudioSession.routeChangeNotification,
                object: AVAudioSession.sharedInstance(),
                queue: .main
            ) { [weak self] notification in
                let rawReason =
                    notification.userInfo?[AVAudioSessionRouteChangeReasonKey]
                    as? UInt

                Task { @MainActor in
                    self?.handleRouteChange(rawReason: rawReason)
                }
            }
        )

        audioSessionObservers.append(
            center.addObserver(
                forName: AVAudioSession.mediaServicesWereResetNotification,
                object: AVAudioSession.sharedInstance(),
                queue: .main
            ) { [weak self] _ in
                Task { @MainActor in
                    self?.handleMediaServicesReset()
                }
            }
        )
    }

    private func handleInterruption(
        rawType: UInt?,
        rawOptions: UInt
    ) {
        guard
            let rawType,
            let type = AVAudioSession.InterruptionType(rawValue: rawType)
        else {
            return
        }

        switch type {
        case .began:
            shouldResumeAfterInterruption = state == .playing
            player.pause()
            if document != nil {
                state = .paused
            }
            persistResume(force: true)
            publishNowPlaying()

        case .ended:
            let options = AVAudioSession.InterruptionOptions(
                rawValue: rawOptions
            )
            let resume = shouldResumeAfterInterruption
                && options.contains(.shouldResume)
            shouldResumeAfterInterruption = false

            if resume {
                activateAudioSession()
                player.playImmediately(atRate: playbackRate)
                state = .playing
                publishNowPlaying()
            }

        @unknown default:
            break
        }
    }

    private func handleRouteChange(rawReason: UInt?) {
        guard
            let rawReason,
            let reason = AVAudioSession.RouteChangeReason(
                rawValue: rawReason
            )
        else {
            return
        }

        if reason == .oldDeviceUnavailable, state == .playing {
            pause()
        }
    }

    private func handleMediaServicesReset() {
        guard let document else {
            configureAudioSession()
            return
        }

        let wasPlaying = state == .playing
        let cursor = elapsedTime
        configureAudioSession()

        if let target = segmentAndOffset(for: cursor, in: document) {
            rebuildQueue(
                startingAt: target.segment.index,
                localOffset: target.offset
            )
        }

        elapsedTime = min(cursor, duration)
        state = wasPlaying ? .playing : .paused

        if wasPlaying {
            activateAudioSession()
            player.playImmediately(atRate: playbackRate)
        }

        persistResume(force: true)
        publishNowPlaying()
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
        let physicalLocal = max(0, currentItem.currentTime().seconds.isFinite
            ? currentItem.currentTime().seconds
            : 0)
        let logicalLocal = logicalOffset(
            forPhysicalOffset: physicalLocal,
            in: segment,
            item: currentItem
        )
        elapsedTime = min(duration, segment.logicalStartTime + logicalLocal)

        var ahead: TimeInterval = 0
        for item in player.items() {
            guard let queuedSegment = segmentByItemId[ObjectIdentifier(item)] else { continue }
            if queuedSegment.index == segment.index {
                ahead += max(0, queuedSegment.logicalDuration - logicalLocal)
            } else if queuedSegment.index > segment.index {
                ahead += queuedSegment.logicalDuration
            }
        }
        bufferedAhead = ahead

        if state != .paused && player.rate > 0 {
            state = .playing
        }
        persistResumeIfNeeded()
        publishNowPlaying()
    }

    private func persistResumeIfNeeded() {
        persistResume(force: false)
    }

    private func persistResume(force: Bool) {
        guard let document, state != .ended else { return }

        let now = Date().timeIntervalSince1970
        if !force, now - lastResumePersistTime < 2 {
            return
        }
        lastResumePersistTime = now

        resumeStore.save(
            ReadPlaybackResumeSnapshot(
                documentId: document.id,
                revisionId: document.revisionId,
                logicalTime: min(
                    max(0, elapsedTime),
                    max(0, duration)
                ),
                playbackRate: playbackRate,
                updatedAt: Date()
            )
        )
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

    private func physicalDuration(
        for segment: ReadPlayableSegment,
        item: AVPlayerItem
    ) -> TimeInterval? {
        if let duration = segment.physicalDuration,
           duration.isFinite,
           duration > 0 {
            return duration
        }

        let itemDuration = item.duration.seconds
        if itemDuration.isFinite, itemDuration > 0 {
            return itemDuration
        }

        return nil
    }

    private func logicalOffset(
        forPhysicalOffset physicalOffset: TimeInterval,
        in segment: ReadPlayableSegment,
        item: AVPlayerItem
    ) -> TimeInterval {
        guard
            let physicalDuration = physicalDuration(for: segment, item: item),
            physicalDuration > 0,
            segment.logicalDuration > 0
        else {
            return min(segment.logicalDuration, max(0, physicalOffset))
        }

        let fraction = min(1, max(0, physicalOffset / physicalDuration))
        return fraction * segment.logicalDuration
    }

    private func physicalOffset(
        forLogicalOffset logicalOffset: TimeInterval,
        in segment: ReadPlayableSegment,
        item: AVPlayerItem
    ) -> TimeInterval {
        guard
            let physicalDuration = physicalDuration(for: segment, item: item),
            physicalDuration > 0,
            segment.logicalDuration > 0
        else {
            return min(max(0, logicalOffset), segment.logicalDuration)
        }

        let fraction = min(1, max(0, logicalOffset / segment.logicalDuration))
        return fraction * physicalDuration
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
