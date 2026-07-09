package com.floently.learn.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floently.learn.i18n.LearnLanguage
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.shared.design.FloentlyThemeMode

private val DrawerBackdrop = Color(0xB0040A18)
private val DrawerSurface = Color(0xFF111B30)
private val DrawerRaised = Color(0xFF172541)
private val DrawerBorder = Color(0xFF243653)
private val DrawerBlue = Color(0xFF5D7DF7)
private val DrawerBlueSoft = Color(0xFF7E98FA)
private val DrawerText = Color(0xFFF7F9FF)
private val DrawerMuted = Color(0xFFA9B6CF)
private val DrawerSection = Color(0xFF7587AA)
private val DrawerGold = Color(0xFFEDD37A)
private val DrawerGoldSurface = Color(0xFF3D3210)
private val DrawerDanger = Color(0xFFFF7474)

@Composable
fun BoxScope.LearnScreenMenuButton(
    onClick: () -> Unit
) {
    Surface(
        color = DrawerBlue,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 10.dp, end = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "Menu",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun LearnUtilityDrawer(
    visible: Boolean,
    email: String,
    selectedLanguage: LearnLanguage = LearnLanguage.EN,
    onLanguageSelected: (LearnLanguage) -> Unit = {},
    onClose: () -> Unit,
    onHome: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit,
    onSignOut: () -> Unit,
    themeMode: FloentlyThemeMode = FloentlyThemeMode.System,
    onThemeModeChange: (FloentlyThemeMode) -> Unit = {}
) {
    if (!visible) return

    var showLanguagePicker by remember(email) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DrawerBackdrop)
                    .clickable(onClick = onClose)
            )

            Surface(
                color = DrawerSurface,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                border = BorderStroke(1.dp, DrawerBorder),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    DrawerProfileHeader(
                        email = email,
                        onClose = onClose
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(17.dp)
                    ) {
                        DrawerStreakCard()

                        DrawerSectionLabel("FLOENTLY SUITE")
                        DrawerRouteItem(
                            icon = "🎓",
                            title = "Floently Learn",
                            subtitle = "Practice Finnish with YKI and professional learning tools.",
                            iconColor = Color(0xFF253668),
                            onClick = onHome
                        )

                        DrawerSectionLabel("WORKPLACE READINESS")
                        DrawerRouteItem(
                            icon = "📘",
                            title = "Everyday Finnish",
                            subtitle = "General cards, vocabulary and roleplay.",
                            iconColor = Color(0xFF213A65),
                            onClick = { onDestinationSelected(LearnFeatureDestination.EverydayFinnish) }
                        )
                        DrawerRouteItem(
                            icon = "🗂️",
                            title = "Workplace Finnish",
                            subtitle = "Choose Everyday Finnish or My profession from the workplace hub.",
                            iconColor = Color(0xFF263A66),
                            onClick = { onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish) }
                        )

                        DrawerSectionLabel("YKI AND SETTLEMENT GOALS")
                        DrawerRouteItem(
                            icon = "◎",
                            title = "YKI Practice",
                            subtitle = "Step-by-step YKI preparation.",
                            iconColor = Color(0xFF2D2D58),
                            onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
                        )
                        DrawerRouteItem(
                            icon = "◇",
                            title = "YKI Mock Exam",
                            subtitle = "Full simulation route for exam readiness.",
                            iconColor = Color(0xFF2D2D58),
                            onClick = { onDestinationSelected(LearnFeatureDestination.YkiMockExam) }
                        )

                        DrawerSectionLabel("ACCOUNT AND ACCESS")
                        DrawerRouteItem(
                            icon = "💳",
                            title = "Plans and access",
                            subtitle = "Access options for individuals, employers, and municipalities.",
                            iconColor = Color(0xFF253047),
                            onClick = { onDestinationSelected(LearnFeatureDestination.Account) }
                        )
                        DrawerRouteItem(
                            icon = "⚙️",
                            title = "Settings",
                            subtitle = "Theme, profile, audio, and learning preferences.",
                            iconColor = Color(0xFF253047),
                            onClick = { onDestinationSelected(LearnFeatureDestination.Settings) }
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            DrawerLanguageCard(
                                selectedLanguage = selectedLanguage,
                                expanded = showLanguagePicker,
                                onClick = { showLanguagePicker = !showLanguagePicker }
                            )
                            if (showLanguagePicker) {
                                DrawerLanguagePickerPopover(
                                    selectedLanguage = selectedLanguage,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = (-318).dp),
                                    onPick = { language ->
                                        onLanguageSelected(language)
                                        showLanguagePicker = false
                                    }
                                )
                            }
                        }

                        DrawerBottomPanels(themeMode = themeMode, onThemeModeChange = onThemeModeChange, onSignOut = onSignOut)
                        Spacer(modifier = Modifier.height(92.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerProfileHeader(
    email: String,
    onClose: () -> Unit
) {
    Surface(
        color = DrawerBlue,
        shape = RoundedCornerShape(topStart = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = DrawerBlueSoft.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 18.dp)
                    .size(48.dp)
                    .clickable(onClick = onClose)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "×",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, end = 18.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "V",
                            color = DrawerBlue,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vitus Idi",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = "B1–B2 · YKI prep",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerStreakCard() {
    Surface(
        color = DrawerGoldSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF7F6521)),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔥", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Daily streak",
                color = DrawerGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "1",
                color = DrawerGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        color = DrawerSection,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.8.sp,
        modifier = Modifier.padding(top = 3.dp, bottom = 1.dp)
    )
}

@Composable
private fun DrawerRouteItem(
    icon: String,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = iconColor,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DrawerText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = DrawerMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "›",
            color = DrawerMuted,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun DrawerLanguageCard(
    selectedLanguage: LearnLanguage,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = DrawerRaised,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DrawerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Language",
                    color = DrawerText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (expanded) "Choose language" else selectedLanguage.nativeLabel,
                    color = DrawerMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                color = Color(0xFF223465),
                shape = CircleShape,
                border = BorderStroke(1.dp, DrawerBlue),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = drawerFlag(selectedLanguage), fontSize = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun DrawerLanguagePickerPopover(
    selectedLanguage: LearnLanguage,
    modifier: Modifier,
    onPick: (LearnLanguage) -> Unit
) {
    Surface(
        color = Color(0xFF070D1C),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF223465)),
        modifier = modifier
            .width(202.dp)
            .height(318.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LearnLanguage.enabledLanguages.forEach { language ->
                DrawerLanguageOption(
                    flag = drawerFlag(language),
                    label = language.nativeLabel,
                    selected = language == selectedLanguage,
                    onClick = { onPick(language) }
                )
            }
        }
    }
}

@Composable
private fun DrawerLanguageOption(
    flag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF18295A) else Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flag, fontSize = 19.sp)
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = label,
                color = DrawerText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun drawerFlag(language: LearnLanguage): String = when (language) {
    LearnLanguage.FI -> "🇫🇮"
    LearnLanguage.SV -> "🇸🇪"
    LearnLanguage.RU -> "🇷🇺"
    LearnLanguage.ET -> "🇪🇪"
    LearnLanguage.UK -> "🇺🇦"
    LearnLanguage.AR -> "🇸🇦"
    LearnLanguage.EN -> "🇬🇧"
    LearnLanguage.SO -> "🇸🇴"
    LearnLanguage.FA -> "🇮🇷"
    LearnLanguage.ZH -> "🇨🇳"
    LearnLanguage.SQ -> "🇦🇱"
    LearnLanguage.KU -> "☀️"
    LearnLanguage.VI -> "🇻🇳"
    LearnLanguage.BN -> "🇧🇩"
    LearnLanguage.TR -> "🇹🇷"
    LearnLanguage.TL -> "🇵🇭"
    LearnLanguage.TH -> "🇹🇭"
    LearnLanguage.NE -> "🇳🇵"
    LearnLanguage.ES -> "🇪🇸"
    LearnLanguage.UR -> "🇵🇰"
}

@Composable
private fun DrawerBottomPanels(
    themeMode: FloentlyThemeMode,
    onThemeModeChange: (FloentlyThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = DrawerRaised,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DrawerBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clickable {
                    onThemeModeChange(
                        if (themeMode == FloentlyThemeMode.Light) FloentlyThemeMode.Dark else FloentlyThemeMode.Light
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Theme",
                        color = DrawerText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (themeMode == FloentlyThemeMode.Light) "Light mode" else "Dark mode",
                        color = DrawerMuted,
                        fontSize = 14.sp
                    )
                }
                Surface(
                    color = DrawerBlue,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.size(width = 54.dp, height = 32.dp)
                ) {
                    Box(contentAlignment = if (themeMode == FloentlyThemeMode.Light) Alignment.CenterStart else Alignment.CenterEnd) {
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(24.dp)
                        ) {}
                    }
                }
            }
        }

        Surface(
            color = DrawerGoldSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF705B1E)),
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Session",
                        color = DrawerText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "End this device session",
                        color = DrawerMuted,
                        fontSize = 14.sp
                    )
                }
                Surface(
                    color = DrawerSurface,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(onClick = onSignOut)
                ) {
                    Text(
                        text = "Log out",
                        color = DrawerDanger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
