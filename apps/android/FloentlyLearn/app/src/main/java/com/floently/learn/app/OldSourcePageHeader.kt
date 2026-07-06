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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette

@Composable
fun OldSourcePageHeader(
    palette: FloentlyPalette,
    eyebrow: String? = null,
    showEyebrow: Boolean = true,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionPress: (() -> Unit)? = null,
    onMenuPress: (() -> Unit)? = null,
    pulseMenu: Boolean = false,
    compact: Boolean = false,
    showLogo: Boolean = true
) {
    val primary = palette.primary
    val raised = palette.cardMuted
    val border = palette.border
    val text = palette.text
    val muted = palette.muted

    val transition = rememberInfiniteTransition(label = "old-page-header")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulseMenu) 1.045f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "menu-pulse"
    )
    val beacon by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "menu-attention"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (showLogo) {
                    Column {
                        Text(
                            text = "floently",
                            color = Color(0xFF18B9FF),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "FLOENTLY",
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.5.sp
                        )
                    }
                } else {
                    Text(
                        text = "Floently",
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionLabel != null && onActionPress != null) {
                    OldSourceHeaderPill(
                        text = actionLabel,
                        color = primary,
                        background = raised,
                        border = border,
                        onClick = onActionPress
                    )
                }

                if (onMenuPress != null) {
                    Box(
                        modifier = Modifier.padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(84.dp)
                                .height(54.dp)
                                .scale(0.96f + (beacon * 0.04f))
                                .border(
                                    BorderStroke(1.dp, Color(0xBF8FB1FF)),
                                    RoundedCornerShape(999.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(82.dp)
                                .height(52.dp)
                                .background(Color(0x2E4F7FFF), RoundedCornerShape(999.dp))
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = ((beacon * 24f) - 12f).dp, y = (-22).dp)
                                .size(8.dp)
                                .background(Color(0xFFAFC4FF), RoundedCornerShape(4.dp))
                        )
                        OldSourceHeaderPill(
                            text = "Menu",
                            color = primary,
                            background = palette.primary.copy(alpha = 0.16f),
                            border = border,
                            onClick = onMenuPress,
                            modifier = Modifier.scale(pulse)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (showEyebrow && eyebrow != null) {
                Surface(
                    color = primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.22f))
                ) {
                    Text(
                        text = eyebrow.uppercase(),
                        color = primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = title,
                color = text,
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (compact) (-0.2).sp else (-0.3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OldSourceHeaderPill(
    text: String,
    color: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .heightIn(min = 36.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
