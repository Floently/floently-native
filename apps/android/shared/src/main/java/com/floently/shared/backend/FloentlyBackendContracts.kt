package com.floently.shared.backend

import com.floently.shared.access.FloentlyAccessProduct

enum class FloentlyBackendMethod {
    Get,
    Post,
    Multipart
}

enum class FloentlyBackendContractStatus {
    ContractReady,
    ServicePending
}

data class FloentlyBackendContract(
    val id: String,
    val title: String,
    val product: FloentlyAccessProduct?,
    val method: FloentlyBackendMethod,
    val pathTemplate: String,
    val authRequired: Boolean,
    val status: FloentlyBackendContractStatus,
    val releaseGate: String
)

data class FloentlyBackendDashboardState(
    val contracts: List<FloentlyBackendContract>,
    val isLoading: Boolean,
    val errorMessage: String?
) {
    val readyCount: Int
        get() = contracts.count { it.status == FloentlyBackendContractStatus.ContractReady }

    val pendingCount: Int
        get() = contracts.count { it.status == FloentlyBackendContractStatus.ServicePending }
}
