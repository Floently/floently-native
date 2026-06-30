package com.floently.learn.account

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
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
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyPalette
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun AccountScreen(
    session: FloentlyAuthSession,
    repository: AccountRepository,
    copy: LearnCopy,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    var dashboardState by remember { mutableStateOf<AccountDashboardState?>(null) }

    LaunchedEffect(repository, session.user.email) {
        dashboardState = repository.dashboard(session.user.email)
    }

    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = copy.accountTitle,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.signedIn(session.user.email),
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            AccountRouteHeader(
                email = session.user.email,
                palette = palette
            )

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                AccountStatusCard(
                    title = "Ladataan tiliä…",
                    body = "Haetaan Learn-käyttöoikeutta, laitteita ja laskutusrajaa.",
                    palette = palette
                )
            } else {
                dashboard.errorMessage?.let { message ->
                    AccountStatusCard(
                        title = "Huomio",
                        body = message,
                        palette = palette
                    )
                }

                AccountPlanCard(
                    plan = dashboard.plan,
                    email = dashboard.email,
                    palette = palette
                )

                AccountBillingCard(
                    actions = dashboard.billingActions,
                    palette = palette
                )

                AccountAccessBoundaryCard(
                    notes = dashboard.productAccessNotes,
                    palette = palette
                )

                AccountDevicesCard(
                    usedDeviceSlots = dashboard.usedDeviceSlots,
                    maxDevices = dashboard.maxDevices,
                    devices = dashboard.devices,
                    palette = palette
                )
            }

            FloentlyPrimaryButton(
                title = copy.backToLearn,
                product = FloentlyProduct.Learn,
                onClick = onBack
            )
            FloentlyPrimaryButton(
                title = copy.signOut,
                product = FloentlyProduct.Learn,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun AccountRouteHeader(
    email: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TILI",
                color = palette.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "Floently Learn -tili",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = email,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountTinyPill("Tuote", "Learn", palette.primary)
                AccountTinyPill("Pääsy", "Erillinen", palette.accent)
            }
        }
    }
}

