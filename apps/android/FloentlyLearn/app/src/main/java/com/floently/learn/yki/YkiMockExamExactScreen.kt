package com.floently.learn.yki

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val darkBg = Color(0xFF071837)
private val darkCard = Color(0xFF10264C)
private val darkBorder = Color(0xFF284A85)
private val darkText = Color(0xFFEAF1FF)
private val darkMuted = Color(0xFFA8B8D8)
private val blue = Color(0xFF2F5BEA)
private val blueSoft = Color(0xFFEAF1FF)
private val lightBg = Color(0xFFF5F8FC)
private val white = Color.White
private val border = Color(0xFFD7E1F1)
private val mockText = Color(0xFF111827)
private val muted = Color(0xFF697386)
private val green = Color(0xFF3F946E)
private val red = Color(0xFFD94255)
private val orange = Color(0xFFFF9F1C)

private enum class MockRouteStage {
    Landing,
    Summary,
    Exam,
    Results
}

private enum class WritingState {
    Draft,
    Saved
}

private enum class MockSpeakingStage {
    PromptReading,
    PreparingResponse,
    Recording,
    Completed
}

@Composable
fun YkiMockExamExactScreen(
    onBack: () -> Unit
) {
    val tasks = remember { YkiMockExamBank.tasks() }
    var routeStage by remember { mutableStateOf(MockRouteStage.Landing) }
    var currentIndex by remember { mutableIntStateOf(0) }

    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    val writingAnswers = remember { mutableStateMapOf<Int, String>() }
    val writingStates = remember { mutableStateMapOf<Int, WritingState>() }
    val speakingDurations = remember { mutableStateMapOf<Int, Int>() }
    val speakingFiles = remember { mutableStateMapOf<Int, String>() }
    val listeningPlayed = remember { mutableStateMapOf<Int, Boolean>() }

    when (routeStage) {
        MockRouteStage.Landing -> MockLandingScreen(
            onBack = onBack,
            onStart = {
                routeStage = MockRouteStage.Summary
            }
        )

        MockRouteStage.Summary -> MockExamSummaryScreen(
            onBack = {
                routeStage = MockRouteStage.Landing
            },
            onStart = {
                currentIndex = 0
                routeStage = MockRouteStage.Exam
            }
        )

        MockRouteStage.Exam -> {
            if (currentIndex >= tasks.size) {
                routeStage = MockRouteStage.Results
            } else {
                val task = tasks[currentIndex]
                MockExamTaskScreen(
                    task = task,
                    totalTasks = tasks.size,
                    currentIndex = currentIndex,
                    selectedIndex = selectedAnswers[currentIndex],
                    writingValue = writingAnswers[currentIndex].orEmpty(),
                    writingState = writingStates[currentIndex] ?: WritingState.Draft,
                    recordingDuration = speakingDurations[currentIndex],
                    listeningPlayed = listeningPlayed[currentIndex] == true,
                    onBack = {
                        if (currentIndex > 0) {
                            currentIndex -= 1
                        } else {
                            routeStage = MockRouteStage.Summary
                        }
                    },
                    onSelect = { selectedAnswers[currentIndex] = it },
                    onWritingChange = {
                        writingAnswers[currentIndex] = it
                        writingStates[currentIndex] = WritingState.Draft
                    },
                    onSaveWriting = {
                        writingStates[currentIndex] = WritingState.Saved
                    },
                    onListeningPlayed = {
                        listeningPlayed[currentIndex] = true
                    },
                    onSpeakingComplete = { duration, path ->
                        speakingDurations[currentIndex] = duration
                        if (!path.isNullOrBlank()) speakingFiles[currentIndex] = path
                    },
                    onNext = {
                        if (task.finalSubmit || currentIndex >= tasks.lastIndex) {
                            routeStage = MockRouteStage.Results
                        } else {
                            currentIndex += 1
                        }
                    }
                )
            }
        }

        MockRouteStage.Results -> MockDeepResultsScreen(
            tasks = tasks,
            selectedAnswers = selectedAnswers.toMap(),
            writingAnswers = writingAnswers.toMap(),
            speakingDurations = speakingDurations.toMap(),
            speakingFiles = speakingFiles.toMap(),
            onBack = {
                routeStage = MockRouteStage.Exam
                currentIndex = tasks.lastIndex
            },
            onRestart = {
                currentIndex = 0
                selectedAnswers.clear()
                writingAnswers.clear()
                writingStates.clear()
                speakingDurations.clear()
                speakingFiles.clear()
                listeningPlayed.clear()
                routeStage = MockRouteStage.Landing
            }
        )
    }
}

