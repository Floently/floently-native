package com.floently.learn.yki

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bg = Color(0xFF08101F)
private val panel = Color(0xFF111C34)
private val panel2 = Color(0xFF172541)
private val border = Color(0xFF293B63)
private val text = Color(0xFFF7F9FF)
private val muted = Color(0xFFA9B7D1)
private val soft = Color(0xFF7F8DA8)
private val purple = Color(0xFF9B7BFF)
private val orange = Color(0xFFFFA326)
private val green = Color(0xFF2FC075)
private val red = Color(0xFFE05562)

private typealias Skill = YkiPracticeSkill
private typealias PracticeTask = YkiPracticeBankTask

private enum class WritingButtonState {
    Draft,
    Saved
}

private enum class SpeakingStage {
    Prompt,
    Preparing,
    Speaking,
    Saved,
    Sent
}

@Composable
fun YkiPracticeExactScreen(
    onBack: () -> Unit
) {
    val tasks = remember { YkiPracticeBank.practiceTasks() }
    var index by remember { mutableIntStateOf(-1) }
    val selected = remember { mutableStateMapOf<Int, Int>() }
    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val writingAnswers = remember { mutableStateMapOf<Int, String>() }
    val writingButtonStates = remember { mutableStateMapOf<Int, WritingButtonState>() }
    val speakingRecordings = remember { mutableStateMapOf<Int, String>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PracticeTopBar(onBack)

            if (index < 0) {
                PracticeLanding(
                    onStart = { index = 0 }
                )
            } else if (index >= tasks.size) {
                PracticeEvaluationScreen(
                    tasks = tasks,
                    selectedAnswers = selected.toMap(),
                    checkedAnswers = checked.toMap(),
                    writingAnswers = writingAnswers.toMap(),
                    recordingPaths = speakingRecordings.toMap(),
                    onRestart = {
                        index = -1
                        selected.clear()
                        checked.clear()
                        writingAnswers.clear()
                        writingButtonStates.clear()
                        speakingRecordings.clear()
                    }
                )
            } else {
                val task = tasks[index]
                PracticeTaskScreen(
                    task = task,
                    taskIndex = index,
                    selectedIndex = selected[index],
                    isChecked = checked[index] == true,
                    writingValue = writingAnswers[index].orEmpty(),
                    writingButtonState = writingButtonStates[index] ?: WritingButtonState.Draft,
                    onWritingChange = { value ->
                        writingAnswers[index] = value
                        writingButtonStates[index] = WritingButtonState.Draft
                    },
                    onSaveWriting = {
                        writingButtonStates[index] = WritingButtonState.Saved
                    },
                    onSpeakingSaved = { path ->
                        if (!path.isNullOrBlank()) {
                            speakingRecordings[index] = path
                        }
                    },
                    onSelect = { selected[index] = it },
                    onCheck = { checked[index] = true },
                    onNext = {
                        if (index < tasks.lastIndex) {
                            index += 1
                        } else {
                            index = tasks.size
                        }
                    },
                    onPrevious = {
                        if (index > 0) index -= 1 else index = -1
                    }
                )
            }
        }
    }
}

