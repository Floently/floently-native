package com.floently.learn.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.floently.learn.account.AccountScreen
import com.floently.learn.account.PreviewAccountRepository
import com.floently.learn.cards.CardsScreen
import com.floently.learn.cards.PreviewCardsRepository
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.learn.professional.PreviewProfessionalFinnishRepository
import com.floently.learn.professional.ProfessionalFinnishScreen
import com.floently.learn.progress.LearnProgressScreen
import com.floently.learn.progress.PreviewLearnProgressRepository
import com.floently.learn.roleplay.PreviewRoleplayRepository
import com.floently.learn.roleplay.RoleplayScreen
import com.floently.learn.yki.PreviewYkiRepository
import com.floently.learn.yki.YkiFeatureScreen
import com.floently.shared.auth.FloentlyAuthSession

@Composable
fun LearnSignedInShell(
    session: FloentlyAuthSession,
    onSignOut: () -> Unit,
    onBackToSuite: (() -> Unit)? = null
) {
    var selectedDestination by remember { mutableStateOf<LearnFeatureDestination?>(null) }
    val ykiRepository = remember { PreviewYkiRepository() }
    val professionalFinnishRepository = remember { PreviewProfessionalFinnishRepository() }
    val roleplayRepository = remember { PreviewRoleplayRepository() }
    val cardsRepository = remember { PreviewCardsRepository() }
    val progressRepository = remember { PreviewLearnProgressRepository() }
    val accountRepository = remember { PreviewAccountRepository() }
    val destination = selectedDestination

    if (destination == null) {
        LearnHomeScreen(
            session = session,
            onSignOut = onSignOut,
            onBackToSuite = onBackToSuite,
            onDestinationSelected = { selectedDestination = it }
        )
    } else {
        when (destination) {
            LearnFeatureDestination.YkiPractice -> YkiFeatureScreen(
                repository = ykiRepository,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.ProfessionalFinnish -> ProfessionalFinnishScreen(
                repository = professionalFinnishRepository,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Roleplay -> RoleplayScreen(
                repository = roleplayRepository,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Cards -> CardsScreen(
                repository = cardsRepository,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Progress -> LearnProgressScreen(
                repository = progressRepository,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Account -> AccountScreen(
                session = session,
                repository = accountRepository,
                onBack = { selectedDestination = null },
                onSignOut = onSignOut
            )
        }
    }
}
