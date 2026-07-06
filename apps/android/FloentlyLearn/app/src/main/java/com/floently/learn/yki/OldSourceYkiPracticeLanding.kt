package com.floently.learn.yki

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val oldYkiPurple = Color(0xFF7C3AED)
private val oldPracticeSurface = Color.White
private val oldPracticeText = Color(0xFF0F172A)
private val oldPracticeMuted = Color(0xFF64748B)
private val oldPracticeBorder = Color(0xFFE2E8F0)
private val oldPracticeInfo = Color(0xFFF0F4FF)
private val oldPracticeBlue = Color(0xFF2563EB)
private val oldPracticeAmber = Color(0xFFF59E0B)

@Composable
internal fun OldSourceYkiPracticeLanding(onStart: () -> Unit) {
    var selectedLevel by remember { mutableStateOf("B1-B2") }
    var selectedFocus by remember { mutableStateOf("Mixed") }
    val focusOptions = listOf("Mixed", "Reading", "Listening", "Writing", "Speaking")
    val counts = listOf("Reading" to 24, "Listening" to 18, "Writing" to 16, "Speaking" to 14)
    val recommendedSections = "Reading • Listening • Writing • Speaking"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Practice YKI with guided blocks before the full exam simulation.",
            color = oldPracticeMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("A2-B1", "B1-B2", "B2-C1").forEach { band ->
                OldPracticePill(band, selectedLevel == band, Modifier.weight(1f)) { selectedLevel = band }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            focusOptions.take(3).forEach { focus ->
                OldPracticePill(focus, selectedFocus == focus, Modifier.weight(1f)) { selectedFocus = focus }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            focusOptions.drop(3).forEach { focus ->
                OldPracticePill(focus, selectedFocus == focus, Modifier.weight(1f)) { selectedFocus = focus }
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OldPracticeTaskCard("Overview", "Adaptive preparation for reading, listening, writing, and speaking.", "B1-B2 pathway", "Open", oldYkiPurple, Modifier.weight(1f)) {}
            OldPracticeTaskCard("Guided pathway", "Start with the next recommended ${selectedFocus.lowercase()} block for $selectedLevel.", "15 min", "Start practice", oldPracticeBlue, Modifier.weight(1f), onStart)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OldPracticeTaskCard("Mock cycle", "Move from short practice into timed mock exam pressure.", "Recommended after practice", "Open", oldPracticeAmber, Modifier.weight(1f), onStart)
            OldPracticeTaskCard("Full exam", "Run a full YKI-style practice set for $selectedLevel.", "Reading • Listening • Writing • Speaking", "Start", oldYkiPurple, Modifier.weight(1f), onStart)
        }

        OldPracticeInfoCard("Find this later", "YKI Practice stays available from Home → YKI Prep. Mock exam and full exam are separate so practice never mixes with test pressure.")
        OldPracticeInfoCard(
            "Bank coverage for $selectedLevel",
            "72 tasks available · Recommended focus ${selectedFocus.lowercase()} · Sections: $recommendedSections",
            counts
        )

        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = oldYkiPurple),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Start exam block", color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OldPracticePill(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (active) oldYkiPurple else oldPracticeSurface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (active) oldYkiPurple else oldPracticeBorder),
        modifier = modifier.height(42.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (active) Color.White else oldPracticeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun OldPracticeTaskCard(title: String, detail: String, meta: String, actionLabel: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = oldPracticeSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, oldPracticeBorder),
        modifier = modifier.height(178.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.14f)))
            Text(title, color = oldPracticeText, fontSize = 15.sp, fontWeight = FontWeight.Black, lineHeight = 18.sp)
            Text(detail, color = oldPracticeMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Text(meta, color = oldPracticeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(actionLabel, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OldPracticeInfoCard(title: String, body: String, metrics: List<Pair<String, Int>> = emptyList()) {
    Surface(
        color = oldPracticeInfo,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFD8E3F2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = oldPracticeText, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(body, color = Color(0xFF4B5573), fontSize = 13.sp, lineHeight = 20.sp)
            if (metrics.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    metrics.forEach { (label, value) ->
                        Surface(color = Color.White, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color(0xFFD8E3F2))) {
                            Text("$label $value", color = oldPracticeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }
    }
}
