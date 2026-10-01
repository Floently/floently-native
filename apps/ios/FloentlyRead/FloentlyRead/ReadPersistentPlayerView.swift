import Foundation
import SwiftUI
import FloentlyShared

struct ReadPersistentPlayerView: View {
    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var loader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var voiceSettings: ReadVoiceSettings
    @EnvironmentObject private var sessionStore: FloentlySessionStore
    @State private var seekDraft: TimeInterval?

    private let palette = FloentlyPalette.read

    private var displayedElapsed: TimeInterval {
        seekDraft ?? playback.elapsedTime
    }

    private var isActive: Bool {
        switch playback.state {
        case .playing, .preparing:
            return true
        default:
            return false
        }
    }

    var body: some View {
        if let document = playback.document {
            VStack(spacing: 10) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(document.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(palette.text)
                            .lineLimit(1)

                        Text(statusText)
                            .font(.caption)
                            .foregroundStyle(palette.muted)
                            .lineLimit(1)
                    }

                    Spacer(minLength: 8)

                    Button {
                        playback.seekBy(-15)
                    } label: {
                        Image(systemName: "gobackward.15")
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Back 15 seconds")

                    Button {
                        playback.togglePlayPause()
                    } label: {
                        Image(systemName: isActive ? "pause.fill" : "play.fill")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(palette.accent)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(isActive ? "Pause reading" : "Play reading")

                    Button {
                        playback.seekBy(15)
                    } label: {
                        Image(systemName: "goforward.15")
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Forward 15 seconds")

                    Menu {
                        ForEach([0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0], id: \.self) { rate in
                            Button {
                                playback.playbackRate = Float(rate)
                            } label: {
                                if abs(Double(playback.playbackRate) - rate) < 0.001 {
                                    Label(rateLabel(rate), systemImage: "checkmark")
                                } else {
                                    Text(rateLabel(rate))
                                }
                            }
                        }
                    } label: {
                        Text(rateLabel(Double(playback.playbackRate)))
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(palette.text)
                            .frame(minWidth: 44, minHeight: 36)
                            .background(palette.elevated)
                            .clipShape(Capsule())
                    }

                    Menu {
                        ForEach(voiceOptions) { voice in
                            Button {
                                voiceSettings.select(
                                    voiceId: voice.id,
                                    for: loader.activeLanguage
                                )
                                loader.changeVoice(
                                    to: voice.id,
                                    playback: playback
                                )
                            } label: {
                                if voice.id == currentVoiceId {
                                    Label(
                                        voice.name,
                                        systemImage: "checkmark"
                                    )
                                } else {
                                    Text(voice.name)
                                }
                            }
                        }
                    } label: {
                        Image(systemName: "waveform")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(palette.text)
                            .frame(width: 36, height: 36)
                            .background(palette.elevated)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Reading voice")
                }

                HStack(spacing: 8) {
                    Text(clock(displayedElapsed))
                        .frame(width: 48, alignment: .leading)

                    Slider(
                        value: Binding(
                            get: { displayedElapsed },
                            set: { seekDraft = $0 }
                        ),
                        in: 0...max(playback.duration, 1),
                        onEditingChanged: { editing in
                            if !editing, let target = seekDraft {
                                seekDraft = nil
                                playback.seek(to: target)
                            }
                        }
                    )
                    .tint(palette.accent)
                    .accessibilityLabel("Document position")

                    Text(clock(playback.duration))
                        .frame(width: 48, alignment: .trailing)
                }
                .font(.caption.monospacedDigit())
                .foregroundStyle(palette.muted)
            }
            .padding(12)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(palette.border, lineWidth: 1)
            )
            .padding(.horizontal, 10)
            .padding(.bottom, 6)
            .accessibilityElement(children: .contain)
            .task {
                await voiceSettings.refresh(
                    sessionStore: sessionStore
                )
            }
        }
    }

    private var currentVoiceId: String {
        loader.activeVoiceId
            ?? voiceSettings.voiceId(
                for: loader.activeLanguage
            )
    }

    private var voiceOptions: [ReadVoiceOption] {
        let available = voiceSettings.availableVoices(
            for: loader.activeLanguage
        )

        if !available.isEmpty {
            return available
        }

        return [
            ReadVoiceOption(
                id: "google:en-US-Neural2-C",
                name: "English Neural",
                language: "en",
                locale: "en-US",
                gender: nil,
                accent: nil
            ),
            ReadVoiceOption(
                id: "azure:fi-FI-SelmaNeural",
                name: "Selma",
                language: "fi",
                locale: "fi-FI",
                gender: nil,
                accent: nil
            )
        ]
    }

    private var statusText: String {
        switch playback.state {
        case .idle:
            return "Idle"
        case .preparing:
            return playback.bufferedAhead > 0
                ? "Preparing · \(clock(playback.bufferedAhead)) ready ahead"
                : "Preparing audio"
        case .ready:
            return "Ready"
        case .playing:
            return playback.bufferedAhead > 0
                ? "Playing · \(clock(playback.bufferedAhead)) ready ahead"
                : "Playing"
        case .paused:
            return "Paused"
        case .ended:
            return "Finished"
        case .failed(let message):
            return message
        }
    }

    private func rateLabel(_ value: Double) -> String {
        if value.rounded() == value {
            return "\(Int(value))×"
        }
        return String(format: "%.2g×", value)
    }

    private func clock(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds.rounded()))
        let hours = total / 3_600
        let minutes = (total % 3_600) / 60
        let remaining = total % 60

        if hours > 0 {
            return String(
                format: "%d:%02d:%02d",
                hours,
                minutes,
                remaining
            )
        }

        return String(
            format: "%d:%02d",
            minutes,
            remaining
        )
    }
}
