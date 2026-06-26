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
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun AccountScreen(
    session: FloentlyAuthSession,
    repository: AccountRepository,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Native account foundation for profile, plan access, devices, and sign-out controls.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            val dashboard = dashboardState
            if (dashboard == null || dashboard.isLoading) {
                Text(text = "Loading account...", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                dashboard.errorMessage?.let { message ->
                    FloentlyCard(product = FloentlyProduct.Learn) {
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Email: ${dashboard.email}", style = MaterialTheme.typography.bodyMedium)
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Learn access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Product: ${dashboard.plan.product}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Status: ${dashboard.plan.status.name}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Plan: ${dashboard.plan.planName}", style = MaterialTheme.typography.bodySmall)
                    Text(text = dashboard.plan.renewalText, style = MaterialTheme.typography.bodySmall)
                    Text(text = dashboard.plan.accessNote, style = MaterialTheme.typography.bodySmall)
                }

                FloentlyCard(product = FloentlyProduct.Learn) {
                    Text(text = "Device access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Device slots: ${dashboard.usedDeviceSlots}/${dashboard.maxDevices}", style = MaterialTheme.typography.bodyMedium)
                    dashboard.devices.forEach { device ->
                        Text(text = "${device.name}: ${device.status.name}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Last seen: ${device.lastSeenText}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Revoke available: ${device.revokeAvailable}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            FloentlyPrimaryButton(title = "Back to Learn", product = FloentlyProduct.Learn, onClick = onBack)
            FloentlyPrimaryButton(title = "Sign out", product = FloentlyProduct.Learn, onClick = onSignOut)
        }
    }
}
