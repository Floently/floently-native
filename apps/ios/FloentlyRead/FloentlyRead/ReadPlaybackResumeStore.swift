import Foundation

struct ReadPlaybackResumeSnapshot: Codable, Equatable {
    let documentId: String
    let revisionId: String
    let logicalTime: TimeInterval
    let playbackRate: Float
    let updatedAt: Date
}

@MainActor
final class ReadPlaybackResumeStore {
    private let defaults: UserDefaults
    private let prefix = "floently.read.resume."

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load(
        documentId: String,
        revisionId: String
    ) -> ReadPlaybackResumeSnapshot? {
        guard
            let data = defaults.data(
                forKey: key(
                    documentId: documentId,
                    revisionId: revisionId
                )
            ),
            let snapshot = try? JSONDecoder().decode(
                ReadPlaybackResumeSnapshot.self,
                from: data
            ),
            snapshot.documentId == documentId,
            snapshot.revisionId == revisionId,
            snapshot.logicalTime.isFinite,
            snapshot.logicalTime >= 0,
            snapshot.playbackRate.isFinite
        else {
            return nil
        }

        return snapshot
    }

    func save(_ snapshot: ReadPlaybackResumeSnapshot) {
        guard
            snapshot.logicalTime.isFinite,
            snapshot.logicalTime >= 0,
            snapshot.playbackRate.isFinite
        else {
            return
        }

        guard let data = try? JSONEncoder().encode(snapshot) else {
            return
        }

        defaults.set(
            data,
            forKey: key(
                documentId: snapshot.documentId,
                revisionId: snapshot.revisionId
            )
        )
    }

    func remove(
        documentId: String,
        revisionId: String
    ) {
        defaults.removeObject(
            forKey: key(
                documentId: documentId,
                revisionId: revisionId
            )
        )
    }

    private func key(
        documentId: String,
        revisionId: String
    ) -> String {
        prefix + documentId + "::" + revisionId
    }
}
