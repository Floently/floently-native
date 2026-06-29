package com.floently.learn.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.floently.learn.auth.LearnAuthMode
import com.floently.shared.access.FloentlyAccessProduct
import com.floently.shared.access.FloentlyAccessRepository
import com.floently.shared.access.FloentlyAccessResult
import com.floently.shared.auth.FloentlyAuthRepository
import com.floently.shared.auth.FloentlyAuthResult
import com.floently.shared.auth.FloentlyAuthSession

class LearnAppController(
    private val authRepository: FloentlyAuthRepository,
    private val accessRepository: FloentlyAccessRepository
) {
    var state by mutableStateOf<LearnAppState>(LearnAppState.Loading)
        private set

    suspend fun boot() {
        val cachedSession = authRepository.cachedSession()
        if (cachedSession == null) {
            state = LearnAppState.SignedOut
        } else {
            checkLearnAccess(cachedSession)
        }
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

        when (result) {
            is FloentlyAuthResult.Success -> checkLearnAccess(result.session)
            is FloentlyAuthResult.Failure -> state = LearnAppState.AuthError(result.message)
        }
    }

    suspend fun retryAccess(session: FloentlyAuthSession) {
        checkLearnAccess(session)
    }

    suspend fun signOut() {
        authRepository.signOut()
        state = LearnAppState.SignedOut
    }

    private suspend fun checkLearnAccess(session: FloentlyAuthSession) {
        state = LearnAppState.CheckingAccess(session)
        state = when (val result = accessRepository.requireAccess(FloentlyAccessProduct.Learn)) {
            is FloentlyAccessResult.Allowed -> LearnAppState.SignedIn(session)
            is FloentlyAccessResult.Blocked -> LearnAppState.AccessBlocked(session, result.reason)
            is FloentlyAccessResult.Error -> LearnAppState.AccessError(
                session = session,
                message = result.message
            )
        }
    }
}

sealed interface LearnAppState {
    data object Loading : LearnAppState
    data object SignedOut : LearnAppState
    data class Authenticating(val mode: LearnAuthMode) : LearnAppState
    data class AuthError(val message: String) : LearnAppState
    data class CheckingAccess(val session: FloentlyAuthSession) : LearnAppState
    data class AccessBlocked(val session: FloentlyAuthSession, val reason: String) : LearnAppState
    data class AccessError(val session: FloentlyAuthSession, val message: String) : LearnAppState
    data class SignedIn(val session: FloentlyAuthSession) : LearnAppState
}
