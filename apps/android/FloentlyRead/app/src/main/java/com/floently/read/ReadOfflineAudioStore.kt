package com.floently.read

import android.content.Context
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReadOfflineAudioAsset(
    val localFile: File,
    val durationMs: Long?
)

object ReadOfflineAudioStore {
    private const val SCHEMA_VERSION = 1
    private const val DIRECTORY_NAME =
        "read-offline-audio-v1"

    suspend fun asset(
        context: Context,
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String,
        segment: ReadingManifestSegmentV1
    ): ReadOfflineAudioAsset? =
        withContext(Dispatchers.IO) {
            val directory = bundleDirectory(
                context = context,
                accountIdentity = accountIdentity,
                documentId = manifest.documentId,
                revisionId = manifest.revisionId,
                voiceId = voiceId
            )
            val metadata = loadMetadata(
                directory
            ) ?: return@withContext null

            if (
                metadata.optInt(
                    "schemaVersion",
                    0
                ) != SCHEMA_VERSION
                || metadata.optString("documentId")
                    != manifest.documentId
                || metadata.optString("revisionId")
                    != manifest.revisionId
                || metadata.optString("voiceId")
                    != voiceId
            ) {
                return@withContext null
            }

            val values =
                metadata.optJSONArray("segments")
                    ?: return@withContext null

            var item: JSONObject? = null
            for (index in 0 until values.length()) {
                val candidate =
                    values.optJSONObject(index)
                        ?: continue
                if (
                    candidate.optInt("index", -1)
                        == segment.index
                    && candidate.optString("id")
                        == segment.id
                ) {
                    item = candidate
                    break
                }
            }

            val resolved =
                item ?: return@withContext null
            val fileName =
                resolved.optString("fileName")
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: return@withContext null
            val file = File(
                directory,
                fileName
            )
            if (!file.isFile) {
                return@withContext null
            }

            val expected =
                resolved.optString("sha256")
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: return@withContext null
            if (checksum(file) != expected) {
                directory.deleteRecursively()
                return@withContext null
            }

            ReadOfflineAudioAsset(
                localFile = file,
                durationMs =
                    if (
                        resolved.has("durationMs")
                        && !resolved.isNull(
                            "durationMs"
                        )
                    ) {
                        resolved.optLong(
                            "durationMs"
                        )
                    } else {
                        null
                    }
            )
        }

    suspend fun isComplete(
        context: Context,
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            val directory = bundleDirectory(
                context = context,
                accountIdentity = accountIdentity,
                documentId = manifest.documentId,
                revisionId = manifest.revisionId,
                voiceId = voiceId
            )
            val metadata = loadMetadata(
                directory
            ) ?: return@withContext false

            if (
                metadata.optInt(
                    "schemaVersion",
                    0
                ) != SCHEMA_VERSION
                || metadata.optString("documentId")
                    != manifest.documentId
                || metadata.optString("revisionId")
                    != manifest.revisionId
                || metadata.optString("voiceId")
                    != voiceId
            ) {
                return@withContext false
            }

            val values =
                metadata.optJSONArray("segments")
                    ?: return@withContext false
            if (
                values.length()
                != manifest.segments.size
            ) {
                return@withContext false
            }

            val byIndex =
                mutableMapOf<Int, JSONObject>()
            for (index in 0 until values.length()) {
                val item =
                    values.optJSONObject(index)
                        ?: continue
                byIndex[
                    item.optInt("index", -1)
                ] = item
            }

            manifest.segments.all { segment ->
                val item =
                    byIndex[segment.index]
                        ?: return@all false
                if (
                    item.optString("id")
                    != segment.id
                ) {
                    return@all false
                }

                val fileName =
                    item.optString("fileName")
                fileName.isNotBlank()
                    && File(
                        directory,
                        fileName
                    ).isFile
            }
        }

