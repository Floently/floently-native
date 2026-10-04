import { describe, expect, it } from "vitest";
import {
  localOriginalFromStorage,
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
  it("stores raw bytes and omits a pending null hash", async () => {
    const pending = record({ contentHash: null });
    const stored = await originalRecordForStorage(pending);

    expect("blob" in stored).toBe(false);
    expect("contentHash" in stored).toBe(false);
    expect(stored.bytes).toBeInstanceOf(ArrayBuffer);
    expect(
      new TextDecoder().decode(stored.bytes),
    ).toBe("pdf");
    expect(stored).toMatchObject({
      id: pending.id,
      quickSignature: pending.quickSignature,
      projectId: pending.projectId,
    });
  });

  it("keeps a completed content hash available to the IndexedDB hash index", async () => {
    const completed = record({ contentHash: "sha256-ready" });
    const stored = await originalRecordForStorage(completed);

    expect(stored.contentHash).toBe("sha256-ready");
  });

  it("reconstructs app-facing Blobs from current byte records", async () => {
    const stored = await originalRecordForStorage(record());
    const restored = localOriginalFromStorage(stored);

    expect(restored?.blob).toBeInstanceOf(Blob);
    expect(restored?.blob.type).toBe("application/pdf");
    expect(await restored?.blob.text()).toBe("pdf");
  });

  it("keeps legacy Blob-backed records readable", async () => {
    const legacy = record();
    const restored = localOriginalFromStorage(legacy);

    expect(restored?.blob).toBe(legacy.blob);
    expect(await restored?.blob.text()).toBe("pdf");
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
