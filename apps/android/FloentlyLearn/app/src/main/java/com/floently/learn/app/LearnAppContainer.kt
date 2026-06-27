package com.floently.learn.app

import android.content.Context
import com.floently.create.CreateStudioService
import com.floently.create.ServiceCreateStudioRepository
import com.floently.learn.cards.CardsService
import com.floently.learn.cards.ServiceCardsRepository
import com.floently.learn.progress.LearnProgressService
import com.floently.learn.progress.ServiceLearnProgressRepository
import com.floently.learn.roleplay.RoleplayService
import com.floently.learn.roleplay.ServiceRoleplayRepository
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
    private val roleplayService = RoleplayService(api = apiClient)
    private val cardsService = CardsService(api = apiClient)
    private val progressService = LearnProgressService(api = apiClient)

    val authRepository = FloentlyAuthRepository(service = authService, store = sessionStore)
    val accessRepository = FloentlyAccessRepository(service = accessService)
    val billingRepository = ServiceFloentlyBillingRepository(service = billingService)
    val readRepository = ServiceReadRepository(service = readService)
    val createStudioRepository = ServiceCreateStudioRepository(service = createStudioService)
    val roleplayRepository = ServiceRoleplayRepository(service = roleplayService)
    val cardsRepository = ServiceCardsRepository(service = cardsService)
    val progressRepository = ServiceLearnProgressRepository(service = progressService)
}
