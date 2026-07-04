package com.floently.learn.yki

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import java.util.Locale
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay

private val mockBankBg = Color(0xFFF7F8FC)
private val mockBankCard = Color.White
private val mockBankBorder = Color(0xFFDCE2EE)
private val mockBankText = Color(0xFF172033)
private val mockBankMuted = Color(0xFF69748A)
private val mockBankBlue = Color(0xFF3B63FF)
private val mockBankPurple = Color(0xFF7B61FF)
private val mockBankGreen = Color(0xFF18A05E)
private val mockBankRed = Color(0xFFD94255)
private val mockBankOrange = Color(0xFFFF9F1C)

private enum class MockWritingState {
    Draft,
    Saved
}

@Composable
fun YkiMockExamExactScreen(
    onBack: () -> Unit
) {
    val tasks = remember { YkiMockExamBank.tasks() }
    var index by remember { mutableIntStateOf(0) }
    val selected = remember { mutableStateMapOf<Int, Int>() }
    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val writingAnswers = remember { mutableStateMapOf<Int, String>() }
    val writingStates = remember { mutableStateMapOf<Int, MockWritingState>() }
    val recordingPaths = remember { mutableStateMapOf<Int, String>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mockBankBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            MockTopBar(onBack)

            if (index >= tasks.size) {
                MockResultsScreen(
                    tasks = tasks,
                    selectedAnswers = selected.toMap(),
                    checkedAnswers = checked.toMap(),
                    writingAnswers = writingAnswers.toMap(),
                    recordingPaths = recordingPaths.toMap(),
                    onRestart = {
                        index = 0
                        selected.clear()
                        checked.clear()
                        writingAnswers.clear()
                        writingStates.clear()
                        recordingPaths.clear()
                    },
                    onBack = {
                        index = (tasks.lastIndex).coerceAtLeast(0)
                    }
                )
            } else {
                val task = tasks[index]
                when (task.phase) {
                    YkiMockPhase.Overview -> MockOverview(
                        task = task,
                        onStart = { index = (index + 1).coerceAtMost(tasks.size) }
                    )
                    YkiMockPhase.Choice,
                    YkiMockPhase.Writing,
                    YkiMockPhase.ListeningTimer -> MockTask(
                        task = task,
                        selectedIndex = selected[index],
                        isChecked = checked[index] == true,
                        writingValue = writingAnswers[index].orEmpty(),
                        writingState = writingStates[index] ?: MockWritingState.Draft,
                        onSelect = { selected[index] = it },
                        onCheck = { checked[index] = true },
                        onWritingChange = { value ->
                            writingAnswers[index] = value
                            writingStates[index] = MockWritingState.Draft
                        },
                        onSaveWriting = {
                            writingStates[index] = MockWritingState.Saved
                        },
                        onPrevious = { index = (index - 1).coerceAtLeast(0) },
                        onNext = { index = (index + 1).coerceAtMost(tasks.size) }
                    )
                    YkiMockPhase.PreparationTimer,
                    YkiMockPhase.RecordingTimer -> MockTimedTask(
                        task = task,
                        isSent = recordingPaths[index].orEmpty().isNotBlank(),
                        onSent = { path ->
                            if (!path.isNullOrBlank()) recordingPaths[index] = path
                        },
                        onPrevious = { index = (index - 1).coerceAtLeast(0) },
                        onNext = { index = (index + 1).coerceAtMost(tasks.size) }
                    )
                    YkiMockPhase.Submitted,
                    YkiMockPhase.Results -> {
                        index = tasks.size
                    }
                }

                Text(
                    text = "${task.screenshotLabel} mapped from YKI Mock screenshot state",
                    color = mockBankMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MockTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MockPill("Back", mockBankBlue, onBack)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "YKI Mock Exam",
            color = mockBankText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MockOverview(
    task: YkiMockExamTask,
    onStart: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = mockBankCard,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, mockBankBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = "FULL YKI EXAM",
                    color = mockBankPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.4.sp
                )
                Text(
                    text = task.title,
                    color = mockBankText,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = task.prompt,
                    color = mockBankMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )
            }
        }

        MockSectionCard("Reading comprehension", "Read passages and answer questions.", "18 min")
        MockSectionCard("Writing", "Write messages and structured answers.", "20 min")
        MockSectionCard("Listening", "Timed listening and answer states.", "12 min")
        MockSectionCard("Speaking", "Timed preparation and automatic recording.", "15 min")

        MockPrimaryButton(
            text = if (task.screenshots.contains("IMG_0434")) "Start YKI exam" else "Continue overview",
            color = mockBankBlue,
            onClick = onStart
        )
    }
}

