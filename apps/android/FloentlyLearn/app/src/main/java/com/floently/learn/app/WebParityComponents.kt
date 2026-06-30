package com.floently.learn.app

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
import androidx.compose.material3.MaterialTheme
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
fun WebChromeHeader(palette: FloentlyPalette, onHome: () -> Unit, onMenu: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("floently", color = Color(0xFF18B9FF), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("FLOENTLY", color = palette.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        WebTopPill("Home", palette, selected = false, onClick = onHome)
        Spacer(modifier = Modifier.width(10.dp))
        WebTopPill("Menu", palette, selected = true, onClick = onMenu)
    }
}

@Composable
fun WebTopPill(text: String, palette: FloentlyPalette, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFF183168) else Color(0xFF111B30),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF355AA0) else palette.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(text, color = if (selected) palette.primary else palette.text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontWeight = FontWeight.Black)
    }
}

@Composable
fun WebSectionLabel(text: String, palette: FloentlyPalette) {
    Text(text, color = palette.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, letterSpacing = 3.5.sp)
}

@Composable
fun WebStatusPill(text: String, palette: FloentlyPalette) {
    Surface(color = Color(0xFF103B3C), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color(0xFF1D7671))) {
        Text(text, color = palette.accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun WebHeroCard(label: String, title: String, body: String, action: String, palette: FloentlyPalette, progressText: String = "–", onAction: () -> Unit) {
    Surface(color = Color(0xFF13264A), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color(0xFF264170)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WebSectionLabel(label, palette)
                    Spacer(modifier = Modifier.width(10.dp))
                    WebStatusPill("• Vire päällä", palette)
                }
                Text(title, color = palette.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Surface(color = palette.primary, shape = RoundedCornerShape(999.dp), modifier = Modifier.clickable(onClick = onAction)) {
                    Text(action, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp), fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(76.dp).clip(CircleShape).border(9.dp, palette.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(progressText, color = palette.muted, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun WebPathTile(title: String, body: String, tag: String, dot: Color, palette: FloentlyPalette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(color = Color(0xFF101B2F), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFF223653)), modifier = modifier.height(196.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(dot.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(13.dp).clip(CircleShape).background(dot))
            }
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Surface(color = dot.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp)) {
                Text(tag, color = dot, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun WebStatRow(palette: FloentlyPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        WebStatBox("0", "Sanastokohteet", palette, Modifier.weight(1f))
        WebStatBox("1", "Päiväputki", palette, Modifier.weight(1f))
        WebStatBox("B1", "Arvioitu taso", palette, Modifier.weight(1f))
    }
}

@Composable
private fun WebStatBox(value: String, label: String, palette: FloentlyPalette, modifier: Modifier) {
    Surface(color = Color(0xFF101B2F), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF223653)), modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = palette.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, color = palette.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun WebInfoCard(label: String, title: String, body: String, primary: String, secondary: String? = null, palette: FloentlyPalette, onPrimary: () -> Unit, onSecondary: (() -> Unit)? = null) {
    Surface(color = Color(0xFF13264A), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, Color(0xFF2A4372)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WebSectionLabel(label, palette)
            Text(title, color = palette.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(body, color = palette.muted, style = MaterialTheme.typography.titleMedium)
            if (secondary == null) {
                Surface(color = palette.primary, shape = RoundedCornerShape(999.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onPrimary)) {
                    Text(primary, color = Color.White, modifier = Modifier.padding(vertical = 13.dp), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(color = Color.Transparent, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color(0xFF355AA0)), modifier = Modifier.weight(1f).clickable { onSecondary?.invoke() }) {
                        Text(secondary, color = palette.text, modifier = Modifier.padding(vertical = 13.dp), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                    Surface(color = palette.primary, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f).clickable(onClick = onPrimary)) {
                        Text(primary, color = Color.White, modifier = Modifier.padding(vertical = 13.dp), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun WebBackgroundGlow(palette: FloentlyPalette) {
    Box(modifier = Modifier.size(280.dp).offset(x = (-130).dp, y = (-90).dp).clip(CircleShape).background(palette.backgroundGlow.copy(alpha = 0.28f)))
}
