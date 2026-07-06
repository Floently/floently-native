package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourceHomeActionCard(
    label: String,
    title: String,
    body: String,
    primary: String,
    secondary: String? = null,
    palette: FloentlyPalette,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(OldSourceHomeTokens.largeCardRadius),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OldSourceSectionLabel(text = label, palette = palette)

            Text(
                text = title,
                color = palette.text,
                fontSize = OldSourceHomeTokens.guideTitleSize,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp
            )

            Text(
                text = body,
                color = palette.muted,
                fontSize = OldSourceHomeTokens.guideBodySize,
                lineHeight = OldSourceHomeTokens.guideBodyLineHeight
            )

            if (secondary == null) {
                Surface(
                    color = palette.primary,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPrimary)
                ) {
                    Text(
                        text = primary,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 13.dp)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.62f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSecondary?.invoke() }
                    ) {
                        Text(
                            text = secondary,
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 13.dp)
                        )
                    }

                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onPrimary)
                    ) {
                        Text(
                            text = primary,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 13.dp)
                        )
                    }
                }
            }
        }
    }
}
