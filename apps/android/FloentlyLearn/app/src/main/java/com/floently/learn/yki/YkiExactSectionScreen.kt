package com.floently.learn.yki

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private val practiceBg = Color(0xFF080F20)
private val practiceCard = Color(0xFF121D36)
private val practiceCard2 = Color(0xFF172443)
private val practiceBorder = Color(0xFF263A67)
private val practiceText = Color(0xFFF6F8FF)
private val practiceMuted = Color(0xFFAAB8D3)
private val ykiPurple = Color(0xFF7C5CFF)
private val ykiOrange = Color(0xFFFF9F43)
private val ykiGreen = Color(0xFF26B56F)
private val ykiRed = Color(0xFFE45B63)

private val mockBg = Color(0xFFF7F8FC)
private val mockCard = Color.White
private val mockBorder = Color(0xFFE4E8F2)
private val mockText = Color(0xFF17213A)
private val mockMuted = Color(0xFF6A7284)
private val mockBlue = Color(0xFF317AF4)
private val mockPurple = Color(0xFF755CFF)
private val mockRed = Color(0xFFE84E5A)
private val mockGreen = Color(0xFF1FA66A)

private data class PracticeStep(
    val screenshot: String,
    val kind: PracticeKind,
    val progress: String = "",
    val skill: String = "Reading",
    val title: String = "",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val selected: Int? = null,
    val correct: Int? = null,
    val explanation: String = "",
    val answerText: String = "",
    val wordCount: String = "",
    val saved: Boolean = false
)

private enum class PracticeKind {
    Landing,
    Choice,
    Listening,
    Writing,
    Speaking
}

private data class MockStep(
    val screenshot: String,
    val kind: MockKind,
    val progressIndex: Int = 0,
    val section: String = "",
    val title: String = "",
    val prompt: String = "",
    val passage: String = "",
    val options: List<String> = emptyList(),
    val selected: Int? = null,
    val timer: String = "",
    val answerText: String = "",
    val activeRecording: Boolean = false,
    val submitted: Boolean = false
)

private enum class MockKind {
    Overview,
    Choice,
    Writing,
    Timer,
    Results
}

@Composable
fun YkiExactSectionScreen(
    mode: YkiScreenMode,
    onBack: () -> Unit
) {
    if (mode == YkiScreenMode.MockExam) {
        YkiMockExactFlow(onBack = onBack)
    } else {
        YkiPracticeExactFlow(onBack = onBack)
    }
}

