import Foundation

/// Converts between platform string indexes and ReadingManifest's canonical
/// Unicode-scalar coordinate space.
///
/// ReadingManifest offsets are Unicode scalar counts. They are not Swift
/// grapheme indexes and not UTF-16 offsets from WebKit/JavaScript APIs.
enum ReadScalarOffsets {
    static func scalarCount(_ text: String) -> Int {
        text.unicodeScalars.count
    }

    static func stringIndex(
        in text: String,
        scalarOffset: Int
    ) -> String.Index {
        let scalars = text.unicodeScalars
        let bounded = min(
            max(0, scalarOffset),
            scalars.count
        )

        return scalars.index(
            scalars.startIndex,
            offsetBy: bounded
        )
    }

    static func utf16Offset(
        in text: String,
        scalarOffset: Int
    ) -> Int {
        let index = stringIndex(
            in: text,
            scalarOffset: scalarOffset
        )
        return text.utf16.distance(
            from: text.utf16.startIndex,
            to: index
        )
    }

    static func scalarOffset(
        in text: String,
        utf16Offset: Int
    ) -> Int? {
        guard
            utf16Offset >= 0,
            utf16Offset <= text.utf16.count
        else {
            return nil
        }

        let utf16Index = text.utf16.index(
            text.utf16.startIndex,
            offsetBy: utf16Offset
        )
        guard
            let stringIndex = utf16Index.samePosition(in: text),
            let scalarIndex = stringIndex.samePosition(
                in: text.unicodeScalars
            )
        else {
            // A UTF-16 offset in the middle of a surrogate pair is not a
            // canonical source position and must not be rounded silently.
            return nil
        }

        return text.unicodeScalars.distance(
            from: text.unicodeScalars.startIndex,
            to: scalarIndex
        )
    }
}
