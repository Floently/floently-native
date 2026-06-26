package com.floently.learn.app

import android.content.Context
import com.floently.create.CreateStudioService
import com.floently.create.ServiceCreateStudioRepository
import com.floently.read.ReadService
import com.floently.read.ServiceReadRepository
import com.floently.shared.access.FloentlyAccessRepository
import com.floently.shared.access.FloentlyAccessService
import com.floently.shared.api.FloentlyApiClient
import com.floently.shared.auth.FloentlyAuthRepository
import com.floently.shared.auth.FloentlyAuthService
import com.floently.shared.auth.FloentlySecureSessionStore
import com.floently.shared.billing.FloentlyBillingService
import com.floently.shared.billing.ServiceFloentlyBillingRepository

class LearnAppContainer(context: Context) {
    private val sessionStore = FloentlySecureSessionStore(context.applicationContext)
    private val apiClient = FloentlyApiClient(tokenProvider = { sessionStore.session?.token })
    private val authService = FloentlyAuthService(api = apiClient, store = sessionStore)
    private val accessService = FloentlyAccessService(api = apiClient)
    private val billingService = FloentlyBillingService(api = apiClient)
    private val readService = ReadService(api = apiClient)
    private val createStudioService = CreateStudioService(api = apiClient)

    val authRepository = FloentlyAuthRepository(service = authService, store = sessionStore)
    val accessRepository = FloentlyAccessRepository(service = accessService)
    val billingRepository = ServiceFloentlyBillingRepository(service = billingService)
    val readRepository = ServiceReadRepository(service = readService)
    val createStudioRepository = ServiceCreateStudioRepository(service = createStudioService)
}
