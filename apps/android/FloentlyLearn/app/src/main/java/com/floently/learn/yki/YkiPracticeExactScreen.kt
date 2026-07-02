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

private enum class Skill { Reading, Listening, Writing, Speaking }

private data class PracticeTask(
    val screenshot: String,
    val skill: Skill,
    val number: String,
    val title: String,
    val cefr: String,
    val passage: String,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val defaultWrongIndex: Int = -1,
    val answerText: String = "",
    val wordCounter: String = "",
    val saved: Boolean = false,
    val audioScript: String = "",
    val bankSource: String = "native-yki-practice-bank"
)

@Composable
fun YkiPracticeExactScreen(
    onBack: () -> Unit
) {
    val tasks = remember { YkiPracticeBank.practiceTasks() }
    var index by remember { mutableIntStateOf(-1) }
    val selected = remember { mutableStateMapOf<Int, Int>() }
    val checked = remember { mutableStateMapOf<Int, Boolean>() }
    val writingAnswers = remember { mutableStateMapOf<Int, String>() }

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
            } else {
                val task = tasks[index]
                PracticeTaskScreen(
                    task = task,
                    taskIndex = index,
                    selectedIndex = selected[index],
                    isChecked = checked[index] == true,
                    writingValue = writingAnswers[index].orEmpty(),
                    onWritingChange = { value -> writingAnswers[index] = value },
                    onSelect = { selected[index] = it },
                    onCheck = { checked[index] = true },
                    onNext = {
                        if (index < tasks.lastIndex) {
                            index += 1
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
    onWritingChange: (String) -> Unit,
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
                    SpeakingRecordingControls(task = task)
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
                        task.skill == Skill.Writing && !task.saved -> {
                            SmallButton("Save answer", orange, Color.White, onNext)
                        }
                        else -> {
                            SmallButton("Next task", purple, Color.White, onNext)
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
private fun SpeakingRecordingControls(task: PracticeTask) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var savedPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            error = "Microphone permission is required for speaking practice."
        }
    }

    fun startRecording() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
            isRecording = true
        } catch (e: Exception) {
            nextRecorder.release()
            error = "Recording could not start."
            isRecording = false
        }
    }

    fun stopRecording() {
        val active = recorder ?: return
        try {
            active.stop()
        } catch (_: RuntimeException) {
            error = "Recording was too short to save."
        } finally {
            active.release()
            recorder = null
            isRecording = false
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
            text = if (isRecording) "Stop recording" else "Start voice recording",
            color = if (isRecording) red else orange,
            onClick = {
                if (isRecording) stopRecording() else startRecording()
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
                    isRecording -> "Recording... speak your answer now."
                    savedPath != null -> "Recording saved for this speaking task."
                    else -> "Use the microphone to record your speaking response."
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

private object YkiPracticeBank {
    fun practiceTasks(): List<PracticeTask> = listOf(
    PracticeTask(
        screenshot = "IMG_0410-IMG_0411",
        skill = Skill.Reading,
        number = "1/17",
        title = "Lue artikkeli maahanmuutosta ja vastaa.",
        cefr = "B2 — Can understand articles on social topics.",
        passage = "Suomi tarvitsee lähivuosikymmeninä merkittävää maahanmuuttoa väestön ikääntymisestä johtuvan työvoimapulan paikkaamiseksi. Erityisesti sosiaali- ja terveysala sekä rakennussektori kärsivät jo nyt osaajapulasta.\n\nMaahanmuuttajien kotoutuminen on avainasemassa. Onnistunut kotoutuminen edellyttää kielitaitoa, työllistymistä ja sosiaalisia verkostoja. Suomen kielen oppiminen on usein suurin este nopealle työllistymiselle.\n\nViranomaiset ja järjestöt tarjoavat kotouttamispalveluja, mutta resurssit eivät aina riitä yksilölliseen tukeen. Erityisesti heikosti koulutettujen maahanmuuttajien kohdalla prosessi voi venyä vuosiksi. Asiantuntijat korostavat, että panostaminen varhaiseen kielenopetukseen ja työllistymisen tukeen maksaa itsensä takaisin yhteiskunnalle.",
        question = "Miksi Suomi tarvitsee maahanmuuttoa asiantuntijoiden mukaan?",
        options = listOf(
            "Maahanmuuttajat tuovat uutta teknologiaa Suomeen",
            "Ikääntymisestä johtuva työvoimapula on keskeinen syy",
            "Suomen syntyvyys on laskenut alle nollatason",
            "Suomessa ei enää ole riittävästi nuoria kouluttautumaan"
        ),
        correctIndex = 1
    ),
    PracticeTask(
        screenshot = "IMG_0412",
        skill = Skill.Reading,
        number = "2/17",
        title = "Lue artikkeli etätyöstä ja vastaa.",
        cefr = "B2 — Can understand articles and reports on contemporary work.",
        passage = "Etätyö on yleistynyt monilla aloilla nopeasti. Osa työntekijöistä kokee sen helpottavan arkea, koska työmatkoihin ei kulu aikaa ja työpäivää on helpompi rytmittää kotona. Sen sijaan perheen kanssa ahtaissa oloissa asuvat kokevat usein etätyön raskaammaksi kuin toimistossa työskentelyn.\n\nOsa asiantuntijoista on huolissaan siitä, että pitkittynyt etätyö heikentää tiimien yhteenkuuluvuutta ja vaikeuttaa uusien työntekijöiden perehdyttämistä. Ratkaisuna monet yritykset ovat ottaneet käyttöön hybridimallin, jossa toimistolla käydään muutamana päivänä viikossa.",
        question = "Miksi etätyö voi olla haastavampaa joillekin työntekijöille kuin toisille?",
        options = listOf(
            "Koska etätyö on aina vähemmän tuottavaa kuin toimistotyö",
            "Koska työnantajat eivät luota etätyöntekijöihin",
            "Koska ahtaat asuinolosuhteet voivat tehdä etätyöstä raskaampaa",
            "Koska kotona ei ole riittävästi teknologiaa"
        ),
        correctIndex = 2
    ),
    PracticeTask(
        screenshot = "IMG_0413-IMG_0414",
        skill = Skill.Reading,
        number = "3/17",
        title = "Lue lehtiartikkeli ja vastaa.",
        cefr = "B2 — Can understand articles and reports on contemporary problems.",
        passage = "Suomen terveydenhuoltojärjestelmä perustuu universaaliin hoitovelvollisuuteen: jokainen Suomessa asuva on oikeutettu terveydenhoitopalveluihin asuinpaikastaan riippumatta. Käytännössä palvelut tuotetaan kuntien, hyvinvointialueiden ja yksityisen sektorin yhteistyönä.\n\nViime vuosina yksityisten terveyspalvelujen käyttö on kasvanut huomattavasti. Syynä on usein julkisen sektorin pitkät jonotusajat erikoissairaanhoitoon. Maksukykyisillä on mahdollisuus ohittaa jonot yksityisellä vastaanotolla, mikä herättää kysymyksiä tasa-arvosta.\n\nSosiaali- ja terveysministeriö on pyrkinyt purkamaan hoitojonoja lisäämällä lähipalveluja ja tehostamalla digitaalisia palveluja. Digilääkäripalveluiden käyttö on erityisesti nuorten ja työssäkäyvien parissa yleistynyt nopeasti.",
        question = "Mitä artikkeli sanoo yksityisten terveyspalvelujen lisääntymisestä?",
        options = listOf(
            "Se on parantanut kaikkien suomalaisten pääsyä hoitoon tasapuolisesti",
            "Se on vähentänyt digitaalisten palvelujen tarvetta",
            "Se herättää kysymyksiä yhdenvertaisuudesta, koska hoitoon pääsy riippuu maksukyvystä",
            "Se on korvannut kokonaan julkisen terveydenhuollon"
        ),
        correctIndex = 2,
        defaultWrongIndex = 0
    ),
    PracticeTask(
        screenshot = "IMG_0415",
        skill = Skill.Reading,
        number = "4/17",
        title = "Lue artikkeli julkisista palveluista ja vastaa.",
        cefr = "B2 — Can understand argumentation in public-service texts.",
        passage = "Kehitys on tuonut mukanaan tehokkuutta ja joustavuutta. Palveluja voi käyttää vuorokauden ympäri, ja jonotusajat ovat lyhentyneet. Kuitenkin kaikille digitaaliset palvelut eivät ole yhtä helppokäyttöisiä. Ikääntyneet, maahanmuuttajat ja henkilöt, joilla on heikko digilukutaito, tarvitsevat usein henkilökohtaista tukea asioinnissa.\n\nJulkishallinto onkin pyrkinyt pitämään henkilökohtaisen asioinnin vaihtoehdon saatavilla niille, jotka eivät pysty tai halua käyttää sähköisiä kanavia.",
        question = "Mikä on artikkelin pääviesti digitalisaatiosta julkisissa palveluissa?",
        options = listOf(
            "Digilukutaito on parantunut kaikissa väestöryhmissä",
            "Digitalisointi on hyödyllistä, mutta kaikki eivät pysty hyödyntämään sitä yhtäläisesti",
            "Julkishallinto on luopunut kokonaan perinteisestä asioinnista",
            "Digitalisointi on täysin epäonnistunut julkisissa palveluissa"
        ),
        correctIndex = 1
    ),
    PracticeTask(
        screenshot = "IMG_0416",
        skill = Skill.Reading,
        number = "5/17",
        title = "Lue artikkeli ympäristöasioista ja vastaa.",
        cefr = "B2 — Can understand opinion and criticism in articles.",
        passage = "Silti kuluttajavalinnoilla on merkitystä. Kasvipohjaisen ruokavalion yleistyminen, joukkoliikenteen suosiminen ja energiatehokkaan asumisen lisääntyminen ovat konkreettisia tapoja pienentää omaa hiilijalanjälkeä.\n\nKriitikot kuitenkin muistuttavat, että vastuun siirtäminen yksilöille vie huomion pois rakenteellisista ratkaisuista, kuten energiapolitiikasta ja teollisuuden sääntelystä. Tehokkain muutos syntyy yhdistämällä poliittiset päätökset ja yksilöllinen toiminta.",
        question = "Mitä kriitikot sanovat yksilöiden vastuusta ympäristöasioissa?",
        options = listOf(
            "Vastuun painottaminen yksilöille voi haitata rakenteellisten ratkaisujen hakemista",
            "Yksilöt ovat päävastuussa ilmastonmuutoksen torjumisesta",
            "Yksilöiden valinnoilla ei ole mitään merkitystä",
            "Kierrätys on riittävä toimenpide ilmastonmuutoksen hidastamiseen"
        ),
        correctIndex = 0,
        audioScript = "Kuuntele ote luennosta. Eksekutiivinen funktio tarkoittaa kykyä vaihtaa tehtävien välillä ja hallita huomiota."
    ),
    PracticeTask(
        screenshot = "IMG_0417-IMG_0418",
        skill = Skill.Listening,
        number = "6/17",
        title = "Kuuntele radiouutinen ja vastaa.",
        cefr = "B2 — Can understand radio news and identify the main information.",
        passage = "",
        question = "Mitä liikuntakeskuksen vastustajat ehdottavat?",
        options = listOf(
            "Nykyisten tilojen kunnostamista uuden rakentamisen sijaan",
            "Hankkeen kokonaan peruuttamista",
            "Halvempaa rakennustapaa",
            "Yksityistä rahoitusta hankkeelle"
        ),
        correctIndex = 0,
        defaultWrongIndex = 1,
        audioScript = "Kuuntele radiouutinen. Liikuntakeskuksen vastustajat ehdottavat nykyisten tilojen kunnostamista uuden rakentamisen sijaan."
    ),
    PracticeTask(
        screenshot = "IMG_0419",
        skill = Skill.Listening,
        number = "7/17",
        title = "Kuuntele ote luennosta ja vastaa.",
        cefr = "B2 — Can follow extended speech in a lecture on familiar topics.",
        passage = "",
        question = "Mitä tarkoitetaan eksekutiivisella funktiolla tässä yhteydessä?",
        options = listOf(
            "Kykyä vaihtaa tehtävien välillä ja hallita huomiota",
            "Laajaa sanavarastoa molemmissa kielissä",
            "Pitkäkestoista muistia",
            "Kykyä puhua kahta kieltä samanaikaisesti"
        ),
        correctIndex = 0
    ),
    PracticeTask(
        screenshot = "IMG_0420",
        skill = Skill.Listening,
        number = "8/17",
        title = "Kuuntele haastattelu ja vastaa.",
        cefr = "B2 — Can understand interviews on professional topics.",
        passage = "",
        question = "Mitä asiantuntija pitää parhaana keinona löytää töitä nuorille?",
        options = listOf(
            "Kansainvälinen kokemus ulkomailla",
            "Mahdollisimman monen hakemuksen lähettäminen",
            "Työnhakukurssien suorittaminen",
            "Verkostoituminen opiskeluaikana ja alan tapahtumissa"
        ),
        correctIndex = 3,
        audioScript = "Kuuntele haastattelu. Asiantuntija sanoo, että verkostoituminen opiskeluaikana ja alan tapahtumissa auttaa nuoria löytämään töitä."
    ),
    PracticeTask(
        screenshot = "IMG_0421",
        skill = Skill.Listening,
        number = "9/17",
        title = "Kuuntele työtovereiden keskustelu ja vastaa.",
        cefr = "B1 — Can follow the main points of extended discussion on familiar topics.",
        passage = "",
        question = "Miksi projekti on myöhässä?",
        options = listOf(
            "Projektitiimillä on liian vähän resursseja",
            "Alihankkija ei ole toimittanut osia ajoissa",
            "Johto muutti projektin tavoitteita",
            "Asiakas on muuttanut vaatimuksiaan"
        ),
        correctIndex = 1,
        defaultWrongIndex = 0,
        audioScript = "Kuuntele työtovereiden keskustelu. Projekti on myöhässä, koska alihankkija ei ole toimittanut osia ajoissa."
    ),
    PracticeTask(
        screenshot = "IMG_0422",
        skill = Skill.Writing,
        number = "10/17",
        title = "Kirjoita mielipidekirjoitus.",
        cefr = "B1 — Can write accounts of experiences, expressing opinions with reasons.",
        passage = "Jotkut ihmiset ajattelevat, että älypuhelimet ovat tehneet sosiaalisesta elämästä köyhempää, koska ihmiset katsovat puhelimiaan seurueessa ollessaan. Toiset taas ajattelevat, että älypuhelimet ovat parantaneet yhteydenpitoa.\n\nKirjoita 80-120 sanaa. Esitä oma mielipiteesi ja perustele se kahdella argumentilla. Käytä asiatyyliä.\n\nAloita esittämällä mielipiteesi selkeästi. Kirjoita kaksi selkeää perustelua. Päätä lyhyellä yhteenvedolla tai johtopäätöksellä.",
        answerText = "Hyvä sulle kuuluu on ollut parisuhde on tosi pelottavaa kaikki mitä rakastin sitä ei se ole niin helppoa se ei vastannut mitään ei seuraa sua ja voidaan sopia myös se on ihan ok jos se ei vastannut vielä yksi on joukosta löydät täältä ei oo vielä siinä viestissä on ollut tosi kiltti tyttö ei se on ihan ok mutta en haluaa olla mukana tekemässä tikusta ei oo vielä nukkunut",
        wordCounter = "69 / 100 words"
    ),
    PracticeTask(
        screenshot = "IMG_0423",
        skill = Skill.Writing,
        number = "11/17",
        title = "Kirjoita argumentoiva teksti.",
        cefr = "B2 — Can write an essay presenting arguments for and against a position.",
        passage = "Kaupunki harkitsee yksityisautoilun rajoittamista keskustassa ilmanlaadun ja liikenteen sujuvuuden parantamiseksi. Monet asukkaat vastustavat muutosta.\n\nKirjoita 100-130 sanaa. Esitä sekä puolesta- että vastaargumentteja ja päätä omaan kantaasi. Käytä asiallista ja selkeää kieltä.\n\nEsitä vähintään yksi puolesta- ja yksi vastaargumentti. Perustele oma kantasi loppukappaleessa. Vältä liian arkista kieltä.",
        answerText = "Yö on tosi vaikea saada kokeilemaan jos yöllinen se että alle 5 vuota ei oo vielä siinä viestissä saat olla missä tahansa Yu ei oo vielä nukkunut muutama vuosi on vaihtunut se että kiusan se että kiusan sua ja sun Tarja Turunen olen yrittänyt että se ei vastannut mitään muuta kuin suomi ei se että alle ja kuoli ja se näkyy sun pitää sinut lämpimänä tai jäähtyneenä se että alle 5 ei kerro koko ajan lisää se on ihan hyvä mutta",
        wordCounter = "79 / 115 words"
    ),
    PracticeTask(
        screenshot = "IMG_0424-IMG_0425",
        skill = Skill.Writing,
        number = "12/17",
        title = "Kirjoita lyhyt raportti.",
        cefr = "B2 — Can write a structured report on a familiar topic.",
        passage = "Olet osallistunut yhteisösi asukasiltaan, jossa käsiteltiin lähipuiston kunnostamista. Kirjoita lyhyt raportti kokouksen tuloksista puiston suunnitteluryhmälle.\n\nRaportin tulee sisältää:\n- kokouksessa esitetyt ongelmat\n- asukkaiden toiveet\n- suositeltava seuraava toimenpide\n\nKirjoita 90-120 sanaa.\n\nKäytä selkeää rakennetta: ongelmat > toiveet > toimenpide. Raporttikirjoitus on tiivistä ja asiallista.",
        answerText = "Vuoden ensimmäinen kokonainen broileri on tosi pelottavaa ja se näkyy myös siinä tapauksessa jos se ei ole mitään järkeä ja sen jälkeen lupasit monta vuotta hyvää kuvaa ei ole mitään järkeä on ollut parisuhde ja se näkyy sun käytössä on myös se on ihan hyvä juttu siinä vaiheessa että se ei vastannut vielä ole mutta en löytänyt mutta en löytänyt nopeasti palaa takaisin ja masturboida webcam amatoori isot upeat ja se näkyy myös päätyi kuukausi ja",
        wordCounter = "76 / 105 words",
        saved = true
    ),
    PracticeTask(
        screenshot = "IMG_0426-IMG_0427",
        skill = Skill.Writing,
        number = "13/17",
        title = "Kirjoita virallinen sähköpostiviesti.",
        cefr = "B1 — Can write a formal letter or email on familiar topics.",
        passage = "Olet tilannut verkkokaupasta tuotteen, joka on tullut rikki. Kirjoita reklamaatioviesti verkkokaupan asiakaspalveluun.\n\nViestin tulee sisältää:\n- tilausnumero (keksitty, esim. TK-20481)\n- kuvaus ongelmasta\n- mitä toivot tilanteen ratkaisuksi\n\nKirjoita 80-110 sanaa virallisella asiakaspalvelutyyliä käyttäen.\n\nKäytä virallista tervehdystä ja lopetusta. Esitä faktat selkeässä järjestyksessä. Pyyntösi pitää olla yksiselitteinen.",
        answerText = "Nuo on ollut tosi huono puoli että on aika erota asiasanat arviointi on tosi vaikea löytää jos se ei ole vielä julkisia että se ei ole vielä julkisia että se ei vastannut mitään tekemistä niin että ne on niin tärkeä nyt on tilanne oli joku suunnitelma se että kiusan se että kiusan sua ei oo vielä siinä viestissä on tosi pelottavaa kaikki nähtävä se että kiusan se on ihan ok jos et haluaa ostaa mitä lupasit monta",
        wordCounter = "77 / 95 words",
        saved = true
    ),
    PracticeTask(
        screenshot = "IMG_0428",
        skill = Skill.Speaking,
        number = "14/17",
        title = "Keskustelu työn haasteista.",
        cefr = "B2 — Can take an active part in discussions in familiar contexts.",
        passage = "Osallistu keskusteluun työn stressistä AI-kumppanin kanssa. Kerro omista kokemuksistasi, anna mielipiteesi stressinhallinnan keinoista ja kysy kumppanin ajatuksia.\n\nReagoi AI:n vastauksiin — älä vain monologoi. Käytä korjauskieltä jos et ymmärrä: 'Tarkoitatko, että...?'"
    ),
    PracticeTask(
        screenshot = "IMG_0429",
        skill = Skill.Speaking,
        number = "15/17",
        title = "Terveydenhuollon roolipeli.",
        cefr = "B2 — Can describe experiences and give reasons for professional decisions.",
        passage = "Harjoittele terveydenhuollon tilanteita AI-kumppanin kanssa. Olet potilas, joka käy lääkärissä. Kerro oireesi selkeästi ja kysy tietoa hoitovaihtoehdoista.\n\nKäytä täsmällistä kieltä oireista: milloin alkoi, kuinka vakava, mikä helpottaa. Esitä myös yksi kysymys lääkärille."
    ),
    PracticeTask(
        screenshot = "IMG_0430",
        skill = Skill.Speaking,
        number = "16/17",
        title = "Mielipide suomalaisesta ruokakulttuurista.",
        cefr = "B1 — Can express and justify opinions on familiar topics.",
        passage = "Kerro suomalaisesta ruokakulttuurista AI-kumppanille. Puhu ainakin kahdesta suomalaisesta ruoasta ja sano, pidätkö niistä vai et. Perustele mielipiteesi.\n\nKäytä rakennetta: mainitse ruoka > anna mielipide > perustele. Vältä liian lyhyitä vastauksia."
    ),
    PracticeTask(
        screenshot = "IMG_0431",
        skill = Skill.Speaking,
        number = "17/17",
        title = "Ajanvaraus puhelimitse.",
        cefr = "B1 — Can deal with most situations likely to arise when booking appointments.",
        passage = "Soitat palvelutoimistoon varataksesi ajan. Sinun täytyy selittää syy käyntiisi, ehdottaa sopivaa aikaa ja vahvistaa varaus. Harjoittele tätä puhelinkeskustelua AI-kumppanin kanssa.\n\nKäytä kohteliasta kieltä. Kuuntele AI:n kysymykset tarkasti ja vastaa täysillä lauseilla. Tavoite on viisi vuoroa."
    )
)
}
