package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourcePathwayCard(
    title: String,
    subtitle: String,
    tag: String,
    accent: Color,
    palette: FloentlyPalette,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
            .height(150.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = if (locked) 0.10f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (locked) 8.dp else 12.dp)
                        .clip(CircleShape)
                        .background(if (locked) palette.muted else accent)
                )
            }

            Text(
                text = title,
                color = if (locked) palette.muted else palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                color = palette.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = accent.copy(alpha = if (locked) 0.10f else 0.16f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = if (locked) 0.10f else 0.18f))
            ) {
                Text(
                    text = tag,
                    color = if (locked) palette.muted else accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun OldSourceSectionLabel(
    text: String,
    palette: FloentlyPalette,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = palette.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        modifier = modifier.fillMaxWidth()
    )
}
