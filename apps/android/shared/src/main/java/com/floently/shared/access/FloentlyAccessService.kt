package com.floently.shared.access

import com.floently.shared.api.FloentlyApiClient

class FloentlyAccessService(private val api: FloentlyApiClient) {
    suspend fun getAccess(product: FloentlyAccessProduct): FloentlyProductAccess {
        val response = api.get("/api/v1/access/${product.apiName}")
        return productAccessFromJson(product = product, json = response)
    }
}
