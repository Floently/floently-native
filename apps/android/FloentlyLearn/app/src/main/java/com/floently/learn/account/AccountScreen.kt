package com.floently.learn.account

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
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
                text = "Native account foundation for profile, subscription, device access, and sign-out controls.",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = "Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Email: ${session.user.email}", style = MaterialTheme.typography.bodyMedium)
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = "Learn access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Access is checked before entering the signed-in Learn shell.", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Subscription, plan, and renewal details are still behind the account service boundary.", style = MaterialTheme.typography.bodySmall)
            }

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = "Device access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Device limit UI is ready as a native account section.", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Real device list and revoke actions remain gated until service sync is wired.", style = MaterialTheme.typography.bodySmall)
            }

            FloentlyPrimaryButton(title = "Back to Learn", product = FloentlyProduct.Learn, onClick = onBack)
            FloentlyPrimaryButton(title = "Sign out", product = FloentlyProduct.Learn, onClick = onSignOut)
        }
    }
}