@Composable
private fun PracticeTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF111A2F),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier
                .height(42.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = Color(0xFFAEC4FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "YKI Exam",
            color = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = Color(0xFF111A2F),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.height(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Menu",
                    color = Color(0xFFAEC4FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 15.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelTabs() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "LEVEL",
            color = soft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            LevelTab("A1-A2", false, Modifier.weight(1f))
            LevelTab("B1-B2", true, Modifier.weight(1f))
            LevelTab("C1-C2", false, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LevelTab(
    label: String,
    active: Boolean,
    modifier: Modifier
) {
    Surface(
        color = if (active) purple else Color(0xFF111A2F),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (active) purple else border),
        modifier = modifier.height(35.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (active) Color.White else muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PracticeLanding(onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        LevelTabs()

        Surface(
            color = panel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "B1-B2 exam block",
                    color = text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SkillChip("Reading 5", purple)
                    SkillChip("Listening 4", purple)
                    SkillChip("Writing 4", orange)
                }
                SkillChip("Speaking 4", orange)
                Text(
                    text = "Exact practice screens are rebuilt from IMG_0409-IMG_0431. Start here and move through the same reading, listening, writing and speaking order.",
                    color = muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Surface(
            color = panel,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "YKI cards",
                    color = text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Start the same task order shown in the screenshots. Reading first, then listening, writing and speaking.",
                    color = muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Surface(
                    color = purple,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(onClick = {})
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Open YKI cards", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        BigButton("Start exam block", purple, onStart)
    }
}

@Composable
private fun PracticeTaskScreen(
    task: PracticeTask,
    taskIndex: Int,
    selectedIndex: Int?,
    isChecked: Boolean,
    writingValue: String,
    writingButtonState: WritingButtonState,
    onWritingChange: (String) -> Unit,
    onSaveWriting: () -> Unit,
    onSpeakingSaved: (String?) -> Unit,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LevelTabs()

        Surface(
            color = panel,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkillChip(task.skill.name, if (task.skill == Skill.Reading) purple else orange)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = task.number,
                        color = muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = task.title,
                    color = text,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = task.cefr,
                    color = muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (task.passage.isNotBlank()) {
                    Surface(
                        color = Color(0xFF1C2C4E),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, Color(0xFF273E6B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = task.passage,
                            color = Color(0xFFDCE7FF),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                }

                if (task.skill == Skill.Listening) {
                    ListeningAudioButton(task = task)
                }

                if (task.question.isNotBlank()) {
                    Text(
                        text = task.question,
                        color = text,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                task.options.forEachIndexed { i, option ->
                    PracticeOption(
                        text = option,
                        selected = selectedIndex == i,
                        checked = isChecked,
                        correct = task.correctIndex == i,
                        onClick = { onSelect(i) }
                    )
                }

                if (isChecked) {
                    val correct = selectedIndex == task.correctIndex
                    Surface(
                        color = if (correct) Color(0xFF113926) else Color(0xFF3A1D2A),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, if (correct) green else red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (correct) "✓ Correct!" else "× Not quite — the correct answer is highlighted above.",
                            color = if (correct) Color(0xFFBDF4D4) else Color(0xFFFFC6CC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (task.skill == Skill.Writing) {
                    WritingBlock(
                        task = task,
                        value = writingValue,
                        onValueChange = onWritingChange
                    )
                }

                if (task.skill == Skill.Speaking) {
                    SpeakingRecordingControls(task = task, onNext = onNext, onSaved = onSpeakingSaved)
                    Surface(
                        color = Color(0xFF111A2F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mark complete > Next task",
                            color = muted,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 13.dp)
                        )
                    }
                }

                if (task.skill != Skill.Speaking) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SmallButton("Back", Color(0xFF111A2F), muted, onPrevious)
                        Spacer(modifier = Modifier.weight(1f))
                        when {
                            task.options.isNotEmpty() && !isChecked -> {
                                SmallButton(
                                    text = "Check answer",
                                    bgColor = if (selectedIndex == null) Color(0xFF4D4578) else purple,
                                    fgColor = Color.White,
                                    onClick = {
                                        if (selectedIndex != null) onCheck()
                                    }
                                )
                            }
                            task.skill == Skill.Writing && writingButtonState == WritingButtonState.Draft -> {
                                SmallButton("Save answer", orange, Color.White, onSaveWriting)
                            }
                            else -> {
                                SmallButton("Next task", purple, Color.White, onNext)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = task.screenshot,
            color = soft,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}



@Composable
private fun PracticeEvaluationScreen(
    tasks: List<PracticeTask>,
    selectedAnswers: Map<Int, Int>,
    checkedAnswers: Map<Int, Boolean>,
    writingAnswers: Map<Int, String>,
    recordingPaths: Map<Int, String>,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val report = remember(
        tasks,
        selectedAnswers,
        checkedAnswers,
        writingAnswers,
        recordingPaths
    ) {
        YkiPracticeEvaluation.build(
            tasks = tasks,
            selectedAnswers = selectedAnswers,
            checkedAnswers = checkedAnswers,
            writingAnswers = writingAnswers,
            recordingPaths = recordingPaths
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LevelTabs()

        Surface(
            color = panel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkillChip("Evaluation", purple)

                Text(
                    text = report.title,
                    color = text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = report.scoreLine,
                    color = Color(0xFFDCE7FF),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = report.summary,
                    color = muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Surface(
                    color = Color(0xFF1C2C4E),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color(0xFF273E6B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.lines.takeLast(5).joinToString("\n"),
                        color = Color(0xFFDCE7FF),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(13.dp)
                    )
                }

                BigButton(
                    text = "Download PDF evaluation",
                    color = purple,
                    onClick = {
                        YkiPracticeEvaluationExporter.share(
                            context = context,
                            report = report,
                            format = YkiPracticeExportFormat.Pdf
                        )
                    }
                )

                BigButton(
                    text = "Download Word evaluation",
                    color = orange,
                    onClick = {
                        YkiPracticeEvaluationExporter.share(
                            context = context,
                            report = report,
                            format = YkiPracticeExportFormat.Word
                        )
                    }
                )

                SmallButton(
                    text = "Restart practice",
                    bgColor = Color(0xFF111A2F),
                    fgColor = muted,
                    onClick = onRestart
                )
            }
        }
    }
}

@Composable
private fun ListeningAudioButton(task: PracticeTask) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsHolder[0]?.language = Locale("fi", "FI")
                ready = true
            }
        }
        ttsHolder[0] = engine

        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    Surface(
        color = Color(0xFF223153),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Listening audio", color = muted, fontSize = 12.sp)
            Surface(
                color = if (playing) orange else purple,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clickable {
                        val engine = ttsHolder[0]
                        if (playing) {
                            engine?.stop()
                            playing = false
                        } else if (ready && engine != null) {
                            val script = task.audioScript.ifBlank {
                                listOf(task.title, task.question, task.options.joinToString(". ")).joinToString(". ")
                            }
                            engine.speak(script, TextToSpeech.QUEUE_FLUSH, null, "yki-practice-${task.screenshot}")
                            playing = true
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            playing -> "Stop audio"
                            ready -> "Play audio"
                            else -> "Preparing audio..."
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = "Audio comes from the YKI Practice bank task script.",
                color = muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SpeakingRecordingControls(
    task: PracticeTask,
    onNext: () -> Unit,
    onSaved: (String?) -> Unit
) {
    val context = LocalContext.current
    val screenshotState = remember(task.screenshot) { YkiPracticeStateMap.forTask(task) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var stage by remember(task.screenshot) { mutableStateOf(SpeakingStage.Prompt) }
    var seconds by remember(task.screenshot) { mutableIntStateOf(0) }
    var savedPath by remember(task.screenshot) { mutableStateOf<String?>(null) }
    var error by remember(task.screenshot) { mutableStateOf<String?>(null) }

    fun startRecording() {
        if (!hasPermission) {
            error = "Allow microphone permission before starting preparation."
            stage = SpeakingStage.Prompt
            seconds = 0
            return
        }

        val output = File(context.cacheDir, "yki-practice-${task.screenshot}-${System.currentTimeMillis()}.m4a")
        val nextRecorder = newYkiMediaRecorder(context)
        try {
            nextRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            nextRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            nextRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            nextRecorder.setOutputFile(output.absolutePath)
            nextRecorder.prepare()
            nextRecorder.start()
            recorder = nextRecorder
            savedPath = output.absolutePath
            error = null
            stage = SpeakingStage.Speaking
            seconds = screenshotState.responseSeconds.coerceAtLeast(60)
        } catch (e: Exception) {
            nextRecorder.release()
            error = "Recording could not start."
            stage = SpeakingStage.Prompt
            seconds = 0
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
        stage = SpeakingStage.Saved
        seconds = 0
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            error = null
            stage = SpeakingStage.Preparing
            seconds = screenshotState.prepareSeconds.coerceAtLeast(30)
        } else {
            error = "Microphone permission is required for speaking practice."
        }
    }

    LaunchedEffect(stage, seconds) {
        when {
            stage == SpeakingStage.Preparing && seconds > 0 -> {
                delay(1000)
                seconds -= 1
            }
            stage == SpeakingStage.Preparing && seconds <= 0 -> {
                startRecording()
            }
            stage == SpeakingStage.Speaking && seconds > 0 -> {
                delay(1000)
                seconds -= 1
            }
            stage == SpeakingStage.Speaking && seconds <= 0 -> {
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

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        BigButton(
            text = when (stage) {
                SpeakingStage.Prompt -> screenshotState.buttonLabel
                SpeakingStage.Preparing -> "Prepare to speak 00:${seconds.toString().padStart(2, '0')}"
                SpeakingStage.Speaking -> "Recording 00:${seconds.toString().padStart(2, '0')}"
                SpeakingStage.Saved -> "Send answer"
                SpeakingStage.Sent -> "Next task"
            },
            color = when (stage) {
                SpeakingStage.Speaking -> red
                SpeakingStage.Saved,
                SpeakingStage.Sent -> purple
                else -> orange
            },
            onClick = {
                when (stage) {
                    SpeakingStage.Prompt -> {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            error = null
                            stage = SpeakingStage.Preparing
                            seconds = screenshotState.prepareSeconds.coerceAtLeast(30)
                        }
                    }
                    SpeakingStage.Preparing -> Unit
                    SpeakingStage.Speaking -> saveRecording()
                    SpeakingStage.Saved -> {
                        onSaved(savedPath)
                        stage = SpeakingStage.Sent
                    }
                    SpeakingStage.Sent -> onNext()
                }
            }
        )

        Surface(
            color = Color(0xFF111A2F),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    error != null -> error.orEmpty()
                    stage == SpeakingStage.Prompt -> "Read the YKI bank speaking prompt, then start preparation."
                    stage == SpeakingStage.Preparing -> "Prepare your answer. Recording starts automatically when the timer ends."
                    stage == SpeakingStage.Speaking -> "Recording is active. It saves automatically when the timer ends, or tap to save now."
                    stage == SpeakingStage.Saved -> "Recording saved. Send answer to include it in your evaluation."
                    stage == SpeakingStage.Sent -> "Speaking answer sent. Use Next task to continue."
                    else -> "Speaking answer sent. Use Next task to continue."
                },
                color = if (error != null) Color(0xFFFFC6CC) else muted,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 13.dp, horizontal = 12.dp)
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun newYkiMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        MediaRecorder()
    }


@Composable
private fun WritingBlock(
    task: PracticeTask,
    value: String,
    onValueChange: (String) -> Unit
) {
    val target = task.wordCounter.substringAfter("/", "100 words").trim().ifBlank { "100 words" }
    val count = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFFE7ECFF),
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            imeAction = ImeAction.Default
        ),
        singleLine = false,
        minLines = 8,
        shape = RoundedCornerShape(15.dp)
    )

    Text(
        text = "$count / $target",
        color = muted,
        fontSize = 12.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PracticeOption(
    text: String,
    selected: Boolean,
    checked: Boolean,
    correct: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        checked && correct -> Color(0xFF123626)
        checked && selected && !correct -> Color(0xFF3A1D2A)
        selected -> Color(0xFF24385F)
        else -> Color(0xFF101A30)
    }
    val borderColor = when {
        checked && correct -> green
        checked && selected && !correct -> red
        selected -> purple
        else -> border
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        checked && correct -> "✓"
                        checked && selected && !correct -> "×"
                        selected -> "•"
                        else -> ""
                    },
                    color = borderColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color(0xFFF7F9FF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SkillChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun BigButton(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        color = color,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (bgColor == Color(0xFF111A2F)) border else bgColor),
        modifier = Modifier
            .height(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = fgColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp)
            )
        }
    }
}
