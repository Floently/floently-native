package com.floently.read

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReadEpubSourceStore {
    private const val DIRECTORY_NAME =
        "read-epub-sources-v1"
    private const val MARKER_NAME =
        "floently-package.json"

    suspend fun packageFor(
        context: Context,
        projectId: String,
        revisionId: String,
        sourceFile: File
    ): ReadLocalEpubPackage =
        withContext(Dispatchers.IO) {
            val directory =
                packageDirectory(
                    context = context,
                    projectId = projectId,
                    revisionId = revisionId
                )

            loadCached(directory)
                ?.let {
                    return@withContext it
                }

            directory.deleteRecursively()
            check(
                directory.parentFile
                    ?.let {
                        it.exists()
                            || it.mkdirs()
                    } == true
            ) {
                "Could not create EPUB source storage."
            }

            val metadata =
                try {
                    ReadCoreNative.extractEpub(
                        inputFile = sourceFile,
                        outputDirectory =
                            directory
                    )
                } catch (error: Throwable) {
                    directory.deleteRecursively()
                    throw error
                }

            require(
                validate(
                    metadata,
                    directory
                )
            ) {
                directory.deleteRecursively()
                .let {
                    "The EPUB package could not be validated."
                }
            }

            File(
                directory,
                MARKER_NAME
            ).writeText(
                org.json.JSONObject()
                    .put(
                        "schemaVersion",
                        metadata.schemaVersion
                    )
                    .put(
                        "title",
                        metadata.title
                    )
                    .put(
                        "packagePath",
                        metadata.packagePath
                    )
                    .put(
                        "spine",
                        org.json.JSONArray().apply {
                            metadata.spine.forEach {
                                item ->
                                put(
                                    org.json.JSONObject()
                                        .put(
                                            "index",
                                            item.index
                                        )
                                        .put(
                                            "idref",
                                            item.idref
                                        )
                                        .put(
                                            "path",
                                            item.path
                                        )
                                        .put(
                                            "mediaType",
                                            item.mediaType
                                        )
                                )
                            }
                        }
                    )
                    .toString()
            )

            ReadLocalEpubPackage(
                metadata = metadata,
                rootDirectory = directory
            )
        }

    suspend fun delete(
        context: Context,
        projectId: String
    ) = withContext(Dispatchers.IO) {
        projectDirectory(
            context = context,
            projectId = projectId
        ).deleteRecursively()
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        root(context)
            .deleteRecursively()
        Unit
    }

    private fun loadCached(
        directory: File
    ): ReadLocalEpubPackage? {
        val marker =
            File(
                directory,
                MARKER_NAME
            )
        if (!marker.isFile) {
            return null
        }

        val metadata =
            runCatching {
                ReadEpubPackage.decode(
                    marker.readText()
                )
            }.getOrNull()
                ?: return null

        if (
            metadata.schemaVersion != 1
            || !validate(
                metadata,
                directory
            )
        ) {
            return null
        }

        return ReadLocalEpubPackage(
            metadata = metadata,
            rootDirectory = directory
        )
    }

    private fun validate(
        packageValue: ReadEpubPackage,
        directory: File
    ): Boolean {
        if (packageValue.spine.isEmpty()) {
            return false
        }

        return packageValue.spine.all {
            item ->
            if (
                item.path.startsWith("/")
                || item.path.split("/")
                    .contains("..")
            ) {
                return@all false
            }

            File(
                directory,
                item.path
            ).isFile
        }
    }

    private fun root(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun projectDirectory(
        context: Context,
        projectId: String
    ): File = File(
        root(context),
        storageKey(projectId)
    )

    private fun packageDirectory(
        context: Context,
        projectId: String,
        revisionId: String
    ): File = File(
        projectDirectory(
            context = context,
            projectId = projectId
        ),
        storageKey(revisionId)
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
            .joinToString("") {
                byte ->
                "%02x".format(
                    byte.toInt()
                        and 0xff
                )
            }
}
