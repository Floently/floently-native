import SwiftUI
import FloentlyShared

struct ReadFullPlayerView: View {
    @Environment(\.dismiss)
    private var dismiss
    @EnvironmentObject private var playback:
        ReadPlaybackSession
    @EnvironmentObject private var loader:
        ReadDocumentPlaybackLoader
    @EnvironmentObject private var voiceSettings:
        ReadVoiceSettings
    @EnvironmentObject private var sessionStore:
        FloentlySessionStore

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
        ZStack {
            FloentlyDesignTokens
                .Colors
                .canvas
                .ignoresSafeArea()

            if let document = playback.document {
                ScrollView {
                    VStack(
                        spacing:
                            FloentlyDesignTokens
                                .Space
                                .s6
                    ) {
                        topBar

                        artwork

                        VStack(
                            spacing:
                                FloentlyDesignTokens
                                    .Space
                                    .s2
                        ) {
                            Text(document.title)
                                .font(
                                    .system(
                                        size:
                                            FloentlyDesignTokens
                                                .TypeScale
                                                .h2,
                                        weight: .bold
                                    )
                                )
                                .foregroundStyle(
                                    palette.text
                                )
                                .multilineTextAlignment(
                                    .center
                                )
                                .lineLimit(3)

                            Text(statusText)
                                .font(
                                    .system(
                                        size:
                                            FloentlyDesignTokens
                                                .TypeScale
                                                .small,
                                        weight: .regular
                                    )
                                )
                                .foregroundStyle(
                                    palette.muted
                                )
                                .multilineTextAlignment(
                                    .center
                                )
                        }
                        .frame(maxWidth: 560)

                        progressControls

                        transportControls

                        secondaryTools

                        Spacer(minLength: 20)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(
                        .horizontal,
                        FloentlyDesignTokens
                            .Space
                            .s6
                    )
                    .padding(.bottom, 20)
                }
            } else {
                VStack(
                    spacing:
                        FloentlyDesignTokens
                            .Space
                            .s4
                ) {
                    Text("Nothing is playing")
                        .font(
                            .system(
                                size:
                                    FloentlyDesignTokens
                                        .TypeScale
                                        .h3,
                                weight: .bold
                            )
                        )
                        .foregroundStyle(
                            palette.text
                        )

                    Button("Close") {
                        dismiss()
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(
                        FloentlyDesignTokens
                            .Colors
                            .brandBright
                    )
                }
            }
        }
        .task {
            await voiceSettings.refresh(
                sessionStore: sessionStore
            )
        }
    }

    private var topBar: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(
                    systemName: "chevron.down"
                )
                .font(
                    .system(
                        size: 18,
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
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close player")

            Spacer()

            Text("Now listening")
                .font(
                    .system(
                        size:
                            FloentlyDesignTokens
                                .TypeScale
                                .title,
                        weight: .semibold
                    )
                )
                .foregroundStyle(palette.text)

            Spacer()

            Menu {
                Button(
                    role: .destructive
                ) {
                    playback.clear()
                    dismiss()
                } label: {
                    Label(
                        "Stop playback",
                        systemImage: "stop.fill"
                    )
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(
                        .system(
                            size: 18,
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
            }
            .accessibilityLabel("Player options")
        }
        .frame(height: 56)
    }

    private var artwork: some View {
        RoundedRectangle(
            cornerRadius:
                FloentlyDesignTokens
                    .Radius
                    .xl,
            style: .continuous
        )
        .fill(
            LinearGradient(
                colors: [
                    FloentlyDesignTokens
                        .Colors
                        .surface3,
                    FloentlyDesignTokens
                        .Colors
                        .brandTint
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .frame(width: 156, height: 156)
        .overlay {
            Image(systemName: "waveform")
                .font(
                    .system(
                        size: 48,
                        weight: .medium
                    )
                )
                .foregroundStyle(
                    FloentlyDesignTokens
                        .Colors
                        .brandBright
                )
        }
        .overlay {
            RoundedRectangle(
                cornerRadius:
                    FloentlyDesignTokens
                        .Radius
                        .xl,
                style: .continuous
            )
            .stroke(
                FloentlyDesignTokens
                    .Colors
                    .borderSoft,
                lineWidth: 1
            )
        }
        .accessibilityHidden(true)
    }

    private var progressControls: some View {
        VStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s3
        ) {
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

            HStack {
                Text(clock(displayedElapsed))
                Spacer()
                Text(clock(playback.duration))
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(palette.muted)
        }
        .frame(maxWidth: 560)
    }

    private var transportControls: some View {
        HStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s6
        ) {
            transportButton(
                systemName: "gobackward.15",
                size:
                    FloentlyDesignTokens
                        .Control
                        .iconTarget,
                primary: false,
                label: "Back 15 seconds"
            ) {
                playback.seekBy(-15)
            }

            transportButton(
                systemName:
                    isActive
                    ? "pause.fill"
                    : "play.fill",
                size:
                    FloentlyDesignTokens
                        .Control
                        .primaryPlay,
                primary: true,
                label:
                    isActive
                    ? "Pause reading"
                    : "Play reading"
            ) {
                playback.togglePlayPause()
            }

            transportButton(
                systemName: "goforward.15",
                size:
                    FloentlyDesignTokens
                        .Control
                        .iconTarget,
                primary: false,
                label: "Forward 15 seconds"
            ) {
                playback.seekBy(15)
            }
        }
    }

    private var secondaryTools: some View {
        HStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s3
        ) {
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
                                    playback.playbackRate
                                ) - rate
                            ) < 0.001
                        {
                            Label(
                                rateLabel(rate),
                                systemImage:
                                    "checkmark"
                            )
                        } else {
                            Text(
                                rateLabel(rate)
                            )
                        }
                    }
                }
            } label: {
                secondaryToolLabel(
                    text:
                        rateLabel(
                            Double(
                                playback
                                    .playbackRate
                            )
                        ),
                    systemName:
                        "speedometer"
                )
            }
            .accessibilityLabel("Reading speed")

            Menu {
                ForEach(voiceOptions) {
                    voice in
                    Button {
                        voiceSettings.select(
                            voiceId: voice.id,
                            for:
                                loader
                                    .activeLanguage
                        )
                        loader.changeVoice(
                            to: voice.id,
                            playback: playback
                        )
                    } label: {
                        if
                            voice.id
                                == currentVoiceId
                        {
                            Label(
                                voice.name,
                                systemImage:
                                    "checkmark"
                            )
                        } else {
                            Text(voice.name)
                        }
                    }
                }
            } label: {
                secondaryToolLabel(
                    text: currentVoiceName,
                    systemName: "waveform"
                )
            }
            .accessibilityLabel("Reading voice")
        }
        .frame(maxWidth: 560)
    }

