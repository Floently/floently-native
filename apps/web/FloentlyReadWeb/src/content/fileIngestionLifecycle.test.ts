import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  uploadContentProject: vi.fn(),
  fingerprintDocumentFile: vi.fn(),
  getProjectForFileFingerprint: vi.fn(),
  rememberProjectForFileFingerprint: vi.fn(),
  getContentProject: vi.fn(),
}));

const {
  uploadContentProject,
  fingerprintDocumentFile,
  getProjectForFileFingerprint,
  rememberProjectForFileFingerprint,
  getContentProject,
} = mocks;

vi.mock("./documentUploadApi", () => ({
  uploadContentProject,
}));

vi.mock("./documentFingerprintStore", () => ({
  fingerprintDocumentFile,
  getProjectForFileFingerprint,
  rememberProjectForFileFingerprint,
}));

vi.mock("./projectApi", () => ({
  createProjectFromText: vi.fn(),
  getContentProject,
}));

import {
  beginFileIngestion,
  ingestionBudgets,
} from "./unifiedDocumentIngestion";

function project(id = "project-1") {
  return {
    id,
    title: "Book",
    kind: "document",
    status: "ready",
    sourceType: "pdf",
    sourceUrl: null,
    language: "en",
    textHash: "hash",
    wordCount: 100,
    characterCount: 600,
    createdAt: "2026-10-01T00:00:00Z",
    updatedAt: "2026-10-01T00:00:00Z",
    progress: null,
    rawText: "Readable book text.",
  };
}

describe("file ingestion lifecycle", () => {
  beforeEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    fingerprintDocumentFile.mockResolvedValue(null);
    getProjectForFileFingerprint.mockReturnValue(null);
  });

  it("lets canonical extraction outlive the foreground fast-open budget", async () => {
    vi.useFakeTimers();

    const upload = {
      resolve: (_value: ReturnType<typeof project>) => undefined,
    };
    uploadContentProject.mockReturnValue(
      new Promise((resolve) => {
        upload.resolve = resolve;
      }),
    );

    const file = new File(
      [new Uint8Array(ingestionBudgets.bookSizeThresholdBytes + 1)],
      "large.pdf",
      { type: "application/pdf" },
    );

    const ingestion = beginFileIngestion(file);

    const immediate = expect(ingestion.immediate).rejects.toThrow(
      "open the original immediately",
    );

    await vi.advanceTimersByTimeAsync(
      ingestionBudgets.bookFastOpenBudgetMs,
    );
    await immediate;

    upload.resolve(project());
    await expect(ingestion.canonical).resolves.toMatchObject({
      project: { id: "project-1" },
      reused: false,
    });
  });

  it("can disable the foreground budget when the original is already visible", async () => {
    vi.useFakeTimers();

    const upload = {
      resolve: (_value: ReturnType<typeof project>) => undefined,
    };
    uploadContentProject.mockReturnValue(
      new Promise((resolve) => {
        upload.resolve = resolve;
      }),
    );

    const file = new File(
      [new Uint8Array(ingestionBudgets.bookSizeThresholdBytes + 1)],
      "visible.pdf",
      { type: "application/pdf" },
    );

    const ingestion = beginFileIngestion(file, {
      fastOpen: false,
    });

    let settled = false;
    void ingestion.immediate.finally(() => {
      settled = true;
    });

    await vi.advanceTimersByTimeAsync(
      ingestionBudgets.bookFastOpenBudgetMs * 2,
    );
    expect(settled).toBe(false);

    upload.resolve(project("project-visible"));

    await expect(ingestion.immediate).resolves.toMatchObject({
      project: { id: "project-visible" },
    });
    await expect(ingestion.canonical).resolves.toMatchObject({
      project: { id: "project-visible" },
    });
  });
});
