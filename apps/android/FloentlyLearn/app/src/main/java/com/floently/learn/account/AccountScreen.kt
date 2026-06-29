package com.floently.learn.account

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floently.learn.i18n.LearnCopy
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
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
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
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

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading account...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                dashboard.errorMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = "Account note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = copy.settingsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = dashboard.email, style = MaterialTheme.typography.bodyMedium)
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = copy.openAccount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = dashboard.plan.planName, style = MaterialTheme.typography.bodyMedium)
                    Text(text = dashboard.plan.status.friendlyText(), style = MaterialTheme.typography.bodySmall)
                    Text(text = dashboard.plan.renewalText, style = MaterialTheme.typography.bodySmall)
                    Text(text = dashboard.plan.accessNote, style = MaterialTheme.typography.bodySmall)
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Billing and subscription", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Current plan: ${dashboard.plan.planName}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = dashboard.plan.status.friendlyText(), style = MaterialTheme.typography.bodySmall)
                    dashboard.billingActions.forEach { action ->
                        Text(text = action.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = action.statusText, style = MaterialTheme.typography.bodySmall)
                        Text(text = action.body, style = MaterialTheme.typography.bodySmall)
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Product access boundaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (dashboard.productAccessNotes.isEmpty()) {
                        Text(text = "Learn, Read, and Create access are checked separately.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        dashboard.productAccessNotes.forEach { note ->
                            Text(text = note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Devices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "${dashboard.usedDeviceSlots} of ${dashboard.maxDevices} device slot(s) used", style = MaterialTheme.typography.bodyMedium)
                    if (dashboard.devices.isEmpty()) {
                        Text(text = "This device will appear here after device access is confirmed.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        dashboard.devices.forEach { device ->
                            Text(text = device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = device.status.friendlyText(), style = MaterialTheme.typography.bodySmall)
                            Text(text = "Last seen: ${device.lastSeenText}", style = MaterialTheme.typography.bodySmall)
                            Text(text = if (device.revokeAvailable) "Can be removed from account" else "Current device cannot be removed here", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            FloentlyPrimaryButton(title = copy.backToLearn, product = FloentlyProduct.Learn, onClick = onBack)
            FloentlyPrimaryButton(title = copy.signOut, product = FloentlyProduct.Learn, onClick = onSignOut)
        }
    }
}

private fun LearnPlanStatus.friendlyText(): String = when (this) {
    LearnPlanStatus.Active -> "Access active"
    LearnPlanStatus.Pending -> "Access pending"
    LearnPlanStatus.Missing -> "Access needed"
}

private fun LearnDeviceStatus.friendlyText(): String = when (this) {
    LearnDeviceStatus.Current -> "Current device"
    LearnDeviceStatus.Registered -> "Registered device"
}
