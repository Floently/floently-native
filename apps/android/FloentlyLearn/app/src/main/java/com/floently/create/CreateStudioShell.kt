package com.floently.create

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
fun CreateStudioShell(
    session: FloentlyAuthSession,
    onBackToSuite: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Create) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floently Create Studio",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            FloentlyCard(product = FloentlyProduct.Create) {
                Text(text = "Create-only native shell", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "This is the separate Create Studio entry. It does not show Read tabs and it keeps Create access separate.", style = MaterialTheme.typography.bodyMedium)
            }
            FloentlyCard(product = FloentlyProduct.Create) {
                Text(text = "Next vertical slice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Create dashboard, direct function cards, input screen, result screen, saved projects, export, and share boundary.", style = MaterialTheme.typography.bodyMedium)
            }
            FloentlyPrimaryButton(title = "Back to Floently", product = FloentlyProduct.Create, onClick = onBackToSuite)
        }
    }
}
