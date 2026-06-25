package com.floently.shared.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FloentlyApiError(
    val code: String,
    override val message: String,
    val retryable: Boolean = false
) : Exception(message)

class FloentlyApiClient(
    private val baseUrl: String = "https://learn.floently.com",
    private val tokenProvider: () -> String? = { null }
) {
    suspend fun get(path: String): JSONObject = request(path = path, method = "GET", body = null)

    suspend fun post(path: String, body: JSONObject): JSONObject = request(path = path, method = "POST", body = body)

    private suspend fun request(path: String, method: String, body: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val normalizedPath = if (path.startsWith("/")) path else "/"
        val connection = URL(baseUrl.trimEnd('/') + normalizedPath).openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        tokenProvider()?.takeIf { it.isNotBlank() }?.let {
            connection.setRequestProperty("Authorization", "Bearer ")
        }

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        }

        val status = connection.responseCode
        val raw = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }

        val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)

        if (json.has("ok")) {
            if (json.optBoolean("ok")) {
                return@withContext json.optJSONObject("data") ?: JSONObject()
            }

            val error = json.optJSONObject("error")
            throw FloentlyApiError(
                code = error?.optString("code")?.takeIf { it.isNotBlank() } ?: "API_ERROR",
                message = error?.optString("message")?.takeIf { it.isNotBlank() } ?: "Request failed.",
                retryable = error?.optBoolean("retryable") ?: false
            )
        }

        if (status in 200..299) return@withContext json

        throw FloentlyApiError(code = "HTTP_", message = "Request failed with status .")
    }
}
