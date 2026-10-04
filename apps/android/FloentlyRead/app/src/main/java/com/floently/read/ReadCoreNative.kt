package com.floently.read

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
