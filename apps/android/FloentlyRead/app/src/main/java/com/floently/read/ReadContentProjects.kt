package com.floently.read

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

data class ReadContentProject(
    val id: String,
    val title: String,
    val kind: String,
    val status: String,
    val sourceType: String,
    val sourceUrl: String?,
    val language: String?,
    val textHash: String,
    val wordCount: Int,
    val characterCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val lastOpenedAt: String?,
    val rawText: String?
) {
    val revisionId: String
        get() = textHash.trim().takeIf { it.isNotEmpty() }
            ?: updatedAt.trim().takeIf { it.isNotEmpty() }
            ?: "project-$id"

    val displaySource: String
        get() = when (sourceType.lowercase()) {
            "pdf" -> "PDF"
            "epub" -> "EPUB"
            "docx", "word" -> "Document"
            "web", "website" -> "Website"
            "text", "txt", "markdown", "md" -> "Text"
            else -> sourceType.ifBlank { "Document" }
                .replaceFirstChar { it.titlecase() }
        }
}

private fun projectFromJson(value: JSONObject): ReadContentProject? {
    fun string(vararg keys: String, fallback: String = ""): String {
        keys.forEach { key ->
            if (value.has(key) && !value.isNull(key)) {
                val candidate = value.optString(key)
                if (candidate.isNotBlank()) return candidate
            }
        }
        return fallback
    }

    fun integer(vararg keys: String): Int {
        keys.forEach { key ->
            if (value.has(key) && !value.isNull(key)) {
                return value.optInt(key, 0)
            }
        }
        return 0
    }

    val id = string("id").trim()
    val title = string("title").trim()
    if (id.isEmpty() || title.isEmpty()) return null

    return ReadContentProject(
        id = id,
        title = title,
        kind = string("kind", fallback = "document"),
        status = string("status", fallback = "ready"),
        sourceType = string(
            "sourceType",
            "source_type",
            fallback = "text"
        ),
        sourceUrl = string(
            "sourceUrl",
            "source_url"
        ).takeIf { it.isNotBlank() },
        language = string("language")
            .takeIf { it.isNotBlank() },
        textHash = string("textHash", "text_hash"),
        wordCount = integer("wordCount", "word_count"),
        characterCount = integer(
            "characterCount",
            "character_count"
        ),
        createdAt = string("createdAt", "created_at"),
        updatedAt = string("updatedAt", "updated_at"),
        lastOpenedAt = string(
            "lastOpenedAt",
            "last_opened_at"
        ).takeIf { it.isNotBlank() },
        rawText = string("rawText", "raw_text")
            .takeIf { it.isNotBlank() }
    )
}

