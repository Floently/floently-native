package com.floently.learn.roleplay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberUpdatedState
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

private const val RoleplayUiTargetTurns = 5

enum class RoleplayRouteMode {
    Everyday,
    Yki,
    Professional
}

internal enum class RoleplaySpeechPhase {
    Idle,
    Listening,
    Processing,
    Ready,
    Error,
    Unavailable
}

@Composable
fun YkiRoleplayScreen(
    repository: RoleplayRepository,
    copy: LearnCopy,
    onBack: () -> Unit
) {
    RoleplayScreen(
        repository = repository,
        copy = copy,
        onBack = onBack,
        routeMode = RoleplayRouteMode.Yki
    )
}

@Composable
fun RoleplayScreen(
    repository: RoleplayRepository,
    copy: LearnCopy,
    onBack: () -> Unit,
    routeMode: RoleplayRouteMode = RoleplayRouteMode.Everyday
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf(RoleplayLevel.A1_A2) }
    var dashboardState by remember { mutableStateOf<RoleplayDashboardState?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<RoleplaySession?>(null) }
    var autoStartedLevel by remember { mutableStateOf<RoleplayLevel?>(null) }

    fun startScenario(scenario: RoleplayScenario) {
        if (scenario.locked) {
            statusMessage = "This roleplay is locked. Choose a ready topic."
            return
        }
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

    val session = activeSession
    if (session != null) {
        RoleplaySessionScreen(
            session = session,
            repository = repository,
            copy = copy,
            routeMode = routeMode,
            onSessionChange = { activeSession = it },
            onExit = { activeSession = null }
        )
    } else {
        LaunchedEffect(repository, selectedLevel, routeMode) {
            autoStartedLevel = null
            val dashboard = repository.dashboard(selectedLevel)
            dashboardState = dashboard
            statusMessage = dashboard.errorMessage
        }

        LaunchedEffect(dashboardState, selectedLevel, activeSession, routeMode) {
            val dashboard = dashboardState ?: return@LaunchedEffect
            if (activeSession == null && autoStartedLevel != selectedLevel) {
                val scenario = dynamicAutoScenario(dashboard.scenarios)
                if (scenario != null) {
                    autoStartedLevel = selectedLevel
                    startScenario(scenario)
                }
            }
        }

        FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = routeMode.title(),
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = routeMode.subtitle(),
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
                        body = "Loading ${selectedLevel.displayName} generated speaking topics.",
                        palette = palette
                    )
                } else if (dashboard.scenarios.isEmpty()) {
                    RoleplayStatusCard(
                        title = "No conversations yet for ${dashboard.selectedLevel.displayName}",
                        body = "Choose another level band or come back later when generated topics have been added.",
                        palette = palette
                    )
                } else {
                    val recommended = dashboard.scenarios.firstOrNull { !it.locked && it.recommended }
                        ?: dashboard.scenarios.firstOrNull { !it.locked }
                    recommended?.let { scenario ->
                        FloentlyPrimaryButton(
                            title = "Restart recommended ${dashboard.selectedLevel.displayName} roleplay",
                            product = FloentlyProduct.Learn,
                            onClick = { startScenario(scenario) }
                        )
                    }

                    dashboard.scenarios.forEach { scenario ->
                        OldSourceRoleplayScenarioCard(
                            scenario = scenario,
                            palette = palette,
                            actionLabel = if (scenario.locked) "Locked" else "Start roleplay",
                            onClick = { startScenario(scenario) }
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
    routeMode: RoleplayRouteMode,
    onSessionChange: (RoleplaySession) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val concluded = session.learnerTurns >= RoleplayUiTargetTurns
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    var reply by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var recordedText by remember(session.id, session.learnerTurns) { mutableStateOf("") }
    var statusMessage by remember(session.id, session.learnerTurns) { mutableStateOf<String?>(null) }
    var speechPhase by remember(session.id, session.learnerTurns) { mutableStateOf(RoleplaySpeechPhase.Idle) }
    var hasAudioPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var spokenMessageId by remember(session.id) { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator.release() }
    }

    fun playStartMicSound() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
    }

    fun playStopMicSound() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 90)
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        uri?.let {
            writeRoleplayExport(context, it, roleplayPdfBytes(session, routeMode))
            statusMessage = "PDF conversation download saved."
        }
    }
    val wordExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/msword")) { uri: Uri? ->
        uri?.let {
            writeRoleplayExport(context, it, roleplayWordBytes(session, routeMode))
            statusMessage = "Word conversation download saved."
        }
    }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                ttsEngine?.language = Locale.forLanguageTag("fi-FI")
            }
        }
        ttsEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsEngine = null
        }
    }

    LaunchedEffect(ttsReady, ttsEngine) {
        if (ttsReady) {
            ttsEngine?.language = Locale.forLanguageTag("fi-FI")
        }
    }

    LaunchedEffect(ttsReady, session.id, session.messages.size) {
        if (ttsReady) {
            val aiMessage = session.messages.lastOrNull { it.speaker == RoleplaySpeaker.Partner || it.speaker == RoleplaySpeaker.Coach }
            if (aiMessage != null && aiMessage.id != spokenMessageId) {
                spokenMessageId = aiMessage.id
                ttsEngine?.speak(aiMessage.text, TextToSpeech.QUEUE_FLUSH, null, aiMessage.id)
            }
        }
    }

    val speechAvailable = remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(context, speechAvailable) {
        if (speechAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("fi-FI").toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Puhu suomea")
        }
    }

    fun submitReply(rawText: String) {
        val cleanReply = rawText.trim()
        if (cleanReply.isBlank()) {
            statusMessage = "Speak or write a short Finnish answer before sending."
            return
        }
        if (concluded) {
            statusMessage = "Conversation complete. Download the PDF or Word document/book."
            return
        }
        speechPhase = RoleplaySpeechPhase.Processing
        statusMessage = "Sending transcription to the conversation…"
        scope.launch {
            when (val result = repository.sendLearnerMessage(session, cleanReply)) {
                is RoleplaySessionResult.Ready -> {
                    reply = ""
                    recordedText = ""
                    speechPhase = RoleplaySpeechPhase.Idle
                    statusMessage = if (result.session.learnerTurns >= RoleplayUiTargetTurns) {
                        "Conversation complete. Final AI response is ready. Download PDF or Word."
                    } else {
                        null
                    }
                    onSessionChange(result.session)
                }
                is RoleplaySessionResult.Blocked -> {
                    speechPhase = RoleplaySpeechPhase.Ready
                    statusMessage = result.reason
                }
                is RoleplaySessionResult.Error -> {
                    speechPhase = RoleplaySpeechPhase.Error
                    statusMessage = result.message
                }
            }
        }
    }

    val currentSubmitReply by rememberUpdatedState(newValue = { text: String -> submitReply(text) })
    val currentSpeechAvailable by rememberUpdatedState(newValue = speechAvailable)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
        if (granted && speechRecognizer != null && !concluded) {
            recordedText = ""
            speechPhase = RoleplaySpeechPhase.Listening
            statusMessage = null
            ttsEngine?.stop()
            playStartMicSound()
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
                statusMessage = "Listening… press the mic again to stop and send."
            }

            override fun onBeginningOfSpeech() {
                speechPhase = RoleplaySpeechPhase.Listening
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                speechPhase = RoleplaySpeechPhase.Processing
                statusMessage = "Preparing transcription…"
            }

            override fun onError(error: Int) {
                speechPhase = if (currentSpeechAvailable) RoleplaySpeechPhase.Error else RoleplaySpeechPhase.Unavailable
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
                    statusMessage = "Transcribed and sending…"
                    currentSubmitReply(transcript)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.firstSpeechResult()?.let { partial ->
                    recordedText = partial
                    reply = partial
                    statusMessage = "Live transcription: $partial"
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
        if (concluded) {
            statusMessage = "Conversation complete. Download the PDF or Word document/book."
            return
        }
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
        ttsEngine?.stop()
        playStartMicSound()
        speechRecognizer.cancel()
        speechRecognizer.startListening(speechIntent)
    }

    fun stopSpeech() {
        playStopMicSound()
        speechRecognizer?.stopListening()
        speechPhase = RoleplaySpeechPhase.Processing
        statusMessage = "Stopping recording and sending transcription…"
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
                routeMode = routeMode,
                palette = palette,
                onExit = onExit
            )

            OldSourceRoleplaySessionProgressCard(
                turns = minOf(session.learnerTurns, RoleplayUiTargetTurns),
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
                speechAvailable = speechAvailable && !concluded,
                concluded = concluded,
                palette = palette,
                onStart = ::startSpeech,
                onStop = ::stopSpeech,
                onClear = ::clearSpeech
            )

            if (recordedText.isNotBlank() && !concluded) {
                OldSourceRecordedResponseCard(
                    recordedText = recordedText,
                    palette = palette,
                    onUseResponse = { reply = recordedText }
                )
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = if (concluded) "Conversation complete" else "Your response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (concluded) {
                    Text(
                        text = "You completed ${RoleplayUiTargetTurns} user responses. The final AI response concludes the interaction. Download a PDF or Word document/book below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FloentlyPrimaryButton(
                        title = "Download PDF",
                        product = FloentlyProduct.Learn,
                        onClick = { pdfExportLauncher.launch("floently-${routeMode.exportSlug()}-roleplay-${session.id}.pdf") }
                    )
                    FloentlyPrimaryButton(
                        title = "Download Word document",
                        product = FloentlyProduct.Learn,
                        onClick = { wordExportLauncher.launch("floently-${routeMode.exportSlug()}-roleplay-${session.id}.doc") }
                    )
                } else {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        label = { Text("Live transcription / typed fallback") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Old flow: AI speaks automatically, tap mic to speak, tap mic again to stop and send. Start/stop sound feedback is enabled.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FloentlyPrimaryButton(
                        title = "Send typed fallback",
                        product = FloentlyProduct.Learn,
                        onClick = { submitReply(reply) }
                    )
                }
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun dynamicAutoScenario(scenarios: List<RoleplayScenario>): RoleplayScenario? {
    val ready = scenarios.filter { !it.locked }
    if (ready.isEmpty()) return null
    val index = ((System.currentTimeMillis() / 1000L) % ready.size).toInt()
    return ready[index]
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

private fun writeRoleplayExport(context: Context, uri: Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(bytes)
    }
}

private fun roleplayExportLines(session: RoleplaySession, routeMode: RoleplayRouteMode): List<String> = buildList {
    add("Floently ${routeMode.exportTitle()} Conversation")
    add("Topic: ${session.scenario.title}")
    add("Level: ${session.scenario.level.displayName}")
    add("Turns: ${session.learnerTurns}/$RoleplayUiTargetTurns")
    add("")
    session.messages.forEach { message ->
        val speaker = when (message.speaker) {
            RoleplaySpeaker.Learner -> "User"
            RoleplaySpeaker.Partner -> "AI speaker"
            RoleplaySpeaker.Coach -> "Coach"
        }
        add("$speaker: ${message.text}")
        message.coachingNote?.takeIf { it.isNotBlank() }?.let { note -> add("Coach note: $note") }
        add("")
    }
    add("Conclusion: conversation completed after five user responses.")
}

private fun roleplayWordBytes(session: RoleplaySession, routeMode: RoleplayRouteMode): ByteArray {
    val body = roleplayExportLines(session, routeMode).joinToString("\\par\n") { it.rtfEscaped() }
    return "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\fs24 $body}".toByteArray(Charsets.UTF_8)
}

private fun roleplayPdfBytes(session: RoleplaySession, routeMode: RoleplayRouteMode): ByteArray {
    val lines = roleplayExportLines(session, routeMode).flatMap { it.wrapForPdf(82) }.take(52)
    val textCommands = buildString {
        append("BT /F1 10 Tf 40 790 Td 13 TL\n")
        lines.forEach { line -> append("(${line.pdfEscaped()}) Tj T*\n") }
        append("ET")
    }
    val objects = listOf(
        "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
        "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
        "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
        "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
        "5 0 obj\n<< /Length ${textCommands.toByteArray(Charsets.ISO_8859_1).size} >>\nstream\n$textCommands\nendstream\nendobj\n"
    )
    val header = "%PDF-1.4\n"
    val body = StringBuilder(header)
    val offsets = mutableListOf(0)
    objects.forEach { obj ->
        offsets.add(body.toString().toByteArray(Charsets.ISO_8859_1).size)
        body.append(obj)
    }
    val xrefStart = body.toString().toByteArray(Charsets.ISO_8859_1).size
    body.append("xref\n0 ${objects.size + 1}\n")
    body.append("0000000000 65535 f \n")
    offsets.drop(1).forEach { offset -> body.append(offset.toString().padStart(10, '0')).append(" 00000 n \n") }
    body.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF")
    return body.toString().toByteArray(Charsets.ISO_8859_1)
}

private fun String.wrapForPdf(width: Int): List<String> {
    if (length <= width) return listOf(this)
    val words = split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        current = if (current.isBlank()) {
            word
        } else if (current.length + word.length + 1 <= width) {
            "$current $word"
        } else {
            lines.add(current)
            word
        }
    }
    if (current.isNotBlank()) lines.add(current)
    return lines
}

private fun String.pdfEscaped(): String = replace("\\", "\\\\")
    .replace("(", "\\(")
    .replace(")", "\\)")
    .replace(Regex("\\s+"), " ")

private fun String.rtfEscaped(): String = buildString {
    this@rtfEscaped.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '{' -> append("\\{")
            '}' -> append("\\}")
            '\n' -> append("\\par ")
            else -> if (ch.code < 128) append(ch) else append("\\u${ch.code}?")
        }
    }
}

