import Combine
import Foundation

enum ReadReaderTextSize:
    String,
    CaseIterable,
    Identifiable
{
    case compact
    case standard
    case large

    var id: String { rawValue }

    var label: String {
        switch self {
        case .compact:
            return "Compact"
        case .standard:
            return "Default"
        case .large:
            return "Large"
        }
    }

    var fontSize: CGFloat {
        switch self {
        case .compact:
            return 18
        case .standard:
            return 20
        case .large:
            return 22
        }
    }

    var baseLineHeight: CGFloat {
        switch self {
        case .compact:
            return 30
        case .standard:
            return 32
        case .large:
            return 36
        }
    }
}

enum ReadReaderLineRhythm:
    String,
    CaseIterable,
    Identifiable
{
    case tight
    case standard
    case relaxed

    var id: String { rawValue }

    var label: String {
        switch self {
        case .tight:
            return "Tight"
        case .standard:
            return "Standard"
        case .relaxed:
            return "Relaxed"
        }
    }

    var lineHeightAdjustment: CGFloat {
        switch self {
        case .tight:
            return -2
        case .standard:
            return 0
        case .relaxed:
            return 4
        }
    }
}

@MainActor
final class ReadReaderAppearanceSettings:
    ObservableObject
{
    @Published var textSize: ReadReaderTextSize {
        didSet { persist() }
    }

    @Published var lineRhythm:
        ReadReaderLineRhythm
    {
        didSet { persist() }
    }

    private let defaults: UserDefaults
    private let textSizeKey =
        "floently.read.reader.text-size.v1"
    private let lineRhythmKey =
        "floently.read.reader.line-rhythm.v1"

    init(
        defaults: UserDefaults = .standard
    ) {
        self.defaults = defaults
        textSize =
            defaults.string(
                forKey: textSizeKey
            )
            .flatMap(
                ReadReaderTextSize.init(
                    rawValue:
                )
            )
            ?? .standard

        lineRhythm =
            defaults.string(
                forKey: lineRhythmKey
            )
            .flatMap(
                ReadReaderLineRhythm.init(
                    rawValue:
                )
            )
            ?? .standard
    }

    var fontSize: CGFloat {
        textSize.fontSize
    }

    var desiredLineHeight: CGFloat {
        max(
            fontSize + 6,
            textSize.baseLineHeight
                + lineRhythm
                    .lineHeightAdjustment
        )
    }

    /// SwiftUI exposes added line spacing rather than an absolute
    /// line-height. The native system font at these sizes carries roughly
    /// four points of intrinsic leading, so subtract that before adding the
    /// remaining space. This preserves the frozen 18/30, 20/32 and 22/36
    /// default reader rhythms.
    var additionalLineSpacing: CGFloat {
        max(
            0,
            desiredLineHeight
                - fontSize
                - 4
        )
    }

    private func persist() {
        defaults.set(
            textSize.rawValue,
            forKey: textSizeKey
        )
        defaults.set(
            lineRhythm.rawValue,
            forKey: lineRhythmKey
        )
    }
}
