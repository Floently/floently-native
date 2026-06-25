package com.floently.shared.billing

import com.floently.shared.api.FloentlyApiClient

class FloentlyAccessService(
    private val api: FloentlyApiClient
) {
    suspend fun fetchStatus(): FloentlyAccessStatus {
        return accessStatusFromJson(api.get("/api/v1/subscription/status"))
    }
}
