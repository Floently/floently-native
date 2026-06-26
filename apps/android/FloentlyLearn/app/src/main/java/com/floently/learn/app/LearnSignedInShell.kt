package com.floently.learn.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.learn.yki.PreviewYkiRepository
import com.floently.learn.yki.YkiFeatureScreen
import com.floently.shared.auth.FloentlyAuthSession

@Composable
fun LearnSignedInShell(
    session: FloentlyAuthSession,
    onSignOut: () -> Unit
) {
    var selectedDestination by remember { mutableStateOf<LearnFeatureDestination?>(null) }
    val ykiRepository = remember { PreviewYkiRepository() }
    val destination = selectedDestination

    if (destination == null) {
        LearnHomeScreen(
            session = session,
            onSignOut = onSignOut,
            onDestinationSelected = { selectedDestination = it }
        )
    } else {
        when (destination) {
            LearnFeatureDestination.YkiPractice -> YkiFeatureScreen(
                repository = ykiRepository,
                onBack = { selectedDestination = null }
            )
            else -> LearnFeaturePlaceholderScreen(
                destination = destination,
                onBack = { selectedDestination = null }
            )
        }
    }
}
