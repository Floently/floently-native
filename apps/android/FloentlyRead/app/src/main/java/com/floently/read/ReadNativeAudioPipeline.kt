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
    val contentHash: String,
    val durationMs: Long?,
    val voiceId: String,
    val provider: String?,
    val model: String?
)

class ReadNativeTtsClient(
    private val baseUrl: String = "https://flowreader-api.onrender.com"
) {
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

        val digest = sha256Hex(
            listOf(
                value,
                language,
                resolvedVoice,
                "render-read-prerender"
            ).joinToString("\u001f")
        )

        val cacheKey = source.optString("cacheKey")
            .ifBlank { source.optString("cache_key") }
            .ifBlank { "read-tts:sha256:$digest" }

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
            contentHash = "sha256:$digest",
            durationMs = durationMs,
            voiceId = resolvedVoice,
            provider = source.optString("provider")
                .takeIf { it.isNotBlank() },
            model = source.optString("model")
                .takeIf { it.isNotBlank() }
        )
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

class ReadNativeAudioCache(
    context: Context,
    private val maximumBytes: Long =
        256L * 1024L * 1024L
) {
    private val root = File(
        context.applicationContext.cacheDir,
        "floently-read-audio-v1"
    ).apply { mkdirs() }
    private val protectionLock = Any()
    private var protectedPaths: Set<String> = emptySet()

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
            destination.setLastModified(System.currentTimeMillis())
            prune(
                additionallyProtected =
                    setOf(destination.canonicalPath)
            )
            return@withContext destination
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

            if (destination.exists()) {
                destination.delete()
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }

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
