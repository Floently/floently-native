package com.floently.learn.roleplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floently.shared.design.FloentlyPalette

@Composable
internal fun RoleplayLevelStrip(
    selectedLevel: RoleplayLevel,
    palette: FloentlyPalette,
    onSelect: (RoleplayLevel) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RoleplayLevel.entries.forEach { level ->
            val active = level == selectedLevel
            Surface(
                color = if (active) palette.primary.copy(alpha = 0.18f) else palette.cardMuted,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (active) palette.primary else palette.border),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(level) }
            ) {
                Text(
                    text = level.displayName,
                    color = if (active) palette.primary else palette.muted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
internal fun RoleplayStatusCard(
    title: String,
    body: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = body,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
