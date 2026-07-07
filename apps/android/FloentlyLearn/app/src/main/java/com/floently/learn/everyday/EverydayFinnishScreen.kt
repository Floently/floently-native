package com.floently.learn.everyday

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun EverydayFinnishScreen(
    onBack: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Everyday Finnish",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Choose how you want to practise normal daily Finnish.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            EverydayHeroCard(palette = palette)

            EverydaySectionLabel("PRACTICE MODES", palette)

            EverydayActionCard(
                label = "ROLEPLAY",
                title = "Roleplay",
                body = "Short daily conversations with coaching and variation.",
                accent = palette.accent,
                palette = palette,
                onClick = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
            )

            EverydayActionCard(
                label = "CARDS",
                title = "Cards",
                body = "Vocabulary and sentence cards for fast recall.",
                accent = palette.primary,
                palette = palette,
                onClick = { onDestinationSelected(LearnFeatureDestination.Cards) }
            )

            EverydayActionCard(
                label = "INTERVIEW",
                title = "Interview",
                body = "Practise realistic job interview conversations with the roleplay engine.",
                accent = Color(0xFF9D7CFF),
                palette = palette,
                onClick = { onDestinationSelected(LearnFeatureDestination.Roleplay) }
            )

            EverydayActionCard(
                label = "SPEECH",
                title = "Speech recording",
                body = "Practise spoken answers through the YKI speaking and roleplay flows.",
                accent = palette.warning,
                palette = palette,
                onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
            )

            FloentlyPrimaryButton(
                title = "Back to Learn",
                product = FloentlyProduct.Learn,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun EverydayHeroCard(
    palette: FloentlyPalette
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "EVERYDAY FINNISH",
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.4.sp
            )
            Text(
                text = "Roleplay, cards, interview and speech",
                color = palette.text,
                fontSize = 31.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "This hub must not skip straight into cards. It should behave like the old app: choose the practice type first, then enter the selected flow.",
                color = palette.muted,
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
        }
    }
}

@Composable
private fun EverydaySectionLabel(
    text: String,
    palette: FloentlyPalette
) {
    Text(
        text = text,
        color = palette.soft,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.4.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EverydayActionCard(
    label: String,
    title: String,
    body: String,
    accent: Color,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = label,
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Text(
                text = title,
                color = palette.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = body,
                color = palette.muted,
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
