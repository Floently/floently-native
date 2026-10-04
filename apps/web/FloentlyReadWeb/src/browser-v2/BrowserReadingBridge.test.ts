import { describe, expect, it } from "vitest";
import type { ReadingManifestSummary } from "../readCore.types";
import type { WebPlaybackSnapshot } from "../playback/webPlaybackSession";
import type {
  BrowserReadingAdapter,
  BrowserReadingAnchor,
  BrowserReadingDocument,
} from "./browserContracts";
import { BrowserReadingBridge } from "./BrowserReadingBridge";

function pageDocument(
  overrides: Partial<BrowserReadingDocument> = {},
): BrowserReadingDocument {
  return {
    documentId: "page-1",
    revision: "rev-1",
    title: "Article",
    language: "en",
    canonicalUrl: "https://example.com/article",
    sentences: [
      {
        id: "sentence-1",
        text: "First sentence.",
        order: 1,
        wordCount: 2,
        anchor: { id: "anchor-1", revision: "rev-1" },
      },
      {
        id: "sentence-2",
        text: "Second sentence.",
        order: 2,
        wordCount: 2,
        anchor: { id: "anchor-2", revision: "rev-1" },
      },
    ],
    ...overrides,
  };
}

function playbackSnapshot(
  patch: Partial<WebPlaybackSnapshot> = {},
): WebPlaybackSnapshot {
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
    ...patch,
  };
}

function manifestFor(
  input: {
    documentId: string;
    revisionId: string;
    title: string;
    language: string;
    text: string;
  },
): ReadingManifestSummary {
  return {
    handle: `${input.documentId}:${input.revisionId}`,
    schemaVersion: 1,
    documentId: input.documentId,
    revisionId: input.revisionId,
    title: input.title,
    language: input.language,
    wordCount: input.text.split(/\s+/).length,
    textScalarLength: Array.from(input.text).length,
    estimatedSourceDurationMs: 20_000,
    segmentCount: 1,
    firstSegments: [],
    buildMs: 1,
  };
}

class FakeCore {
  builtText: string | null = null;
  dropped: string[] = [];
  scalarRequests: number[] = [];
  buildManifestOverride:
    | ((input: {
        documentId: string;
        revisionId: string;
        title: string;
        language: string;
        text: string;
      }) => Promise<ReadingManifestSummary>)
    | null = null;

  async buildManifest(input: {
    documentId: string;
    revisionId: string;
    title: string;
    language: string;
    text: string;
  }): Promise<ReadingManifestSummary> {
    this.builtText = input.text;
    if (this.buildManifestOverride) {
      return this.buildManifestOverride(input);
    }
    return manifestFor(input);
  }

  async logicalTimeForScalar(
    _handle: string,
    scalarOffset: number,
  ): Promise<number> {
    this.scalarRequests.push(scalarOffset);
    return scalarOffset * 100;
  }

  async dropManifest(handle: string): Promise<void> {
    this.dropped.push(handle);
  }
}

class FakePlayback {
  snapshot = playbackSnapshot();
  listeners = new Set<() => void>();
  clearCount = 0;
  playCount = 0;
  pauseCount = 0;
  seekRequests: number[] = [];

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = () => this.snapshot;

  loadDocument(manifest: ReadingManifestSummary): void {
    this.snapshot = playbackSnapshot({
      status: "ready",
      documentId: manifest.documentId,
      revisionId: manifest.revisionId,
      title: manifest.title,
      durationMs: manifest.estimatedSourceDurationMs,
      canonicalScalarCursor: 0,
    });
    this.emit();
  }

  clear(): void {
    this.clearCount += 1;
    this.snapshot = playbackSnapshot();
    this.emit();
  }

  async seek(elapsedMs: number): Promise<void> {
    this.seekRequests.push(elapsedMs);
    this.snapshot = {
      ...this.snapshot,
      elapsedMs,
    };
    this.emit();
  }

  async play(): Promise<void> {
    this.playCount += 1;
    this.snapshot = {
      ...this.snapshot,
      status: "playing",
    };
    this.emit();
  }

  pause(): void {
    this.pauseCount += 1;
    this.snapshot = {
      ...this.snapshot,
      status: "paused",
    };
    this.emit();
  }

  setCursor(scalarOffset: number): void {
    this.snapshot = {
      ...this.snapshot,
      canonicalScalarCursor: scalarOffset,
    };
    this.emit();
  }

  setForeignDocument(): void {
    this.snapshot = playbackSnapshot({
      status: "playing",
      documentId: "local-library-doc",
      revisionId: "local-rev",
      canonicalScalarCursor: 3,
    });
    this.emit();
  }

  private emit(): void {
    for (const listener of this.listeners) listener();
  }
}

class FakeAdapter implements BrowserReadingAdapter {
  document = pageDocument();
  reextractDocument: BrowserReadingDocument | null = null;
  highlights: BrowserReadingAnchor[] = [];
  scrolls: BrowserReadingAnchor[] = [];
  clears: string[] = [];
  revisionListener: ((revision: string) => void) | null = null;
  resolvedAnchor: BrowserReadingAnchor | null = {
    id: "anchor-2",
    revision: "rev-1",
  };
  extractionCount = 0;
  extractDocumentOverride:
    | (() => Promise<BrowserReadingDocument>)
    | null = null;

