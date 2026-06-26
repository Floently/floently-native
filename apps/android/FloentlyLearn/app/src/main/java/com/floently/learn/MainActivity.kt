package com.floently.learn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.floently.learn.app.LearnAccessGateScreen
import com.floently.learn.app.LearnAppContainer
import com.floently.learn.app.LearnAppController
import com.floently.learn.app.LearnAppState
import com.floently.learn.app.LearnLoadingScreen
import com.floently.learn.auth.LearnAuthScreen
import com.floently.suite.FloentlySuiteShell
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val appContainer = remember { LearnAppContainer(applicationContext) }
                val controller = remember {
                    LearnAppController(
                        authRepository = appContainer.authRepository,
                        accessRepository = appContainer.accessRepository
                    )
                }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    controller.boot()
                }

                when (val state = controller.state) {
                    LearnAppState.Loading -> LearnLoadingScreen()
                    LearnAppState.SignedOut -> LearnAuthScreen(
                        isBusy = false,
                        errorMessage = null,
                        onSubmit = { mode, email, credential, name ->
                            scope.launch { controller.submitAuth(mode, email, credential, name) }
                        }
                    )
                    is LearnAppState.Authenticating -> LearnAuthScreen(
                        isBusy = true,
                        errorMessage = null,
                        onSubmit = { mode, email, credential, name ->
                            scope.launch { controller.submitAuth(mode, email, credential, name) }
                        }
                    )
                    is LearnAppState.AuthError -> LearnAuthScreen(
                        isBusy = false,
                        errorMessage = state.message,
                        onSubmit = { mode, email, credential, name ->
                            scope.launch { controller.submitAuth(mode, email, credential, name) }
                        }
                    )
                    is LearnAppState.CheckingAccess -> LearnLoadingScreen()
                    is LearnAppState.AccessBlocked -> LearnAccessGateScreen(
                        session = state.session,
                        message = state.reason,
                        canRetry = false,
                        onRetry = {},
                        onSignOut = { scope.launch { controller.signOut() } }
                    )
                    is LearnAppState.AccessError -> LearnAccessGateScreen(
                        session = state.session,
                        message = state.message,
                        canRetry = true,
                        onRetry = { scope.launch { controller.retryAccess(state.session) } },
                        onSignOut = { scope.launch { controller.signOut() } }
                    )
                    is LearnAppState.SignedIn -> FloentlySuiteShell(
                        session = state.session,
                        accessRepository = appContainer.accessRepository,
                        billingRepository = appContainer.billingRepository,
                        onSignOut = { scope.launch { controller.signOut() } }
                    )
                }
            }
        }
    }
}