private fun RoleplayRouteMode.title(): String = when (this) {
    RoleplayRouteMode.Everyday -> "Roleplay"
    RoleplayRouteMode.Yki -> "YKI Roleplay"
    RoleplayRouteMode.Professional -> "Professional Roleplay"
}

private fun RoleplayRouteMode.subtitle(): String = when (this) {
    RoleplayRouteMode.Everyday -> "Opening Roleplay starts a generated Finnish conversation automatically. Listen, tap the mic, answer five times, then download the conversation."
    RoleplayRouteMode.Yki -> "YKI speaking route: AI examiner starts automatically, you answer with the mic, and the five-turn conversation concludes with an export."
    RoleplayRouteMode.Professional -> "Professional speaking route: practice workplace Finnish with automatic AI speech, mic transcription, and a downloadable conversation record."
}

private fun RoleplayRouteMode.label(): String = when (this) {
    RoleplayRouteMode.Everyday -> "Everyday roleplay"
    RoleplayRouteMode.Yki -> "YKI roleplay"
    RoleplayRouteMode.Professional -> "Professional roleplay"
}

private fun RoleplayRouteMode.exportTitle(): String = when (this) {
    RoleplayRouteMode.Everyday -> "Roleplay"
    RoleplayRouteMode.Yki -> "YKI Roleplay"
    RoleplayRouteMode.Professional -> "Professional Roleplay"
}

private fun RoleplayRouteMode.exportSlug(): String = when (this) {
    RoleplayRouteMode.Everyday -> "everyday"
    RoleplayRouteMode.Yki -> "yki"
    RoleplayRouteMode.Professional -> "professional"
}
