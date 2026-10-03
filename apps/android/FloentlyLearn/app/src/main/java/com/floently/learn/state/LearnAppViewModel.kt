package com.floently.learn.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.api.FloentlyApiError
import com.floently.shared.auth.FloentlyAuthService
import com.floently.shared.auth.FloentlySecureSessionStore
import com.floently.shared.auth.FloentlyUser
import com.floently.shared.billing.FloentlyAccessService
import com.floently.shared.billing.FloentlyAccessStatus
import com.floently.shared.learn.LearningEventOutboxV1
import com.floently.shared.learn.LearningSessionStateV1
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LearnAppPhase {
    Bootstrapping,
    SignedOut,
    SignedIn
}

data class LearnAppUiState(
    val phase: LearnAppPhase = LearnAppPhase.Bootstrapping,
    val accessStatus: FloentlyAccessStatus? = null,
    val isAuthenticating: Boolean = false,
    val isRefreshingAccess: Boolean = false,
    val authError: String? = null,
    val connectionNotice: String? = null,
    val accessNotice: String? = null
)

class LearnAppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionStore = FloentlySecureSessionStore(application)

    val learningSession = LearningSessionStateV1()
    val eventOutbox = LearningEventOutboxV1()

    private val api = FloentlyApiClient(
        tokenProvider = { sessionStore.session?.token }
    )
    private val authService = FloentlyAuthService(api, sessionStore)
    private val accessService = FloentlyAccessService(api)

    private val _uiState = MutableStateFlow(LearnAppUiState())
    val uiState: StateFlow<LearnAppUiState> = _uiState.asStateFlow()

    val currentUser: FloentlyUser?
        get() = sessionStore.session?.user

    init {
        bootstrap()
    }

    private fun bootstrap() {
        if (_uiState.value.phase != LearnAppPhase.Bootstrapping) return

        if (sessionStore.session == null) {
            _uiState.update { it.copy(phase = LearnAppPhase.SignedOut) }
            return
        }

        viewModelScope.launch {
            runCatching { authService.restoreSession() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            phase = LearnAppPhase.SignedIn,
                            connectionNotice = null
                        )
                    }
                    refreshAccess()
                }
                .onFailure { error ->
                    if (isAuthenticationFailure(error)) {
                        sessionStore.clear()
                        _uiState.value = LearnAppUiState(
                            phase = LearnAppPhase.SignedOut
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                phase = LearnAppPhase.SignedIn,
                                connectionNotice = "We could not verify your session right now. Some online features may be unavailable until you reconnect."
                            )
                        }
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        if (_uiState.value.isAuthenticating) return

        _uiState.update {
            it.copy(
                isAuthenticating = true,
                authError = null,
                connectionNotice = null
            )
        }

        viewModelScope.launch {
            runCatching {
                authService.login(email.trim(), password)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        phase = LearnAppPhase.SignedIn,
                        isAuthenticating = false,
                        authError = null
                    )
                }
                refreshAccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = userFacingAuthMessage(error)
                    )
                }
            }
        }
    }

    fun register(email: String, password: String, name: String?) {
        if (_uiState.value.isAuthenticating) return

        _uiState.update {
            it.copy(
                isAuthenticating = true,
                authError = null,
                connectionNotice = null
            )
        }

        viewModelScope.launch {
            runCatching {
                authService.register(
                    email = email.trim(),
                    password = password,
                    name = name?.trim()?.takeIf { it.isNotEmpty() }
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        phase = LearnAppPhase.SignedIn,
                        isAuthenticating = false,
                        authError = null
                    )
                }
                refreshAccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = userFacingAuthMessage(error)
                    )
                }
            }
        }
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> {
        return runCatching {
            authService.requestPasswordReset(email.trim())
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun refreshAccess() {
        if (_uiState.value.phase != LearnAppPhase.SignedIn) return
        if (_uiState.value.isRefreshingAccess) return

        _uiState.update {
            it.copy(
                isRefreshingAccess = true,
                accessNotice = null
            )
        }

        viewModelScope.launch {
            runCatching { accessService.fetchStatus() }
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            accessStatus = status,
                            isRefreshingAccess = false,
                            accessNotice = null,
                            connectionNotice = null
                        )
                    }
                }
                .onFailure { error ->
                    if (isAuthenticationFailure(error)) {
                        sessionStore.clear()
                        _uiState.value = LearnAppUiState(
                            phase = LearnAppPhase.SignedOut,
                            authError = "Your session has expired. Sign in again to continue."
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                accessStatus = null,
                                isRefreshingAccess = false,
                                accessNotice = "Your access status is temporarily unavailable."
                            )
                        }
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            learningSession.clear()
            eventOutbox.clear()
            _uiState.value = LearnAppUiState(
                phase = LearnAppPhase.SignedOut
            )
        }
    }

    private fun isAuthenticationFailure(error: Throwable): Boolean {
        val apiError = error as? FloentlyApiError ?: return false
        return apiError.code in setOf(
            "HTTP_401",
            "HTTP_403",
            "UNAUTHENTICATED",
            "INVALID_SESSION"
        )
    }

    private fun userFacingAuthMessage(error: Throwable): String {
        val apiError = error as? FloentlyApiError
            ?: return "Could not connect to KieliValmis. Check your connection and try again."

        return when (apiError.code) {
            "INVALID_CREDENTIALS",
            "AUTH_INVALID_CREDENTIALS",
            "HTTP_401" -> "The email or password is incorrect."

            "EMAIL_ALREADY_EXISTS",
            "ACCOUNT_EXISTS",
            "CONFLICT" -> "An account already exists for this email."

            "RATE_LIMITED",
            "HTTP_429" -> "Too many attempts. Try again shortly."

            "HTTP_503",
            "SERVICE_UNAVAILABLE" -> "KieliValmis is temporarily unavailable. Try again shortly."

            else -> apiError.message.ifBlank {
                "Could not complete the request. Try again."
            }
        }
    }
}
