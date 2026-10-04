import Foundation
import SwiftUI
import FloentlyShared

struct ReadPersistentPlayerView: View {
    @Environment(\.accessibilityReduceMotion)
    private var reduceMotion
    @EnvironmentObject private var playback: ReadPlaybackSession
    @EnvironmentObject private var loader: ReadDocumentPlaybackLoader
    @EnvironmentObject private var voiceSettings: ReadVoiceSettings
    @EnvironmentObject private var sessionStore: FloentlySessionStore

    @State private var seekDraft: TimeInterval?
    @State private var toolsExpanded = false

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
            VStack(spacing: 0) {
                ProgressView(
                    value:
                        playback.duration > 0
                        ? min(
                            1,
                            max(
                                0,
                                playback.elapsedTime
                                    / playback.duration
                            )
                        )
                        : 0
                )
                .progressViewStyle(.linear)
                .tint(
                    FloentlyDesignTokens
                        .Colors
                        .brand
                )
                .frame(height: 2)

                HStack(
                    spacing:
                        FloentlyDesignTokens
                            .Space
                            .s3
                ) {
                    sourceGlyph

                    VStack(
                        alignment: .leading,
                        spacing:
                            FloentlyDesignTokens
                                .Space
                                .s1
                    ) {
                        Text(document.title)
                            .font(
                                .system(
                                    size:
                                        FloentlyDesignTokens
                                            .TypeScale
                                            .small,
                                    weight: .semibold
                                )
                            )
                            .foregroundStyle(palette.text)
                            .lineLimit(1)

                        Text(statusText)
                            .font(
                                .system(
                                    size:
                                        FloentlyDesignTokens
                                            .TypeScale
                                            .caption,
                                    weight: .regular
                                )
                            )
                            .foregroundStyle(palette.muted)
                            .lineLimit(1)
                    }

                    Spacer(minLength: 4)

                    Button {
                        playback.togglePlayPause()
                    } label: {
                        Image(
                            systemName:
                                isActive
                                ? "pause.fill"
                                : "play.fill"
                        )
                        .font(
                            .system(
                                size: 18,
                                weight: .bold
                            )
                        )
                        .foregroundStyle(
                            FloentlyDesignTokens
                                .Colors
                                .textOnBrand
                        )
                        .frame(
                            width:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget,
                            height:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget
                        )
                        .background(
                            FloentlyDesignTokens
                                .Colors
                                .brand
                        )
                        .clipShape(
                            RoundedRectangle(
                                cornerRadius:
                                    FloentlyDesignTokens
                                        .Radius
                                        .m,
                                style: .continuous
                            )
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(
                        isActive
                        ? "Pause reading"
                        : "Play reading"
                    )

                    Button {
                        toolsExpanded.toggle()
                    } label: {
                        Image(
                            systemName:
                                toolsExpanded
                                ? "chevron.down"
                                : "ellipsis"
                        )
                        .font(
                            .system(
                                size: 17,
                                weight: .semibold
                            )
                        )
                        .foregroundStyle(palette.text)
                        .frame(
                            width:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget,
                            height:
                                FloentlyDesignTokens
                                    .Control
                                    .iconTarget
                        )
                        .background(
                            FloentlyDesignTokens
                                .Colors
                                .surface1
                        )
                        .clipShape(
                            RoundedRectangle(
                                cornerRadius:
                                    FloentlyDesignTokens
                                        .Radius
                                        .m,
                                style: .continuous
                            )
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(
                        toolsExpanded
                        ? "Hide player tools"
                        : "Show player tools"
                    )
                }
                .padding(
                    .horizontal,
                    FloentlyDesignTokens
                        .Space
                        .s3
                )
                .frame(height: 70)

                if toolsExpanded {
                    expandedTools
                        .transition(
                            .opacity.combined(
                                with: .move(edge: .bottom)
                            )
                        )
                }
            }
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface2
            )
            .clipShape(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .l,
                    style: .continuous
                )
            )
            .overlay {
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .l,
                    style: .continuous
                )
                .stroke(
                    FloentlyDesignTokens
                        .Colors
                        .borderSoft,
                    lineWidth: 1
                )
            }
            .padding(
                .horizontal,
                FloentlyDesignTokens
                    .Space
                    .s3
            )
            .padding(.bottom, 6)
            .accessibilityElement(
                children: .contain
            )
            .animation(
                reduceMotion
                ? nil
                : .easeInOut(
                    duration:
                        FloentlyDesignTokens
                            .Motion
                            .standard
                ),
                value: toolsExpanded
            )
            .task {
                await voiceSettings.refresh(
                    sessionStore: sessionStore
                )
            }
        }
    }

    private var sourceGlyph: some View {
        RoundedRectangle(
            cornerRadius:
                FloentlyDesignTokens
                    .Radius
                    .m,
            style: .continuous
        )
        .fill(
            FloentlyDesignTokens
                .Colors
                .surface1
        )
        .frame(
            width:
                FloentlyDesignTokens
                    .Control
                    .compactHeight,
            height:
                FloentlyDesignTokens
                    .Control
                    .compactHeight
        )
        .overlay {
            Image(systemName: "waveform")
                .font(
                    .system(
                        size: 17,
                        weight: .semibold
                    )
                )
                .foregroundStyle(
                    FloentlyDesignTokens
                        .Colors
                        .brandBright
                )
        }
        .accessibilityHidden(true)
    }

    private var expandedTools: some View {
        VStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s3
        ) {
            HStack(
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s2
            ) {
                playerToolButton(
                    systemName: "gobackward.15",
                    label: "Back 15 seconds"
                ) {
                    playback.seekBy(-15)
                }

                speedMenu

                voiceMenu

                playerToolButton(
                    systemName: "goforward.15",
                    label: "Forward 15 seconds"
                ) {
                    playback.seekBy(15)
                }
            }
            .frame(maxWidth: .infinity)

            HStack(
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s2
            ) {
                Text(clock(displayedElapsed))
                    .frame(
                        width: 48,
                        alignment: .leading
                    )

                Slider(
                    value: Binding(
                        get: {
                            displayedElapsed
                        },
                        set: {
                            seekDraft = $0
                        }
                    ),
                    in: 0...max(
                        playback.duration,
                        1
                    ),
                    onEditingChanged: {
                        editing in
                        if
                            !editing,
                            let target = seekDraft
                        {
                            seekDraft = nil
                            playback.seek(
                                to: target
                            )
                        }
                    }
                )
                .tint(
                    FloentlyDesignTokens
                        .Colors
                        .brand
                )
                .accessibilityLabel(
                    "Document position"
                )

                Text(clock(playback.duration))
                    .frame(
                        width: 48,
                        alignment: .trailing
                    )
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(palette.muted)
        }
        .padding(
            .horizontal,
            FloentlyDesignTokens
                .Space
                .s3
        )
        .padding(
            .bottom,
            FloentlyDesignTokens
                .Space
                .s3
        )
    }

    private var speedMenu: some View {
        Menu {
            ForEach(
                [
                    0.75,
                    1.0,
                    1.25,
                    1.5,
                    2.0,
                    2.5,
                    3.0
                ],
                id: \.self
            ) {
                rate in
                Button {
                    playback.playbackRate =
                        Float(rate)
                } label: {
                    if
                        abs(
                            Double(
                                playback
                                    .playbackRate
                            ) - rate
                        ) < 0.001
                    {
                        Label(
                            rateLabel(rate),
                            systemImage: "checkmark"
                        )
                    } else {
                        Text(
                            rateLabel(rate)
                        )
                    }
                }
            }
        } label: {
            Text(
                rateLabel(
                    Double(
                        playback.playbackRate
                    )
                )
            )
            .font(
                .caption.weight(
                    .semibold
                )
            )
            .foregroundStyle(palette.text)
            .frame(
                minWidth:
                    FloentlyDesignTokens
                        .Control
                        .iconTarget,
                minHeight:
                    FloentlyDesignTokens
                        .Control
                        .iconTarget
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface1
            )
            .clipShape(
                RoundedRectangle(
                    cornerRadius:
                        FloentlyDesignTokens
                            .Radius
                            .m,
                    style: .continuous
                )
            )
        }
        .accessibilityLabel("Reading speed")
    }

    private var voiceMenu: some View {
        Menu {
            ForEach(voiceOptions) {
                voice in
                Button {
                    voiceSettings.select(
                        voiceId: voice.id,
                        for:
                            loader.activeLanguage
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
                .font(
                    .system(
                        size: 15,
                        weight: .semibold
                    )
                )
                .foregroundStyle(palette.text)
                .frame(
                    width:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget,
                    height:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget
                )
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens
                                .Radius
                                .m,
                        style: .continuous
                    )
                )
        }
        .accessibilityLabel("Reading voice")
    }

    private func playerToolButton(
        systemName: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(
                    .system(
                        size: 17,
                        weight: .semibold
                    )
                )
                .foregroundStyle(palette.text)
                .frame(
                    width:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget,
                    height:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget
                )
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            FloentlyDesignTokens
                                .Radius
                                .m,
                        style: .continuous
                    )
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var currentVoiceId: String {
        loader.activeVoiceId
            ?? voiceSettings.voiceId(
                for: loader.activeLanguage
            )
    }

    private var voiceOptions: [ReadVoiceOption] {
        let available =
            voiceSettings.availableVoices(
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
                ? "Preparing · "
                    + clock(
                        playback.bufferedAhead
                    )
                    + " ready ahead"
                : "Preparing audio"
        case .ready:
            return "Ready"
        case .playing:
            return playback.bufferedAhead > 0
                ? "Playing · "
                    + clock(
                        playback.bufferedAhead
                    )
                    + " ready ahead"
                : "Playing"
        case .paused:
            return "Paused"
        case .ended:
            return "Finished"
        case .failed(let message):
            return message
        }
    }

    private func rateLabel(
        _ value: Double
    ) -> String {
        if value.rounded() == value {
            return "\(Int(value))×"
        }
        return String(
            format: "%.2g×",
            value
        )
    }

    private func clock(
        _ seconds: TimeInterval
    ) -> String {
        let total = max(
            0,
            Int(seconds.rounded())
        )
        let hours = total / 3_600
        let minutes =
            (total % 3_600) / 60
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
