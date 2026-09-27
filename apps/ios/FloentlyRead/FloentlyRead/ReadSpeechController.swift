import AVFoundation
import Combine
import Foundation
import NaturalLanguage

@MainActor
final class ReadSpeechController: NSObject, ObservableObject {
    @Published private(set) var isSpeaking = false
    @Published private(set) var isPaused = false

    private let synthesizer = AVSpeechSynthesizer()
    private var activeText = ""

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func toggle(text: String) {
        let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return }

        if activeText == value && synthesizer.isPaused {
            synthesizer.continueSpeaking()
            isSpeaking = true
            isPaused = false
            return
        }

        if activeText == value && synthesizer.isSpeaking {
            if synthesizer.pauseSpeaking(at: .word) {
                isSpeaking = false
                isPaused = true
            }
            return
        }

        stop()
        activeText = value

        let utterance = AVSpeechUtterance(string: value)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.pitchMultiplier = 1.0

        let recognizer = NLLanguageRecognizer()
        recognizer.processString(String(value.prefix(4000)))
        if let language = recognizer.dominantLanguage?.rawValue,
           let voice = AVSpeechSynthesisVoice(language: language) {
            utterance.voice = voice
        }

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try session.setActive(true, options: [])
        } catch {
            // AVSpeechSynthesizer can still attempt playback with the current
            // audio-session configuration. Do not make Reader text unusable.
        }

        synthesizer.speak(utterance)
        isSpeaking = true
        isPaused = false
    }

    func stop() {
        if synthesizer.isSpeaking || synthesizer.isPaused {
            synthesizer.stopSpeaking(at: .immediate)
        }
        isSpeaking = false
        isPaused = false
        activeText = ""
    }
}

extension ReadSpeechController: AVSpeechSynthesizerDelegate {
    nonisolated func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didFinish utterance: AVSpeechUtterance
    ) {
        Task { @MainActor [weak self] in
            self?.isSpeaking = false
            self?.isPaused = false
            self?.activeText = ""
        }
    }

    nonisolated func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didCancel utterance: AVSpeechUtterance
    ) {
        Task { @MainActor [weak self] in
            self?.isSpeaking = false
            self?.isPaused = false
            self?.activeText = ""
        }
    }
}
