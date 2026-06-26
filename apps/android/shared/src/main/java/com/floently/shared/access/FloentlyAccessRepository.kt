package com.floently.shared.access

class FloentlyAccessRepository(private val service: FloentlyAccessService) {
    suspend fun requireAccess(product: FloentlyAccessProduct): FloentlyAccessResult {
        return runCatching { service.getAccess(product) }
            .fold(
                onSuccess = { access ->
                    if (access.isAllowed) {
                        FloentlyAccessResult.Allowed(access)
                    } else {
                        FloentlyAccessResult.Blocked(
                            product = product,
                            reason = access.reason ?: "Access is required for this product."
                        )
                    }
                },
                onFailure = {
                    FloentlyAccessResult.Error(
                        product = product,
                        message = it.message?.takeIf { message -> message.isNotBlank() }
                            ?: "We could not verify access right now."
                    )
                }
            )
    }
}

sealed interface FloentlyAccessResult {
    data class Allowed(val access: FloentlyProductAccess) : FloentlyAccessResult
    data class Blocked(val product: FloentlyAccessProduct, val reason: String) : FloentlyAccessResult
    data class Error(val product: FloentlyAccessProduct, val message: String) : FloentlyAccessResult
}