    suspend fun install(
        context: Context,
        accountIdentity: String,
        manifest: ReadingManifestV1,
        voiceId: String,
        segments: List<ReadPlaybackSegment>
    ) = withContext(Dispatchers.IO) {
        require(
            manifest.segments.isNotEmpty()
                && segments.size
                    == manifest.segments.size
        ) {
            "Read could not finish the offline audio bundle."
        }

        val byIndex =
            segments.associateBy {
                it.index
            }

        require(
            manifest.segments.all {
                byIndex[it.index]?.id == it.id
            }
        ) {
            "Read could not finish the offline audio bundle."
        }

        val target = bundleDirectory(
            context = context,
            accountIdentity = accountIdentity,
            documentId = manifest.documentId,
            revisionId = manifest.revisionId,
            voiceId = voiceId
        )
        val parent =
            requireNotNull(target.parentFile)
        check(
            parent.exists() || parent.mkdirs()
        ) {
            "Could not create offline audio storage."
        }

        val staging = File(
            parent,
            target.name
                + ".staging-"
                + UUID.randomUUID()
                    .toString()
        )
        staging.deleteRecursively()
        check(staging.mkdirs()) {
            "Could not create offline audio storage."
        }

        val written = JSONArray()

        try {
            manifest.segments.forEach {
                sourceSegment ->
                val prepared =
                    requireNotNull(
                        byIndex[sourceSegment.index]
                    )
                val audioUri =
                    prepared.audioUri
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: error(
                            "Read received an invalid local audio source while saving offline."
                        )
                val uri = URI(audioUri)
                require(uri.scheme == "file") {
                    "Read received an invalid local audio source while saving offline."
                }
                val source = File(uri)
                require(source.isFile) {
                    "Read received an invalid local audio source while saving offline."
                }

                val extension =
                    source.extension
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: "audio"
                val fileName =
                    "segment-"
                        + sourceSegment.index
                            .toString()
                            .padStart(
                                6,
                                '0'
                            )
                        + "."
                        + extension
                val destination = File(
                    staging,
                    fileName
                )

                source.copyTo(
                    target = destination,
                    overwrite = false
                )

                written.put(
                    JSONObject()
                        .put(
                            "id",
                            sourceSegment.id
                        )
                        .put(
                            "index",
                            sourceSegment.index
                        )
                        .put(
                            "fileName",
                            fileName
                        )
                        .put(
                            "sha256",
                            checksum(destination)
                        )
                        .apply {
                            prepared
                                .actualDurationMs
                                ?.let {
                                    put(
                                        "durationMs",
                                        it
                                    )
                                }
                        }
                )
            }

            val metadata = JSONObject()
                .put(
                    "schemaVersion",
                    SCHEMA_VERSION
                )
                .put(
                    "documentId",
                    manifest.documentId
                )
                .put(
                    "revisionId",
                    manifest.revisionId
                )
                .put(
                    "voiceId",
                    voiceId
                )
                .put(
                    "segments",
                    written
                )

            File(
                staging,
                "bundle.json"
            ).writeText(
                metadata.toString()
            )

            if (
                target.exists()
                && !target.deleteRecursively()
            ) {
                error(
                    "Could not replace the existing offline audio bundle."
                )
            }

            if (!staging.renameTo(target)) {
                staging.copyRecursively(
                    target = target,
                    overwrite = true
                )
                staging.deleteRecursively()
            }
        } finally {
            if (staging.exists()) {
                staging.deleteRecursively()
            }
        }
    }

    suspend fun remove(
        context: Context,
        accountIdentity: String,
        documentId: String,
        revisionId: String,
        voiceId: String
    ) = withContext(Dispatchers.IO) {
        bundleDirectory(
            context = context,
            accountIdentity = accountIdentity,
            documentId = documentId,
            revisionId = revisionId,
            voiceId = voiceId
        ).deleteRecursively()
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        root(context).deleteRecursively()
        Unit
    }

    private fun loadMetadata(
        directory: File
    ): JSONObject? {
        val file = File(
            directory,
            "bundle.json"
        )
        if (!file.isFile) {
            return null
        }

        return runCatching {
            JSONObject(
                file.readText()
            )
        }.getOrNull()
    }

    private fun bundleDirectory(
        context: Context,
        accountIdentity: String,
        documentId: String,
        revisionId: String,
        voiceId: String
    ): File =
        File(
            File(
                File(
                    File(
                        root(context),
                        storageKey(
                            accountIdentity
                        )
                    ),
                    storageKey(documentId)
                ),
                storageKey(revisionId)
            ),
            storageKey(voiceId)
        )

    private fun root(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun storageKey(
        value: String
    ): String = checksumBytes(
        value.toByteArray(
            Charsets.UTF_8
        )
    )

    private fun checksum(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance("SHA-256")
        file.inputStream()
            .buffered()
            .use { input ->
                val buffer =
                    ByteArray(
                        256 * 1024
                    )
                while (true) {
                    val count =
                        input.read(buffer)
                    if (count < 0) {
                        break
                    }
                    if (count > 0) {
                        digest.update(
                            buffer,
                            0,
                            count
                        )
                    }
                }
            }
        return digest.digest()
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }
    }

    private fun checksumBytes(
        bytes: ByteArray
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }
}
