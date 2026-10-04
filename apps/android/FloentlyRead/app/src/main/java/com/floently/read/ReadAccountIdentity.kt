package com.floently.read

fun readAccountIdentity(
    userId: String,
    email: String
): String? {
    val id = userId.trim()
    if (id.isNotEmpty()) {
        return "id:" + id
    }

    val normalizedEmail = email
        .trim()
        .lowercase()

    return normalizedEmail
        .takeIf { it.isNotEmpty() }
        ?.let { "email:" + it }
}
