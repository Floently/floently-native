package com.floently.read

import java.security.MessageDigest

data class ReadBrowserReadingSource(
    val kind: Kind,
    val url: String,
    val title: String,
    val language: String,
    val text: String
) {
    enum class Kind(val wireValue: String) {
        PAGE("page"),
        SELECTION("selection")
    }
}

object ReadBrowserNativeReading {
    fun manifest(
        source: ReadBrowserReadingSource
    ): ReadingManifestV1 {
        val language = normalizedLanguage(
            source.language
        )

        return ReadCoreNative.buildManifest(
            documentId = documentId(source),
            revisionId = revisionId(source),
            title = source.title,
            language = language,
            text = source.text
        )
    }

    fun defaultVoiceId(
        language: String
    ): String {
        val primary = normalizedLanguage(language)
            .lowercase()
            .substringBefore('-')

        return if (primary == "fi") {
            "azure:fi-FI-SelmaNeural"
        } else {
            "google:en-US-Neural2-C"
        }
    }

    private fun documentId(
        source: ReadBrowserReadingSource
    ): String {
        val value = listOf(
            "web",
            source.kind.wireValue,
            source.url
        ).joinToString("\u001f")

        return "web:" + source.kind.wireValue + ":" + sha256(value)
    }

    private fun revisionId(
        source: ReadBrowserReadingSource
    ): String = "sha256:" + sha256(source.text)

    private fun normalizedLanguage(
        value: String
    ): String = value.trim().ifBlank { "auto" }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
