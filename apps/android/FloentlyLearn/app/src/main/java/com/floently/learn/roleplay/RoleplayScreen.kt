package com.floently.learn.roleplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import java.util.Locale
import kotlinx.coroutines.launch

internal enum class RoleplaySpeechPhase {
    Idle,
    Listening,
    Processing,
    Ready,
    Error,
    Unavailable
}

@Composable
fun RoleplayScreen(
    repository: RoleplayRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(RoleplayLevel.A1) }
    var dashboardState by remember { mutableStateOf<RoleplayDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<RoleplaySession?>(null) }

    val session = activeSession
    if (session != null) {
        RoleplaySessionScreen(
            session = session,
            repository = repository,
            copy = copy,
            onSessionChange = { activeSession = it },
            onExit = { activeSession = null }
        )
    } else {
        LaunchedEffect(repository, selectedLevel) {
            val dashboard = repository.dashboard(selectedLevel)
            dashboardState = dashboard
            statusMessage = dashboard.errorMessage
        }

        FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Roleplay",
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Practice short, realistic Finnish conversations with speech, transcript review, and AI-backed coaching.",
                    color = palette.muted,
                    style = MaterialTheme.typography.titleMedium
                )

                OldSourceRoleplayRouteHeader(palette = palette)
                RoleplayLevelStrip(
                    selectedLevel = selectedLevel,
                    palette = palette,
                    onSelect = { selectedLevel = it }
                )

                statusMessage?.let { message ->
                    RoleplayStatusCard(
                        title = "Notice",
                        body = message,
                        palette = palette
                    )
                }

                val dashboard = dashboardState
                if (dashboard == null || dashboard.isLoading) {
                    RoleplayStatusCard(
                        title = "Loading conversations…",
                        body = "Loading ${selectedLevel.name} practice scenarios.",
                        palette = palette
                    )
                } else if (dashboard.scenarios.isEmpty()) {
                    RoleplayStatusCard(
                        title = "No conversations yet for ${dashboard.selectedLevel.name}",
                        body = "Choose another level or come back later when new scenarios have been added.",
                        palette = palette
                    )
                } else {
                    dashboard.scenarios.forEach { scenario ->
                        OldSourceRoleplayScenarioCard(
                            scenario = scenario,
                            palette = palette,
                            actionLabel = if (scenario.locked) "Locked" else "Start roleplay",
                            onClick = {
                                scope.launch {
                                    when (val result = repository.startSession(scenario.id)) {
                                        is RoleplaySessionResult.Ready -> {
                                            statusMessage = null
                                            activeSession = result.session
                                        }
                                        is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                        is RoleplaySessionResult.Error -> statusMessage = result.message
                                    }
                                }
                            }
                        )
                    }
                }

                FloentlyPrimaryButton(
                    title = "Back to Learn",
                    product = FloentlyProduct.Learn,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun RoleplaySessionScreen(
    session: RoleplaySession,
    repository: RoleplayRepository,
    copy: LearnCopy,
    onSessionChange: (RoleplaySession) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reply by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var recordedText by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.learnerTurns) { mutableStateOf<String?>(null) }
    var speechPhase by remember(session.id, session.learnerTurns) { mutableStateOf(RoleplaySpeechPhase.Idle) }
    var hasAudioPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(context, speechAvailable) {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("fi", "FI").toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Puhu suomea")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
        if (granted && speechRecognizer != null) {
            recordedText = ""
            speechPhase = RoleplaySpeechPhase.Listening
            statusMessage = null
            speechRecognizer.startListening(speechIntent)
        } else if (!granted) {
            speechPhase = RoleplaySpeechPhase.Error
            statusMessage = "Microphone permission is needed for speaking practice. You can still type your answer."
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speechPhase = RoleplaySpeechPhase.Listening
                statusMessage = "Listening… speak one clear Finnish answer."
            }

            override fun onBeginningOfSpeech() {
                speechPhase = RoleplaySpeechPhase.Listening
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                speechPhase = RoleplaySpeechPhase.Processing
                statusMessage = "Preparing your recorded answer…"
            }

            override fun onError(error: Int) {
                speechPhase = if (speechAvailable) RoleplaySpeechPhase.Error else RoleplaySpeechPhase.Unavailable
                statusMessage = roleplaySpeechErrorMessage(error)
            }

            override fun onResults(results: Bundle?) {
                val transcript = results.firstSpeechResult()
                if (transcript == null) {
                    speechPhase = RoleplaySpeechPhase.Error
                    statusMessage = "I could not hear enough Finnish. Try again or type your answer."
                } else {
                    recordedText = transcript
                    reply = transcript
                    speechPhase = RoleplaySpeechPhase.Ready
                    statusMessage = "Recorded response captured. Review it, edit if needed, then send."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.firstSpeechResult()?.let { partial ->
                    recordedText = partial
                    reply = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startSpeech() {
        if (speechRecognizer == null) {
            speechPhase = RoleplaySpeechPhase.Unavailable
            statusMessage = "Speech recognition is not available on this device. Type your response instead."
            return
        }
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        recordedText = ""
        speechPhase = RoleplaySpeechPhase.Listening
        statusMessage = null
        speechRecognizer.cancel()
        speechRecognizer.startListening(speechIntent)
    }

    fun stopSpeech() {
        speechRecognizer?.stopListening()
        speechPhase = RoleplaySpeechPhase.Processing
        statusMessage = "Preparing your recorded answer…"
    }

    fun clearSpeech() {
        speechRecognizer?.cancel()
        recordedText = ""
        if (speechPhase != RoleplaySpeechPhase.Unavailable) {
            speechPhase = RoleplaySpeechPhase.Idle
        }
        statusMessage = null
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OldSourceRoleplayScenarioHeader(
                scenario = session.scenario,
                palette = palette,
                onExit = onExit
            )

            OldSourceRoleplaySessionProgressCard(
                turns = session.learnerTurns,
                repeatedCueCount = session.repeatedCueCount,
                palette = palette
            )

            session.messages.forEach { message ->
                OldSourceRoleplayTranscriptBubble(
                    message = message,
                    palette = palette
                )
            }

            OldSourceRoleplayMicPanel(
                phase = speechPhase,
                recordedText = recordedText,
                speechAvailable = speechAvailable,
                palette = palette,
                onStart = ::startSpeech,
                onStop = ::stopSpeech,
                onClear = ::clearSpeech
            )

            if (recordedText.isNotBlank()) {
                OldSourceRecordedResponseCard(
                    recordedText = recordedText,
                    palette = palette,
                    onUseResponse = { reply = recordedText }
                )
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Your response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Speak or write in Finnish") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tip: one clear sentence is enough. Example: Kiitos, se sopii hyvin.",
                    style = MaterialTheme.typography.bodySmall
                )
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FloentlyPrimaryButton(
                    title = "Send response",
                    product = FloentlyProduct.Learn,
                    onClick = {
                        val cleanReply = reply.trim()
                        if (cleanReply.isBlank()) {
                            statusMessage = "Speak or write a short Finnish answer before sending."
                        } else {
                            scope.launch {
                                when (val result = repository.sendLearnerMessage(session, cleanReply)) {
                                    is RoleplaySessionResult.Ready -> {
                                        reply = ""
                                        recordedText = ""
                                        speechPhase = RoleplaySpeechPhase.Idle
                                        statusMessage = null
                                        onSessionChange(result.session)
                                    }
                                    is RoleplaySessionResult.Blocked -> statusMessage = result.reason
                                    is RoleplaySessionResult.Error -> statusMessage = result.message
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun Bundle?.firstSpeechResult(): String? = this
    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    ?.firstOrNull()
    ?.trim()
    ?.takeIf { it.isNotBlank() }

private fun roleplaySpeechErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "The microphone could not capture audio. Try again or type your answer."
    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition stopped. Try the mic again or type your answer."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for speaking practice."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a working network connection on this device."
    SpeechRecognizer.ERROR_NO_MATCH -> "I could not hear enough Finnish. Try again or type your answer."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The microphone is already busy. Wait a moment and try again."
    SpeechRecognizer.ERROR_SERVER -> "Speech recognition service is temporarily unavailable. Type your answer for now."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected. Tap the mic and answer with one clear Finnish sentence."
    else -> "Speech recognition stopped. Try again or type your answer."
}
