package com.floently.read

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReadOriginalDocumentStore {
    private const val DIRECTORY_NAME =
        "read-original-documents"

    suspend fun savePdf(
        context: Context,
        projectId: String,
        uri: Uri,
        resolver: ContentResolver
    ): File? = withContext(Dispatchers.IO) {
        val mime = resolver.getType(uri)
            ?.lowercase()
            .orEmpty()
        val looksLikePdf =
            mime == "application/pdf"
                || uri.lastPathSegment
                    ?.lowercase()
                    ?.endsWith(".pdf") == true

        if (!looksLikePdf) {
            return@withContext null
        }

        val directory = directory(context)
        check(
            directory.exists() || directory.mkdirs()
        ) {
            "Could not create local original-document storage."
        }

        val target = pdfFilePath(
            context = context,
            projectId = projectId
        )
        val temporary = File(
            directory,
            storageKey(projectId) +
                "-" + UUID.randomUUID().toString() +
                ".tmp"
        )

        try {
            resolver.openInputStream(uri)
                ?.use { input ->
                    temporary.outputStream()
                        .buffered(256 * 1024)
                        .use { output ->
                            input.copyTo(
                                output,
                                256 * 1024
                            )
                        }
                }
                ?: error(
                    "The original PDF could not be opened."
                )

            if (target.exists() && !target.delete()) {
                error(
                    "The previous local PDF copy could not be replaced."
                )
            }

            if (!temporary.renameTo(target)) {
                temporary.copyTo(
                    target = target,
                    overwrite = true
                )
                temporary.delete()
            }

            target
        } finally {
            if (temporary.exists()) {
                temporary.delete()
            }
        }
    }

    fun pdfFile(
        context: Context,
        projectId: String
    ): File? {
        val value = pdfFilePath(
            context = context,
            projectId = projectId
        )
        return value.takeIf {
            it.isFile && it.length() > 0L
        }
    }

    suspend fun delete(
        context: Context,
        projectId: String
    ) = withContext(Dispatchers.IO) {
        pdfFilePath(
            context = context,
            projectId = projectId
        ).delete()
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        directory(context).deleteRecursively()
        Unit
    }

    private fun directory(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun pdfFilePath(
        context: Context,
        projectId: String
    ): File = File(
        directory(context),
        storageKey(projectId) + ".pdf"
    )

    private fun storageKey(
        projectId: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(projectId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
}
