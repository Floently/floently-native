package com.floently.learn.app

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.floently.learn.account.AccountScreen
import com.floently.learn.account.PreviewAccountRepository
import com.floently.learn.cards.CardsRepository
import com.floently.learn.cards.CardsScreen
import com.floently.learn.i18n.LearnTranslations
import com.floently.learn.i18n.persistLearnLanguage
import com.floently.learn.i18n.rememberLearnLanguageState
import com.floently.learn.navigation.LearnFeatureDestination
import com.floently.learn.professional.PreviewProfessionalFinnishRepository
import com.floently.learn.professional.ProfessionalFinnishScreen
import com.floently.learn.progress.LearnProgressRepository
import com.floently.learn.progress.LearnProgressScreen
import com.floently.learn.roleplay.RoleplayRepository
import com.floently.learn.roleplay.RoleplayScreen
import com.floently.learn.settings.LearnSettingsScreen
import com.floently.learn.yki.PreviewYkiRepository
import com.floently.learn.yki.YkiFeatureScreen
import com.floently.shared.auth.FloentlyAuthSession

@Composable
fun LearnSignedInShell(
    session: FloentlyAuthSession,
    roleplayRepository: RoleplayRepository,
    cardsRepository: CardsRepository,
    progressRepository: LearnProgressRepository,
    onSignOut: () -> Unit,
    onBackToSuite: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val languageState = rememberLearnLanguageState()
    val copy = LearnTranslations.copy(languageState.value)
    var selectedDestination by remember { mutableStateOf<LearnFeatureDestination?>(null) }
    var showShellDrawer by remember(session.user.email) { mutableStateOf(false) }
    val ykiRepository = remember { PreviewYkiRepository() }
    val professionalFinnishRepository = remember { PreviewProfessionalFinnishRepository() }
    val accountRepository = remember { PreviewAccountRepository() }
    val destination = selectedDestination

    if (destination == null) {
        LearnHomeScreen(
            session = session,
            copy = copy,
            selectedLanguage = languageState.value,
            onLanguageSelected = { language ->
                languageState.value = language
                persistLearnLanguage(context, language)
            },
            onSignOut = onSignOut,
            onBackToSuite = onBackToSuite,
            onDestinationSelected = { selectedDestination = it }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            when (destination) {
            LearnFeatureDestination.YkiPractice -> YkiFeatureScreen(
                repository = ykiRepository,
                copy = copy,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.ProfessionalFinnish -> ProfessionalFinnishScreen(
                repository = professionalFinnishRepository,
                copy = copy,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Roleplay -> RoleplayScreen(
                repository = roleplayRepository,
                copy = copy,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Cards -> CardsScreen(
                repository = cardsRepository,
                copy = copy,
                selectedLanguage = languageState.value,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Progress -> LearnProgressScreen(
                repository = progressRepository,
                copy = copy,
                onBack = { selectedDestination = null }
            )
            LearnFeatureDestination.Settings -> LearnSettingsScreen(
                copy = copy,
                selectedLanguage = languageState.value,
                onLanguageSelected = { language ->
                    languageState.value = language
                    persistLearnLanguage(context, language)
                },
                onBack = { selectedDestination = null },
                onSignOut = onSignOut
            )
            LearnFeatureDestination.Account -> AccountScreen(
                session = session,
                repository = accountRepository,
                copy = copy,
                onBack = { selectedDestination = null },
                onSignOut = onSignOut
            )
            }
            LearnScreenMenuButton(onClick = { showShellDrawer = true })
            LearnUtilityDrawer(
                visible = showShellDrawer,
                email = session.user.email,
                selectedLanguage = languageState.value,
                onLanguageSelected = { language ->
                    languageState.value = language
                    persistLearnLanguage(context, language)
                },
                onClose = { showShellDrawer = false },
                onHome = {
                    showShellDrawer = false
                    selectedDestination = null
                },
                onDestinationSelected = { nextDestination ->
                    showShellDrawer = false
                    selectedDestination = nextDestination
                },
                onSignOut = {
                    showShellDrawer = false
                    onSignOut()
                }
            )
        }
    }
}
