package com.floently.learn.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun OldSourceHomeHeroCard(
    palette: FloentlyPalette,
    eyebrow: String,
    title: String,
    subtitle: String,
    ctaLabel: String,
    completedPct: Int,
    streakDays: Int,
    onCta: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "old-home-hero")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero-sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-pulse"
    )

    Surface(
        color = Color(0xFF0F1F45),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF1C2F5A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(220.dp)
                    .offset(x = ((sweep * 540f) - 240f).dp, y = (-24).dp)
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = eyebrow.uppercase(),
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )

                        Surface(
                            color = Color(0x263EC58A),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0x333EC58A)),
                            modifier = Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((6f * pulse).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3EC58A))
                                )
                                Text(
                                    text = "Vire päällä",
                                    color = Color(0xFF8FF2C3),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Text(
                        text = title,
                        color = palette.text,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subtitle,
                        color = palette.muted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        color = palette.primary,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable(onClick = onCta)
                    ) {
                        Text(
                            text = ctaLabel,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(7.dp, palette.cardMuted), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(7.dp, palette.primary), CircleShape)
                        )
                        Text(
                            text = "$completedPct%",
                            color = palette.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (streakDays > 0) {
                        Surface(
                            color = Color(0x26F0C86D),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0x40F0C86D))
                        ) {
                            Text(
                                text = "$streakDays päivää",
                                color = Color(0xFFF0C86D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
