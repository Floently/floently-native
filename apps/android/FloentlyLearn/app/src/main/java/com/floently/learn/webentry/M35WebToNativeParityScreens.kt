package com.floently.learn.webentry

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

/**
 * M35 Agent C isolated native equivalents for the old public web-to-Learn path.
 *
 * These screens are intentionally not wired to shared navigation here. Agent C owns
 * the isolated UI surfaces; Agent E/integration owns shared navigation, auth shell,
 * global theme, shared i18n, settings, billing, account, and release gates.
 */
@Composable
fun M35PublicFloentlyLandingNativeScreen(
    onBookDemo: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSupport: () -> Unit = {},
    onLegal: () -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            M35Hero(
                eyebrow = "YKI, work, and life in Finland",
                title = "Pass YKI. Succeed in work and life in Finland.",
                body = "Native equivalent of the old Floently web hero with dark premium layout, animated correction demo, trust chips, and public CTAs.",
                primary = "Book a demo",
                secondary = "Sign in",
                palette = palette,
                onPrimary = onBookDemo,
                onSecondary = onSignIn
            )

            M35Section(
                eyebrow = "THE WALL",
                title = "Finnish practice for real outcomes",
                body = "The old landing outcomes are represented as native cards for YKI readiness, workplace communication, settlement support, and professional Finnish.",
                palette = palette
            )

            M35TwoColumn(
                left = { M35Metric("01", "YKI readiness", palette.primary, palette) },
                right = { M35Metric("02", "Workplace communication", palette.accent, palette) }
            )
            M35TwoColumn(
                left = { M35Metric("03", "Settlement support", palette.warning, palette) },
                right = { M35Metric("04", "Professional Finnish", Color(0xFF3EC5A8), palette) }
            )

            M35Section(
                eyebrow = "PATHWAYS",
                title = "Choose the right Floently path",
                body = "Learners, employers, and cities match the old web pathway structure.",
                palette = palette,
                dark = true
            )

            M35ActionCard("FOR LEARNERS", "Learn Finnish for YKI, work, and daily life", "Start with placement, then follow the recommended practice path.", "Sign in", palette.primary, palette, onSignIn)
            M35ActionCard("FOR EMPLOYERS", "Support workplace Finnish", "Book a demo for teams, care work, customer service, and onboarding.", "Book a demo", Color(0xFF3EC5A8), palette, onBookDemo)
            M35ActionCard("FOR CITIES", "Scalable integration support", "Programme-ready Finnish practice for settlement and employment.", "Book a demo", Color(0xFF7A9FFF), palette, onBookDemo)

            M35Section(
                eyebrow = "INSIDE FLOENTLY",
                title = "YKI, Professional Finnish, and everyday life",
                body = "Native equivalent of the old platform cards.",
                palette = palette
            )

            M35PlatformCard("YKI practice", "Exam-style reading, writing, listening, and speaking practice.", palette.primary, palette)
            M35PlatformCard("Professional Finnish", "Profession-aware work situations, reports, calls, and roleplay.", Color(0xFF3EC5A8), palette)
            M35PlatformCard("Everyday Finnish", "Daily situations, vocabulary, cards, and roleplay for life in Finland.", Color(0xFF7A9FFF), palette)

            M35FooterLinks(palette = palette, onSupport = onSupport, onLegal = onLegal)
        }
    }
}

@Composable
fun M35LearnLandingNativeScreen(
    onStartPlacement: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onBookDemo: () -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            M35Hero(
                eyebrow = "FLOENTLY LEARN",
                title = "Pass YKI and succeed in work and life in Finland",
                body = "Native Learn landing equivalent for learn.floently.com before the authenticated Learn shell.",
                primary = "Start placement",
                secondary = "Sign in",
                palette = palette,
                onPrimary = onStartPlacement,
                onSecondary = onSignIn
            )
            M35ActionCard("TRUST", "YKI-aligned and profession-specific", "The old trust row is represented as native cards and chips.", "Book a demo", palette.accent, palette, onBookDemo)
        }
    }
}

