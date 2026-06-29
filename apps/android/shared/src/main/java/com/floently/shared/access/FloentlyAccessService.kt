package com.floently.shared.access

import com.floently.shared.api.FloentlyApiClient

class FloentlyAccessService(private val api: FloentlyApiClient) {
    suspend fun getAccess(product: FloentlyAccessProduct): FloentlyProductAccess {
        val nativePath = "/api/native/access/${product.apiName}"
        val legacyPath = "/api/v1/access/${product.apiName}"
        val response = runCatching { api.get(nativePath) }
            .getOrElse { api.get(legacyPath) }
        return productAccessFromJson(product = product, json = response)
    }
}
