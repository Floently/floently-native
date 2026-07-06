package com.floently.learn.app

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.learn.i18n.LearnLanguage
import com.floently.learn.i18n.LearnTranslations
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen
import com.floently.shared.design.FloentlySecondaryButton

@Composable
fun LearnHomeScreen(
    session: FloentlyAuthSession,
    copy: LearnCopy = LearnTranslations.copy(LearnLanguage.EN),
    selectedLanguage: LearnLanguage = LearnLanguage.EN,
    onLanguageSelected: (LearnLanguage) -> Unit = {},
    onSignOut: () -> Unit,
    onBackToSuite: (() -> Unit)? = null,
    onDestinationSelected: (LearnFeatureDestination) -> Unit = {}
) {
    var showOldAppHomeHint by remember(session.user.email) { mutableStateOf(true) }
    var showOldAppDrawer by remember(session.user.email) { mutableStateOf(false) }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Box {
            OldAppEmberBackground(palette = palette)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(OldSourceHomeTokens.pageGap)
            ) {
                OldSourcePageHeader(
                    palette = palette,
                    eyebrow = "KieliTaika",
                    showEyebrow = false,
                    showLogo = true,
                    title = greeting(session),
                    subtitle = "All caught up! Choose the pathway that needs attention next.",
                    pulseMenu = true,
                    onMenuPress = { showOldAppDrawer = true }
                )

                OldSourceHomeHeroCard(
                    palette = palette,
                    eyebrow = "Language to Work",
                    title = "Next best step",
                    subtitle = "Bundle pathway active — continue YKI Prep or your profession track today.",
                    ctaLabel = "Continue pathway →",
                    completedPct = 100,
                    streakDays = 1,
                    onCta = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                )

                OldSourceSectionLabel(text = "Pathways", palette = palette)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    OldSourcePathwayCard(
                        title = "Everyday Finnish",
                        subtitle = "Roleplay, cards, interview and speech recording",
                        tag = "Everyday",
                        accent = palette.primary,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(LearnFeatureDestination.EverydayFinnish) }
                    )
                    OldSourcePathwayCard(
                        title = "Workplace scenarios",
                        subtitle = "Instructions, handovers, and reporting",
                        tag = "Scenarios",
                        accent = palette.warning,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    OldSourcePathwayCard(
                        title = "YKI Prep",
                        subtitle = "Practice and exam simulation",
                        tag = "YKI",
                        accent = Color(0xFF9D7CFF),
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                    )
                    OldSourcePathwayCard(
                        title = "All professions",
                        subtitle = "Vocabulary, interview, and work-ready Finnish",
                        tag = "Profession",
                        accent = palette.accent,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                    )
                }
                OldSourceReadinessCards(palette = palette)
                OldSourceHomeStatsRow(palette = palette)
                OldSourceHomeActionCard(
                    label = null,
                    title = "Deployment-ready flow",
                    body = "Navigation follows paid access, while Home and Billing still show the locked pathways clearly. That keeps upgrades understandable during deployment and later organization rollout.",
                    primary = "Helpful hint",
                    secondary = "Menu",
                    palette = palette,
                    onPrimary = { showOldAppHomeHint = true },
                    onSecondary = { showOldAppDrawer = true }
                )
                FloentlyPrimaryButton(title = copy.signOut, product = FloentlyProduct.Learn, onClick = onSignOut)
                onBackToSuite?.let { back -> FloentlySecondaryButton(title = copy.backToProducts, product = FloentlyProduct.Learn, onClick = back) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            OldAppHomeHintPopup(
                visible = showOldAppHomeHint,
                badgeLabel = "Helpful hint",
                title = "Looking for YKI Prep?",
                body = "Open the sidebar and choose YKI Prep when you want formal exam work, or use Workplace Scenarios when you want spoken work communication.",
                primaryLabel = "Take me there",
                secondaryLabel = "Dismiss",
                onPrimary = {
                    showOldAppHomeHint = false
                    onDestinationSelected(LearnFeatureDestination.YkiPractice)
                },
                onSecondary = { showOldAppHomeHint = false }
            )
            LearnUtilityDrawer(
                visible = showOldAppDrawer,
                email = session.user.email,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                onClose = { showOldAppDrawer = false },
                onHome = {
                    showOldAppDrawer = false
                },
                onDestinationSelected = { destination ->
                    showOldAppDrawer = false
                    onDestinationSelected(destination)
                },
                onSignOut = {
                    showOldAppDrawer = false
                    onSignOut()
                }
            )
        }
    }
}

@Composable
private fun OldAppEmberBackground(palette: com.floently.shared.design.FloentlyPalette) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (-150).dp, y = (-120).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = 30.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size((10 + index * 3).dp)
                    .offset(x = (34 + index * 54).dp, y = (122 + index * 41).dp)
                    .clip(CircleShape)
                    .background(
                        when (index % 3) {
                            0 -> palette.primary.copy(alpha = 0.22f)
                            1 -> palette.accent.copy(alpha = 0.18f)
                            else -> palette.warning.copy(alpha = 0.18f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun OldAppHomeHintPopup(
    visible: Boolean,
    badgeLabel: String,
    title: String,
    body: String,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    if (!visible) return

    val overlay = Color(0x9E040A18)
    val surface = Color(0xFF112346)
    val border = Color(0xFF263B6B)
    val borderStrong = Color(0xFF36508A)
    val text = Color(0xFFF5F9FF)
    val textMuted = Color(0xFFA8BAD6)
    val primary = Color(0xFF5A85FF)
    val primarySurface = Color(0xFF1B2C5D)

    Dialog(
        onDismissRequest = onSecondary,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlay)
                    .clickable(onClick = onSecondary)
            )

            Surface(
                color = surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = primarySurface,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = badgeLabel.uppercase(),
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = title,
                        color = text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = body,
                        color = textMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = surface,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, borderStrong),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable(onClick = onSecondary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = secondaryLabel,
                                    color = text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = primary,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .clickable(onClick = onPrimary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = primaryLabel,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun greeting(session: FloentlyAuthSession): String {
    val name = session.user.email.substringBefore("@").split(".", "-", "_").filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifBlank { "Vitus Idi" }
    return "Hyvää huomenta, $name"
}
