package com.floently.learn.app

import android.content.Context
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.auth.FloentlyAuthRepository
import com.floently.shared.auth.FloentlyAuthService
import com.floently.shared.auth.FloentlySecureSessionStore

class LearnAppContainer(context: Context) {
    private val sessionStore = FloentlySecureSessionStore(context.applicationContext)
    private val apiClient = FloentlyApiClient()
    private val authService = FloentlyAuthService(api = apiClient, store = sessionStore)

    val authRepository = FloentlyAuthRepository(service = authService, store = sessionStore)
}
