import { describe, expect, it } from "vitest";
import {
  sameOriginalIdentity,
  withCompletedContentHash,
  type LocalOriginalDocumentRecord,
} from "./localOriginalDocuments";

function record(
  patch: Partial<LocalOriginalDocumentRecord> = {},
): LocalOriginalDocumentRecord {
  return {
    id: "local-1",
    blob: new Blob(["pdf"], { type: "application/pdf" }),
    name: "paper.pdf",
    type: "application/pdf",
    size: 3,
    lastModified: 1,
    quickSignature: "sample:quick",
    contentHash: null,
    projectId: "project-123",
    createdAt: 10,
    updatedAt: 20,
    ...patch,
  };
}

describe("local original document hash completion", () => {
  it("preserves a project link attached while hashing was in flight", () => {
    const latest = record({
      projectId: "project-linked-after-handoff",
      updatedAt: 200,
    });

    const completed = withCompletedContentHash(
      latest,
      "full-sha256",
      300,
    );

    expect(completed).toMatchObject({
      id: "local-1",
      projectId: "project-linked-after-handoff",
      contentHash: "full-sha256",
      updatedAt: 300,
    });
  });

  it("can inherit an existing semantic project when a full hash matches", () => {
    const completed = withCompletedContentHash(
      record({ projectId: null }),
      "same-content",
      400,
      "project-from-existing-copy",
    );

    expect(completed.projectId).toBe("project-from-existing-copy");
    expect(completed.contentHash).toBe("same-content");
  });

  it("uses full hash first and quick signature before hashing", () => {
    expect(
      sameOriginalIdentity(
        record({ contentHash: "full", quickSignature: "a" }),
        record({ id: "other", contentHash: "full", quickSignature: "b" }),
      ),
    ).toBe(true);

    expect(
      sameOriginalIdentity(
        record({ contentHash: null, quickSignature: "quick" }),
        record({ id: "other", contentHash: null, quickSignature: "quick" }),
      ),
    ).toBe(true);

    expect(
      sameOriginalIdentity(
        record({ contentHash: "left", quickSignature: "same" }),
        record({ id: "other", contentHash: "right", quickSignature: "same" }),
      ),
    ).toBe(false);
  });

  it("does not mutate the current record", () => {
    const current = record();
    const completed = withCompletedContentHash(
      current,
      "hash",
      30,
    );

    expect(current.contentHash).toBeNull();
    expect(completed).not.toBe(current);
  });
});
