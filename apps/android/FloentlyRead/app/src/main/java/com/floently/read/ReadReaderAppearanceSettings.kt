package com.floently.read

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ReadReaderTextSize(
    val storageValue: String,
    val label: String,
    val fontSizeSp: Float,
    val baseLineHeightSp: Float
) {
    Compact(
        storageValue = "compact",
        label = "Compact",
        fontSizeSp = 18f,
        baseLineHeightSp = 30f
    ),
    Standard(
        storageValue = "standard",
        label = "Default",
        fontSizeSp = 20f,
        baseLineHeightSp = 32f
    ),
    Large(
        storageValue = "large",
        label = "Large",
        fontSizeSp = 22f,
        baseLineHeightSp = 36f
    );

    companion object {
        fun fromStorage(
            value: String?
        ): ReadReaderTextSize =
            entries.firstOrNull {
                it.storageValue == value
            } ?: Standard
    }
}

enum class ReadReaderLineRhythm(
    val storageValue: String,
    val label: String,
    val lineHeightAdjustmentSp: Float
) {
    Tight(
        storageValue = "tight",
        label = "Tight",
        lineHeightAdjustmentSp = -2f
    ),
    Standard(
        storageValue = "standard",
        label = "Standard",
        lineHeightAdjustmentSp = 0f
    ),
    Relaxed(
        storageValue = "relaxed",
        label = "Relaxed",
        lineHeightAdjustmentSp = 4f
    );

    companion object {
        fun fromStorage(
            value: String?
        ): ReadReaderLineRhythm =
            entries.firstOrNull {
                it.storageValue == value
            } ?: Standard
    }
}

class ReadReaderAppearanceSettings(
    context: Context
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                "floently_read_reader_appearance",
                Context.MODE_PRIVATE
            )

    var textSize by mutableStateOf(
        ReadReaderTextSize.fromStorage(
            preferences.getString(
                "text_size_v1",
                null
            )
        )
    )
        private set

    var lineRhythm by mutableStateOf(
        ReadReaderLineRhythm.fromStorage(
            preferences.getString(
                "line_rhythm_v1",
                null
            )
        )
    )
        private set

    val fontSizeSp: Float
        get() = textSize.fontSizeSp

    val lineHeightSp: Float
        get() = maxOf(
            textSize.fontSizeSp + 6f,
            textSize.baseLineHeightSp
                + lineRhythm
                    .lineHeightAdjustmentSp
        )

    fun selectTextSize(
        value: ReadReaderTextSize
    ) {
        if (textSize == value) {
            return
        }

        textSize = value
        preferences.edit()
            .putString(
                "text_size_v1",
                value.storageValue
            )
            .apply()
    }

    fun selectLineRhythm(
        value: ReadReaderLineRhythm
    ) {
        if (lineRhythm == value) {
            return
        }

        lineRhythm = value
        preferences.edit()
            .putString(
                "line_rhythm_v1",
                value.storageValue
            )
            .apply()
    }
}
