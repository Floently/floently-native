package com.floently.read

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReadSourceHighlight(
    val id: String,
    val projectId: String,
    val revisionId: String,
    val sourceScalarStart: Int,
    val sourceScalarLength: Int,
    val quote: String,
    val prefixContext: String,
    val suffixContext: String,
    val createdAtMs: Long
)

object ReadSourceHighlightStore {
    private const val SCHEMA_VERSION = 1
    private const val DIRECTORY_NAME =
        "read-source-highlights-v1"

    suspend fun highlights(
        context: Context,
        accountIdentity: String,
        projectId: String,
        revisionId: String
    ): List<ReadSourceHighlight> =
        withContext(Dispatchers.IO) {
            load(
                context = context,
                accountIdentity =
                    accountIdentity,
                projectId = projectId
            )
                .filter {
                    it.revisionId
                        == revisionId
                }
                .sortedWith(
                    compareBy<
                        ReadSourceHighlight
                    > {
                        it.sourceScalarStart
                    }
                        .thenBy {
                            it.createdAtMs
                        }
                )
        }

    suspend fun add(
        context: Context,
        accountIdentity: String,
        projectId: String,
        revisionId: String,
        sourceText: String,
        sourceScalarStart: Int,
        sourceScalarLength: Int
    ): ReadSourceHighlight =
        withContext(Dispatchers.IO) {
            val scalarCount =
                ReadScalarOffsets
                    .scalarCount(
                        sourceText
                    )
            val start =
                sourceScalarStart
                    .coerceIn(
                        0,
                        scalarCount
                    )
            val end =
                (
                    start
                        + sourceScalarLength
                            .coerceAtLeast(
                                0
                            )
                    )
                    .coerceAtMost(
                        scalarCount
                    )
            require(end > start) {
                "The selected source range could not be highlighted."
            }

            val quote =
                scalarSubstring(
                    sourceText,
                    start,
                    end
                )
            val prefix =
                scalarSubstring(
                    sourceText,
                    (start - 32)
                        .coerceAtLeast(0),
                    start
                )
            val suffix =
                scalarSubstring(
                    sourceText,
                    end,
                    (end + 32)
                        .coerceAtMost(
                            scalarCount
                        )
                )

            val values =
                load(
                    context = context,
                    accountIdentity =
                        accountIdentity,
                    projectId =
                        projectId
                )
                    .filterNot {
                        it.revisionId
                            == revisionId
                            && it.sourceScalarStart
                                == start
                            && it.sourceScalarLength
                                == end - start
                    }
                    .toMutableList()

            val value =
                ReadSourceHighlight(
                    id =
                        UUID.randomUUID()
                            .toString(),
                    projectId =
                        projectId,
                    revisionId =
                        revisionId,
                    sourceScalarStart =
                        start,
                    sourceScalarLength =
                        end - start,
                    quote = quote,
                    prefixContext =
                        prefix,
                    suffixContext =
                        suffix,
                    createdAtMs =
                        System.currentTimeMillis()
                )
            values += value

            persist(
                context = context,
                accountIdentity =
                    accountIdentity,
                projectId =
                    projectId,
                highlights = values
            )

            value
        }

    suspend fun remove(
        context: Context,
        accountIdentity: String,
        projectId: String,
        id: String
    ) = withContext(Dispatchers.IO) {
        val values =
            load(
                context = context,
                accountIdentity =
                    accountIdentity,
                projectId = projectId
            )
                .filterNot {
                    it.id == id
                }

        persist(
            context = context,
            accountIdentity =
                accountIdentity,
            projectId = projectId,
            highlights = values
        )
    }

    suspend fun removeProject(
        context: Context,
        accountIdentity: String,
        projectId: String
    ) = withContext(Dispatchers.IO) {
        projectFile(
            context = context,
            accountIdentity =
                accountIdentity,
            projectId = projectId
        ).delete()
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        root(context)
            .deleteRecursively()
        Unit
    }

