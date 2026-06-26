package com.floently.shared.auth

class FloentlyAuthRepository(
    private val service: FloentlyAuthService,
    private val store: FloentlySessionStore
) {
    fun cachedSession(): FloentlyAuthSession? = store.session

    suspend fun signIn(email: String, password: String): FloentlyAuthResult = guarded {
        service.login(email = email, password = password)
    }

    suspend fun createAccount(email: String, password: String, name: String?): FloentlyAuthResult = guarded {
        service.register(email = email, password = password, name = name)
    }

    suspend fun restoreRemoteSession(): FloentlyAuthResult = guarded {
        service.restoreSession()
    }

    suspend fun signOut() {
        service.logout()
    }

    private suspend fun guarded(block: suspend () -> FloentlyAuthSession): FloentlyAuthResult {
        return runCatching { block() }
            .fold(
                onSuccess = { FloentlyAuthResult.Success(it) },
                onFailure = {
                    FloentlyAuthResult.Failure(
                        message = it.message?.takeIf { message -> message.isNotBlank() }
                            ?: "We could not complete that request.",
                        retryable = false
                    )
                }
            )
    }
}

sealed interface FloentlyAuthResult {
    data class Success(val session: FloentlyAuthSession) : FloentlyAuthResult

    data class Failure(
        val message: String,
        val retryable: Boolean
    ) : FloentlyAuthResult
}
