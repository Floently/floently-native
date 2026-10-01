import Foundation
import FloentlyReadCore

enum ReadCoreNativeError: LocalizedError {
    case manifestBuildFailed
    case invalidUTF8Result

    var errorDescription: String? {
        switch self {
        case .manifestBuildFailed:
            return "The shared Read Core could not build this document."
        case .invalidUTF8Result:
            return "The shared Read Core returned invalid document data."
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
}
