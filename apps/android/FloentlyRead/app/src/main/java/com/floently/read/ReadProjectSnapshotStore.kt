package com.floently.read

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ReadProjectSnapshotStore {
    private const val SCHEMA_VERSION = 1
    private const val DIRECTORY_NAME =
        "read-project-snapshots"

    suspend fun load(
        context: Context,
        accessToken: String
    ): List<ReadContentProject> =
        withContext(Dispatchers.IO) {
            val file = snapshotFile(
                context = context,
                accessToken = accessToken
            )
            if (!file.isFile) {
                return@withContext emptyList()
            }

            val root = runCatching {
                JSONObject(file.readText())
            }.getOrNull()
                ?: return@withContext emptyList()

            if (
                root.optInt(
                    "schemaVersion",
                    0
                ) != SCHEMA_VERSION
            ) {
                return@withContext emptyList()
            }

            val values =
                root.optJSONArray("projects")
                    ?: return@withContext emptyList()

            buildList {
                for (index in 0 until values.length()) {
                    decodeProject(
                        values.optJSONObject(index)
                    )?.let(::add)
                }
            }
        }

    suspend fun save(
        context: Context,
        accessToken: String,
        projects: List<ReadContentProject>
    ) = withContext(Dispatchers.IO) {
        val directory = directory(context)
        check(
            directory.exists() || directory.mkdirs()
        ) {
            "Could not create local Read project storage."
        }

        val root = JSONObject()
            .put(
                "schemaVersion",
                SCHEMA_VERSION
            )
            .put(
                "projects",
                JSONArray().apply {
                    projects.forEach {
                        put(encodeProject(it))
                    }
                }
            )

        val target = snapshotFile(
            context = context,
            accessToken = accessToken
        )
        val temporary = File(
            directory,
            target.name + ".tmp"
        )

        try {
            temporary.writeText(root.toString())

            if (target.exists() && !target.delete()) {
                error(
                    "Could not replace local Read project storage."
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

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        directory(context).deleteRecursively()
        Unit
    }

    private fun encodeProject(
        project: ReadContentProject
    ): JSONObject =
        JSONObject()
            .put("id", project.id)
            .put("title", project.title)
            .put("kind", project.kind)
            .put("status", project.status)
            .put("sourceType", project.sourceType)
            .apply {
                project.sourceUrl?.let {
                    put("sourceUrl", it)
                }
                project.language?.let {
                    put("language", it)
                }
            }
            .put("textHash", project.textHash)
            .put("wordCount", project.wordCount)
            .put(
                "characterCount",
                project.characterCount
            )
            .put("createdAt", project.createdAt)
            .put("updatedAt", project.updatedAt)
            .apply {
                project.lastOpenedAt?.let {
                    put("lastOpenedAt", it)
                }
                project.rawText?.let {
                    put("rawText", it)
                }
                project.progress?.let {
                    put(
                        "progress",
                        encodeProgress(it)
                    )
                }
            }

    private fun encodeProgress(
        progress: ReadProjectProgress
    ): JSONObject =
        JSONObject()
            .put(
                "projectId",
                progress.projectId
            )
            .put(
                "currentSegmentIndex",
                progress.currentSegmentIndex
            )
            .put(
                "currentCharacterOffset",
                progress.currentCharacterOffset
            )
            .put(
                "progressPercent",
                progress.progressPercent
            )
            .apply {
                progress.voiceId?.let {
                    put("voiceId", it)
                }
                progress.playbackRate?.let {
                    put("playbackRate", it)
                }
            }
            .put(
                "updatedAt",
                progress.updatedAt
            )

    private fun decodeProject(
        value: JSONObject?
    ): ReadContentProject? {
        value ?: return null

        val id = value.optString("id")
            .trim()
        val title = value.optString("title")
            .trim()
        if (id.isEmpty() || title.isEmpty()) {
            return null
        }

        return ReadContentProject(
            id = id,
            title = title,
            kind = value.optString("kind"),
            status = value.optString("status"),
            sourceType =
                value.optString("sourceType"),
            sourceUrl =
                value.optString("sourceUrl")
                    .takeIf {
                        it.isNotBlank()
                    },
            language =
                value.optString("language")
                    .takeIf {
                        it.isNotBlank()
                    },
            textHash =
                value.optString("textHash"),
            wordCount =
                value.optInt("wordCount", 0),
            characterCount =
                value.optInt(
                    "characterCount",
                    0
                ),
            createdAt =
                value.optString("createdAt"),
            updatedAt =
                value.optString("updatedAt"),
            lastOpenedAt =
                value.optString("lastOpenedAt")
                    .takeIf {
                        it.isNotBlank()
                    },
            rawText =
                value.optString("rawText")
                    .takeIf {
                        it.isNotBlank()
                    },
            progress = decodeProgress(
                value.optJSONObject("progress")
            )
        )
    }

    private fun decodeProgress(
        value: JSONObject?
    ): ReadProjectProgress? {
        value ?: return null

        return ReadProjectProgress(
            projectId =
                value.optString("projectId"),
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
            updatedAt =
                value.optString("updatedAt")
        )
    }

    private fun directory(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun snapshotFile(
        context: Context,
        accessToken: String
    ): File = File(
        directory(context),
        storageKey(accessToken) + ".json"
    )

    private fun storageKey(
        accessToken: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                accessToken.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
}
