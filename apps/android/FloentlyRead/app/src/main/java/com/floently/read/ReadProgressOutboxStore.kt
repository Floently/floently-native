package com.floently.read

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReadPendingProgressWrite(
    val nonce: String,
    val projectId: String,
    val currentSegmentIndex: Int,
    val currentCharacterOffset: Int,
    val progressPercent: Double,
    val voiceId: String?,
    val playbackRate: Double?,
    val baseServerUpdatedAt: String?,
    val createdAtMs: Long
)

object ReadProgressOutboxStore {
    private const val SCHEMA_VERSION = 1
    private const val DIRECTORY_NAME =
        "read-progress-outbox"

    suspend fun save(
        context: Context,
        accountIdentity: String,
        write: ReadPendingProgressWrite
    ) = withContext(Dispatchers.IO) {
        val directory = accountDirectory(
            context = context,
            accountIdentity = accountIdentity
        )
        check(
            directory.exists()
                || directory.mkdirs()
        ) {
            "Could not create Read progress outbox storage."
        }

        val root = JSONObject()
            .put(
                "schemaVersion",
                SCHEMA_VERSION
            )
            .put(
                "write",
                encode(write)
            )

        val target = writeFile(
            context = context,
            accountIdentity = accountIdentity,
            projectId = write.projectId
        )
        val temporary = File(
            directory,
            target.name + ".tmp"
        )

        try {
            temporary.writeText(
                root.toString()
            )
            if (
                target.exists()
                && !target.delete()
            ) {
                error(
                    "Could not replace pending Read progress."
                )
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(
                    target = target,
                    overwrite = true
                )
                temporary.delete()
            }
        } finally {
            if (temporary.exists()) {
                temporary.delete()
            }
        }
    }

    suspend fun entries(
        context: Context,
        accountIdentity: String
    ): List<ReadPendingProgressWrite> =
        withContext(Dispatchers.IO) {
            val directory = accountDirectory(
                context = context,
                accountIdentity = accountIdentity
            )
            directory.listFiles()
                ?.asSequence()
                ?.filter {
                    it.isFile
                        && it.extension
                            .equals(
                                "json",
                                ignoreCase = true
                            )
                }
                ?.mapNotNull { file ->
                    val root = runCatching {
                        JSONObject(
                            file.readText()
                        )
                    }.getOrNull()
                        ?: return@mapNotNull null

                    if (
                        root.optInt(
                            "schemaVersion",
                            0
                        ) != SCHEMA_VERSION
                    ) {
                        return@mapNotNull null
                    }

                    decode(
                        root.optJSONObject(
                            "write"
                        )
                    )
                }
                ?.sortedBy {
                    it.createdAtMs
                }
                ?.toList()
                .orEmpty()
        }

    suspend fun currentWrite(
        context: Context,
        accountIdentity: String,
        projectId: String
    ): ReadPendingProgressWrite? =
        withContext(Dispatchers.IO) {
            val file = writeFile(
                context = context,
                accountIdentity = accountIdentity,
                projectId = projectId
            )
            if (!file.isFile) {
                return@withContext null
            }

            val root = runCatching {
                JSONObject(
                    file.readText()
                )
            }.getOrNull()
                ?: return@withContext null

            if (
                root.optInt(
                    "schemaVersion",
                    0
                ) != SCHEMA_VERSION
            ) {
                return@withContext null
            }

            decode(
                root.optJSONObject("write")
            )
        }

    suspend fun removeIfMatches(
        context: Context,
        accountIdentity: String,
        projectId: String,
        nonce: String
    ) = withContext(Dispatchers.IO) {
        val file = writeFile(
            context = context,
            accountIdentity = accountIdentity,
            projectId = projectId
        )
        if (!file.isFile) {
            return@withContext
        }

        val root = runCatching {
            JSONObject(
                file.readText()
            )
        }.getOrNull()
            ?: return@withContext
        val write = decode(
            root.optJSONObject("write")
        ) ?: return@withContext

        if (write.nonce == nonce) {
            file.delete()
        }
    }

    suspend fun removeProject(
        context: Context,
        accountIdentity: String,
        projectId: String
    ) = withContext(Dispatchers.IO) {
        writeFile(
            context = context,
            accountIdentity = accountIdentity,
            projectId = projectId
        ).delete()
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        File(
            context.applicationContext.filesDir,
            DIRECTORY_NAME
        ).deleteRecursively()
        Unit
    }

    private fun encode(
        write: ReadPendingProgressWrite
    ): JSONObject =
        JSONObject()
            .put("nonce", write.nonce)
            .put("projectId", write.projectId)
            .put(
                "currentSegmentIndex",
                write.currentSegmentIndex
            )
            .put(
                "currentCharacterOffset",
                write.currentCharacterOffset
            )
            .put(
                "progressPercent",
                write.progressPercent
            )
            .apply {
                write.voiceId?.let {
                    put("voiceId", it)
                }
                write.playbackRate?.let {
                    put("playbackRate", it)
                }
                write.baseServerUpdatedAt
                    ?.let {
                        put(
                            "baseServerUpdatedAt",
                            it
                        )
                    }
            }
            .put(
                "createdAtMs",
                write.createdAtMs
            )

    private fun decode(
        value: JSONObject?
    ): ReadPendingProgressWrite? {
        value ?: return null
        val nonce =
            value.optString("nonce")
                .takeIf {
                    it.isNotBlank()
                }
                ?: return null
        val projectId =
            value.optString("projectId")
                .takeIf {
                    it.isNotBlank()
                }
                ?: return null

        return ReadPendingProgressWrite(
            nonce = nonce,
            projectId = projectId,
            currentSegmentIndex =
                value.optInt(
                    "currentSegmentIndex",
                    0
                ).coerceAtLeast(0),
            currentCharacterOffset =
                value.optInt(
                    "currentCharacterOffset",
                    0
                ).coerceAtLeast(0),
            progressPercent =
                value.optDouble(
                    "progressPercent",
                    0.0
                ).coerceIn(0.0, 100.0),
            voiceId =
                value.optString("voiceId")
                    .takeIf {
                        it.isNotBlank()
                    },
            playbackRate =
                if (
                    value.has("playbackRate")
                    && !value.isNull(
                        "playbackRate"
                    )
                ) {
                    value.optDouble(
                        "playbackRate"
                    ).takeIf {
                        it.isFinite()
                    }
                } else {
                    null
                },
            baseServerUpdatedAt =
                value.optString(
                    "baseServerUpdatedAt"
                ).takeIf {
                    it.isNotBlank()
                },
            createdAtMs =
                value.optLong(
                    "createdAtMs",
                    0L
                )
        )
    }

    private fun accountDirectory(
        context: Context,
        accountIdentity: String
    ): File = File(
        File(
            context.applicationContext.filesDir,
            DIRECTORY_NAME
        ),
        storageKey(accountIdentity)
    )

    private fun writeFile(
        context: Context,
        accountIdentity: String,
        projectId: String
    ): File = File(
        accountDirectory(
            context = context,
            accountIdentity = accountIdentity
        ),
        storageKey(projectId)
            + ".json"
    )

    private fun storageKey(
        value: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
}
