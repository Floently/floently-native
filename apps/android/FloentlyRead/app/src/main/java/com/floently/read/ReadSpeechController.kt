package com.floently.read

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.UUID

class ReadSpeechController(context: Context) : TextToSpeech.OnInitListener {
    var isReady by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, this)
    private var activeText = ""
    private var currentOffset = 0
    private var utteranceBaseOffset = 0
    private var currentUtteranceId: String? = null
    private var pauseRequested = false

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post {
                    if (utteranceId == currentUtteranceId) {
                        isSpeaking = true
                        isPaused = false
                    }
                }
            }

            override fun onRangeStart(
                utteranceId: String?,
                start: Int,
                end: Int,
                frame: Int
            ) {
                if (utteranceId != currentUtteranceId) return
                main.post {
                    currentOffset = (utteranceBaseOffset + start)
                        .coerceIn(0, activeText.length)
                }
            }

            override fun onDone(utteranceId: String?) {
                main.post {
                    if (utteranceId != currentUtteranceId) return@post
                    isSpeaking = false
                    isPaused = false
                    activeText = ""
                    currentOffset = 0
                    utteranceBaseOffset = 0
                    currentUtteranceId = null
                    pauseRequested = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                finishWithError(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                finishWithError(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post {
                    if (utteranceId != currentUtteranceId) return@post
                    isSpeaking = false
                    if (pauseRequested) {
                        isPaused = true
                        pauseRequested = false
                    }
                }
            }

            private fun finishWithError(utteranceId: String?) {
                main.post {
                    if (utteranceId != currentUtteranceId) return@post
                    isSpeaking = false
                    isPaused = false
                    currentUtteranceId = null
                }
            }
        })
    }

    override fun onInit(status: Int) {
        main.post {
            isReady = status == TextToSpeech.SUCCESS
            if (!isReady) return@post
            tts.language = Locale.getDefault()
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    fun toggle(text: String) {
        val value = text.trim()
        if (value.isEmpty() || !isReady) return

        if (activeText == value && isSpeaking) {
            pauseRequested = true
            tts.stop()
            return
        }

        if (activeText == value && isPaused) {
            isPaused = false
            speakFrom(currentOffset)
            return
        }

        tts.stop()
        activeText = value
        currentOffset = 0
        isPaused = false
        pauseRequested = false
        speakFrom(0)
    }

    fun stop() {
        pauseRequested = false
        tts.stop()
        isSpeaking = false
        isPaused = false
        activeText = ""
        currentOffset = 0
        utteranceBaseOffset = 0
        currentUtteranceId = null
    }

    fun shutdown() {
        stop()
        tts.shutdown()
        isReady = false
    }

    private fun speakFrom(offset: Int) {
        if (!isReady || activeText.isEmpty()) return
        val start = offset.coerceIn(0, activeText.length)
        val remaining = activeText.substring(start).trimStart()
        if (remaining.isEmpty()) {
            stop()
            return
        }
        val skippedWhitespace = activeText.substring(start)
            .indexOfFirst { !it.isWhitespace() }
            .let { if (it < 0) 0 else it }
        utteranceBaseOffset = start + skippedWhitespace
        currentOffset = utteranceBaseOffset
        val id = UUID.randomUUID().toString()
        currentUtteranceId = id
        val result = tts.speak(remaining, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
        if (result == TextToSpeech.ERROR) {
            isSpeaking = false
            isPaused = false
            currentUtteranceId = null
        } else {
            isSpeaking = true
        }
    }
}
