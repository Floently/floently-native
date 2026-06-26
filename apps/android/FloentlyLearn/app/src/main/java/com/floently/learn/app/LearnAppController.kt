package com.floently.learn.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.floently.learn.auth.LearnAuthMode
import com.floently.shared.auth.FloentlyAuthRepository
import com.floently.shared.auth.FloentlyAuthResult
import com.floently.shared.auth.FloentlyAuthSession

class LearnAppController(
    private val authRepository: FloentlyAuthRepository
) {
    var state by mutableStateOf<LearnAppState>(LearnAppState.Loading)
        private set

    fun boot() {
        state = authRepository.cachedSession()
            ?.let { LearnAppState.SignedIn(it) }
            ?: LearnAppState.SignedOut
    }

    suspend fun submitAuth(mode: LearnAuthMode, email: String, password: String, name: String?) {
        val normalizedEmail = email.trim().lowercase()
        val cleanedName = name?.trim()?.takeIf { it.isNotBlank() }

        if (normalizedEmail.isBlank() || password.length < 6) {
            state = LearnAppState.AuthError("Enter a valid email and at least 6 password characters.")
            return
        }

        state = LearnAppState.Authenticating(mode)

        val result = when (mode) {
            LearnAuthMode.SignIn -> authRepository.signIn(normalizedEmail, password)
            LearnAuthMode.Create -> authRepository.createAccount(normalizedEmail, password, cleanedName)
        }

        state = when (result) {
            is FloentlyAuthResult.Success -> LearnAppState.SignedIn(result.session)
            is FloentlyAuthResult.Failure -> LearnAppState.AuthError(result.message)
        }
    }

    suspend fun signOut() {
        authRepository.signOut()
        state = LearnAppState.SignedOut
    }
}

sealed interface LearnAppState {
    data object Loading : LearnAppState
    data object SignedOut : LearnAppState
    data class Authenticating(val mode: LearnAuthMode) : LearnAppState
    data class AuthError(val message: String) : LearnAppState
    data class SignedIn(val session: FloentlyAuthSession) : LearnAppState
}
