import Foundation
import FloentlyReadCore

enum ReadCoreNativeError: LocalizedError {
    case manifestBuildFailed
    case invalidUTF8Result
    case epubExtractionFailed

    var errorDescription: String? {
        switch self {
        case .manifestBuildFailed:
            return "The shared Read Core could not build this document."
        case .invalidUTF8Result:
            return "The shared Read Core returned invalid document data."
        case .epubExtractionFailed:
            return "The shared Read Core could not open this EPUB."
        }
    }
}

enum ReadCoreNative {
    static func buildManifest(
        documentId: String,
        revisionId: String,
        title: String,
        language: String,
        text: String,
        maxScalars: Int = 1_600
    ) throws -> ReadingManifestV1 {
        let pointer = documentId.withCString { documentIdPointer in
            revisionId.withCString { revisionIdPointer in
                title.withCString { titlePointer in
                    language.withCString { languagePointer in
                        text.withCString { textPointer in
                            floently_read_build_manifest_json(
                                documentIdPointer,
                                revisionIdPointer,
                                titlePointer,
                                languagePointer,
                                textPointer,
                                max(600, maxScalars)
                            )
                        }
                    }
                }
            }
        }

        guard let pointer else {
            throw ReadCoreNativeError.manifestBuildFailed
        }
        defer {
            floently_read_string_free(pointer)
        }

        guard let json = String(
            validatingUTF8: pointer
        ) else {
            throw ReadCoreNativeError.invalidUTF8Result
        }

        return try ReadingManifestV1.decode(
            data: Data(json.utf8)
        )
    }

    static func extractEPUB(
        inputURL: URL,
        outputDirectory: URL
    ) throws -> ReadEpubPackage {
        let pointer =
            inputURL.path.withCString {
                inputPointer in
                outputDirectory.path
                    .withCString {
                        outputPointer in
                        floently_read_extract_epub_json(
                            inputPointer,
                            outputPointer
                        )
                    }
            }

        guard let pointer else {
            throw ReadCoreNativeError
                .epubExtractionFailed
        }
        defer {
            floently_read_string_free(
                pointer
            )
        }

        guard
            let json = String(
                validatingUTF8: pointer
            )
        else {
            throw ReadCoreNativeError
                .invalidUTF8Result
        }

        let package =
            try JSONDecoder().decode(
                ReadEpubPackage.self,
                from: Data(json.utf8)
            )
        guard
            package.schemaVersion == 1,
            !package.spine.isEmpty
        else {
            throw ReadCoreNativeError
                .epubExtractionFailed
        }

        return package
    }
}
