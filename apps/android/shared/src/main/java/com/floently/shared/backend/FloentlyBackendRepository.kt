package com.floently.shared.backend

import com.floently.shared.access.FloentlyAccessProduct

interface FloentlyBackendRepository {
    suspend fun dashboard(): FloentlyBackendDashboardState
}

class PreviewFloentlyBackendRepository : FloentlyBackendRepository {
    override suspend fun dashboard(): FloentlyBackendDashboardState = FloentlyBackendDashboardState(
        contracts = listOf(
            FloentlyBackendContract(
                id = "access-product",
                title = "Product access check",
                product = null,
                method = FloentlyBackendMethod.Get,
                pathTemplate = "/api/native/access/{product}",
                authRequired = true,
                status = FloentlyBackendContractStatus.ContractReady,
                releaseGate = "Backend must return separated Learn, Read, and Create entitlement status."
            ),
            FloentlyBackendContract(
                id = "billing-checkout",
                title = "Checkout intent",
                product = null,
                method = FloentlyBackendMethod.Post,
                pathTemplate = "/api/native/billing/checkout",
                authRequired = true,
                status = FloentlyBackendContractStatus.ContractReady,
                releaseGate = "Payment provider session creation must be wired."
            ),
            FloentlyBackendContract(
                id = "read-import",
                title = "Read import",
                product = FloentlyAccessProduct.Read,
                method = FloentlyBackendMethod.Multipart,
                pathTemplate = "/api/native/read/import",
                authRequired = true,
                status = FloentlyBackendContractStatus.ServicePending,
                releaseGate = "Native file upload must connect to Read processing."
            ),
            FloentlyBackendContract(
                id = "read-generate",
                title = "Read generate",
                product = FloentlyAccessProduct.Read,
                method = FloentlyBackendMethod.Post,
                pathTemplate = "/api/native/read/generate",
                authRequired = true,
                status = FloentlyBackendContractStatus.ServicePending,
                releaseGate = "Language detection, generation, and reader session creation must be wired."
            ),
            FloentlyBackendContract(
                id = "create-generate",
                title = "Create generate",
                product = FloentlyAccessProduct.Create,
                method = FloentlyBackendMethod.Post,
                pathTemplate = "/api/native/create/generate",
                authRequired = true,
                status = FloentlyBackendContractStatus.ServicePending,
                releaseGate = "Direct Create functions must call the backend service."
            ),
            FloentlyBackendContract(
                id = "learn-roleplay",
                title = "Learn roleplay",
                product = FloentlyAccessProduct.Learn,
                method = FloentlyBackendMethod.Post,
                pathTemplate = "/api/native/learn/roleplay",
                authRequired = true,
                status = FloentlyBackendContractStatus.ServicePending,
                releaseGate = "Dynamic roleplay generation and review must be connected."
            ),
            FloentlyBackendContract(
                id = "progress-sync",
                title = "Progress sync",
                product = FloentlyAccessProduct.Learn,
                method = FloentlyBackendMethod.Post,
                pathTemplate = "/api/native/progress/events",
                authRequired = true,
                status = FloentlyBackendContractStatus.ServicePending,
                releaseGate = "Durable progress events, offline queue, and merge handling must be connected."
            )
        ),
        isLoading = false,
        errorMessage = null
    )
}
