package com.floently.learn.audio

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import java.util.Locale

@Composable
fun NativeTtsButton(
    text: String,
    label: String = "Listen",
    languageTag: String = "fi-FI",
    modifier: Modifier = Modifier
) {
    val speechText = text.trim()
    if (speechText.isBlank()) return

    val context = LocalContext.current.applicationContext
    var engine by remember(languageTag) { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember(languageTag) { mutableStateOf(false) }
    var statusMessage by remember(languageTag) { mutableStateOf<String?>(null) }

    DisposableEffect(context, languageTag) {
        var localEngine: TextToSpeech? = null

        localEngine = TextToSpeech(context) { initStatus ->
            if (initStatus == TextToSpeech.SUCCESS) {
                val locale = Locale.forLanguageTag(languageTag)
                val languageStatus = localEngine?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                val supported = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
                    languageStatus != TextToSpeech.LANG_NOT_SUPPORTED

                isReady = supported
                statusMessage = if (supported) {
                    null
                } else {
                    "Finnish speech voice is not installed on this device."
                }
            } else {
                isReady = false
                statusMessage = "Speech engine is not ready on this device."
            }
        }

        engine = localEngine

        onDispose {
            localEngine?.stop()
            localEngine?.shutdown()
            engine = null
            isReady = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FloentlyPrimaryButton(
            title = if (isReady) label else "$label loading",
            product = FloentlyProduct.Learn,
            onClick = {
                val activeEngine = engine
                if (!isReady || activeEngine == null) {
                    statusMessage = "Speech engine is still loading."
                } else {
                    val result = activeEngine.speak(
                        speechText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "floently-tts-${System.nanoTime()}"
                    )
                    statusMessage = if (result == TextToSpeech.ERROR) {
                        "Speech could not start on this device."
                    } else {
                        null
                    }
                }
            }
        )

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
