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
    ): File? = saveOriginal(
        context = context,
        projectId = projectId,
        uri = uri,
        resolver = resolver,
        fileExtension = "pdf",
        mimeTypes = setOf(
            "application/pdf"
        )
    )

    suspend fun saveEpub(
        context: Context,
        projectId: String,
        uri: Uri,
        resolver: ContentResolver
    ): File? = saveOriginal(
        context = context,
        projectId = projectId,
        uri = uri,
        resolver = resolver,
        fileExtension = "epub",
        mimeTypes = setOf(
            "application/epub+zip"
        )
    )

    fun pdfFile(
        context: Context,
        projectId: String
    ): File? = existingOriginalFile(
        context = context,
        projectId = projectId,
        fileExtension = "pdf"
    )

    fun epubFile(
        context: Context,
        projectId: String
    ): File? = existingOriginalFile(
        context = context,
        projectId = projectId,
        fileExtension = "epub"
    )

    suspend fun delete(
        context: Context,
        projectId: String
    ) = withContext(Dispatchers.IO) {
        listOf(
            "pdf",
            "epub"
        ).forEach { fileExtension ->
            originalFilePath(
                context = context,
                projectId = projectId,
                fileExtension =
                    fileExtension
            ).delete()
        }
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        directory(context)
            .deleteRecursively()
        Unit
    }

    private suspend fun saveOriginal(
        context: Context,
        projectId: String,
        uri: Uri,
        resolver: ContentResolver,
        fileExtension: String,
        mimeTypes: Set<String>
    ): File? = withContext(Dispatchers.IO) {
        val mime = resolver.getType(uri)
            ?.lowercase()
            .orEmpty()
        val looksLikeType =
            mime in mimeTypes
                || uri.lastPathSegment
                    ?.lowercase()
                    ?.endsWith(
                        "."
                            + fileExtension
                    ) == true

        if (!looksLikeType) {
            return@withContext null
        }

        val directory = directory(context)
        check(
            directory.exists()
                || directory.mkdirs()
        ) {
            "Could not create local original-document storage."
        }

        val target = originalFilePath(
            context = context,
            projectId = projectId,
            fileExtension = fileExtension
        )
        val temporary = File(
            directory,
            storageKey(projectId)
                + "-"
                + UUID.randomUUID()
                    .toString()
                + ".tmp"
        )

        try {
            resolver.openInputStream(uri)
                ?.use { input ->
                    temporary.outputStream()
                        .buffered(
                            256 * 1024
                        )
                        .use { output ->
                            input.copyTo(
                                output,
                                256 * 1024
                            )
                        }
                }
                ?: error(
                    "The original "
                        + fileExtension
                            .uppercase()
                        + " could not be opened."
                )

            if (
                target.exists()
                && !target.delete()
            ) {
                error(
                    "The previous local original document could not be replaced."
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

    private fun existingOriginalFile(
        context: Context,
        projectId: String,
        fileExtension: String
    ): File? {
        val value = originalFilePath(
            context = context,
            projectId = projectId,
            fileExtension =
                fileExtension
        )

        return value.takeIf {
            it.isFile
                && it.length() > 0L
        }
    }

    private fun directory(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun originalFilePath(
        context: Context,
        projectId: String,
        fileExtension: String
    ): File = File(
        directory(context),
        storageKey(projectId)
            + "."
            + fileExtension
    )

    private fun storageKey(
        projectId: String
    ): String =
        MessageDigest.getInstance(
            "SHA-256"
        )
            .digest(
                projectId.toByteArray(
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
