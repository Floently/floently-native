package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourceReadinessCards(
    palette: FloentlyPalette,
    estimatedLevel: String = "B1"
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(OldSourceHomeTokens.largeCardRadius),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Readiness pillars",
                    color = palette.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = palette.card,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        text = "Est. level $estimatedLevel",
                        color = palette.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OldSourceSkillMeter("Kuuntelu", 78, palette.primary, palette)
                OldSourceSkillMeter("Puhuminen", 64, palette.accent, palette)
                OldSourceSkillMeter("Kirjoittaminen", 56, palette.warning, palette)
            }
        }
    }
}

@Composable
private fun OldSourceSkillMeter(
    label: String,
    pct: Int,
    color: Color,
    palette: FloentlyPalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$pct%",
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.card)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.coerceIn(0, 100) / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}
