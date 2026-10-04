package com.floently.read

import org.json.JSONObject

data class ReadSourceAnchorResolution(
    val scalarStart: Int,
    val scalarLength: Int
)

class ReadCoreNative private constructor() {
    companion object {
        init {
            System.loadLibrary(
                "floently_read_core_native"
            )
        }

        @JvmStatic
        private external fun nativeExtractEpubJson(
            inputPath: String,
            outputDirectory: String
        ): String

        @JvmStatic
        private external fun nativeResolveSourceAnchorJson(
            sourceText: String,
            quote: String,
            prefixContext: String,
            suffixContext: String
        ): String

        @JvmStatic
        private external fun nativeBuildManifestJson(
            documentId: String,
            revisionId: String,
            title: String,
            language: String,
            text: String,
            maxScalars: Int
        ): String

        fun buildManifest(
            documentId: String,
            revisionId: String,
            title: String,
            language: String,
            text: String,
            maxScalars: Int = 1_600
        ): ReadingManifestV1 {
            val json = nativeBuildManifestJson(
                documentId = documentId,
                revisionId = revisionId,
                title = title,
                language = language,
                text = text,
                maxScalars = maxScalars.coerceAtLeast(600)
            )

            return ReadingManifestV1Codec.decode(json)
        }

        fun resolveSourceAnchor(
            sourceText: String,
            quote: String,
            prefixContext: String,
            suffixContext: String
        ): ReadSourceAnchorResolution? {
            val json =
                nativeResolveSourceAnchorJson(
                    sourceText = sourceText,
                    quote = quote,
                    prefixContext =
                        prefixContext,
                    suffixContext =
                        suffixContext
                )

            if (json == "null") {
                return null
            }

            val value = JSONObject(json)
            val scalarStart =
                value.optInt(
                    "scalarStart",
                    -1
                )
            val scalarLength =
                value.optInt(
                    "scalarLength",
                    -1
                )

            if (
                scalarStart < 0
                || scalarLength <= 0
            ) {
                return null
            }

            return ReadSourceAnchorResolution(
                scalarStart = scalarStart,
                scalarLength = scalarLength
            )
        }

        fun extractEpub(
            inputFile: java.io.File,
            outputDirectory: java.io.File
        ): ReadEpubPackage {
            require(inputFile.isFile) {
                "EPUB input file is missing."
            }

            val json =
                nativeExtractEpubJson(
                    inputPath =
                        inputFile.absolutePath,
                    outputDirectory =
                        outputDirectory
                            .absolutePath
                )

            return ReadEpubPackage
                .decode(json)
        }
    }
}