  async extractDocument(): Promise<BrowserReadingDocument> {
    this.extractionCount += 1;
    if (this.extractDocumentOverride) {
      return this.extractDocumentOverride();
    }
    if (this.extractionCount > 1 && this.reextractDocument) {
      return this.reextractDocument;
    }
    return this.document;
  }

  async resolvePoint(): Promise<BrowserReadingAnchor | null> {
    return this.resolvedAnchor;
  }

  async getSelection() {
    return null;
  }

  async highlightSentence(
    _tabId: string,
    anchor: BrowserReadingAnchor,
  ): Promise<void> {
    this.highlights.push({ ...anchor });
  }

  async highlightWord(): Promise<void> {}

  async clearHighlights(tabId: string): Promise<void> {
    this.clears.push(tabId);
  }

  async scrollToSentence(
    _tabId: string,
    anchor: BrowserReadingAnchor,
  ): Promise<void> {
    this.scrolls.push({ ...anchor });
  }

  subscribeRevision(
    _tabId: string,
    listener: (revision: string) => void,
  ): () => void {
    this.revisionListener = listener;
    return () => {
      if (this.revisionListener === listener) {
        this.revisionListener = null;
      }
    };
  }

  revise(revision = "rev-2"): void {
    this.revisionListener?.(revision);
  }
}

