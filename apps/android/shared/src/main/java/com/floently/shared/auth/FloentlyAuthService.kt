package com.floently.shared.auth

import com.floently.shared.api.FloentlyApiClient
import org.json.JSONObject

class FloentlyAuthService(
    private val api: FloentlyApiClient,
    private val store: FloentlySecureSessionStore
) {
    suspend fun login(email: String, password: String): FloentlyAuthSession {
        val response = api.post(
            "/api/v1/auth/login/password",
            JSONObject().put("email", email).put("password", password)
        )
        return authSessionFromJson(response).also { store.save(it) }
    }

    suspend fun register(email: String, password: String, name: String?): FloentlyAuthSession {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)

        if (!name.isNullOrBlank()) body.put("name", name)

        val response = api.post("/api/v1/auth/register/password", body)
        return authSessionFromJson(response).also { store.save(it) }
    }

    suspend fun requestPasswordReset(email: String) {
        api.post("/api/v1/auth/password-reset/request", JSONObject().put("email", email))
    }

    suspend fun confirmPasswordReset(token: String, password: String) {
        api.post(
            "/api/v1/auth/password-reset/confirm",
            JSONObject().put("token", token).put("password", password)
        )
    }

    suspend fun restoreSession(): FloentlyAuthSession {
        val response = api.get("/api/v1/auth/session")
        return authSessionFromJson(response).also { store.save(it) }
    }

    suspend fun logout() {
        runCatching {
            api.post(
                "/api/v1/auth/logout",
                JSONObject().put("refresh_token", store.session?.refreshToken)
            )
        }
        store.clear()
    }
}
