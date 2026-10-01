import { describe, expect, it } from "vitest";
import {
  normalizeProject,
  normalizeProjectProgress,
} from "./projectApi";

describe("projectApi normalization", () => {
  it("normalizes a synced Read project and optional language", () => {
    expect(
      normalizeProject({
        id: "project-1",
        title: "Clinical PDF",
        kind: "document",
        status: "ready",
        sourceType: "pdf",
        sourceUrl: null,
        language: "fi",
        textHash: "abc",
        wordCount: 1234,
        characterCount: 9000,
        createdAt: "2026-10-01T00:00:00Z",
        updatedAt: "2026-10-01T01:00:00Z",
        rawText: "Readable text",
        progress: {
          projectId: "project-1",
          currentSegmentIndex: 3,
          currentCharacterOffset: 814,
          progressPercent: 42.5,
          voiceId: "azure:fi-FI-SelmaNeural",
          playbackRate: 1.75,
          updatedAt: "2026-10-01T01:05:00Z",
        },
      }),
    ).toMatchObject({
      id: "project-1",
      title: "Clinical PDF",
      sourceType: "pdf",
      language: "fi",
      rawText: "Readable text",
      progress: {
        currentSegmentIndex: 3,
        currentCharacterOffset: 814,
        progressPercent: 42.5,
        playbackRate: 1.75,
      },
    });
  });

  it("rejects incomplete project identity", () => {
    expect(normalizeProject({ title: "No id" })).toBeNull();
    expect(normalizeProject({ id: "project-1" })).toBeNull();
  });

  it("uses stable defaults for malformed optional fields", () => {
    expect(
      normalizeProject({
        id: "project-2",
        title: "Text",
        wordCount: "bad",
        characterCount: null,
        progress: "bad",
      }),
    ).toMatchObject({
      kind: "document",
      status: "ready",
      sourceType: "text",
      wordCount: 0,
      characterCount: 0,
      progress: null,
    });
  });

  it("normalizes project progress independently", () => {
    expect(
      normalizeProjectProgress({
        projectId: "p",
        currentSegmentIndex: 2,
        currentCharacterOffset: 100,
        progressPercent: 20,
        playbackRate: 2,
        updatedAt: "now",
      }),
    ).toEqual({
      projectId: "p",
      currentSegmentIndex: 2,
      currentCharacterOffset: 100,
      progressPercent: 20,
      voiceId: null,
      playbackRate: 2,
      updatedAt: "now",
    });
  });
});