function tick(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function harness() {
  const adapter = new FakeAdapter();
  const core = new FakeCore();
  const playback = new FakePlayback();
  const bridge = new BrowserReadingBridge(adapter, core, playback);

  return { adapter, core, playback, bridge };
}

describe("BrowserReadingBridge", () => {
  it("builds a hidden semantic document without exposing extracted text in UI state", async () => {
    const { bridge, core } = harness();

    await bridge.startReading("cloud-tab-1");

    expect(core.builtText).toBe("First sentence.\n\nSecond sentence.");
    expect(bridge.getSnapshot()).toEqual({
      status: "ready",
      tabId: "cloud-tab-1",
      documentId: "browser:page-1",
      title: "Article",
      canonicalUrl: "https://example.com/article",
      revisionId: "rev-1",
      follow: false,
      error: null,
    });

    expect(
      Object.values(bridge.getSnapshot()).join(" "),
    ).not.toContain("First sentence");
  });

  it("ignores a late extraction after navigation invalidates the browser generation", async () => {
    const { bridge, adapter, core, playback } = harness();

    let resolveExtraction:
      | ((document: BrowserReadingDocument) => void)
      | null = null;
    adapter.extractDocumentOverride = () =>
      new Promise((resolve) => {
        resolveExtraction = resolve;
      });

    const pending = bridge.startReading("cloud-tab-1");
    await tick();

    expect(bridge.getSnapshot().status).toBe("extracting");

    bridge.invalidate("The web page changed.");
    resolveExtraction!(pageDocument());
    await pending;

    expect(core.builtText).toBeNull();
    expect(playback.getSnapshot().documentId).toBeNull();
    expect(bridge.getSnapshot().status).toBe("stale");
    expect(bridge.getSnapshot().error).toBe("The web page changed.");
  });

  it("drops a late manifest build instead of attaching it after invalidation", async () => {
    const { bridge, core, playback } = harness();

    let resolveManifest:
      | ((manifest: ReadingManifestSummary) => void)
      | null = null;
    core.buildManifestOverride = (_input) =>
      new Promise((resolve) => {
        resolveManifest = resolve;
      });

    const pending = bridge.startReading("cloud-tab-1");
    await tick();

    expect(core.builtText).toBe("First sentence.\n\nSecond sentence.");

    bridge.invalidate("The web page changed.");
    resolveManifest!(
      manifestFor({
        documentId: "browser:page-1",
        revisionId: "rev-1",
        title: "Article",
        language: "en",
        text: "First sentence.\n\nSecond sentence.",
      }),
    );
    await pending;
    await tick();

    expect(playback.getSnapshot().documentId).toBeNull();
    expect(core.dropped).toContain("browser:page-1:rev-1");
    expect(bridge.getSnapshot().status).toBe("stale");
  });

  it("highlights the visible page from the document-wide scalar cursor without forcing follow", async () => {
    const { bridge, adapter, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    await tick();

    playback.setCursor(18);
    await tick();

    expect(adapter.highlights.at(-1)?.id).toBe("anchor-2");
    expect(adapter.scrolls).toHaveLength(0);
  });

  it("clears only Floently highlights when semantic highlighting is disabled", async () => {
    const { bridge, adapter, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    await tick();

    bridge.setHighlightEnabled(false);
    await tick();
    const highlightCount = adapter.highlights.length;

    playback.setCursor(18);
    await tick();

    expect(adapter.clears).toContain("cloud-tab-1");
    expect(adapter.highlights).toHaveLength(highlightCount);
    expect(bridge.getSnapshot().status).toBe("ready");
  });

  it("keeps Read From Here functional while visual highlighting is disabled", async () => {
    const { bridge, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    bridge.setHighlightEnabled(false);

    const started = await bridge.readFromHere({
      x: 120,
      y: 250,
      viewportRevision: 7,
    });

    expect(started).toBe(true);
    expect(playback.seekRequests.at(-1)).toBe(1_700);
    expect(playback.playCount).toBe(1);
  });

  it("resumes sentence highlighting from the current cursor when re-enabled", async () => {
    const { bridge, adapter, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    bridge.setHighlightEnabled(false);
    playback.setCursor(18);
    await tick();

    bridge.setHighlightEnabled(true);
    await tick();

    expect(adapter.highlights.at(-1)?.id).toBe("anchor-2");
  });

  it("scrolls the current page anchor only after Follow is enabled", async () => {
    const { bridge, adapter } = harness();

    await bridge.startReading("cloud-tab-1");
    await tick();

    bridge.setFollow(true);
    await tick();

    expect(adapter.scrolls.at(-1)?.id).toBe("anchor-1");
  });

  it("invalidates browser-owned speech immediately when the page revision changes", async () => {
    const { bridge, adapter, core, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    adapter.revise();
    await tick();

    expect(bridge.getSnapshot().status).toBe("stale");
    expect(playback.clearCount).toBe(1);
    expect(adapter.clears).toContain("cloud-tab-1");
    expect(core.dropped).toContain("browser:page-1:rev-1");
  });

  it("does not clear a different document if a background page becomes stale", async () => {
    const { bridge, adapter, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    playback.setForeignDocument();

    adapter.revise();
    await tick();

    expect(playback.clearCount).toBe(0);
    expect(playback.getSnapshot().documentId).toBe("local-library-doc");
  });

  it("fails closed when Read From Here no longer owns app playback", async () => {
    const { bridge, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    playback.setForeignDocument();

    const started = await bridge.readFromHere({
      x: 120,
      y: 250,
      viewportRevision: 7,
    });

    expect(started).toBe(false);
    expect(playback.seekRequests).toHaveLength(0);
    expect(playback.playCount).toBe(0);
    expect(playback.clearCount).toBe(0);
    expect(playback.getSnapshot()).toMatchObject({
      documentId: "local-library-doc",
      revisionId: "local-rev",
      status: "playing",
    });
  });

  it("fails closed when Previous or Next no longer owns app playback", async () => {
    const { bridge, core, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    playback.setForeignDocument();

    const moved = await bridge.moveBySentence(1);

    expect(moved).toBe(false);
    expect(core.scalarRequests).toHaveLength(0);
    expect(playback.seekRequests).toHaveLength(0);
    expect(playback.clearCount).toBe(0);
    expect(playback.getSnapshot().documentId).toBe("local-library-doc");
  });

  it("revalidates the same private page before Read From Here seeks and starts", async () => {
    const { bridge, core, playback } = harness();

    await bridge.startReading("cloud-tab-1");

    const started = await bridge.readFromHere({
      x: 120,
      y: 250,
      viewportRevision: 7,
    });
    await tick();

    // "First sentence." is 15 scalars, then two canonical separators.
    expect(started).toBe(true);
    expect(core.scalarRequests.at(-1)).toBe(17);
    expect(playback.seekRequests.at(-1)).toBe(1_700);
    expect(playback.playCount).toBe(1);
  });

  it("does not issue a redundant second play when Read From Here seeks active playback", async () => {
    const { bridge, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    await playback.play();
    expect(playback.playCount).toBe(1);

    const started = await bridge.readFromHere({
      x: 120,
      y: 250,
      viewportRevision: 7,
    });

    expect(started).toBe(true);
    expect(playback.seekRequests.at(-1)).toBe(1_700);
    expect(playback.playCount).toBe(1);
    expect(playback.getSnapshot().status).toBe("playing");
  });

  it("fails closed when same-session revalidation finds changed page content", async () => {
    const { bridge, adapter, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    adapter.reextractDocument = pageDocument({
      sentences: [
        pageDocument().sentences[0],
        {
          ...pageDocument().sentences[1],
          text: "The page changed without a safe cached identity.",
        },
      ],
    });

    const started = await bridge.readFromHere({
      x: 1,
      y: 1,
      viewportRevision: 7,
    });
    await tick();

    expect(started).toBe(false);
    expect(bridge.getSnapshot().status).toBe("stale");
    expect(playback.seekRequests).toHaveLength(0);
    expect(playback.playCount).toBe(0);
  });

  it("maps previous and next semantic positions through the Rust logical clock", async () => {
    const { bridge, core, playback } = harness();

    await bridge.startReading("cloud-tab-1");
    playback.setCursor(18);

    const moved = await bridge.moveBySentence(-1);

    expect(moved).toBe(true);
    expect(core.scalarRequests.at(-1)).toBe(0);
    expect(playback.seekRequests.at(-1)).toBe(0);
  });
});
