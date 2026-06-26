package com.floently.learn.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.floently.shared.auth.FloentlyAuthSession
import com.floently.shared.design.FloentlyCard
import com.floently.shared.design.FloentlyPrimaryButton
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.FloentlyScreen

@Composable
fun LearnAccessGateScreen(
    session: FloentlyAuthSession,
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Floently Learn access",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = session.user.email,
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )
            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(text = message, style = MaterialTheme.typography.titleMedium)
                if (canRetry) {
                    FloentlyPrimaryButton(
                        title = "Try again",
                        product = FloentlyProduct.Learn,
                        onClick = onRetry
                    )
                }
                FloentlyPrimaryButton(
                    title = "Use another account",
                    product = FloentlyProduct.Learn,
                    onClick = onSignOut
                )
            }
        }
    }
}
