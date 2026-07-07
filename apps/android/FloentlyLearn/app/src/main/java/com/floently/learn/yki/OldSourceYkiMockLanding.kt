package com.floently.learn.yki

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val mockBlue = Color(0xFF2563EB)
private val mockAmber = Color(0xFFF59E0B)
private val mockSurface = Color.White
private val mockText = Color(0xFF0F172A)
private val mockMuted = Color(0xFF64748B)
private val mockBorder = Color(0xFFE2E8F0)
private val mockInfo = Color(0xFFF8FAFC)

@Composable
internal fun OldSourceYkiMockLanding(
    onStartExam: () -> Unit,
    onOpenMockCycle: () -> Unit,
    onOpenPractice: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Full YKI exam simulation flow separated from guided practice.",
            color = mockMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        OldMockSummaryCard()

        OldMockSectionsCard()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OldMockTaskCard(
                title = "Start YKI exam",
                detail = "Run the full runtime flow with reading, listening, writing, and speaking.",
                meta = "Timed exam mode",
                actionLabel = "Start",
                accent = mockBlue,
                modifier = Modifier.weight(1f),
                onClick = onStartExam
            )
            OldMockTaskCard(
                title = "Mock cycle",
                detail = "Practice pressure cycles before a complete exam attempt.",
                meta = "Recommended",
                actionLabel = "Open",
                accent = mockAmber,
                modifier = Modifier.weight(1f),
                onClick = onOpenMockCycle
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OldMockTaskCard(
                title = "Guided practice",
                detail = "Return to guided YKI practice blocks for targeted support.",
                meta = "Practice mode",
                actionLabel = "Open",
                accent = mockBlue,
                modifier = Modifier.weight(1f),
                onClick = onOpenPractice
            )
            OldMockTaskCard(
                title = "Results",
                detail = "Review exam results, section feedback, and certificate readiness.",
                meta = "After exam",
                actionLabel = "Open",
                accent = mockAmber,
                modifier = Modifier.weight(1f),
                onClick = onStartExam
            )
        }

        Button(
            onClick = onStartExam,
            colors = ButtonDefaults.buttonColors(containerColor = mockBlue),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Start YKI exam", color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OldMockSummaryCard() {
    Surface(
        color = mockSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, mockBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Certified bank overview", color = mockText, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(
                "Certified YKI-style tasks from the engine task bank · Authority: engine_v3_2_certified",
                color = mockMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Text(
                "The mock exam uses a complete exam runtime instead of the guided practice subset.",
                color = mockMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun OldMockSectionsCard() {
    val sections = listOf(
        "Reading" to "Text comprehension · about 25 min",
        "Listening" to "Audio comprehension · about 20 min",
        "Writing" to "Written production · about 30 min",
        "Speaking" to "Prompt and response · about 15 min"
    )
    Surface(
        color = mockInfo,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, mockBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Exam coverage", color = mockText, fontSize = 16.sp, fontWeight = FontWeight.Black)
            sections.forEach { (title, meta) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = mockText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(meta, color = mockMuted, fontSize = 12.sp)
                    }
                    Text(title.uppercase(), color = mockBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun OldMockTaskCard(
    title: String,
    detail: String,
    meta: String,
    actionLabel: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = mockSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, mockBorder),
        modifier = modifier.height(166.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.14f)))
            Text(title, color = mockText, fontSize = 14.sp, fontWeight = FontWeight.Black, lineHeight = 18.sp)
            Text(detail, color = mockMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Text(meta, color = mockMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(actionLabel, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}
