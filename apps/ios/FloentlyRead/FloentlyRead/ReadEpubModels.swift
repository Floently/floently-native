import Foundation

struct ReadEpubSpineItem:
    Codable,
    Equatable,
    Sendable,
    Identifiable
{
    let index: Int
    let idref: String
    let path: String
    let mediaType: String

    var id: Int { index }
}

struct ReadEpubPackage:
    Codable,
    Equatable,
    Sendable
{
    let schemaVersion: Int
    let title: String
    let packagePath: String
    let spine: [ReadEpubSpineItem]
}

struct ReadLocalEpubPackage:
    Equatable,
    Sendable
{
    let metadata: ReadEpubPackage
    let rootDirectory: URL

    func chapterURL(
        at index: Int
    ) -> URL? {
        guard
            metadata.spine.indices
                .contains(index)
        else {
            return nil
        }

        let relative =
            metadata.spine[index].path
        guard
            !relative.hasPrefix("/"),
            !relative
                .split(separator: "/")
                .contains("..")
        else {
            return nil
        }

        let value =
            rootDirectory.appending(
                path: relative
            )

        return FileManager.default
            .fileExists(
                atPath: value.path
            )
            ? value
            : nil
    }
}