@Composable
private fun MockSectionCard(
    title: String,
    body: String,
    time: String
) {
    Surface(
        color = mockBankCard,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, mockBankBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(title, color = mockBankText, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(body, color = mockBankMuted, fontSize = 15.sp, lineHeight = 21.sp)
            }
            Surface(
                color = Color(0xFFEAF1FF),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = time,
                    color = mockBankBlue,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MockTask(
    task: YkiMockExamTask,
    selectedIndex: Int?,
    isChecked: Boolean,
    writingValue: String,
    writingState: MockWritingState,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onWritingChange: (String) -> Unit,
    onSaveWriting: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MockProgressDots(task.skill)

        Surface(
            color = mockBankCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, mockBankBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = task.section.uppercase(),
                    color = mockBankPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.8.sp
                )

                Text(
                    text = task.title,
                    color = mockBankText,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black
                )

                if (task.phase == YkiMockPhase.ListeningTimer) {
                    MockStaticTimer(task = task)
                    MockListeningAudioButton(task = task)
                }

                if (task.passage.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF4F6FB),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, mockBankBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = task.passage,
                            color = mockBankText,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.padding(15.dp)
                        )
                    }
                }

                Text(
                    text = task.prompt,
                    color = mockBankMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )

                if (task.phase == YkiMockPhase.Writing) {
                    OutlinedTextField(
                        value = writingValue,
                        onValueChange = onWritingChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 190.dp),
                        textStyle = TextStyle(
                            color = mockBankText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Default
                        ),
                        singleLine = false,
                        minLines = 8,
                        shape = RoundedCornerShape(18.dp)
                    )
                }

                task.options.forEachIndexed { i, option ->
                    MockOption(
                        text = option,
                        selected = selectedIndex == i,
                        checked = isChecked,
                        correct = task.correctIndex == i,
                        onClick = { onSelect(i) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MockGhostButton("Previous", onPrevious)
                    Spacer(modifier = Modifier.weight(1f))
                    when {
                        task.options.isNotEmpty() -> {
                            MockActionButton(
                                text = if (task.phase == YkiMockPhase.ListeningTimer) "Save listening answer" else "Continue",
                                color = if (selectedIndex == null) Color(0xFFB9C5E7) else mockBankBlue,
                                onClick = { if (selectedIndex != null) onNext() }
                            )
                        }
                        task.phase == YkiMockPhase.Writing && writingState == MockWritingState.Draft -> {
                            MockActionButton("Save answer", mockBankOrange, onSaveWriting)
                        }
                        task.phase == YkiMockPhase.Writing && writingState == MockWritingState.Saved -> {
                            MockActionButton("Continue to next section", mockBankBlue, onNext)
                        }
                        else -> {
                            MockActionButton("Next question", mockBankBlue, onNext)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockTimedTask(
    task: YkiMockExamTask,
    isSent: Boolean,
    onSent: (String?) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var remaining by remember(task.bankTaskId) { mutableIntStateOf(task.timerSeconds) }
    var recorder by remember(task.bankTaskId) { mutableStateOf<MediaRecorder?>(null) }
    var recordingStarted by remember(task.bankTaskId) { mutableStateOf(false) }
    var savedPath by remember(task.bankTaskId) { mutableStateOf<String?>(null) }
    var error by remember(task.bankTaskId) { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            error = "Microphone permission is required for recording tasks."
        }
    }

    fun startRecording() {
        if (!hasPermission || recordingStarted || isSent) return

        val output = File(context.cacheDir, "yki-mock-${task.bankTaskId}-${System.currentTimeMillis()}.m4a")
        val nextRecorder = newMockMediaRecorder(context)
        try {
            nextRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            nextRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            nextRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            nextRecorder.setOutputFile(output.absolutePath)
            nextRecorder.prepare()
            nextRecorder.start()
            recorder = nextRecorder
            savedPath = output.absolutePath
            recordingStarted = true
            error = null
        } catch (e: Exception) {
            nextRecorder.release()
            error = "Recording could not start."
        }
    }

    fun saveRecording() {
        val active = recorder
        if (active != null) {
            try {
                active.stop()
            } catch (_: RuntimeException) {
                error = "Recording was too short to save."
            } finally {
                active.release()
                recorder = null
            }
        }
        onSent(savedPath)
    }

    LaunchedEffect(task.bankTaskId, remaining, hasPermission, isSent) {
        when {
            task.phase == YkiMockPhase.PreparationTimer && remaining > 0 -> {
                delay(1000)
                remaining -= 1
            }
            task.phase == YkiMockPhase.PreparationTimer && remaining <= 0 -> {
                onNext()
            }
            task.phase == YkiMockPhase.RecordingTimer && hasPermission && !recordingStarted && !isSent -> {
                startRecording()
            }
            task.phase == YkiMockPhase.RecordingTimer && remaining > 0 && !isSent -> {
                delay(1000)
                remaining -= 1
            }
            task.phase == YkiMockPhase.RecordingTimer && remaining <= 0 && !isSent -> {
                saveRecording()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: RuntimeException) {
            } finally {
                recorder?.release()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MockProgressDots(task.skill)

        Surface(
            color = mockBankCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, mockBankBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = task.section.uppercase(),
                    color = mockBankPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.8.sp
                )

                Text(
                    text = task.title,
                    color = mockBankText,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Surface(
                    color = if (task.phase == YkiMockPhase.RecordingTimer && !isSent) Color(0xFFFFEEF0) else Color(0xFFF4F6FB),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.dp,
                        if (task.phase == YkiMockPhase.RecordingTimer && !isSent) mockBankRed.copy(alpha = 0.45f) else mockBankBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "00:${remaining.coerceAtLeast(0).toString().padStart(2, '0')}",
                            color = if (task.phase == YkiMockPhase.RecordingTimer && !isSent) mockBankRed else mockBankText,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = when {
                                isSent -> "Submitted"
                                task.phase == YkiMockPhase.PreparationTimer -> "Prepare your answer"
                                task.phase == YkiMockPhase.RecordingTimer -> "Recording"
                                else -> "Timed task"
                            },
                            color = if (task.phase == YkiMockPhase.RecordingTimer && !isSent) mockBankRed else mockBankMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = when {
                        error != null -> error.orEmpty()
                        isSent -> "Answer sent. Continue to the next state."
                        task.phase == YkiMockPhase.PreparationTimer -> "Preparation runs automatically. The next state starts when the timer ends."
                        task.phase == YkiMockPhase.RecordingTimer && !hasPermission -> "Allow microphone permission to start the automatic recording."
                        task.phase == YkiMockPhase.RecordingTimer -> "Recording runs automatically and is saved when the timer ends."
                        else -> task.prompt
                    },
                    color = if (error != null) mockBankRed else mockBankMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MockGhostButton("Previous", onPrevious)
                    Spacer(modifier = Modifier.weight(1f))

                    when {
                        task.phase == YkiMockPhase.RecordingTimer && !hasPermission -> {
                            MockActionButton("Allow microphone", mockBankOrange) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        task.phase == YkiMockPhase.RecordingTimer && !isSent -> {
                            MockActionButton("Save answer", mockBankRed) {
                                saveRecording()
                            }
                        }
                        isSent -> {
                            MockActionButton(
                                text = if (task.finalSubmit || task.screenshots.contains("IMG_0477")) "Submit exam" else "Next question",
                                color = mockBankGreen,
                                onClick = onNext
                            )
                        }
                        else -> {
                            MockActionButton("Timer running", Color(0xFFB9C5E7)) {}
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MockListeningAudioButton(task: YkiMockExamTask) {
    val context = LocalContext.current
    var ready by remember(task.bankTaskId) { mutableStateOf(false) }
    var playing by remember(task.bankTaskId) { mutableStateOf(false) }
    val engineHolder = remember(task.bankTaskId) { arrayOfNulls<TextToSpeech>(1) }

    DisposableEffect(task.bankTaskId) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engineHolder[0]?.language = Locale("fi", "FI")
                ready = true
            }
        }
        engineHolder[0] = engine

        onDispose {
            engine.stop()
            engine.shutdown()
            playing = false
        }
    }

    MockPrimaryButton(
        text = when {
            playing -> "Stop audio"
            ready -> "Play listening audio"
            else -> "Preparing audio..."
        },
        color = if (playing) mockBankRed else mockBankBlue,
        onClick = {
            val engine = engineHolder[0]
            if (engine != null && ready) {
                if (playing) {
                    engine.stop()
                    playing = false
                } else {
                    engine.speak(
                        task.audioScript.ifBlank { task.prompt },
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "yki-mock-${task.bankTaskId}"
                    )
                    playing = true
                }
            }
        }
    )
}

@Composable
private fun MockStaticTimer(task: YkiMockExamTask) {
    var remaining by remember(task.bankTaskId) { mutableIntStateOf(task.timerSeconds) }

    LaunchedEffect(task.bankTaskId, remaining) {
        if (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    Surface(
        color = Color(0xFFF4F6FB),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, mockBankBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "00:${remaining.coerceAtLeast(0).toString().padStart(2, '0')}",
                color = mockBankText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Timed answer",
                color = mockBankMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun MockOption(
    text: String,
    selected: Boolean,
    checked: Boolean,
    correct: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFEAF1FF) else Color.White
    val stroke = if (selected) mockBankBlue else mockBankBorder

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, stroke),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) mockBankBlue else Color(0xFFE7EBF3)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selected) "•" else "",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = mockBankText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MockResultsScreen(
    tasks: List<YkiMockExamTask>,
    selectedAnswers: Map<Int, Int>,
    checkedAnswers: Map<Int, Boolean>,
    writingAnswers: Map<Int, String>,
    recordingPaths: Map<Int, String>,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val report = remember(tasks, selectedAnswers, checkedAnswers, writingAnswers, recordingPaths) {
        buildMockEvaluationReport(tasks, selectedAnswers, checkedAnswers, writingAnswers, recordingPaths)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MockProgressDots(YkiMockSkill.Results)

        Surface(
            color = mockBankCard,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, mockBankBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "RESULTS",
                    color = mockBankPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.4.sp
                )
                Text(
                    text = "YKI Mock Exam complete",
                    color = mockBankText,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black
                )

                Surface(
                    color = Color(0xFFEAF8F1),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, mockBankGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("B1/B2 readiness", color = mockBankGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(report.scoreLine.substringAfter(": ").ifBlank { "Review" }, color = mockBankText, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    }
                }

                Text(
                    text = report.summary,
                    color = mockBankMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                MockPrimaryButton("Download PDF evaluation", mockBankBlue) {
                    YkiPracticeEvaluationExporter.share(context, report, YkiPracticeExportFormat.Pdf)
                }

                MockPrimaryButton("Download Word evaluation", mockBankOrange) {
                    YkiPracticeEvaluationExporter.share(context, report, YkiPracticeExportFormat.Word)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MockGhostButton("Previous", onBack)
                    Spacer(modifier = Modifier.weight(1f))
                    MockActionButton("Restart", mockBankPurple, onRestart)
                }

                Text(
                    text = "IMG_0478 and IMG_0479 mapped: results overview and share/export state.",
                    color = mockBankMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun buildMockEvaluationReport(
    tasks: List<YkiMockExamTask>,
    selectedAnswers: Map<Int, Int>,
    checkedAnswers: Map<Int, Boolean>,
    writingAnswers: Map<Int, String>,
    recordingPaths: Map<Int, String>
): YkiPracticeEvaluationReport {
    val choiceTasks = tasks.withIndex().filter { (_, task) -> task.options.isNotEmpty() && task.correctIndex >= 0 }
    val writingTasks = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Writing }
    val speakingTasks = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Speaking && task.phase == YkiMockPhase.RecordingTimer }

    val correct = choiceTasks.count { (index, task) ->
        selectedAnswers[index] == task.correctIndex
    }
    val writingSubmitted = writingTasks.count { (index, _) -> writingAnswers[index].orEmpty().trim().isNotBlank() }
    val speakingSubmitted = speakingTasks.count { (index, _) -> recordingPaths[index].orEmpty().isNotBlank() }

    val percent = if (choiceTasks.isEmpty()) 0 else ((correct.toDouble() / choiceTasks.size.toDouble()) * 100).toInt()

    val lines = mutableListOf<String>()
    lines += "YKI Mock Exam feedback"
    lines += "- Reading/listening choice score: $correct / ${choiceTasks.size}"
    lines += "- Writing submitted: $writingSubmitted / ${writingTasks.size}"
    lines += "- Speaking recordings sent: $speakingSubmitted / ${speakingTasks.size}"
    lines += ""
    tasks.forEachIndexed { index, task ->
        lines += "- ${task.screenshotLabel} ${task.section}: ${task.title}"
        if (task.options.isNotEmpty()) {
            lines += "  Selected: ${selectedAnswers[index]?.let { task.options.getOrNull(it) }.orEmpty().ifBlank { "No answer" }}"
        }
    }

    return YkiPracticeEvaluationReport(
        title = "YKI Mock Exam Evaluation",
        createdAt = "Created in Floently Native",
        scoreLine = "B1/B2 readiness: $percent%",
        summary = "The mock exam captured reading/listening checks, writing answers, speaking recordings, and screenshot-driven timer states.",
        lines = lines
    )
}

@Composable
private fun MockProgressDots(skill: YkiMockSkill) {
    val active = when (skill) {
        YkiMockSkill.Overview -> 0
        YkiMockSkill.Reading -> 1
        YkiMockSkill.Writing -> 2
        YkiMockSkill.Listening -> 3
        YkiMockSkill.Speaking -> 4
        YkiMockSkill.Results -> 5
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(6) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == active) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (i <= active) mockBankBlue else Color(0xFFD6DCE8))
            )
        }
    }
}

@Composable
private fun MockPill(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, mockBankBorder),
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }
    }
}

@Composable
private fun MockGhostButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, mockBankBorder),
        modifier = Modifier
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = mockBankMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp)
            )
        }
    }
}

@Composable
private fun MockActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun MockPrimaryButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Suppress("DEPRECATION")
private fun newMockMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        MediaRecorder()
    }
