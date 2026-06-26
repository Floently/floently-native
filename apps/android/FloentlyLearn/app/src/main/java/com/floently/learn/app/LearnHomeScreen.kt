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
fun LearnHomeScreen(
    session: FloentlyAuthSession,
    onSignOut: () -> Unit
) {
    FloentlyScreen(product = FloentlyProduct.Learn) { palette ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Floently Learn",
                color = palette.text,
                style = MaterialTheme.typography.displaySmall
            )

            Text(
                text = "Signed in as ${session.user.email}",
                color = palette.muted,
                style = MaterialTheme.typography.titleMedium
            )

            FloentlyCard(product = FloentlyProduct.Learn) {
                Text(
                    text = "Your account session is stored securely on this device.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "The Learn shell is separated from authentication, API, session, and access foundations so YKI, roleplay, cards, and subscriptions can be rebuilt without mixing screen code with backend logic.",
                    style = MaterialTheme.typography.bodyMedium
                )
                FloentlyPrimaryButton(
                    title = "Sign out",
                    product = FloentlyProduct.Learn,
                    onClick = onSignOut
                )
            }
        }
    }
}
