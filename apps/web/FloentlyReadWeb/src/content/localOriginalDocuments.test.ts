import { describe, expect, it } from "vitest";
import {
  originalRecordForStorage,
  sameOriginalIdentity,
  storageBlobForFile,
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

describe("local original File persistence", () => {
  it("materializes a plain Blob snapshot before IndexedDB storage", async () => {
    const file = new File(
      [new Uint8Array([10, 20, 30])],
      "paper.pdf",
      {
        type: "application/pdf",
        lastModified: 123,
      },
    );

    const blob = storageBlobForFile(file);

    expect(blob).toBeInstanceOf(Blob);
    expect(blob).not.toBeInstanceOf(File);
    expect(blob.type).toBe("application/pdf");
    expect(blob.size).toBe(file.size);
    expect(
      Array.from(new Uint8Array(await blob.arrayBuffer())),
    ).toEqual([10, 20, 30]);
  });
});

describe("local original document IndexedDB storage", () => {
  it("omits a pending null content hash so WebKit does not index an invalid key", () => {
    const pending = record({ contentHash: null });
    const stored = originalRecordForStorage(pending);

    expect("contentHash" in stored).toBe(false);
    expect(stored).toMatchObject({
      id: pending.id,
      quickSignature: pending.quickSignature,
      projectId: pending.projectId,
    });
  });

  it("keeps a completed content hash available to the IndexedDB hash index", () => {
    const completed = record({ contentHash: "sha256-ready" });
    const stored = originalRecordForStorage(completed);

    expect(stored.contentHash).toBe("sha256-ready");
  });
});

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
