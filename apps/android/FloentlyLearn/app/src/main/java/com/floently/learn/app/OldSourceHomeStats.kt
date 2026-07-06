package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourceHomeStatsRow(
    palette: FloentlyPalette,
    vocabularyCount: String = "0",
    streakDays: String = "1",
    estimatedLevel: String = "B1"
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OldSourceHomeStatBox(
            value = vocabularyCount,
            label = "Sanastokohteet",
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        OldSourceHomeStatBox(
            value = streakDays,
            label = "Päiväputki",
            palette = palette,
            modifier = Modifier.weight(1f)
        )
        OldSourceHomeStatBox(
            value = estimatedLevel,
            label = "Arvioitu taso",
            palette = palette,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OldSourceHomeStatBox(
    value: String,
    label: String,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                color = palette.text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = palette.muted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