@Composable
fun M35OnboardingPlacementNativeScreen(
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M35Section(
                eyebrow = "QUICK PLACEMENT",
                title = "Find your Finnish starting point",
                body = "Native representation of the old placement flow: welcome, goal, self-assessment, adaptive quiz, result, and route into Learn.",
                palette = palette,
                dark = true
            )

            M35PlacementStep("01", "Welcome", "Nine adaptive questions, about two minutes.", palette.primary, palette)
            M35PlacementStep("02", "Intent quiz", "Choose Everyday/YKI or workplace-professional Finnish.", palette.accent, palette)
            M35PlacementStep("03", "Plan selection", "Pick the path that matches your goal and available time.", palette.warning, palette)
            M35PlacementStep("04", "Practice frequency", "Set a daily or weekly practice routine.", Color(0xFF9D7CFF), palette)
            M35PlacementStep("05", "Profession selection", "Keep the profession route open when work Finnish is relevant.", Color(0xFF3EC5A8), palette)
            M35PlacementStep("06", "Placement questions", "Adaptive questions fade between items and estimate level.", Color(0xFFFF7A7A), palette)
            M35PlacementStep("07", "Placement result", "Show band, skill profile, confidence, and recommended next step.", palette.primary, palette)

            M35PrimarySecondary(
                primary = "Continue",
                secondary = "Skip for now",
                palette = palette,
                onPrimary = onComplete,
                onSecondary = onSkip
            )
        }
    }
}

@Composable
fun M35ProfessionalBranchNativeScreen(
    onEveryday: () -> Unit = {},
    onProfession: () -> Unit = {},
    onCards: () -> Unit = {},
    onRoleplay: () -> Unit = {}
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M35Section(
                eyebrow = "PROFESSIONAL FINNISH",
                title = "Everyday Finnish or My Profession",
                body = "Native branch parity for choosing foundations or profession-aware workplace Finnish.",
                palette = palette,
                dark = true
            )

            M35TwoColumn(
                left = { M35BranchCard("Everyday Finnish", "Build safer A1-A2 foundations first.", palette.primary, palette, onEveryday) },
                right = { M35BranchCard("My Profession", "Practise work scenarios, reports, calls, and incidents.", palette.accent, palette, onProfession) }
            )

            M35PlacementStep("A1-A2", "Foundation work Finnish", "Simple workplace phrases, introductions, and everyday work routines.", palette.primary, palette)
            M35PlacementStep("B1-B2", "Independent work Finnish", "Phone calls, reports, interviews, customer service, and meetings.", palette.warning, palette)
            M35PlacementStep("C1-C2", "Specialist work Finnish", "Incidents, expert summaries, nuanced workplace communication.", Color(0xFF9D7CFF), palette)

            M35ActionCard("CARDS", "Profession-specific cards", "Route to cards with professional scope when shared navigation is wired.", "Open cards", palette.primary, palette, onCards)
            M35ActionCard("ROLEPLAY", "Profession-specific roleplay", "Route to professional speaking practice without confusing Everyday/YKI roleplay.", "Open roleplay", palette.accent, palette, onRoleplay)
        }
    }
}

@Composable
private fun M35Hero(
    eyebrow: String,
    title: String,
    body: String,
    primary: String,
    secondary: String,
    palette: FloentlyPalette,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Surface(
        color = Color(0xFF0A1838),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, Color(0xFF223E7A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                M35PulseDot(Color(0xFF3EC5A8))
                Spacer(modifier = Modifier.width(10.dp))
                Text(eyebrow, color = Color(0xFFB8C7EA), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Text(title, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, lineHeight = 42.sp)
            Text(body, color = Color(0xFFDDE7FF), style = MaterialTheme.typography.titleMedium)
            M35CorrectionDemo()
            M35PrimarySecondary(primary, secondary, palette, onPrimary, onSecondary, dark = true)
            M35TwoColumn(
                left = { M35TrustPill("YKI aligned", Modifier.fillMaxWidth()) },
                right = { M35TrustPill("Workplace Finnish", Modifier.fillMaxWidth()) }
            )
        }
    }
}

@Composable
private fun M35CorrectionDemo() {
    val transition = rememberInfiniteTransition(label = "m35-correction-demo")
    val scale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Reverse),
        label = "m35-correction-pulse"
    )
    Surface(
        color = Color(0xFF13213F),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF2A3E6E)),
        modifier = Modifier.fillMaxWidth().scale(scale)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("LIVE FINNISH COACH", color = Color(0xFF3EC5A8), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            M35CorrectionLine("Menen työssä huomenna.", "Menen töihin huomenna.")
            M35CorrectionLine("Haluan puhua asiakas.", "Haluan puhua asiakkaan kanssa.")
        }
    }
}