    private fun load(
        context: Context,
        accountIdentity: String,
        projectId: String
    ): List<ReadSourceHighlight> {
        val file =
            projectFile(
                context = context,
                accountIdentity =
                    accountIdentity,
                projectId = projectId
            )
        if (!file.isFile) {
            return emptyList()
        }

        val root =
            runCatching {
                JSONObject(
                    file.readText()
                )
            }.getOrNull()
                ?: return emptyList()

        if (
            root.optInt(
                "schemaVersion",
                0
            ) != SCHEMA_VERSION
        ) {
            return emptyList()
        }

        val array =
            root.optJSONArray(
                "highlights"
            ) ?: return emptyList()

        return buildList {
            for (
                index in 0 until array.length()
            ) {
                decode(
                    array.optJSONObject(
                        index
                    )
                )?.let(::add)
            }
        }
    }

    private fun persist(
        context: Context,
        accountIdentity: String,
        projectId: String,
        highlights:
            List<ReadSourceHighlight>
    ) {
        val target =
            projectFile(
                context = context,
                accountIdentity =
                    accountIdentity,
                projectId = projectId
            )

        if (highlights.isEmpty()) {
            target.delete()
            return
        }

        val directory =
            target.parentFile
                ?: error(
                    "Highlight storage path is unavailable."
                )
        check(
            directory.exists()
                || directory.mkdirs()
        ) {
            "Could not create highlight storage."
        }

        val root = JSONObject()
            .put(
                "schemaVersion",
                SCHEMA_VERSION
            )
            .put(
                "highlights",
                JSONArray().apply {
                    highlights.forEach {
                        put(encode(it))
                    }
                }
            )

        val temporary =
            File(
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
                    "Could not replace highlight storage."
                )
            }

            if (!temporary.renameTo(target)) {
                temporary.copyTo(
                    target,
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

    private fun encode(
        value: ReadSourceHighlight
    ): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put(
                "projectId",
                value.projectId
            )
            .put(
                "revisionId",
                value.revisionId
            )
            .put(
                "sourceScalarStart",
                value.sourceScalarStart
            )
            .put(
                "sourceScalarLength",
                value.sourceScalarLength
            )
            .put("quote", value.quote)
            .put(
                "prefixContext",
                value.prefixContext
            )
            .put(
                "suffixContext",
                value.suffixContext
            )
            .put(
                "createdAtMs",
                value.createdAtMs
            )

    private fun decode(
        value: JSONObject?
    ): ReadSourceHighlight? {
        value ?: return null

        val id =
            value.optString("id")
                .takeIf {
                    it.isNotBlank()
                }
                ?: return null
        val projectId =
            value.optString(
                "projectId"
            )
                .takeIf {
                    it.isNotBlank()
                }
                ?: return null
        val revisionId =
            value.optString(
                "revisionId"
            )
                .takeIf {
                    it.isNotBlank()
                }
                ?: return null

        val start =
            value.optInt(
                "sourceScalarStart",
                -1
            )
        val length =
            value.optInt(
                "sourceScalarLength",
                -1
            )
        if (start < 0 || length <= 0) {
            return null
        }

        return ReadSourceHighlight(
            id = id,
            projectId = projectId,
            revisionId =
                revisionId,
            sourceScalarStart =
                start,
            sourceScalarLength =
                length,
            quote =
                value.optString(
                    "quote"
                ),
            prefixContext =
                value.optString(
                    "prefixContext"
                ),
            suffixContext =
                value.optString(
                    "suffixContext"
                ),
            createdAtMs =
                value.optLong(
                    "createdAtMs",
                    0L
                )
        )
    }

    private fun scalarSubstring(
        text: String,
        startScalar: Int,
        endScalar: Int
    ): String {
        val start =
            ReadScalarOffsets
                .utf16Offset(
                    text = text,
                    scalarOffset =
                        startScalar
                )
        val end =
            ReadScalarOffsets
                .utf16Offset(
                    text = text,
                    scalarOffset =
                        endScalar
                )

        return text.substring(
            start,
            end
        )
    }

    private fun root(
        context: Context
    ): File =
        File(
            context.applicationContext
                .filesDir,
            DIRECTORY_NAME
        )

    private fun projectFile(
        context: Context,
        accountIdentity: String,
        projectId: String
    ): File =
        File(
            File(
                root(context),
                storageKey(
                    accountIdentity
                )
            ),
            storageKey(projectId)
                + ".json"
        )

    private fun storageKey(
        value: String
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256"
            )
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") {
                byte ->
                "%02x".format(
                    byte.toInt()
                        and 0xff
                )
            }
}