@Composable
private fun YkiPracticeExactFlow(
    onBack: () -> Unit
) {
    val steps = remember { practiceSteps() }
    var index by remember { mutableStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(practiceBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            YkiPracticeTopBar(onBack = onBack)

            when (step.kind) {
                PracticeKind.Landing -> YkiPracticeLanding(
                    onStart = { index = 1 }
                )
                PracticeKind.Choice,
                PracticeKind.Listening,
                PracticeKind.Writing,
                PracticeKind.Speaking -> YkiPracticeTask(
                    step = step,
                    onAdvance = { index = (index + 1).coerceAtMost(steps.lastIndex) },
                    onBackStep = { index = (index - 1).coerceAtLeast(0) }
                )
            }

            Text(
                text = "${step.screenshot} / ${steps.size} mapped YKI Practice states",
                color = practiceMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticeTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(2.dp, Color(0xFF314A78)),
            modifier = Modifier
                .height(48.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = Color(0xFF9FB5EE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            color = practiceCard,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, practiceBorder)
        ) {
            Text(
                text = "Menu",
                color = practiceText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticeLanding(
    onStart: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = "YKI Exam",
            color = practiceText,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YkiLevelTab("A1-A2", false)
            YkiLevelTab("B1-B2", true)
            YkiLevelTab("C1-C2", false)
        }

        Surface(
            color = practiceCard,
            shape = RoundedCornerShape(34.dp),
            border = BorderStroke(1.dp, practiceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "B1-B2 YKI Practice",
                    color = practiceText,
                    fontSize = 29.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Practise the official exam sections in the same order: reading, listening, writing, and speaking.",
                    color = practiceMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YkiPracticeChip("Reading", ykiPurple)
                    YkiPracticeChip("Listening", ykiOrange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YkiPracticeChip("Writing", ykiOrange)
                    YkiPracticeChip("Speaking", ykiOrange)
                }
            }
        }

        Surface(
            color = practiceCard2,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, practiceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "YKI cards",
                    color = practiceText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Every task state from IMG_0409-IMG_0431 is represented in this YKI Practice section.",
                    color = practiceMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        YkiPracticePrimaryButton("Start exam block", ykiPurple, onStart)
    }
}

@Composable
private fun YkiLevelTab(
    text: String,
    active: Boolean
) {
    Surface(
        color = if (active) ykiPurple.copy(alpha = 0.22f) else practiceCard,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (active) ykiPurple else practiceBorder),
        modifier = Modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (active) Color.White else practiceMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticeTask(
    step: PracticeStep,
    onAdvance: () -> Unit,
    onBackStep: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(
            text = "YKI Exam",
            color = practiceText,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Black
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YkiLevelTab("A1-A2", false)
            YkiLevelTab("B1-B2", true)
            YkiLevelTab("C1-C2", false)
        }

        Surface(
            color = practiceCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, practiceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YkiPracticeChip(step.skill, if (step.skill == "Reading") ykiPurple else ykiOrange)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = step.progress,
                        color = practiceMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = step.title,
                    color = practiceText,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = step.prompt,
                    color = practiceMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )

                if (step.kind == PracticeKind.Listening) {
                    YkiPracticePlayButton()
                }

                if (step.kind == PracticeKind.Writing) {
                    YkiPracticeWritingBlock(step)
                }

                if (step.kind == PracticeKind.Speaking) {
                    YkiPracticeSpeakingBlock()
                }

                if (step.options.isNotEmpty()) {
                    step.options.forEachIndexed { i, option ->
                        val isSelected = step.selected == i
                        val isCorrect = step.correct == i
                        YkiPracticeOption(
                            text = option,
                            isSelected = isSelected,
                            isCorrect = isCorrect,
                            hasAnswer = step.selected != null
                        )
                    }
                }

                if (step.explanation.isNotBlank()) {
                    Surface(
                        color = Color(0xFF241A2D),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ykiOrange.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step.explanation,
                            color = Color(0xFFFFD49A),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    YkiPracticeGhostButton("Previous", onBackStep)
                    Spacer(modifier = Modifier.weight(1f))
                    val label = when {
                        step.kind == PracticeKind.Writing && !step.saved -> "Save answer"
                        step.kind == PracticeKind.Speaking -> "Next task"
                        step.selected == null && step.options.isNotEmpty() -> "Check answer"
                        else -> "Next task"
                    }
                    val color = if (step.kind == PracticeKind.Writing && !step.saved) ykiOrange else ykiPurple
                    YkiPracticeActionButton(label, color, onAdvance)
                }
            }
        }
    }
}

@Composable
private fun YkiPracticeChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun YkiPracticePlayButton() {
    Surface(
        color = ykiPurple,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "▶  Play audio",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun YkiPracticeWritingBlock(
    step: PracticeStep
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = Color(0xFF0E1931),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, practiceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 178.dp)
        ) {
            Text(
                text = step.answerText.ifBlank { "Write your answer here..." },
                color = if (step.answerText.isBlank()) practiceMuted else practiceText,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        Text(
            text = step.wordCount,
            color = practiceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun YkiPracticeSpeakingBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        YkiPracticePrimaryButton("Start conversation roleplay", ykiOrange, {})
        Surface(
            color = practiceCard2,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, practiceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Mark complete when you have practised the spoken response.",
                color = practiceMuted,
                fontSize = 15.sp,
                modifier = Modifier.padding(15.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticeOption(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    hasAnswer: Boolean
) {
    val color = when {
        hasAnswer && isCorrect -> ykiGreen
        hasAnswer && isSelected && !isCorrect -> ykiRed
        else -> practiceCard2
    }
    val border = when {
        hasAnswer && isCorrect -> ykiGreen
        hasAnswer && isSelected && !isCorrect -> ykiRed
        else -> practiceBorder
    }

    Surface(
        color = color.copy(alpha = if (hasAnswer) 0.22f else 1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(border.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        hasAnswer && isCorrect -> "✓"
                        hasAnswer && isSelected && !isCorrect -> "×"
                        else -> ""
                    },
                    color = border,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = practiceText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun YkiPracticeGhostButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, practiceBorder),
        modifier = Modifier
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = practiceMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticeActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(52.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }
    }
}

@Composable
private fun YkiPracticePrimaryButton(
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
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun YkiMockExactFlow(
    onBack: () -> Unit
) {
    val steps = remember { mockSteps() }
    var index by remember { mutableStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mockBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            YkiMockTopBar(onBack = onBack)

            when (step.kind) {
                MockKind.Overview -> YkiMockOverview(
                    screenshot = step.screenshot,
                    onStart = { index = (index + 1).coerceAtMost(steps.lastIndex) }
                )
                MockKind.Choice,
                MockKind.Writing,
                MockKind.Timer -> YkiMockTask(
                    step = step,
                    onPrevious = { index = (index - 1).coerceAtLeast(0) },
                    onNext = { index = (index + 1).coerceAtMost(steps.lastIndex) }
                )
                MockKind.Results -> YkiMockResults(
                    onPrevious = { index = (index - 1).coerceAtLeast(0) }
                )
            }

            Text(
                text = "${step.screenshot} / ${steps.size} mapped YKI Mock Exam states",
                color = mockMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun YkiMockTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, mockBorder),
            modifier = Modifier
                .height(48.dp)
                .clickable(onClick = onBack)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Back",
                    color = mockBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "YKI Mock Exam",
            color = mockText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun YkiMockOverview(
    screenshot: String,
    onStart: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = mockCard,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, mockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = "FULL YKI EXAM",
                    color = mockPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.4.sp
                )
                Text(
                    text = "Full YKI Exam simulation",
                    color = mockText,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Complete the same sections shown in IMG_0432-IMG_0479: reading, writing, listening and speaking.",
                    color = mockMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )
            }
        }

        YkiMockSectionCard("Reading comprehension", "Read passages and answer multiple-choice questions.", "18 min")
        YkiMockSectionCard("Writing", "Write messages, reports and structured answers.", "20 min")
        YkiMockSectionCard("Listening", "Play audio and answer comprehension questions.", "12 min")
        YkiMockSectionCard("Speaking", "Timed preparation and recording tasks.", "15 min")

        val label = if (screenshot == "IMG_0434") "Start YKI exam" else "Continue overview"
        YkiMockPrimaryButton(label, mockBlue, onStart)
    }
}

@Composable
private fun YkiMockSectionCard(
    title: String,
    body: String,
    time: String
) {
    Surface(
        color = mockCard,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, mockBorder),
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
                Text(
                    text = title,
                    color = mockText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = body,
                    color = mockMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
            Surface(
                color = Color(0xFFEAF1FF),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = time,
                    color = mockBlue,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun YkiMockTask(
    step: MockStep,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        YkiMockProgressDots(active = step.progressIndex)

        Surface(
            color = mockCard,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, mockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = step.section.uppercase(),
                    color = mockPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.8.sp
                )
                Text(
                    text = step.title,
                    color = mockText,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black
                )

                if (step.timer.isNotBlank()) {
                    YkiMockTimerCard(step)
                }

                if (step.passage.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF4F6FB),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, mockBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step.passage,
                            color = mockText,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            modifier = Modifier.padding(15.dp)
                        )
                    }
                }

                Text(
                    text = step.prompt,
                    color = mockMuted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )

                if (step.kind == MockKind.Writing) {
                    OutlinedTextField(
                        value = step.answerText,
                        onValueChange = {},
                        label = { Text("Your answer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                    )
                }

                if (step.options.isNotEmpty()) {
                    step.options.forEachIndexed { i, option ->
                        YkiMockOption(
                            text = option,
                            selected = step.selected == i
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    YkiMockGhostButton("Previous", onPrevious)
                    Spacer(modifier = Modifier.weight(1f))
                    val actionLabel = when {
                        step.submitted -> "Submit exam"
                        step.kind == MockKind.Timer && step.activeRecording -> "Recording"
                        step.kind == MockKind.Timer && step.timer == "00:00" -> "Next question"
                        step.progressIndex >= 14 -> "Submit exam"
                        step.progressIndex >= 7 -> "Next question"
                        else -> "Continue to next question"
                    }
                    val color = when {
                        step.submitted -> mockGreen
                        step.activeRecording -> mockRed
                        else -> mockBlue
                    }
                    YkiMockActionButton(actionLabel, color, onNext)
                }
            }
        }
    }
}

@Composable
private fun YkiMockProgressDots(
    active: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(8) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == active % 8) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (i <= active % 8) mockBlue else Color(0xFFD6DCE8))
            )
        }
    }
}

@Composable
private fun YkiMockTimerCard(
    step: MockStep
) {
    Surface(
        color = if (step.activeRecording) Color(0xFFFFEEF0) else Color(0xFFF4F6FB),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, if (step.activeRecording) mockRed.copy(alpha = 0.45f) else mockBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = step.timer,
                color = if (step.activeRecording) mockRed else mockText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (step.activeRecording) "Recording" else "Prepare your answer",
                color = if (step.activeRecording) mockRed else mockMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun YkiMockOption(
    text: String,
    selected: Boolean
) {
    Surface(
        color = if (selected) Color(0xFFEAF1FF) else Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) mockBlue else mockBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) mockBlue else Color(0xFFE7EBF3)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selected) "✓" else "",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = mockText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun YkiMockResults(
    onPrevious: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        YkiMockProgressDots(active = 7)

        Surface(
            color = mockCard,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, mockBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "RESULTS",
                    color = mockPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.4.sp
                )
                Text(
                    text = "YKI Mock Exam complete",
                    color = mockText,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Surface(
                    color = Color(0xFFEAF8F1),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, mockGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "B1/B2 readiness",
                            color = mockGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "78%",
                            color = mockText,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                YkiResultRow("Reading", "Good")
                YkiResultRow("Writing", "Needs review")
                YkiResultRow("Listening", "Good")
                YkiResultRow("Speaking", "Submitted")

                YkiMockGhostButton("Previous", onPrevious)
            }
        }
    }
}

@Composable
private fun YkiResultRow(
    label: String,
    value: String
) {
    Surface(
        color = Color(0xFFF4F6FB),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, mockBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = mockText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = mockBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun YkiMockGhostButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, mockBorder),
        modifier = Modifier
            .height(50.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = mockMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp)
            )
        }
    }
}

@Composable
private fun YkiMockActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun YkiMockPrimaryButton(
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
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun practiceSteps(): List<PracticeStep> = listOf(
    PracticeStep("IMG_0409", PracticeKind.Landing),
    PracticeStep("IMG_0410", PracticeKind.Choice, "1/19", "Reading", "Reading comprehension", "Read the short notice and choose the best answer.", listOf("The appointment has been cancelled.", "The time has changed.", "The office is closed today.")),
    PracticeStep("IMG_0411", PracticeKind.Choice, "1/19", "Reading", "Reading comprehension", "Read the short notice and choose the best answer.", listOf("The appointment has been cancelled.", "The time has changed.", "The office is closed today."), selected = 1, correct = 1),
    PracticeStep("IMG_0412", PracticeKind.Choice, "2/19", "Reading", "Workplace text", "Choose the meaning of the message.", listOf("Bring the document tomorrow.", "Call the office today.", "Send the report later."), selected = 0, correct = 0),
    PracticeStep("IMG_0413", PracticeKind.Choice, "3/19", "Reading", "Long text task", "Read the longer paragraph. The answer options continue below just like the original screenshot scroll.", listOf("The customer needs a new time.", "The nurse asks for more information.", "The delivery is delayed.")),
    PracticeStep("IMG_0414", PracticeKind.Choice, "3/19", "Reading", "Long text task", "Read the longer paragraph.", listOf("The customer needs a new time.", "The nurse asks for more information.", "The delivery is delayed."), selected = 2, correct = 0, explanation = "Explanation: the key phrase says the customer wants to move the appointment."),
    PracticeStep("IMG_0415", PracticeKind.Choice, "4/19", "Reading", "Instruction task", "Choose the correct interpretation.", listOf("Start immediately.", "Wait for confirmation.", "Cancel the visit."), selected = 0, correct = 1, explanation = "Explanation: the instruction asks you to wait before acting."),
    PracticeStep("IMG_0416", PracticeKind.Choice, "5/19", "Reading", "Message task", "Choose the best response.", listOf("Thank you, I will come at 12.", "I cannot read the message.", "No one is available."), selected = 2, correct = 0, explanation = "Explanation: the selected answer does not match the requested time."),
    PracticeStep("IMG_0417", PracticeKind.Listening, "6/19", "Listening", "Listening comprehension", "Play the audio and choose the best answer.", listOf("The speaker asks for help.", "The speaker confirms the appointment.", "The speaker reports a delay.")),
    PracticeStep("IMG_0418", PracticeKind.Listening, "6/19", "Listening", "Listening comprehension", "Play the audio and choose the best answer.", listOf("The speaker asks for help.", "The speaker confirms the appointment.", "The speaker reports a delay."), selected = 0, correct = 1, explanation = "Explanation: the audio confirms the appointment."),
    PracticeStep("IMG_0419", PracticeKind.Listening, "7/19", "Listening", "Phone message", "Listen and select what the caller wants.", listOf("A new prescription.", "A new appointment.", "A written report."), selected = 1, correct = 1),
    PracticeStep("IMG_0420", PracticeKind.Listening, "8/19", "Listening", "Short announcement", "Listen and choose the correct summary.", listOf("The meeting starts later.", "The meeting is cancelled.", "The meeting is online."), selected = 2, correct = 2),
    PracticeStep("IMG_0421", PracticeKind.Listening, "9/19", "Listening", "Service message", "Listen and select the right action.", listOf("Wait in the lobby.", "Call again tomorrow.", "Send the paper now."), selected = 1, correct = 0, explanation = "Explanation: the message asks the person to wait."),
    PracticeStep("IMG_0422", PracticeKind.Writing, "10/19", "Writing", "Write a short message", "Write at least 100 words about the situation.", answerText = "", wordCount = "60/100 words"),
    PracticeStep("IMG_0423", PracticeKind.Writing, "10/19", "Writing", "Write a short message", "Write at least 100 words about the situation.", answerText = "Hei, haluan kysyä uudesta ajasta. Minulle sopii ensi viikolla maanantai tai tiistai. Kiitos avusta.", wordCount = "84/100 words"),
    PracticeStep("IMG_0424", PracticeKind.Writing, "11/19", "Writing", "Write a report", "Write a short work report based on the notes.", answerText = "Raportti: Asiakas soitti aamulla ja kertoi, että hän ei pääse sovittuun aikaan. Hän pyysi uutta aikaa ja vahvistusta sähköpostilla.", wordCount = "102/100 words"),
    PracticeStep("IMG_0425", PracticeKind.Writing, "11/19", "Writing", "Write a report", "Saved answer.", answerText = "Raportti: Asiakas soitti aamulla ja kertoi, että hän ei pääse sovittuun aikaan. Hän pyysi uutta aikaa ja vahvistusta sähköpostilla.", wordCount = "102/100 words", saved = true),
    PracticeStep("IMG_0426", PracticeKind.Writing, "12/19", "Writing", "Write an email", "Write an email to confirm the meeting.", answerText = "Hei, vahvistan että kokous sopii minulle torstaina klo 13. Voin tuoda tarvittavat paperit mukanani.", wordCount = "76/100 words"),
    PracticeStep("IMG_0427", PracticeKind.Writing, "12/19", "Writing", "Write an email", "Saved answer.", answerText = "Hei, vahvistan että kokous sopii minulle torstaina klo 13. Voin tuoda tarvittavat paperit mukanani.", wordCount = "76/100 words", saved = true),
    PracticeStep("IMG_0428", PracticeKind.Speaking, "13/19", "Speaking", "Speaking roleplay", "Start a conversation and answer as naturally as you can."),
    PracticeStep("IMG_0429", PracticeKind.Speaking, "14/19", "Speaking", "Occupational roleplay", "Explain a workplace situation clearly."),
    PracticeStep("IMG_0430", PracticeKind.Speaking, "15/19", "Speaking", "Restaurant discussion", "Discuss a problem politely."),
    PracticeStep("IMG_0431", PracticeKind.Speaking, "16/19", "Speaking", "Phone call", "Answer the call and ask for the needed information.")
)

private fun mockSteps(): List<MockStep> = listOf(
    MockStep("IMG_0432", MockKind.Overview),
    MockStep("IMG_0433", MockKind.Overview),
    MockStep("IMG_0434", MockKind.Overview),
    MockStep("IMG_0436", MockKind.Choice, 0, "Reading", "Reading comprehension", "Read the passage and choose the best answer.", "The customer has received a message about a changed appointment. The new time is shown at the end of the message.", listOf("The appointment is cancelled.", "The appointment time changed.", "The customer must pay now.")),
    MockStep("IMG_0437", MockKind.Choice, 1, "Reading", "Reading comprehension", "Continue after selecting the answer.", "The customer has received a message about a changed appointment.", listOf("The appointment is cancelled.", "The appointment time changed.", "The customer must pay now."), selected = 1),
    MockStep("IMG_0438", MockKind.Choice, 2, "Reading", "Long reading passage", "Read the longer passage. The original screenshot shows a large text card and answer buttons below.", "A workplace announcement explains changes to opening hours, waiting room instructions and contact details for urgent cases.", listOf("Opening hours changed.", "The office moved.", "The case is closed.")),
    MockStep("IMG_0439", MockKind.Choice, 3, "Reading", "Reading task", "Choose the best answer.", "The text says that customers should wait for a confirmation before arriving.", listOf("Arrive immediately.", "Wait for confirmation.", "Call emergency services.")),
    MockStep("IMG_0440", MockKind.Choice, 4, "Reading", "Reading task", "Continue to the next question.", "The text says that customers should wait for a confirmation before arriving.", listOf("Arrive immediately.", "Wait for confirmation.", "Call emergency services."), selected = 1),
    MockStep("IMG_0441", MockKind.Choice, 5, "Reading", "Reading passage", "Read and answer.", "A longer passage appears here with the same spacing as the old exam task. The answer area sits under the passage.", listOf("The nurse writes a note.", "The client asks for another time.", "The office is closed.")),
    MockStep("IMG_0442", MockKind.Choice, 6, "Reading", "Reading passage", "Selected answer state.", "A longer passage appears here with the same spacing as the old exam task.", listOf("The nurse writes a note.", "The client asks for another time.", "The office is closed."), selected = 1),
    MockStep("IMG_0443", MockKind.Writing, 7, "Writing", "Writing task", "Write your answer in the field.", answerText = ""),
    MockStep("IMG_0444", MockKind.Writing, 7, "Writing", "Writing task", "Answer entered.", answerText = "Hei, haluan varata uuden ajan ensi viikolle. Minulle sopii tiistai aamupäivällä."),
    MockStep("IMG_0445", MockKind.Writing, 7, "Writing", "Structured writing task", "Use the bullet points in your answer.", passage = "Mention: reason for message, requested time, polite ending.", answerText = ""),
    MockStep("IMG_0446", MockKind.Writing, 7, "Writing", "Structured writing task", "Continue to next section.", passage = "Mention: reason for message, requested time, polite ending.", answerText = "Hei, kirjoitan koska tarvitsen uuden ajan. Minulle sopii maanantai tai keskiviikko. Kiitos paljon avusta."),
    MockStep("IMG_0447", MockKind.Timer, 0, "Speaking", "Prepare answer", "Read the task. Timer is visible in the center.", timer = "00:07"),
    MockStep("IMG_0448", MockKind.Timer, 1, "Speaking", "Prepare answer", "You have time to think before recording.", timer = "00:29"),
    MockStep("IMG_0449", MockKind.Timer, 1, "Speaking", "Prepare answer", "Continue preparing.", timer = "00:27"),
    MockStep("IMG_0450", MockKind.Timer, 1, "Speaking", "Prepare answer", "Timer is almost halfway.", timer = "00:17"),
    MockStep("IMG_0451", MockKind.Timer, 1, "Speaking", "Prepare answer", "Timer nearly finished.", timer = "00:03"),
    MockStep("IMG_0452", MockKind.Timer, 2, "Speaking", "Recording task", "Recording state visible.", timer = "00:59", activeRecording = true),
    MockStep("IMG_0453", MockKind.Timer, 2, "Speaking", "Recording task", "Continue recording.", timer = "00:53", activeRecording = true),
    MockStep("IMG_0454", MockKind.Timer, 2, "Speaking", "Recording task", "Continue recording.", timer = "00:38", activeRecording = true),
    MockStep("IMG_0455", MockKind.Timer, 2, "Speaking", "Recording ready", "Next question is active.", timer = "00:30"),
    MockStep("IMG_0456", MockKind.Timer, 3, "Speaking", "Prepare answer", "Timer visible.", timer = "00:09"),
    MockStep("IMG_0457", MockKind.Timer, 3, "Speaking", "Prepare answer", "Timer visible.", timer = "00:05"),
    MockStep("IMG_0458", MockKind.Timer, 4, "Speaking", "Recording task", "Recording timer.", timer = "00:20", activeRecording = true),
    MockStep("IMG_0459", MockKind.Timer, 4, "Speaking", "Recording task", "Recording timer.", timer = "00:58", activeRecording = true),
    MockStep("IMG_0460", MockKind.Timer, 4, "Speaking", "Recording task", "Recording timer.", timer = "00:54", activeRecording = true),
    MockStep("IMG_0461", MockKind.Timer, 4, "Speaking", "Recording task", "Recording timer.", timer = "00:33", activeRecording = true),
    MockStep("IMG_0462", MockKind.Timer, 4, "Speaking", "Recording task", "Red recording state.", timer = "00:26", activeRecording = true),
    MockStep("IMG_0463", MockKind.Timer, 4, "Speaking", "Recording task", "Red recording state.", timer = "00:23", activeRecording = true),
    MockStep("IMG_0465", MockKind.Timer, 5, "Speaking", "Submitted answer", "Continue to next question.", timer = "00:20", submitted = true),
    MockStep("IMG_0466", MockKind.Timer, 5, "Speaking", "Timer complete", "Time is complete.", timer = "00:00"),
    MockStep("IMG_0467", MockKind.Timer, 5, "Speaking", "Prepare answer", "Timer visible.", timer = "00:12"),
    MockStep("IMG_0468", MockKind.Timer, 5, "Speaking", "Prepare answer", "Timer visible.", timer = "00:29"),
    MockStep("IMG_0469", MockKind.Timer, 6, "Listening", "Listening timer", "Timer visible.", timer = "00:58"),
    MockStep("IMG_0470", MockKind.Timer, 6, "Listening", "Listening timer", "Timer visible.", timer = "00:52"),
    MockStep("IMG_0471", MockKind.Timer, 6, "Listening", "Listening timer", "Timer visible.", timer = "00:39"),
    MockStep("IMG_0472", MockKind.Timer, 6, "Listening", "Active state", "Red active state visible.", timer = "00:30", activeRecording = true),
    MockStep("IMG_0473", MockKind.Choice, 6, "Listening", "Answer listening question", "Choose the answer after listening.", options = listOf("The time changed.", "The call ended.", "The message was deleted."), selected = 0, timer = "00:07"),
    MockStep("IMG_0474", MockKind.Timer, 7, "Speaking", "Speaking task", "Timer visible.", timer = "00:30"),
    MockStep("IMG_0475", MockKind.Timer, 7, "Speaking", "Speaking task", "Timer visible.", timer = "00:58"),
    MockStep("IMG_0476", MockKind.Timer, 7, "Speaking", "Speaking task", "Red status visible.", timer = "00:14", activeRecording = true),
    MockStep("IMG_0477", MockKind.Timer, 7, "Speaking", "Submitted speaking", "Submit exam button visible.", timer = "00:00", submitted = true),
    MockStep("IMG_0478", MockKind.Results),
    MockStep("IMG_0479", MockKind.Results)
)