class ReadContentProjectClient(
    private val baseUrl: String =
        "https://learn-api.floently.com"
) {
    suspend fun listProjects(
        accessToken: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<ReadContentProject> {
        val url = baseUrl.trimEnd('/') +
            "/api/v1/projects?limit=" +
            limit.coerceIn(1, 100) +
            "&offset=" +
            offset.coerceAtLeast(0)

        val payload = requestJson(
            url = url,
            method = "GET",
            accessToken = accessToken
        )
        val objectValue = unwrap(payload)
        val projects = objectValue.optJSONArray("projects")
            ?: JSONArray()

        return buildList {
            for (index in 0 until projects.length()) {
                val value = projects.optJSONObject(index) ?: continue
                projectFromJson(value)?.let(::add)
            }
        }
    }

    suspend fun project(
        id: String,
        accessToken: String
    ): ReadContentProject {
        val encoded = URLEncoder.encode(
            id,
            StandardCharsets.UTF_8.name()
        ).replace("+", "%20")

        val payload = requestJson(
            url = baseUrl.trimEnd('/') +
                "/api/v1/projects/$encoded",
            method = "GET",
            accessToken = accessToken
        )
        val objectValue = unwrap(payload)
        val candidate = objectValue.optJSONObject("project")
            ?: objectValue

        val project = projectFromJson(candidate)
            ?: throw IllegalStateException(
                "The project did not include readable content."
            )
        if (project.rawText.isNullOrBlank()) {
            throw IllegalStateException(
                "The project did not include readable text."
            )
        }
        return project
    }

    suspend fun createTextProject(
        title: String?,
        text: String,
        accessToken: String
    ): ReadContentProject {
        val normalized = text.trim()
        require(normalized.isNotEmpty()) {
            "Add text before importing it."
        }

        val body = JSONObject()
            .put("text", normalized)
            .put("sourceType", "text")
        title?.trim()?.takeIf { it.isNotEmpty() }?.let {
            body.put("title", it)
        }

        val payload = requestJson(
            url = baseUrl.trimEnd('/') +
                "/api/v1/projects/from-text",
            method = "POST",
            accessToken = accessToken,
            body = body
        )
        val objectValue = unwrap(payload)
        val candidate = objectValue.optJSONObject("project")
            ?: objectValue

        return projectFromJson(candidate)
            ?: throw IllegalStateException(
                "The saved project did not include readable content."
            )
    }

    suspend fun uploadProject(
        uri: Uri,
        resolver: ContentResolver,
        accessToken: String
    ): ReadContentProject = withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(
            resolver = resolver,
            uri = uri
        ) ?: "document"
        val mime = resolver.getType(uri)
            ?: "application/octet-stream"
        val boundary = "FloentlyRead-" + UUID.randomUUID()

        val connection = URL(
            baseUrl.trimEnd('/') + "/api/v1/projects/upload"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setChunkedStreamingMode(256 * 1024)
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            BufferedOutputStream(
                connection.outputStream,
                256 * 1024
            ).use { output ->
                fun write(value: String) {
                    output.write(
                        value.toByteArray(Charsets.UTF_8)
                    )
                }

                write("--$boundary\r\n")
                write(
                    "Content-Disposition: form-data; " +
                        "name=\"file\"; filename=\"" +
                        fileName.replace("\"", "_") +
                        "\"\r\n"
                )
                write("Content-Type: $mime\r\n\r\n")

                val input = resolver.openInputStream(uri)
                    ?: throw IllegalStateException(
                        "The selected file could not be opened."
                    )
                input.buffered(256 * 1024).use {
                    it.copyTo(output, 256 * 1024)
                }

                write("\r\n--$boundary--\r\n")
                output.flush()
            }

            val status = connection.responseCode
            val raw = readResponse(connection, status)

            if (status !in 200..299) {
                throw IllegalStateException(
                    errorMessage(raw)
                        ?: "Could not import this document."
                )
            }

            val payload = if (raw.isBlank()) {
                JSONObject()
            } else {
                JSONObject(raw)
            }
            val objectValue = unwrap(payload)
            val candidate = objectValue.optJSONObject("project")
                ?: objectValue

            projectFromJson(candidate)
                ?: throw IllegalStateException(
                    "The imported document did not contain readable text."
                )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestJson(
        url: String,
        method: String,
        accessToken: String,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                OutputStreamWriter(
                    connection.outputStream,
                    Charsets.UTF_8
                ).use {
                    it.write(body.toString())
                }
            }

            val status = connection.responseCode
            val raw = readResponse(connection, status)

            if (status !in 200..299) {
                throw IllegalStateException(
                    errorMessage(raw)
                        ?: "Read service failed with HTTP $status."
                )
            }

            if (raw.isBlank()) JSONObject() else JSONObject(raw)
        } finally {
            connection.disconnect()
        }
    }

    private fun unwrap(payload: JSONObject): JSONObject {
        return if (
            payload.optBoolean("ok")
            && payload.optJSONObject("data") != null
        ) {
            payload.optJSONObject("data")!!
        } else {
            payload
        }
    }

    private fun readResponse(
        connection: HttpURLConnection,
        status: Int
    ): String {
        val stream = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        return stream
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
    }

    private fun errorMessage(raw: String): String? {
        if (raw.isBlank()) return null
        val payload = runCatching {
            JSONObject(raw)
        }.getOrNull() ?: return null

        listOf("detail", "message", "error")
            .forEach { key ->
                val value = payload.optString(key)
                if (value.isNotBlank()) {
                    return value
                }
            }

        return payload.optJSONObject("error")
            ?.optString("message")
            ?.takeIf { it.isNotBlank() }
    }

    private fun queryDisplayName(
        resolver: ContentResolver,
        uri: Uri
    ): String? {
        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            if (
                cursor != null
                && cursor.moveToFirst()
            ) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } finally {
            cursor?.close()
        }
    }
}

class ReadProjectStore {
    var projects by mutableStateOf<List<ReadContentProject>>(
        emptyList()
    )
        private set

    var activity by mutableStateOf("idle")
        private set

    var errorMessage by mutableStateOf<String?>(null)

    private val client = ReadContentProjectClient()

    suspend fun refresh(accessToken: String) {
        activity = "loading"
        errorMessage = null

        runCatching {
            client.listProjects(accessToken)
        }
            .onSuccess {
                projects = it
            }
            .onFailure {
                errorMessage = it.message
                    ?: "Could not load your library."
            }

        activity = "idle"
    }

    suspend fun hydrate(
        project: ReadContentProject,
        accessToken: String
    ): ReadContentProject {
        if (!project.rawText.isNullOrBlank()) {
            return project
        }
        return client.project(
            id = project.id,
            accessToken = accessToken
        )
    }

    suspend fun addText(
        title: String?,
        text: String,
        accessToken: String
    ): ReadContentProject {
        errorMessage = null
        activity = "Saving text"
        return try {
            client.createTextProject(
                title = title,
                text = text,
                accessToken = accessToken
            ).also(::upsert)
        } finally {
            activity = "idle"
        }
    }

    suspend fun addFile(
        uri: Uri,
        resolver: ContentResolver,
        accessToken: String
    ): ReadContentProject {
        errorMessage = null
        activity = "Uploading and extracting"
        return try {
            client.uploadProject(
                uri = uri,
                resolver = resolver,
                accessToken = accessToken
            ).also(::upsert)
        } finally {
            activity = "idle"
        }
    }

    private fun upsert(project: ReadContentProject) {
        projects = listOf(project) +
            projects.filterNot { it.id == project.id }
    }
}
