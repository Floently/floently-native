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
import com.floently.learn.navigation.LearnFeatureDestination

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
    onClose: () -> Unit,
    onHome: () -> Unit,
    onDestinationSelected: (LearnFeatureDestination) -> Unit,
    onSignOut: () -> Unit
) {
    if (!visible) return

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
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, DrawerBorder),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f)
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
                        verticalArrangement = Arrangement.spacedBy(18.dp)
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
                            onClick = { onDestinationSelected(LearnFeatureDestination.Cards) }
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
                            onClick = { onDestinationSelected(LearnFeatureDestination.YkiPractice) }
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

                        DrawerLanguageCard(
                            onClick = { onDestinationSelected(LearnFeatureDestination.Settings) }
                        )

                        DrawerBottomPanels(onSignOut = onSignOut)
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
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
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
                    .padding(start = 22.dp, end = 18.dp, bottom = 28.dp),
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

                Column {
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
    }
}

@Composable
private fun DrawerLanguageCard(
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
            Text(
                text = "Language",
                color = DrawerText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = Color(0xFF223465),
                shape = CircleShape,
                border = BorderStroke(1.dp, DrawerBlue),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🇬🇧", fontSize = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun DrawerBottomPanels(
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = DrawerRaised,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DrawerBorder),
            modifier = Modifier
                .weight(1f)
                .height(90.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Theme",
                    color = DrawerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Dark mode",
                        color = DrawerMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = DrawerBlue,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.size(width = 44.dp, height = 28.dp)
                    ) {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .size(22.dp)
                            ) {}
                        }
                    }
                }
            }
        }

        Surface(
            color = DrawerGoldSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF705B1E)),
            modifier = Modifier
                .weight(1f)
                .height(90.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Session",
                    color = DrawerText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
