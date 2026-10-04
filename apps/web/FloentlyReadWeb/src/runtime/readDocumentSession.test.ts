import { describe, expect, it } from "vitest";
import type { ReadCoreWorkerClient } from "../readCore.client";
import type { ReadingManifestSummary } from "../readCore.types";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "../playback/webPlaybackSession";
import {
  ReadDocumentSession,
  type ReadDocumentSource,
} from "./readDocumentSession";

function source(): ReadDocumentSource {
  return {
    id: "doc-1",
    revisionId: "rev-1",
    title: "Cached document",
    language: "en",
    text: "One logical document.",
  };
}

function manifest(): ReadingManifestSummary {
  return {
    handle: "doc-1:rev-1",
    schemaVersion: 1,
    documentId: "doc-1",
    revisionId: "rev-1",
    title: "Cached document",
    language: "en",
    wordCount: 3,
    textScalarLength: 21,
    estimatedSourceDurationMs: 10_000,
    segmentCount: 1,
    firstSegments: [],
    buildMs: 1,
  };
}

function emptyPlayback(): WebPlaybackSnapshot {
  return {
    status: "idle",
    documentId: null,
    revisionId: null,
    title: null,
    author: null,
    durationMs: 0,
    elapsedMs: 0,
    bufferedAheadMs: 0,
    activeSegmentIndex: null,
    canonicalScalarCursor: null,
    speed: 1,
    voiceId: "voice-1",
    error: null,
  };
}

class FakeCore {
  buildCount = 0;
  dropped: string[] = [];

  async buildManifest(): Promise<ReadingManifestSummary> {
    this.buildCount += 1;
    return manifest();
  }

  async dropManifest(handle: string): Promise<void> {
    this.dropped.push(handle);
  }
}

class FakePlayback {
  snapshot = emptyPlayback();
  loaded: ReadingManifestSummary[] = [];
  clearCount = 0;

  getSnapshot = () => this.snapshot;

  loadDocument(next: ReadingManifestSummary): void {
    this.loaded.push(next);
    this.snapshot = {
      ...emptyPlayback(),
      status: "ready",
      documentId: next.documentId,
      revisionId: next.revisionId,
      title: next.title,
      durationMs: next.estimatedSourceDurationMs,
      canonicalScalarCursor: 0,
    };
  }

  clear(): void {
    this.clearCount += 1;
    this.snapshot = emptyPlayback();
  }

  setForeignDocument(): void {
    this.snapshot = {
      ...emptyPlayback(),
      status: "playing",
      documentId: "browser:page-2",
      revisionId: "cloud-nav-4",
      title: "Foreign browser page",
      durationMs: 25_000,
      elapsedMs: 4_000,
      canonicalScalarCursor: 40,
    };
  }
}

function harness() {
  const core = new FakeCore();
  const playback = new FakePlayback();
  const documents = new ReadDocumentSession(
    core as unknown as ReadCoreWorkerClient,
    playback as unknown as WebPlaybackSession,
  );

  return { core, playback, documents };
}

describe("ReadDocumentSession playback ownership", () => {
  it("reuses the cached manifest without rebuilding when ownership is already correct", async () => {
    const { core, playback, documents } = harness();

    const first = await documents.load(source());
    const second = await documents.load(source());

    expect(second).toBe(first);
    expect(core.buildCount).toBe(1);
    expect(playback.loaded).toHaveLength(1);

    documents.destroy();
  });

  it("re-attaches a cached manifest when another surface took playback ownership", async () => {
    const { core, playback, documents } = harness();

    const first = await documents.load(source());
    playback.setForeignDocument();

    const reused = await documents.load(source());

    expect(reused).toBe(first);
    expect(core.buildCount).toBe(1);
    expect(playback.loaded).toHaveLength(2);
    expect(playback.getSnapshot()).toMatchObject({
      documentId: "doc-1",
      revisionId: "rev-1",
      status: "ready",
    });

    documents.destroy();
  });
});