@Composable
private fun MockLandingScreen(
    onBack: () -> Unit,
    onStartMock: () -> Unit
) {
    MockChromeScaffold(onBack = onBack) {
        OldSourceYkiMockLanding(
            onStartExam = onStartMock,
            onOpenMockCycle = onStartMock,
            onOpenPractice = onStartMock
        )
    }
}


@Composable
private fun MockExamSummaryScreen(
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    LightShell {
        BackPill(onBack)
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "YKI B1-B2 exam",
            color = mockText,
            fontSize = 43.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Practice exam with real-format questions at B1-B2 level.\nWork through each section in order.",
            color = Color(0xFF4B5563),
            fontSize = 22.sp,
            lineHeight = 32.sp
        )

        Surface(
            color = white,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Exam sections", color = mockText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                SummaryRow("1", "Reading comprehension", "25 min · 5 tasks")
                SummaryRow("2", "Listening comprehension", "20 min · 4 tasks")
                SummaryRow("3", "Writing tasks", "35 min · 4 tasks")
                SummaryRow("4", "Speaking tasks", "15 min · 4 tasks")

                Surface(color = border, modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                Text(
                    text = "17 tasks total · approx. 95 min",
                    color = muted,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        MockPrimaryButton("Start YKI exam", blue, onStart)
    }
}

@Composable
private fun SummaryRow(
    number: String,
    title: String,
    sub: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(number, color = blue, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(48.dp))
        Column {
            Text(title, color = mockText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(sub, color = muted, fontSize = 19.sp)
        }
    }
}

@Composable
private fun MockExamTaskScreen(
    task: YkiMockExamTask,
    totalTasks: Int,
    currentIndex: Int,
    selectedIndex: Int?,
    writingValue: String,
    writingState: WritingState,
    recordingDuration: Int?,
    listeningPlayed: Boolean,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onWritingChange: (String) -> Unit,
    onSaveWriting: () -> Unit,
    onListeningPlayed: () -> Unit,
    onSpeakingComplete: (Int, String?) -> Unit,
    onNext: () -> Unit
) {
    LightShell {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackPill(onBack)
            Spacer(modifier = Modifier.weight(1f))
            MockProgressDots(total = totalTasks, currentIndex = currentIndex)
        }

        when (task.phase) {
            YkiMockPhase.Choice -> MockChoiceTask(task, selectedIndex, onSelect, onNext)
            YkiMockPhase.Listening -> MockListeningTask(task, selectedIndex, listeningPlayed, onListeningPlayed, onSelect, onNext)
            YkiMockPhase.Writing -> MockWritingTask(task, writingValue, writingState, onWritingChange, onSaveWriting, onNext)
            YkiMockPhase.Speaking -> MockSpeakingTask(task, recordingDuration, onSpeakingComplete, onNext)
        }
    }
}

@Composable
private fun MockChoiceTask(
    task: YkiMockExamTask,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    SectionHeader(task)
    Text(task.instruction, color = Color(0xFF4B5563), fontSize = 22.sp, lineHeight = 31.sp, fontStyle = FontStyle.Italic)
    QuestionCard(task = task, selectedIndex = selectedIndex, onSelect = onSelect)
    MockPrimaryButton(
        text = "Next question",
        color = if (selectedIndex == null) Color(0xFF9FB4E8) else blue,
        onClick = { if (selectedIndex != null) onNext() }
    )
}

@Composable
private fun MockListeningTask(
    task: YkiMockExamTask,
    selectedIndex: Int?,
    listeningPlayed: Boolean,
    onListeningPlayed: () -> Unit,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    var preStartRemaining by remember(task.bankTaskId) { mutableIntStateOf(if (task.sectionTaskNumber == 1) 10 else 0) }
    var taskRemaining by remember(task.bankTaskId) { mutableIntStateOf(60) }

    LaunchedEffect(task.bankTaskId, preStartRemaining) {
        if (preStartRemaining > 0) {
            delay(1000)
            preStartRemaining -= 1
        }
    }

    LaunchedEffect(task.bankTaskId, listeningPlayed, taskRemaining, preStartRemaining) {
        if (preStartRemaining <= 0 && listeningPlayed && taskRemaining > 0) {
            delay(1000)
            taskRemaining -= 1
        }
    }

    SectionHeader(task)

    if (preStartRemaining > 0) {
        TimerPanel(
            title = "Listening starts soon",
            body = "Read the instruction before the listening section starts.",
            time = "00:${preStartRemaining.toString().padStart(2, '0')}",
            active = false
        )
    } else {
        Text(task.instruction, color = Color(0xFF4B5563), fontSize = 22.sp, lineHeight = 31.sp, fontStyle = FontStyle.Italic)
        TimerPanel(
            title = if (listeningPlayed) "Timed listening answer" else "Ready for audio",
            body = if (listeningPlayed) "Choose the best answer before continuing." else "Play the listening audio, then choose an answer.",
            time = "00:${taskRemaining.coerceAtLeast(0).toString().padStart(2, '0')}",
            active = listeningPlayed
        )
        MockListeningAudioButton(task = task, onPlayed = onListeningPlayed)
        QuestionCard(task = task, selectedIndex = selectedIndex, onSelect = onSelect)
        MockPrimaryButton(
            text = "Next question",
            color = if (selectedIndex == null) Color(0xFF9FB4E8) else blue,
            onClick = { if (selectedIndex != null) onNext() }
        )
    }
}

@Composable
private fun MockWritingTask(
    task: YkiMockExamTask,
    value: String,
    writingState: WritingState,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onNext: () -> Unit
) {
    SectionHeader(task)
    Text(task.instruction, color = Color(0xFF4B5563), fontSize = 22.sp, lineHeight = 31.sp, fontStyle = FontStyle.Italic)

    Surface(
        color = white,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(task.prompt, color = mockText, fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp),
                textStyle = TextStyle(color = mockText, fontSize = 18.sp, lineHeight = 27.sp),
                keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Default),
                minLines = 10,
                singleLine = false,
                shape = RoundedCornerShape(20.dp)
            )

            if (writingState == WritingState.Saved) {
                StatusPanel("Answer saved", "Your writing response will be evaluated after the full exam.")
            }
        }
    }

    when (writingState) {
        WritingState.Draft -> MockPrimaryButton(
            text = "Save answer",
            color = if (value.trim().isBlank()) Color(0xFF9FB4E8) else blue,
            onClick = { if (value.trim().isNotBlank()) onSave() }
        )

        WritingState.Saved -> MockPrimaryButton("Next question", blue, onNext)
    }
}