@Composable
private fun AccountTinyPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
    ) {
        Text(
            text = "$label · $value",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccountPlanCard(
    plan: LearnPlanSnapshot,
    email: String,
    palette: FloentlyPalette
) {
    val accent = plan.status.statusColor(palette)

    Surface(
        color = palette.card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = plan.status.friendlyText().uppercase(),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Text(
                text = plan.planName.localizedAccountText(),
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = email,
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AccountMetricBox(
                    label = "Tuote",
                    value = plan.product.localizedAccountText(),
                    color = palette.primary,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                AccountMetricBox(
                    label = "Tila",
                    value = plan.status.shortText(),
                    color = accent,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = plan.renewalText.localizedAccountText(),
                color = palette.soft,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = plan.accessNote.localizedAccountText(),
                color = accent,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AccountMetricBox(
    label: String,
    value: String,
    color: Color,
    palette: FloentlyPalette,
    modifier: Modifier
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun AccountBillingCard(
    actions: List<LearnBillingActionSnapshot>,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Laskutus ja tilaus",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            if (actions.isEmpty()) {
                Text(
                    text = "Laskutusrajat näkyvät täällä, kun palvelu palauttaa ne.",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                actions.forEach { action ->
                    AccountActionRow(
                        title = action.title.localizedAccountText(),
                        status = action.statusText.localizedAccountText(),
                        body = action.body.localizedAccountText(),
                        accent = palette.warning,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountAccessBoundaryCard(
    notes: List<String>,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tuotekohtaiset käyttöoikeudet",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            val visibleNotes = notes.ifEmpty {
                listOf("Learn, Read ja Create tarkistetaan erillisinä käyttöoikeuksina.")
            }
            visibleNotes.forEach { note ->
                AccountActionRow(
                    title = note.localizedAccessTitle(),
                    status = "Erillinen pääsy",
                    body = note.localizedAccountText(),
                    accent = palette.accent,
                    palette = palette
                )
            }
        }
    }
}

@Composable
private fun AccountDevicesCard(
    usedDeviceSlots: Int,
    maxDevices: Int,
    devices: List<LearnDeviceSnapshot>,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Laitteet",
                color = palette.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "$usedDeviceSlots/$maxDevices laitepaikkaa käytössä",
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium
            )

            if (devices.isEmpty()) {
                Text(
                    text = "Tämä laite näkyy täällä, kun laitepääsy vahvistetaan.",
                    color = palette.soft,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                devices.forEach { device ->
                    AccountDeviceRow(
                        device = device,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDeviceRow(
    device: LearnDeviceSnapshot,
    palette: FloentlyPalette
) {
    val accent = device.status.statusColor(palette)

    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.localizedAccountText(),
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.status.friendlyText(),
                    color = accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Viimeksi nähty: ${device.lastSeenText.localizedAccountText()}",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (device.revokeAvailable) "Voidaan poistaa tililtä" else "Nykyistä laitetta ei poisteta tästä näkymästä",
                    color = palette.soft,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AccountActionRow(
    title: String,
    status: String,
    body: String,
    accent: Color,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.cardMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = status,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                color = palette.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AccountStatusCard(
    title: String,
    body: String,
    palette: FloentlyPalette
) {
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(body, color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun LearnPlanStatus.statusColor(palette: FloentlyPalette): Color = when (this) {
    LearnPlanStatus.Active -> palette.accent
    LearnPlanStatus.Pending -> palette.warning
    LearnPlanStatus.Missing -> Color(0xFFFF7A7A)
}

private fun LearnPlanStatus.shortText(): String = when (this) {
    LearnPlanStatus.Active -> "Aktiivinen"
    LearnPlanStatus.Pending -> "Odottaa"
    LearnPlanStatus.Missing -> "Puuttuu"
}

private fun LearnPlanStatus.friendlyText(): String = when (this) {
    LearnPlanStatus.Active -> "Käyttöoikeus aktiivinen"
    LearnPlanStatus.Pending -> "Käyttöoikeus odottaa"
    LearnPlanStatus.Missing -> "Käyttöoikeus tarvitaan"
}

private fun LearnDeviceStatus.statusColor(palette: FloentlyPalette): Color = when (this) {
    LearnDeviceStatus.Current -> palette.accent
    LearnDeviceStatus.Registered -> palette.primary
}

private fun LearnDeviceStatus.friendlyText(): String = when (this) {
    LearnDeviceStatus.Current -> "Nykyinen laite"
    LearnDeviceStatus.Registered -> "Rekisteröity laite"
}

private fun String.localizedAccessTitle(): String = when {
    startsWith("Learn:") -> "Learn"
    startsWith("Read:") -> "Read"
    startsWith("Create Studio:") -> "Create Studio"
    startsWith("No bundle") -> "Bundle"
    else -> "Käyttöoikeus"
}

private fun String.localizedAccountText(): String = when (this) {
    "Floently Learn" -> "Floently Learn"
    "Learn access" -> "Learn-käyttöoikeus"
    "Plan details are being prepared." -> "Paketin tiedot ovat valmistelussa."
    "Learn access is active." -> "Learn-käyttöoikeus on aktiivinen."
    "Current plan" -> "Nykyinen paketti"
    "Floently Learn access is checked separately from Read and Create." -> "Floently Learn -käyttöoikeus tarkistetaan erillään Readista ja Createsta."
    "Access active" -> "Käyttöoikeus aktiivinen"
    "Start 3-day trial" -> "Aloita 3 päivän kokeilu"
    "Trial entry belongs to the billing surface. Android live purchases must use Google Play Billing when enabled." -> "Kokeilun aloitus kuuluu laskutusnäkymään. Androidin live-ostot käyttävät Google Play Billingiä, kun ne otetaan käyttöön."
    "Store billing boundary" -> "Kauppalaskutuksen raja"
    "Manage subscription" -> "Hallitse tilausta"
    "Mobile-origin subscriptions are managed through the store account. Web portal and web checkout remain web-only." -> "Mobiilista alkaneet tilaukset hallitaan kauppatilin kautta. Web-portaali ja web-checkout pysyvät webissä."
    "Provider wiring pending" -> "Palveluntarjoajan kytkentä odottaa"
    "Start checkout" -> "Aloita checkout"
    "Checkout preparation is available through the shared billing boundary and must preserve separate Learn, Read, and Create access." -> "Checkout-valmistelu on jaetussa laskutusrajassa, mutta Learn-, Read- ja Create-pääsy pysyvät erillisinä."
    "Checkout boundary ready" -> "Checkout-raja valmis"
    "Learn: Finnish learning, YKI, roleplay, cards, professional Finnish, progress, and account." -> "Learn: suomen oppiminen, YKI, roolipeli, kortit, työpaikan suomi, edistyminen ja tili."
    "Read: separate document reading access." -> "Read: erillinen dokumenttien lukemisen käyttöoikeus."
    "Create Studio: separate creation tools access." -> "Create Studio: erillinen luontityökalujen käyttöoikeus."
    "No bundle is implied unless a bundle plan is explicitly added later." -> "Bundlea ei oleteta, ellei erillistä bundle-pakettia lisätä myöhemmin."
    "This Android device" -> "Tämä Android-laite"
    "Saved device" -> "Tallennettu laite"
    "Now" -> "Nyt"
    "Recently used" -> "Käytetty äskettäin"
    else -> this
}
