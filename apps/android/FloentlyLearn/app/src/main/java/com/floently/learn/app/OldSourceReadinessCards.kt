package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourceReadinessCards(
    palette: FloentlyPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OldSourceReadinessCard("Kuuntele", "Ääni", palette.primary, palette, Modifier.weight(1f))
        OldSourceReadinessCard("Puhu", "Rooli", palette.accent, palette, Modifier.weight(1f))
        OldSourceReadinessCard("Kirjoita", "YKI", palette.warning, palette, Modifier.weight(1f))
    }
}

@Composable
private fun OldSourceReadinessCard(
    title: String,
    label: String,
    accent: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = Color(0xFF0D1D42),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = title,
                color = palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label.uppercase(),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
        }
    }
}
