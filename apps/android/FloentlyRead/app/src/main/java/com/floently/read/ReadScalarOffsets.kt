package com.floently.read

/**
 * Converts between Android/Java UTF-16 offsets and ReadingManifest's
 * canonical Unicode-scalar coordinate space.
 */
object ReadScalarOffsets {
    fun scalarCount(text: String): Int =
        text.codePointCount(0, text.length)

    fun utf16Offset(
        text: String,
        scalarOffset: Int
    ): Int {
        val scalarCount = scalarCount(text)
        val bounded = scalarOffset.coerceIn(
            0,
            scalarCount
        )
        return text.offsetByCodePoints(0, bounded)
    }

    fun scalarOffset(
        text: String,
        utf16Offset: Int
    ): Int? {
        if (utf16Offset !in 0..text.length) {
            return null
        }

        if (
            utf16Offset > 0
            && utf16Offset < text.length
            && Character.isHighSurrogate(
                text[utf16Offset - 1]
            )
            && Character.isLowSurrogate(
                text[utf16Offset]
            )
        ) {
            // Do not silently round an offset that splits a Unicode scalar.
            return null
        }

        return text.codePointCount(
            0,
            utf16Offset
        )
    }
}
