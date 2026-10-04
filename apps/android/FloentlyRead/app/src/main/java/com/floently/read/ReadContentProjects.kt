package com.floently.read

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

data class ReadProjectProgress(
    val projectId: String,
    val currentSegmentIndex: Int,
    val currentCharacterOffset: Int,
    val progressPercent: Double,
    val voiceId: String?,
    val playbackRate: Double?,
    val updatedAt: String
)

private fun projectProgressFromJson(
    value: JSONObject?
): ReadProjectProgress? {
    value ?: return null

    return ReadProjectProgress(
        projectId = value.optString("projectId"),
        currentSegmentIndex = value.optInt("currentSegmentIndex", 0).coerceAtLeast(0),
        currentCharacterOffset = value.optInt("currentCharacterOffset", 0).coerceAtLeast(0),
        progressPercent = value.optDouble("progressPercent", 0.0).coerceIn(0.0, 100.0),
        voiceId = value.optString("voiceId").takeIf { it.isNotBlank() },
        playbackRate = if (
            value.has("playbackRate") && !value.isNull("playbackRate")
        ) {
            value.optDouble("playbackRate").takeIf { it.isFinite() }
        } else {
            null
        },
        updatedAt = value.optString("updatedAt")
    )
}

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
    val rawText: String?,
    val progress: ReadProjectProgress? = null
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
            "web", "website", "url" -> "Website"
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
        rawText = string("rawText", "raw_text", "text", "content")
            .takeIf { it.isNotBlank() },
        progress = projectProgressFromJson(
            value.optJSONObject("progress")
        )
    )
}