@Composable
private fun M35CorrectionLine(original: String, corrected: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(original, color = Color(0xFFFFA6A6), style = MaterialTheme.typography.bodyMedium)
        Text(corrected, color = Color(0xFFB9FFE9), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun M35Section(
    eyebrow: String,
    title: String,
    body: String,
    palette: FloentlyPalette,
    dark: Boolean = false
) {
    Surface(
        color = if (dark) Color(0xFF0A1838) else palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, if (dark) Color(0xFF223E7A) else palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(eyebrow, color = if (dark) Color(0xFF3EC5A8) else palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(title, color = if (dark) Color.White else palette.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(body, color = if (dark) Color(0xFFDDE7FF) else palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun M35ActionCard(
    eyebrow: String,
    title: String,
    body: String,
    cta: String,
    accent: Color,
    palette: FloentlyPalette,
    onClick: () -> Unit
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(eyebrow, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(title, color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            Text(cta, color = accent, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun M35PlacementStep(label: String, title: String, body: String, accent: Color, palette: FloentlyPalette) {
    Surface(color = palette.card, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, palette.border), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent.copy(alpha = 0.16f), shape = CircleShape, border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))) {
                Text(label, color = accent, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(body, color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun M35PlatformCard(title: String, body: String, accent: Color, palette: FloentlyPalette) {
    Surface(color = palette.cardMuted, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, palette.border), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(accent))
            Text(title, color = palette.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun M35Metric(label: String, value: String, accent: Color, palette: FloentlyPalette) {
    Surface(color = palette.card, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, palette.border), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(value, color = palette.text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun M35BranchCard(title: String, body: String, accent: Color, palette: FloentlyPalette, onClick: () -> Unit) {
    Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = accent, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
            Text(body, color = palette.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun M35PrimarySecondary(
    primary: String,
    secondary: String,
    palette: FloentlyPalette,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    dark: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FloentlyPrimaryButton(title = primary, product = FloentlyProduct.Learn, onClick = onPrimary)
        Surface(
            color = if (dark) Color.White.copy(alpha = 0.08f) else palette.cardMuted,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.18f) else palette.border),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSecondary)
        ) {
            Text(secondary, color = if (dark) Color.White else palette.primary, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 13.dp))
        }
    }
}

@Composable
private fun M35FooterLinks(palette: FloentlyPalette, onSupport: () -> Unit, onLegal: () -> Unit) {
    M35TwoColumn(
        left = { M35TextLink("Support/contact", palette, onSupport) },
        right = { M35TextLink("Privacy / terms", palette, onLegal) }
    )
}

@Composable
private fun M35TextLink(text: String, palette: FloentlyPalette, onClick: () -> Unit) {
    Surface(color = palette.cardMuted, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, palette.border), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(text, color = palette.primary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun M35TrustPill(label: String, modifier: Modifier) {
    Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), modifier = modifier) {
        Text(label, color = Color(0xFFDDE7FF), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun M35PulseDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "m35-pulse-dot")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "m35-pulse-dot-scale"
    )
    Box(modifier = Modifier.size(12.dp).scale(scale).clip(CircleShape).background(color))
}

@Composable
private fun M35TwoColumn(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) { left() }
        Box(modifier = Modifier.weight(1f)) { right() }
    }
}
