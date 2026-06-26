package com.floently.read

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
fun ReadShell(
    session: FloentlyAuthSession,
    onBackToSuite: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Read) { palette ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Floently Read",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            FloentlyCard(product = FloentlyProduct.Read) {
                Text(text = "Native Read MVP boundary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "This is the separate native Read shell. It is not a WebView and it is not mixed with Create Studio.", style = MaterialTheme.typography.bodyMedium)
            }
            FloentlyCard(product = FloentlyProduct.Read) {
                Text(text = "Next vertical slice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "Upload/import, auto language detection, generate reading session, Read automatically setting, reader controls, and saved library.", style = MaterialTheme.typography.bodyMedium)
            }
            FloentlyPrimaryButton(title = "Back to Floently", product = FloentlyProduct.Read, onClick = onBackToSuite)
        }
    }
}