@Composable
private fun MockSpeakingTask(
    task: YkiMockExamTask,
    existingDuration: Int?,
    onSpeakingComplete: (Int, String?) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var stage by remember(task.bankTaskId) {
        mutableStateOf(if (existingDuration != null) MockSpeakingStage.Completed else MockSpeakingStage.PromptReading)
    }
    var remaining by remember(task.bankTaskId) { mutableIntStateOf(task.promptReadSeconds) }
    var recorder by remember(task.bankTaskId) { mutableStateOf<MediaRecorder?>(null) }
    var recordingStarted by remember(task.bankTaskId) { mutableStateOf(false) }
    var savedPath by remember(task.bankTaskId) { mutableStateOf<String?>(null) }
    var savedDuration by remember(task.bankTaskId) { mutableIntStateOf(existingDuration ?: 0) }
    var error by remember(task.bankTaskId) { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            error = "Microphone permission is required for speaking tasks."
        }
    }

    fun startRecording() {
        if (!hasPermission || recordingStarted) return
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

    fun completeRecording(duration: Int) {
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
        savedDuration = duration.coerceAtLeast(0)
        stage = MockSpeakingStage.Completed
        onSpeakingComplete(savedDuration, savedPath)
    }

    LaunchedEffect(task.bankTaskId, stage, remaining, hasPermission) {
        when {
            stage == MockSpeakingStage.PromptReading && remaining > 0 -> {
                delay(1000)
                remaining -= 1
            }
            stage == MockSpeakingStage.PromptReading && remaining <= 0 -> {
                stage = MockSpeakingStage.PreparingResponse
                remaining = task.preparationSeconds
            }
            stage == MockSpeakingStage.PreparingResponse && remaining > 0 -> {
                delay(1000)
                remaining -= 1
            }
            stage == MockSpeakingStage.PreparingResponse && remaining <= 0 && hasPermission -> {
                stage = MockSpeakingStage.Recording
                remaining = task.responseSeconds
                startRecording()
            }
            stage == MockSpeakingStage.Recording && remaining > 0 -> {
                delay(1000)
                remaining -= 1
            }
            stage == MockSpeakingStage.Recording && remaining <= 0 -> {
                completeRecording(task.responseSeconds)
            }
        }
    }

    DisposableEffect(task.bankTaskId) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: RuntimeException) {
            } finally {
                recorder?.release()
            }
        }
    }

    SectionHeader(task)
    Text(task.instruction, color = Color(0xFF4B5563), fontSize = 22.sp, lineHeight = 31.sp, fontStyle = FontStyle.Italic)

    Surface(
        color = white,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text(task.prompt, color = Color(0xFF6B7280), fontSize = 23.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)

            when (stage) {
                MockSpeakingStage.PromptReading -> TimerPanel(
                    title = "Read the prompt",
                    body = "Prompt reading time before response preparation.",
                    time = "00:${remaining.coerceAtLeast(0).toString().padStart(2, '0')}",
                    active = false
                )

                MockSpeakingStage.PreparingResponse -> TimerPanel(
                    title = "Prepare your response",
                    body = "Recording starts automatically when preparation ends.",
                    time = "00:${remaining.coerceAtLeast(0).toString().padStart(2, '0')}",
                    active = false
                )

                MockSpeakingStage.Recording -> {
                    val recorded = task.responseSeconds - remaining
                    TimerPanel(
                        title = "Recording",
                        body = if (recorded < task.minimumRecordingSeconds) {
                            "Minimum valid recording is 30 seconds."
                        } else {
                            "You may finish now or continue to 60 seconds."
                        },
                        time = "00:${remaining.coerceAtLeast(0).toString().padStart(2, '0')}",
                        active = true
                    )
                    MockPrimaryButton(
                        text = if (recorded < task.minimumRecordingSeconds) {
                            "Recording... ${recorded}s / 30s minimum"
                        } else {
                            "Finish recording"
                        },
                        color = if (recorded < task.minimumRecordingSeconds) Color(0xFF9FB4E8) else red,
                        onClick = {
                            if (recorded >= task.minimumRecordingSeconds) completeRecording(recorded)
                        }
                    )
                }

                MockSpeakingStage.Completed -> StatusPanel(
                    title = "Recording complete",
                    body = "Recorded: ${savedDuration.coerceAtLeast(existingDuration ?: 0)}s"
                )
            }

            if (!hasPermission && stage != MockSpeakingStage.Completed) {
                MockPrimaryButton(
                    text = "Allow microphone",
                    color = orange,
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }

            if (error != null) {
                Text(error.orEmpty(), color = red, fontSize = 16.sp)
            }
        }
    }

    if (stage == MockSpeakingStage.Completed) {
        MockPrimaryButton(
            text = if (task.finalSubmit) "Submit exam" else "Next question",
            color = blue,
            onClick = onNext
        )
    }
}

