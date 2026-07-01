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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floently.learn.navigation.LearnFeatureDestination

@Composable
fun BoxScope.LearnScreenMenuButton(
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF112346),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF263B6B)),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "Menu",
            color = Color(0xFFF5F9FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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

    val surface = Color(0xFF0D1D42)
    val surfaceRaised = Color(0xFF112346)
    val border = Color(0xFF263B6B)
    val text = Color(0xFFF5F9FF)
    val muted = Color(0xFFA8BAD6)
    val primary = Color(0xFF5A85FF)
    val accent = Color(0xFF3EC5A8)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xA3040A18))
                    .clickable(onClick = onClose)
            )

            Surface(
                color = surface,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(326.dp)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "floently",
                                color = Color(0xFF18B9FF),
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "LEARN",
                                color = primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                        }
                        Surface(
                            color = surfaceRaised,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, border),
                            modifier = Modifier.clickable(onClick = onClose)
                        ) {
                            Text(
                                text = "Sulje",
                                color = text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(
                        color = surfaceRaised,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Kirjautunut",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.3.sp
                            )
                            Text(
                                text = email,
                                color = text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Päiväputki 1 - arvioitu taso B1",
                                color = muted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "Navigointi",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    LearnDrawerItem("Etusivu", "Palaa Learn-kotiin", primary, onHome)
                    LearnDrawerItem("Sanasto ja kortit", "Kortit, fraasit ja kertaus", primary) {
                        onDestinationSelected(LearnFeatureDestination.Cards)
                    }
                    LearnDrawerItem("Työpaikan tilanteet", "Ammatillinen suomi", Color(0xFFE8B65E)) {
                        onDestinationSelected(LearnFeatureDestination.ProfessionalFinnish)
                    }
                    LearnDrawerItem("YKI-valmistautuminen", "Koe, kirjoittaminen ja puhuminen", Color(0xFF9D7CFF)) {
                        onDestinationSelected(LearnFeatureDestination.YkiPractice)
                    }
                    LearnDrawerItem("Roolipeli", "Ohjattu keskustelu", accent) {
                        onDestinationSelected(LearnFeatureDestination.Roleplay)
                    }
                    LearnDrawerItem("Edistyminen", "Yhteenveto ja päiväputki", Color(0xFF9D7CFF)) {
                        onDestinationSelected(LearnFeatureDestination.Progress)
                    }

                    Text(
                        text = "Tili",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    LearnDrawerItem("Tili", "Käyttöoikeudet ja profiili", muted) {
                        onDestinationSelected(LearnFeatureDestination.Account)
                    }
                    LearnDrawerItem("Asetukset", "Kieli ja sovellusasetukset", muted) {
                        onDestinationSelected(LearnFeatureDestination.Settings)
                    }
                    LearnDrawerItem("Kirjaudu ulos", "Lopeta istunto", Color(0xFFFF7A7A), onSignOut)
                }
            }
        }
    }
}

@Composable
private fun LearnDrawerItem(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF112346),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF263B6B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFF5F9FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFA8BAD6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
