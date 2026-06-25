package com.floently.shared.auth

import org.json.JSONObject

data class FloentlyUser(
    val id: String,
    val email: String,
    val name: String?
)

data class FloentlyAuthSession(
    val user: FloentlyUser,
    val token: String,
    val refreshToken: String?
)

fun FloentlyAuthSession.toJson(): JSONObject {
    val userJson = JSONObject()
        .put("id", user.id)
        .put("email", user.email)
        .put("name", user.name)

    return JSONObject()
        .put("user", userJson)
        .put("token", token)
        .put("refresh_token", refreshToken)
}

fun authSessionFromJson(json: JSONObject): FloentlyAuthSession {
    val userJson = json.optJSONObject("user")
        ?: json.optJSONObject("auth_user")
        ?: JSONObject()

    val tokensJson = json.optJSONObject("tokens")

    return FloentlyAuthSession(
        user = FloentlyUser(
            id = userJson.optString("id", userJson.optString("user_id")),
            email = userJson.optString("email"),
            name = userJson.optString("name")
                .ifBlank { userJson.optString("display_name") }
                .ifBlank { userJson.optString("full_name") }
                .takeIf { it.isNotBlank() }
        ),
        token = json.optString("token")
            .ifBlank { tokensJson?.optString("access_token").orEmpty() },
        refreshToken = json.optString("refresh_token")
            .ifBlank { tokensJson?.optString("refresh_token").orEmpty() }
            .takeIf { it.isNotBlank() }
    )
}