class ReadContentProjectClient(
    private val baseUrl: String =
        "https://flowreader-api.onrender.com"
) {
    suspend fun listProjects(
        accessToken: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<ReadContentProject> {
        val url = baseUrl.trimEnd('/') +
            "/api/v1/documents?limit=" +
            limit.coerceIn(1, 100) +
            "&offset=" +
            offset.coerceAtLeast(0)

        val payload = unwrap(
            requestPayload(
                url = url,
                method = "GET",
                accessToken = accessToken
            )
        )

        val documents = when (payload) {
            is JSONArray -> payload
            is JSONObject ->
                payload.optJSONArray("projects")
                    ?: payload.optJSONArray("documents")
                    ?: payload.optJSONArray("items")
                    ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until documents.length()) {
                val value = documents.optJSONObject(index) ?: continue
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

        val payload = unwrap(
            requestPayload(
                url = baseUrl.trimEnd('/') +
                    "/api/v1/documents/$encoded",
                method = "GET",
                accessToken = accessToken
            )
        )
        val objectValue = payload as? JSONObject
            ?: throw IllegalStateException(
                "The document service returned an invalid response."
            )
        val candidate = objectValue.optJSONObject("project")
            ?: objectValue.optJSONObject("document")
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
            .put("content", normalized)
            .put("language", "auto")
            .put("sourceType", "text")
            .put("source_type", "text")
        title?.trim()?.takeIf { it.isNotEmpty() }?.let {
            body.put("title", it)
        }

        val payload = unwrap(
            requestPayload(
                url = baseUrl.trimEnd('/') +
                    "/api/v1/documents/from-text",
                method = "POST",
                accessToken = accessToken,
                body = body
            )
        )
        val objectValue = payload as? JSONObject
            ?: throw IllegalStateException(
                "The document service returned an invalid response."
            )
        val candidate = objectValue.optJSONObject("project")
            ?: objectValue.optJSONObject("document")
            ?: objectValue

        return projectFromJson(candidate)
            ?: throw IllegalStateException(
                "The saved project did not include readable content."
            )
    }

    suspend fun createUrlProject(
        title: String?,
        sourceUrl: String,
        accessToken: String
    ): ReadContentProject {
        val normalized = sourceUrl.trim()
        val parsed = runCatching {
            java.net.URI(normalized)
        }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        require(
            scheme == "https" || scheme == "http"
        ) {
            "Enter a valid http or https URL."
        }

        val body = JSONObject()
            .put("url", normalized)
        title?.trim()?.takeIf { it.isNotEmpty() }?.let {
            body.put("title", it)
        }

        val payload = unwrap(
            requestPayload(
                url = baseUrl.trimEnd('/') +
                    "/api/v1/documents/from-url",
                method = "POST",
                accessToken = accessToken,
                body = body
            )
        )
        val objectValue = payload as? JSONObject
            ?: throw IllegalStateException(
                "The document service returned an invalid response."
            )
        val candidate = objectValue.optJSONObject("project")
            ?: objectValue.optJSONObject("document")
            ?: objectValue

        return projectFromJson(candidate)
            ?: throw IllegalStateException(
                "The imported link did not contain readable content."
            )
    }

    suspend fun updateProgress(
        projectId: String,
        currentSegmentIndex: Int,
        currentCharacterOffset: Int,
        progressPercent: Double,
        voiceId: String?,
        playbackRate: Double?,
        accessToken: String
    ): ReadProjectProgress? {
        val encoded = URLEncoder.encode(
            projectId,
            StandardCharsets.UTF_8.name()
        ).replace("+", "%20")

        val body = JSONObject()
            .put("currentSegmentIndex", currentSegmentIndex.coerceAtLeast(0))
            .put("currentCharacterOffset", currentCharacterOffset.coerceAtLeast(0))
            .put("progressPercent", progressPercent.coerceIn(0.0, 100.0))

        voiceId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            body.put("voiceId", it)
        }
        playbackRate?.takeIf { it.isFinite() }?.let {
            body.put("playbackRate", it.coerceIn(0.5, 3.0))
        }

        val payload = unwrap(
            requestPayload(
                url = baseUrl.trimEnd('/') + "/api/v1/documents/$encoded/progress",
                method = "PUT",
                accessToken = accessToken,
                body = body
            )
        )
        val objectValue = payload as? JSONObject ?: return null
        return projectProgressFromJson(objectValue.optJSONObject("progress"))
    }

    suspend fun deleteProject(
        id: String,
        accessToken: String
    ) {
        val encoded = URLEncoder.encode(
            id,
            StandardCharsets.UTF_8.name()
        ).replace("+", "%20")

        requestPayload(
            url = baseUrl.trimEnd('/') +
                "/api/v1/documents/$encoded",
            method = "DELETE",
            accessToken = accessToken
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
        val safeFileName = fileName
            .replace("\r", "_")
            .replace("\n", "_")
            .replace("\"", "_")
        val rawMime = resolver.getType(uri)
            .orEmpty()
            .trim()
        val mime = rawMime.takeIf {
            it.matches(
                Regex(
                    "^[A-Za-z0-9!#        val fileName = queryDisplayName(
            resolver = resolver,
            uri = uri
        ) ?: "document"
        val mime = resolver.getType(uri)
            ?: "application/octet-stream"
        val boundary = "FloentlyRead-" + UUID.randomUUID()
^_.+-]+/[A-Za-z0-9!#        val fileName = queryDisplayName(
            resolver = resolver,
            uri = uri
        ) ?: "document"
        val mime = resolver.getType(uri)
            ?: "application/octet-stream"
        val boundary = "FloentlyRead-" + UUID.randomUUID()
^_.+-]+$"
                )
            )
        } ?: "application/octet-stream"
        val fileSize = runCatching {
            resolver.openAssetFileDescriptor(
                uri,
                "r"
            )?.use {
                it.length
            }
        }.getOrNull()
        if (fileSize == 0L) {
            throw IllegalArgumentException(
                "The selected file is empty."
            )
        }
        if (
            fileSize != null
            && fileSize > 75L * 1024L * 1024L
        ) {
            throw IllegalArgumentException(
                "Files larger than 75 MB are not supported."
            )
        }
        val boundary = "FloentlyRead-" + UUID.randomUUID()

        val connection = URL(
            baseUrl.trimEnd('/') + "/api/v1/documents/upload"
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
                        safeFileName +
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
                JSONTokener(raw).nextValue()
            }
            val unwrapped = unwrap(payload)
            val objectValue = unwrapped as? JSONObject
                ?: throw IllegalStateException(
                    "The document service returned an invalid response."
                )
            val candidate = objectValue.optJSONObject("project")
                ?: objectValue.optJSONObject("document")
                ?: objectValue

            projectFromJson(candidate)
                ?: throw IllegalStateException(
                    "The imported document did not contain readable text."
                )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestPayload(
        url: String,
        method: String,
        accessToken: String,
        body: JSONObject? = null
    ): Any = withContext(Dispatchers.IO) {
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

            if (raw.isBlank()) {
                JSONObject()
            } else {
                JSONTokener(raw).nextValue()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unwrap(payload: Any): Any {
        if (payload !is JSONObject) {
            return payload
        }

        return if (
            payload.has("data")
            && !payload.isNull("data")
        ) {
            payload.get("data")
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

class ReadProjectStore(
    context: Context
) {
    private val applicationContext =
        context.applicationContext

    var projects by mutableStateOf<List<ReadContentProject>>(
        emptyList()
    )
        private set

    var activity by mutableStateOf("idle")
        private set

    var errorMessage by mutableStateOf<String?>(null)

    private val client = ReadContentProjectClient()
    private var snapshotAccountIdentity: String? = null
    private val progressSyncMutex = Mutex()
    private var progressSyncGeneration = 0L
    private var progressSyncSequence = 0L

    fun bindAccount(
        userId: String,
        email: String
    ) {
        snapshotAccountIdentity =
            readAccountIdentity(
                userId = userId,
                email = email
            )
    }

    suspend fun refresh(accessToken: String) {
        activity = "loading"
        errorMessage = null

        val cached =
            snapshotAccountIdentity?.let {
                ReadProjectSnapshotStore.load(
                    context = applicationContext,
                    accountIdentity = it
                )
            } ?: emptyList()
        if (
            projects.isEmpty()
            && cached.isNotEmpty()
        ) {
            projects = cached
        }

        try {
            val remote =
                client.listProjects(
                    accessToken
                )
            projects = mergeRemoteProjects(
                remote = remote,
                cached = cached
            )
            flushPendingProgress(
                accessToken = accessToken,
                remoteProjects = remote
            )
            runCatching {
                persistSnapshot()
            }
        } catch (
            error: CancellationException
        ) {
            activity = "idle"
            throw error
        } catch (error: Exception) {
            errorMessage =
                if (projects.isEmpty()) {
                    error.message
                        ?: "Could not load your library."
                } else {
                    "You’re offline. Showing saved library content from this device."
                }
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

        val hydrated = client.project(
            id = project.id,
            accessToken = accessToken
        )
        upsert(hydrated)
        runCatching {
            persistSnapshot()
        }
        return hydrated
    }

    suspend fun addText(
        title: String?,
        text: String,
        accessToken: String
    ): ReadContentProject {
        errorMessage = null
        activity = "Saving text"
        return try {
            val project = client.createTextProject(
                title = title,
                text = text,
                accessToken = accessToken
            )
            upsert(project)
            runCatching {
                persistSnapshot()
            }
            project
        } finally {
            activity = "idle"
        }
    }

    suspend fun addUrl(
        title: String?,
        sourceUrl: String,
        accessToken: String
    ): ReadContentProject {
        errorMessage = null
        activity = "Importing link"
        return try {
            val project = client.createUrlProject(
                title = title,
                sourceUrl = sourceUrl,
                accessToken = accessToken
            )
            upsert(project)
            runCatching {
                persistSnapshot()
            }
            project
        } finally {
            activity = "idle"
        }
    }

    suspend fun delete(
        project: ReadContentProject,
        accessToken: String
    ) {
        errorMessage = null
        client.deleteProject(
            id = project.id,
            accessToken = accessToken
        )
        ReadOriginalDocumentStore.delete(
            context = applicationContext,
            projectId = project.id
        )
        snapshotAccountIdentity?.let {
            ReadOfflineAudioStore.removeDocument(
                context = applicationContext,
                accountIdentity = it,
                documentId = project.id
            )
            ReadProgressOutboxStore.removeProject(
                context = applicationContext,
                accountIdentity = it,
                projectId = project.id
            )
        }
        projects = projects.filterNot {
            it.id == project.id
        }
        runCatching {
            persistSnapshot()
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
            val project = client.uploadProject(
                uri = uri,
                resolver = resolver,
                accessToken = accessToken
            )

            if (
                project.sourceType.lowercase() == "pdf"
                || resolver.getType(uri)
                    ?.lowercase() == "application/pdf"
            ) {
                runCatching {
                    ReadOriginalDocumentStore.savePdf(
                        context = applicationContext,
                        projectId = project.id,
                        uri = uri,
                        resolver = resolver
                    )
                }
            }

            upsert(project)
            runCatching {
                persistSnapshot()
            }
            project
        } finally {
            activity = "idle"
        }
    }

    suspend fun syncProgress(
        projectId: String,
        currentSegmentIndex: Int,
        currentCharacterOffset: Int,
        progressPercent: Double,
        voiceId: String?,
        playbackRate: Double?,
        accessToken: String
    ) {
        val generation = progressSyncGeneration
        progressSyncSequence += 1L
        val sequence = progressSyncSequence
        val accountIdentity =
            snapshotAccountIdentity
        val baseServerUpdatedAt =
            projects.firstOrNull {
                it.id == projectId
            }?.progress?.updatedAt
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
        val pending =
            ReadPendingProgressWrite(
                nonce =
                    UUID.randomUUID()
                        .toString(),
                projectId = projectId,
                currentSegmentIndex =
                    currentSegmentIndex
                        .coerceAtLeast(0),
                currentCharacterOffset =
                    currentCharacterOffset
                        .coerceAtLeast(0),
                progressPercent =
                    progressPercent
                        .coerceIn(
                            0.0,
                            100.0
                        ),
                voiceId = voiceId,
                playbackRate =
                    playbackRate
                        ?.takeIf {
                            it.isFinite()
                        }
                        ?.coerceIn(
                            0.5,
                            3.0
                        ),
                baseServerUpdatedAt =
                    baseServerUpdatedAt,
                createdAtMs =
                    System.currentTimeMillis()
            )

        if (accountIdentity != null) {
            try {
                ReadProgressOutboxStore.save(
                    context = applicationContext,
                    accountIdentity =
                        accountIdentity,
                    write = pending
                )
            } catch (
                error: CancellationException
            ) {
                throw error
            } catch (_: Exception) {
                // Network sync can still proceed if local outbox storage
                // is temporarily unavailable.
            }
        }

        progressSyncMutex.withLock {
            if (
                generation != progressSyncGeneration
                || sequence != progressSyncSequence
            ) {
                return@withLock
            }

            try {
                val progress =
                    client.updateProgress(
                        projectId =
                            pending.projectId,
                        currentSegmentIndex =
                            pending
                                .currentSegmentIndex,
                        currentCharacterOffset =
                            pending
                                .currentCharacterOffset,
                        progressPercent =
                            pending.progressPercent,
                        voiceId =
                            pending.voiceId,
                        playbackRate =
                            pending.playbackRate,
                        accessToken = accessToken
                    )

                if (accountIdentity != null) {
                    ReadProgressOutboxStore
                        .removeIfMatches(
                            context =
                                applicationContext,
                            accountIdentity =
                                accountIdentity,
                            projectId =
                                pending.projectId,
                            nonce =
                                pending.nonce
                        )
                }

                if (
                    generation
                        != progressSyncGeneration
                    || sequence
                        != progressSyncSequence
                ) {
                    return@withLock
                }

                if (progress != null) {
                    projects =
                        projects.map { project ->
                            if (
                                project.id
                                    == projectId
                            ) {
                                project.copy(
                                    progress =
                                        progress
                                )
                            } else {
                                project
                            }
                        }
                }
            } catch (
                error: CancellationException
            ) {
                throw error
            } catch (_: Exception) {
                // The write-ahead outbox retains the newest cursor for
                // reconnect. Local resume remains authoritative offline.
            }
        }
        // Best effort: local resume remains authoritative offline.
        // Serializing writes prevents an older cursor from completing after
        // a newer one and moving cloud progress backwards. Waiting writes are
        // coalesced so only the newest queued cursor reaches the server.
    }

    fun reset() {
        progressSyncGeneration += 1L
        progressSyncSequence += 1L
        snapshotAccountIdentity = null
        projects = emptyList()
        activity = "idle"
        errorMessage = null
    }

    private suspend fun flushPendingProgress(
        accessToken: String,
        remoteProjects: List<ReadContentProject>
    ) {
        val accountIdentity =
            snapshotAccountIdentity
                ?: return
        val remoteById =
            remoteProjects.associateBy {
                it.id
            }
        val writes =
            ReadProgressOutboxStore.entries(
                context = applicationContext,
                accountIdentity = accountIdentity
            )

        progressSyncMutex.withLock {
            for (write in writes) {
                val current =
                    ReadProgressOutboxStore
                        .currentWrite(
                            context =
                                applicationContext,
                            accountIdentity =
                                accountIdentity,
                            projectId =
                                write.projectId
                        )
                if (
                    current == null
                    || current.nonce
                        != write.nonce
                ) {
                    continue
                }

                val remote =
                    remoteById[write.projectId]
                if (remote == null) {
                    ReadProgressOutboxStore
                        .removeIfMatches(
                            context =
                                applicationContext,
                            accountIdentity =
                                accountIdentity,
                            projectId =
                                write.projectId,
                            nonce = write.nonce
                        )
                    continue
                }

                val remoteUpdatedAt =
                    remote.progress
                        ?.updatedAt
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                val baseUpdatedAt =
                    write.baseServerUpdatedAt
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }

                if (
                    remoteUpdatedAt != null
                    && remoteUpdatedAt
                        != baseUpdatedAt
                ) {
                    // The server advanced since this offline write was
                    // based on it. Remote wins so another device's newer
                    // cursor cannot be overwritten by a stale replay.
                    ReadProgressOutboxStore
                        .removeIfMatches(
                            context =
                                applicationContext,
                            accountIdentity =
                                accountIdentity,
                            projectId =
                                write.projectId,
                            nonce = write.nonce
                        )
                    continue
                }

                try {
                    val progress =
                        client.updateProgress(
                            projectId =
                                write.projectId,
                            currentSegmentIndex =
                                write
                                    .currentSegmentIndex,
                            currentCharacterOffset =
                                write
                                    .currentCharacterOffset,
                            progressPercent =
                                write.progressPercent,
                            voiceId =
                                write.voiceId,
                            playbackRate =
                                write.playbackRate,
                            accessToken =
                                accessToken
                        )

                    ReadProgressOutboxStore
                        .removeIfMatches(
                            context =
                                applicationContext,
                            accountIdentity =
                                accountIdentity,
                            projectId =
                                write.projectId,
                            nonce = write.nonce
                        )

                    if (progress != null) {
                        projects =
                            projects.map { project ->
                                if (
                                    project.id
                                        == write.projectId
                                ) {
                                    project.copy(
                                        progress =
                                            progress
                                    )
                                } else {
                                    project
                                }
                            }
                    }
                } catch (
                    error: CancellationException
                ) {
                    throw error
                } catch (_: Exception) {
                    // Keep the pending write for a later online refresh.
                    continue
                }
            }
        }
    }

    private fun mergeRemoteProjects(
        remote: List<ReadContentProject>,
        cached: List<ReadContentProject>
    ): List<ReadContentProject> {
        val cachedById = cached.associateBy {
            it.id
        }

        return remote.map { value ->
            val local = cachedById[value.id]
            if (
                local == null
                || local.revisionId != value.revisionId
            ) {
                value
            } else {
                value.copy(
                    rawText =
                        value.rawText
                            ?: local.rawText,
                    progress =
                        value.progress
                            ?: local.progress
                )
            }
        }
    }

    private suspend fun persistSnapshot() {
        val identity =
            snapshotAccountIdentity
                ?: return

        ReadProjectSnapshotStore.save(
            context = applicationContext,
            accountIdentity = identity,
            projects = projects
        )
    }

    private fun upsert(project: ReadContentProject) {
        projects = listOf(project) +
            projects.filterNot { it.id == project.id }
    }
}