@Composable
private fun SectionHeader(task: YkiMockExamTask) {
    Surface(
        color = white,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${task.section} · TASK ${task.sectionTaskNumber} OF ${task.sectionTaskCount}",
                color = blue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(task.sectionTitle, color = mockText, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(task.durationLabel, color = muted, fontSize = 20.sp)
        }
    }
}

@Composable
private fun QuestionCard(
    task: YkiMockExamTask,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    Surface(
        color = white,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (task.passage.isNotBlank()) {
                Surface(
                    color = Color(0xFFF1F5FB),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .heightIn(min = 180.dp)
                                .background(blue)
                        )
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("TEKSTI", color = blue, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                            Text(task.passage, color = Color(0xFF1F2937), fontSize = 22.sp, lineHeight = 33.sp)
                        }
                    }
                }
            }

            Text(task.prompt, color = mockText, fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black)

            task.options.forEachIndexed { index, option ->
                MockOption(
                    textValue = "${('A'.code + index).toChar()}. $option",
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun MockOption(
    textValue: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFFEAF1FF) else white,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) blue else border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = textValue,
            color = mockText,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            modifier = Modifier.padding(22.dp)
        )
    }
}

@Composable
private fun TimerPanel(
    title: String,
    body: String,
    time: String,
    active: Boolean
) {
    Surface(
        color = if (active) Color(0xFFFFEEF0) else Color(0xFFF1F5FB),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (active) red.copy(alpha = 0.45f) else border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(time, color = if (active) red else mockText, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Text(title, color = if (active) red else blue, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(body, color = muted, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatusPanel(
    title: String,
    body: String
) {
    Surface(
        color = Color(0xFFEEF3FF),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color(0xFF18B77A), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(body, color = muted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun MockListeningAudioButton(
    task: YkiMockExamTask,
    onPlayed: () -> Unit
) {
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
        color = if (playing) red else blue,
        onClick = {
            val engine = engineHolder[0]
            if (engine != null && ready) {
                if (playing) {
                    engine.stop()
                    playing = false
                } else {
                    onPlayed()
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
private fun MockDeepResultsScreen(
    tasks: List<YkiMockExamTask>,
    selectedAnswers: Map<Int, Int>,
    writingAnswers: Map<Int, String>,
    speakingDurations: Map<Int, Int>,
    speakingFiles: Map<Int, String>,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val report = remember(tasks, selectedAnswers, writingAnswers, speakingDurations, speakingFiles) {
        buildDeepMockReport(tasks, selectedAnswers, writingAnswers, speakingDurations, speakingFiles)
    }

    LightShell {
        BackPill(onBack)
        Text("Results overview", color = mockText, fontSize = 43.sp, fontWeight = FontWeight.Black)

        Surface(
            color = white,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("B1-B2 exam summary", color = mockText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(report.scoreLine, color = Color(0xFF4B5563), fontSize = 21.sp)
                Text(report.summary, color = Color(0xFF4B5563), fontSize = 20.sp, lineHeight = 29.sp)
            }
        }

        Surface(
            color = white,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Detailed evaluation", color = mockText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                report.lines.take(18).forEach {
                    Text(it, color = Color(0xFF4B5563), fontSize = 18.sp, lineHeight = 25.sp)
                }
            }
        }

        MockPrimaryButton("Download PDF evaluation", blue) {
            YkiPracticeEvaluationExporter.share(context, report, YkiPracticeExportFormat.Pdf)
        }

        MockPrimaryButton("Download Word evaluation", orange) {
            YkiPracticeEvaluationExporter.share(context, report, YkiPracticeExportFormat.Word)
        }

        MockGhostButton("Restart exam", onRestart)
    }
}

private fun buildDeepMockReport(
    tasks: List<YkiMockExamTask>,
    selectedAnswers: Map<Int, Int>,
    writingAnswers: Map<Int, String>,
    speakingDurations: Map<Int, Int>,
    speakingFiles: Map<Int, String>
): YkiPracticeEvaluationReport {
    val reading = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Reading }
    val listening = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Listening }
    val writing = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Writing }
    val speaking = tasks.withIndex().filter { (_, task) -> task.skill == YkiMockSkill.Speaking }

    val readingCorrect = reading.count { (index, task) -> selectedAnswers[index] == task.correctIndex }
    val listeningCorrect = listening.count { (index, task) -> selectedAnswers[index] == task.correctIndex }
    val objectiveTotal = reading.size + listening.size
    val objectiveCorrect = readingCorrect + listeningCorrect

    val writingSubmitted = writing.count { (index, _) -> writingAnswers[index].orEmpty().trim().isNotBlank() }
    val speakingSubmitted = speaking.count { (index, _) -> speakingDurations[index] ?: 0 >= 30 || speakingFiles[index].orEmpty().isNotBlank() }

    val readiness = when {
        objectiveCorrect >= 8 && writingSubmitted >= 3 && speakingSubmitted >= 3 -> "Strong B1-B2 readiness"
        objectiveCorrect >= 6 && writingSubmitted >= 2 && speakingSubmitted >= 2 -> "Developing B1-B2 readiness"
        else -> "Needs more targeted practice before exam readiness"
    }

    val lines = mutableListOf<String>()
    lines += "Objective skills"
    lines += "- Reading comprehension: $readingCorrect / ${reading.size}"
    lines += "- Listening comprehension: $listeningCorrect / ${listening.size}"
    lines += ""
    lines += "Writing evaluation"
    writing.forEach { (index, task) ->
        val words = writingAnswers[index].orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val status = when {
            words == 0 -> "not submitted"
            words < 25 -> "short answer; expand with reasons and examples"
            words < 70 -> "submitted; add more structure and detail"
            else -> "good length; review grammar, connectors and task focus"
        }
        lines += "- ${task.sectionTitle} task ${task.sectionTaskNumber}: $status"
    }
    lines += ""
    lines += "Speaking evaluation"
    speaking.forEach { (index, task) ->
        val duration = speakingDurations[index] ?: 0
        val status = when {
            duration < 30 -> "too short or missing; aim for at least 45 seconds"
            duration < 45 -> "valid minimum reached; expand fluency and examples"
            duration <= 60 -> "completed in target range; review clarity, grammar and pronunciation"
            else -> "completed"
        }
        lines += "- ${task.sectionTitle} task ${task.sectionTaskNumber}: $status (${duration}s)"
    }
    lines += ""
    lines += "Recommendations"
    lines += "- Review reading/listening questions missed after the exam."
    lines += "- For writing, focus on clear structure: opening, reason, details, ending."
    lines += "- For speaking, practise 45-60 second answers with connectors and concrete examples."
    lines += "- Repeat the full mock under timing pressure before the real exam."

    val created = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    return YkiPracticeEvaluationReport(
        title = "YKI B1-B2 Mock Exam Evaluation",
        createdAt = "Created: $created",
        scoreLine = "Objective score: $objectiveCorrect / $objectiveTotal. Readiness: $readiness.",
        summary = "Completed ${tasks.size} tasks: reading $readingCorrect/${reading.size}, listening $listeningCorrect/${listening.size}, writing submitted $writingSubmitted/${writing.size}, speaking recorded $speakingSubmitted/${speaking.size}.",
        lines = lines
    )
}

@Composable
private fun MockProgressDots(
    total: Int,
    currentIndex: Int
) {
    Row(horizontalArrangement = Arrangement.End) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == currentIndex) 13.dp else 11.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < currentIndex -> green
                            i == currentIndex -> blue
                            else -> Color(0xFFDCE7F5)
                        }
                    )
            )
        }
    }
}

@Composable
private fun LightShell(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 38.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    Surface(
        color = blueSoft,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(58.dp)
            .clickable(onClick = onBack)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "← Back",
                color = blue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun DarkPill(
    textValue: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF12264A),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF1F355A)),
        modifier = Modifier
            .height(56.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = textValue,
                color = Color(0xFF6D8DFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 26.dp)
            )
        }
    }
}

@Composable
private fun Chip(
    textValue: String,
    color: Color,
    bg: Color
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = textValue,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
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
            .height(70.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = white, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MockGhostButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = white,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = blue, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
