package com.floently.read

import android.content.Context
import com.floently.shared.api.FloentlyApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

data class ReadNativeTtsAsset(
    val audioUri: String,
    val cacheKey: String,
    val lookupKey: String,
    val requestHash: String,
    val contentHash: String?,
    val durationMs: Long?,
    val voiceId: String,
    val provider: String?,
    val model: String?
)

class ReadNativeTtsClient(
    private val baseUrl: String = "https://flowreader-api.onrender.com"
) {
    fun lookupKey(
        text: String,
        language: String,
        voiceId: String
    ): String {
        val identity = listOf(
            text.trim(),
            language.ifBlank { "auto" },
            voiceId,
            "read-tts-lookup-v1"
        ).joinToString("\u001f")

        return "sha256:" + sha256Hex(identity)
    }
    suspend fun synthesize(
        text: String,
        language: String,
        voiceId: String,
        accessToken: String? = null
    ): ReadNativeTtsAsset {
        val value = text.trim()
        require(value.isNotEmpty()) {
            "No readable text was available for synthesis."
        }

        val api = FloentlyApiClient(
            baseUrl = baseUrl,
            tokenProvider = { accessToken }
        )

        val response = api.post(
            path = "/api/tts/prerender",
            body = JSONObject()
                .put("text", value)
                .put("language", language.ifBlank { "auto" })
                .put("voiceId", voiceId)
        )

        val source = response.optJSONObject("data") ?: response
        val audioUri = source.optString("audioUrl")
            .ifBlank { source.optString("audio_url") }
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Read TTS returned no playable audio URL."
            )

        val resolvedVoice = source.optString("voiceId")
            .ifBlank { source.optString("voice_id") }
            .ifBlank { voiceId }

        val lookupKey = lookupKey(
            text = value,
            language = language,
            voiceId = voiceId
        )

        val resolvedProvider = source.optString("provider")
            .trim()
            .takeIf { it.isNotBlank() }
        val resolvedModel = source.optString("model")
            .trim()
            .takeIf { it.isNotBlank() }

        val requestDigest = sha256Hex(
            listOf(
                value,
                language.ifBlank { "auto" },
                resolvedVoice,
                resolvedProvider ?: "unknown-provider",
                resolvedModel ?: "unknown-model",
                "read-tts-request-v2"
            ).joinToString("\u001f")
        )
        val requestHash = "sha256:$requestDigest"
        val contentHash = source.optString("contentHash")
            .ifBlank { source.optString("content_hash") }
            .takeIf { it.isNotBlank() }

        val cacheKey = source.optString("cacheKey")
            .ifBlank { source.optString("cache_key") }
            .ifBlank { "read-tts:req:$requestHash" }

        val durationMs = when {
            source.has("durationMs") ->
                source.optLong("durationMs").coerceAtLeast(0L)
            source.has("duration_ms") ->
                source.optLong("duration_ms").coerceAtLeast(0L)
            source.has("durationSeconds") ->
                (source.optDouble("durationSeconds") * 1_000)
                    .toLong()
                    .coerceAtLeast(0L)
            source.has("duration_seconds") ->
                (source.optDouble("duration_seconds") * 1_000)
                    .toLong()
                    .coerceAtLeast(0L)
            else -> null
        }

        return ReadNativeTtsAsset(
            audioUri = audioUri,
            cacheKey = cacheKey,
            lookupKey = lookupKey,
            requestHash = requestHash,
            contentHash = contentHash,
            durationMs = durationMs,
            voiceId = resolvedVoice,
            provider = resolvedProvider,
            model = resolvedModel
        )
    }

    private fun writeIndex(
        asset: ReadNativeTtsAsset,
        fileName: String
    ) {
        val json = JSONObject()
            .put("fileName", fileName)
            .put("voiceId", asset.voiceId)
            .put("requestHash", asset.requestHash)

        asset.durationMs?.let {
            json.put("durationMs", it)
        }
        asset.provider?.let {
            json.put("provider", it)
        }
        asset.model?.let {
            json.put("model", it)
        }
        asset.contentHash?.let {
            json.put("contentHash", it)
        }

        val target = indexFile(asset.lookupKey)
        val temporary = File(
            indexRoot,
            target.name + ".partial-" + System.nanoTime()
        )
        temporary.writeText(json.toString())
        if (target.exists()) {
            target.delete()
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    private fun indexFile(
        lookupKey: String
    ): File = File(
        indexRoot,
        sha256Hex(lookupKey) + ".json"
    )

    private fun verifyContentHash(
        file: File,
        expected: String?
    ): Boolean {
        val normalized = normalizeSha256(expected)
            ?: return true

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) {
                    digest.update(buffer, 0, count)
                }
            }
        }

        val actual = digest.digest()
            .joinToString("") { "%02x".format(it) }
        return actual == normalized
    }

    private fun normalizeSha256(
        value: String?
    ): String? {
        val raw = value
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val hex = if (raw.startsWith("sha256:")) {
            raw.removePrefix("sha256:")
        } else {
            raw
        }

        return hex.takeIf {
            it.length == 64
                && it.all { character ->
                    character in '0'..'9'
                        || character in 'a'..'f'
                }
        }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

data class ReadCachedAudioAsset(
    val localFile: File,
    val durationMs: Long?,
    val voiceId: String,
    val provider: String?,
    val model: String?,
    val requestHash: String,
    val contentHash: String?
)

class ReadNativeAudioCache(
    context: Context,
    private val maximumBytes: Long =
        256L * 1024L * 1024L
) {
    private val root = File(
        context.applicationContext.cacheDir,
        "floently-read-audio-v1"
    ).apply { mkdirs() }
    private val indexRoot = File(
        root,
        "index"
    ).apply { mkdirs() }
    private val protectionLock = Any()
    private var protectedPaths: Set<String> = emptySet()

    suspend fun cachedAsset(
        lookupKey: String
    ): ReadCachedAudioAsset? = withContext(Dispatchers.IO) {
        val metadataFile = indexFile(lookupKey)
        if (!metadataFile.exists()) {
            return@withContext null
        }

        val json = runCatching {
            JSONObject(metadataFile.readText())
        }.getOrNull() ?: return@withContext null

        val fileName = json.optString("fileName")
            .takeIf { it.isNotBlank() }
            ?: return@withContext null
        val audioFile = File(root, fileName)
        if (!audioFile.exists()) {
            metadataFile.delete()
            return@withContext null
        }

        val contentHash = json.optString("contentHash")
            .takeIf { it.isNotBlank() }
        if (!verifyContentHash(audioFile, contentHash)) {
            audioFile.delete()
            metadataFile.delete()
            return@withContext null
        }

        audioFile.setLastModified(System.currentTimeMillis())

        ReadCachedAudioAsset(
            localFile = audioFile,
            durationMs = if (json.has("durationMs")
                && !json.isNull("durationMs")) {
                json.optLong("durationMs")
            } else {
                null
            },
            voiceId = json.optString("voiceId"),
            provider = json.optString("provider")
                .takeIf { it.isNotBlank() },
            model = json.optString("model")
                .takeIf { it.isNotBlank() },
            requestHash = json.optString("requestHash"),
            contentHash = contentHash
        )
    }

    suspend fun localFile(
        asset: ReadNativeTtsAsset
    ): File = withContext(Dispatchers.IO) {
        val extension = runCatching {
            URL(asset.audioUri).path.substringAfterLast(
                '.',
                missingDelimiterValue = "audio"
            )
        }.getOrDefault("audio")
            .ifBlank { "audio" }

        val destination = File(
            root,
            "${sha256Hex(asset.cacheKey)}.$extension"
        )

        if (destination.exists()) {
            if (verifyContentHash(
                    destination,
                    asset.contentHash
                )
            ) {
                destination.setLastModified(
                    System.currentTimeMillis()
                )
                writeIndex(
                    asset = asset,
                    fileName = destination.name
                )
                prune(
                    additionallyProtected =
                        setOf(destination.canonicalPath)
                )
                return@withContext destination
            }

            // Never serve a cached file that failed a checksum asserted by
            // the backend. Delete it and perform one clean download.
            runCatching { destination.delete() }
        }

        val connection = URL(asset.audioUri)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException(
                    "Read audio download failed with HTTP $status."
                )
            }

            val temporary = File(
                root,
                "${destination.name}.partial-${System.nanoTime()}"
            )

            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                }
            }

            if (!verifyContentHash(
                    temporary,
                    asset.contentHash
                )
            ) {
                temporary.delete()
                throw IllegalStateException(
                    "Downloaded Read audio did not match its verified content hash."
                )
            }

            if (destination.exists()) {
                destination.delete()
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            writeIndex(
                asset = asset,
                fileName = destination.name
            )

            prune(
                additionallyProtected =
                    setOf(destination.canonicalPath)
            )
            destination
        } finally {
            connection.disconnect()
        }
    }

    suspend fun replaceProtectedUris(
        audioUris: Collection<String>
    ) = withContext(Dispatchers.IO) {
        val resolved = audioUris.mapNotNull { value ->
            runCatching {
                val uri = URI(value)
                if (uri.scheme != "file") {
                    return@runCatching null
                }
                File(uri).canonicalPath
            }.getOrNull()
        }.toSet()

        synchronized(protectionLock) {
            protectedPaths = resolved
        }

        prune()
    }

    private fun prune(
        additionallyProtected: Set<String> = emptySet()
    ) {
        val now = System.currentTimeMillis()
        root.listFiles()?.forEach { file ->
            if (
                file.isFile
                && file.name.contains(".partial-")
                && now - file.lastModified() > 60L * 60L * 1_000L
            ) {
                runCatching { file.delete() }
            }
        }

        val files = root.listFiles()?.toList().orEmpty()
            .filter {
                it.isFile
                    && !it.name.contains(".partial-")
            }

        var totalBytes = files.sumOf {
            it.length().coerceAtLeast(0L)
        }
        val budget = maximumBytes.coerceAtLeast(
            16L * 1024L * 1024L
        )
        if (totalBytes <= budget) return

        val protected = synchronized(protectionLock) {
            protectedPaths.toSet()
        } + additionallyProtected

        files
            .sortedBy { it.lastModified() }
            .forEach { file ->
                if (totalBytes <= budget) {
                    return@forEach
                }

                val canonicalPath = runCatching {
                    file.canonicalPath
                }.getOrElse {
                    file.absolutePath
                }
                if (canonicalPath in protected) {
                    return@forEach
                }

                val size = file.length()
                    .coerceAtLeast(0L)
                if (runCatching { file.delete() }
                        .getOrDefault(false)) {
                    totalBytes -= size
                }
            }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