    private func secondaryToolLabel(
        text: String,
        systemName: String
    ) -> some View {
        HStack(
            spacing:
                FloentlyDesignTokens
                    .Space
                    .s2
        ) {
            Image(systemName: systemName)
            Text(text)
                .lineLimit(1)
        }
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
        .padding(
            .horizontal,
            FloentlyDesignTokens
                .Space
                .s4
        )
        .frame(
            minWidth: 120,
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

    private func transportButton(
        systemName: String,
        size: CGFloat,
        primary: Bool,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(
                    .system(
                        size:
                            primary
                            ? 24
                            : 19,
                        weight: .bold
                    )
                )
                .foregroundStyle(
                    primary
                    ? FloentlyDesignTokens
                        .Colors
                        .textOnBrand
                    : palette.text
                )
                .frame(
                    width: size,
                    height: size
                )
                .background(
                    primary
                    ? FloentlyDesignTokens
                        .Colors
                        .brand
                    : FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .clipShape(
                    RoundedRectangle(
                        cornerRadius:
                            primary
                            ? FloentlyDesignTokens
                                .Radius
                                .xl
                            : FloentlyDesignTokens
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

    private var currentVoiceName: String {
        voiceOptions.first {
            $0.id == currentVoiceId
        }?.name
        ?? "Voice"
    }

    private var voiceOptions: [ReadVoiceOption] {
        let available =
            voiceSettings.availableVoices(
                for:
                    loader.activeLanguage
            )

        if !available.isEmpty {
            return available
        }

        return [
            ReadVoiceOption(
                id:
                    "google:en-US-Neural2-C",
                name: "English Neural",
                language: "en",
                locale: "en-US",
                gender: nil,
                accent: nil
            ),
            ReadVoiceOption(
                id:
                    "azure:fi-FI-SelmaNeural",
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
        let remaining =
            total % 60

        if hours > 0 {
            return String(
                format:
                    "%d:%02d:%02d",
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
