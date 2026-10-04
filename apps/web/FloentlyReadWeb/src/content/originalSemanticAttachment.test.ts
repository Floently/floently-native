import { beforeEach, describe, expect, it, vi } from "vitest";
import type { UnifiedIngestionResult } from "./unifiedDocumentIngestion";

const mocks = vi.hoisted(() => ({
  linkLocalOriginalToProject: vi.fn(async () => undefined),
}));

vi.mock("./localOriginalDocuments", () => ({
  linkLocalOriginalToProject: mocks.linkLocalOriginalToProject,
}));

import {
  semanticAttachmentInFlight,
  trackCanonicalIngestionForLocalOriginal,
} from "./originalSemanticAttachment";

function ingestionResult(projectId: string): UnifiedIngestionResult {
  return {
    kind: "file",
    project: {
      id: projectId,
    } as UnifiedIngestionResult["project"],
    reused: false,
  };
}

describe("original semantic attachment account isolation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("keeps the same local id independent across authenticated owners", async () => {
    let resolveA!: (value: UnifiedIngestionResult) => void;
    let resolveB!: (value: UnifiedIngestionResult) => void;

    const canonicalA = new Promise<UnifiedIngestionResult>((resolve) => {
      resolveA = resolve;
    });
    const canonicalB = new Promise<UnifiedIngestionResult>((resolve) => {
      resolveB = resolve;
    });

    const taskA = trackCanonicalIngestionForLocalOriginal(
      "account-a",
      "same-local-id",
      canonicalA,
    );
    const taskB = trackCanonicalIngestionForLocalOriginal(
      "account-b",
      "same-local-id",
      canonicalB,
    );

    expect(taskA).not.toBe(taskB);
    expect(
      semanticAttachmentInFlight("account-a", "same-local-id"),
    ).toBe(true);
    expect(
      semanticAttachmentInFlight("account-b", "same-local-id"),
    ).toBe(true);

    resolveA(ingestionResult("project-a"));
    await expect(taskA).resolves.toMatchObject({ id: "project-a" });

    expect(
      mocks.linkLocalOriginalToProject,
    ).toHaveBeenCalledWith(
      "account-a",
      "same-local-id",
      "project-a",
    );
    expect(
      semanticAttachmentInFlight("account-a", "same-local-id"),
    ).toBe(false);
    expect(
      semanticAttachmentInFlight("account-b", "same-local-id"),
    ).toBe(true);

    resolveB(ingestionResult("project-b"));
    await expect(taskB).resolves.toMatchObject({ id: "project-b" });

    expect(
      mocks.linkLocalOriginalToProject,
    ).toHaveBeenCalledWith(
      "account-b",
      "same-local-id",
      "project-b",
    );
    expect(
      semanticAttachmentInFlight("account-b", "same-local-id"),
    ).toBe(false);
  });
});
