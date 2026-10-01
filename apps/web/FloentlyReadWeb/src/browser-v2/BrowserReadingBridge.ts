import type { ReadCoreWorkerClient } from "../readCore.client";
import type { ReadingManifestSummary } from "../readCore.types";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "../playback/webPlaybackSession";
import type {
  BrowserReadingAdapter,
  BrowserReadingAnchor,
  BrowserViewportPoint,
} from "./browserContracts";
import {
  anchorSpanForScalar,
  buildBrowserReadingSource,
  sameBrowserReadingIdentity,
  scalarStartForAnchor,
  type BrowserReadingSource,
} from "./browserReadingSource";

export type BrowserReadingBridgeStatus =
  | "idle"
  | "extracting"
  | "ready"
  | "stale"
  | "error";

export interface BrowserReadingBridgeSnapshot {
  status: BrowserReadingBridgeStatus;
  tabId: string | null;
  title: string | null;
  canonicalUrl: string | null;
  revisionId: string | null;
  follow: boolean;
  error: string | null;
}

type Listener = () => void;

type BrowserReadCorePort = Pick<
  ReadCoreWorkerClient,
  "buildManifest" | "logicalTimeForScalar" | "dropManifest"
>;

type BrowserPlaybackPort = Pick<
  WebPlaybackSession,
  | "loadDocument"
  | "clear"
  | "seek"
  | "play"
  | "pause"
  | "subscribe"
  | "getSnapshot"
>;

interface PendingHighlight {
  generation: number;
  tabId: string;
  anchor: BrowserReadingAnchor;
}

const EMPTY_SNAPSHOT: BrowserReadingBridgeSnapshot = {
  status: "idle",
  tabId: null,
  title: null,
  canonicalUrl: null,
  revisionId: null,
  follow: false,
  error: null,
};

function sameAnchor(
  left: BrowserReadingAnchor | null,
  right: BrowserReadingAnchor | null,
): boolean {
  return Boolean(
    left
    && right
    && left.id === right.id
    && left.revision === right.revision,
  );
}

export class BrowserReadingBridge {
  private readonly listeners = new Set<Listener>();
  private readonly unsubscribePlayback: () => void;

  private generation = 0;
  private source: BrowserReadingSource | null = null;
  private manifest: ReadingManifestSummary | null = null;
  private revisionUnsubscribe: (() => void) | null = null;
  private lastHighlightedAnchor: BrowserReadingAnchor | null = null;
  private pendingHighlight: PendingHighlight | null = null;
  private highlightRunning = false;
  private highlightEnabled = true;
  private snapshot: BrowserReadingBridgeSnapshot = EMPTY_SNAPSHOT;

  constructor(
    private readonly adapter: BrowserReadingAdapter,
    private readonly core: BrowserReadCorePort,
    private readonly playback: BrowserPlaybackPort,
  ) {
    this.unsubscribePlayback = playback.subscribe(() => {
      this.handlePlaybackSnapshot(playback.getSnapshot());
    });
  }

  readonly subscribe = (listener: Listener): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  readonly getSnapshot = (): BrowserReadingBridgeSnapshot => this.snapshot;

  async startReading(tabId: string): Promise<void> {
    const normalizedTabId = tabId.trim();
    if (!normalizedTabId) {
      throw new Error("Browser tab identity is required.");
    }

    const generation = ++this.generation;
    const previous = this.detachCurrent();

    this.clearPlaybackIfOwned(previous.manifest);
    this.releaseDetached(previous);

    this.replaceSnapshot({
      status: "extracting",
      tabId: normalizedTabId,
      title: null,
      canonicalUrl: null,
      revisionId: null,
      error: null,
    });

    try {
      const document = await this.adapter.extractDocument(normalizedTabId);
      if (generation !== this.generation) return;

      const source = buildBrowserReadingSource(document);
      if (!source.text.trim() || source.spans.length === 0) {
        throw new Error("This page does not contain readable text.");
      }

      const manifest = await this.core.buildManifest({
        documentId: `browser:${source.documentId}`,
        revisionId: source.revisionId,
        title: source.title,
        language: source.language,
        text: source.text,
      });

      if (generation !== this.generation) {
        void this.core.dropManifest(manifest.handle).catch(() => undefined);
        return;
      }

      this.source = source;
      this.manifest = manifest;
      this.lastHighlightedAnchor = null;
      this.revisionUnsubscribe = this.adapter.subscribeRevision(
        normalizedTabId,
        () => this.invalidate("The web page changed."),
      );

      this.playback.loadDocument(manifest);
      this.replaceSnapshot({
        status: "ready",
        tabId: normalizedTabId,
        title: source.title,
        canonicalUrl: source.canonicalUrl,
        revisionId: source.revisionId,
        error: null,
      });

      this.handlePlaybackSnapshot(this.playback.getSnapshot());
    } catch (reason) {
      if (generation !== this.generation) return;

      const message =
        reason instanceof Error ? reason.message : String(reason);

      this.replaceSnapshot({
        status: "error",
        tabId: normalizedTabId,
        error: message,
      });
      throw reason;
    }
  }

  invalidate(reason: string | null = null): void {
    this.generation += 1;
    const previous = this.detachCurrent();

    this.clearPlaybackIfOwned(previous.manifest);
    this.releaseDetached(previous);

    this.replaceSnapshot({
      status: "stale",
      title: null,
      canonicalUrl: null,
      revisionId: null,
      error: reason,
    });
  }

  close(): void {
    this.generation += 1;
    const previous = this.detachCurrent();

    this.clearPlaybackIfOwned(previous.manifest);
    this.releaseDetached(previous);
    this.replaceSnapshot({
      ...EMPTY_SNAPSHOT,
      follow: this.snapshot.follow,
    });
  }

  setFollow(follow: boolean): void {
    if (this.snapshot.follow === follow) return;

    this.replaceSnapshot({ follow });

    if (
      follow
      && this.snapshot.status === "ready"
      && this.snapshot.tabId
      && this.lastHighlightedAnchor
    ) {
      const generation = this.generation;
      const tabId = this.snapshot.tabId;
      const anchor = { ...this.lastHighlightedAnchor };

      void this.adapter
        .scrollToSentence(tabId, anchor)
        .catch(() => undefined)
        .then(() => {
          if (generation !== this.generation) return;
        });
    }
  }

  toggleFollow(): void {
    this.setFollow(!this.snapshot.follow);
  }

  setHighlightEnabled(enabled: boolean): void {
    if (this.highlightEnabled === enabled) return;

    this.highlightEnabled = enabled;
    this.pendingHighlight = null;
    this.lastHighlightedAnchor = null;

    const tabId = this.snapshot.tabId;
    if (!enabled) {
      if (tabId) {
        void this.adapter.clearHighlights(tabId).catch(() => undefined);
      }
      return;
    }

    this.handlePlaybackSnapshot(this.playback.getSnapshot());
  }

  async readFromHere(point: BrowserViewportPoint): Promise<boolean> {
    const generation = this.generation;
    const source = this.source;
    const manifest = this.manifest;
    const tabId = this.snapshot.tabId;

    if (
      this.snapshot.status !== "ready"
      || !source
      || !manifest
      || !tabId
    ) {
      return false;
    }

    const anchor = await this.adapter.resolvePoint(tabId, point);
    if (!anchor || !this.isCurrent(generation, tabId, manifest)) {
      return false;
    }

    // Re-extract through the same authenticated private browser session before
    // seeking. A cached anchor alone cannot prove that the visible page is
    // still the page whose manifest owns the current audio timeline.
    const liveDocument = await this.adapter.extractDocument(tabId);
    if (!this.isCurrent(generation, tabId, manifest)) {
      return false;
    }

    const liveSource = buildBrowserReadingSource(liveDocument);
    if (!sameBrowserReadingIdentity(source, liveSource)) {
      this.invalidate("The web page changed.");
      return false;
    }

    const scalarOffset = scalarStartForAnchor(liveSource, anchor);
    if (scalarOffset === null) {
      this.invalidate("The selected reading position is stale.");
      return false;
    }

    const elapsedMs = await this.core.logicalTimeForScalar(
      manifest.handle,
      scalarOffset,
    );
    if (
      elapsedMs === null
      || !this.isCurrent(generation, tabId, manifest)
    ) {
      return false;
    }

    await this.playback.seek(elapsedMs);
    if (!this.isCurrent(generation, tabId, manifest)) {
      return false;
    }

    await this.playback.play();
    if (!this.isCurrent(generation, tabId, manifest)) {
      return false;
    }

    this.enqueueHighlight(anchor);
    return true;
  }

  async moveBySentence(delta: number): Promise<boolean> {
    const generation = this.generation;
    const source = this.source;
    const manifest = this.manifest;
    const tabId = this.snapshot.tabId;

    if (
      !source
      || !manifest
      || !tabId
      || source.spans.length === 0
      || this.snapshot.status !== "ready"
    ) {
      return false;
    }

    const playback = this.playback.getSnapshot();
    const current = anchorSpanForScalar(
      source,
      playback.canonicalScalarCursor ?? 0,
    );
    const currentIndex = current
      ? source.spans.findIndex((span) => span.sentenceId === current.sentenceId)
      : 0;
    const targetIndex = Math.min(
      source.spans.length - 1,
      Math.max(0, currentIndex + Math.trunc(delta)),
    );
    const target = source.spans[targetIndex];
    if (!target) return false;

    const elapsedMs = await this.core.logicalTimeForScalar(
      manifest.handle,
      target.scalarStart,
    );
    if (
      elapsedMs === null
      || !this.isCurrent(generation, tabId, manifest)
    ) {
      return false;
    }

    await this.playback.seek(elapsedMs);
    if (!this.isCurrent(generation, tabId, manifest)) {
      return false;
    }

    this.enqueueHighlight(target.anchor);
    return true;
  }

  destroy(): void {
    this.unsubscribePlayback();
    this.close();
    this.listeners.clear();
  }

  private handlePlaybackSnapshot(playback: WebPlaybackSnapshot): void {
    const source = this.source;
    const manifest = this.manifest;
    const tabId = this.snapshot.tabId;

    if (
      this.snapshot.status !== "ready"
      || !source
      || !manifest
      || !tabId
      || playback.documentId !== manifest.documentId
      || playback.revisionId !== manifest.revisionId
      || playback.canonicalScalarCursor === null
    ) {
      return;
    }

    const span = anchorSpanForScalar(
      source,
      playback.canonicalScalarCursor,
    );
    if (!span || sameAnchor(span.anchor, this.lastHighlightedAnchor)) {
      return;
    }

    this.enqueueHighlight(span.anchor);
  }

  private enqueueHighlight(anchor: BrowserReadingAnchor): void {
    const tabId = this.snapshot.tabId;
    if (
      !this.highlightEnabled
      || !tabId
      || this.snapshot.status !== "ready"
    ) return;

    this.pendingHighlight = {
      generation: this.generation,
      tabId,
      anchor: { ...anchor },
    };

    void this.flushHighlightQueue();
  }

  private async flushHighlightQueue(): Promise<void> {
    if (this.highlightRunning) return;
    this.highlightRunning = true;

    try {
      while (this.pendingHighlight) {
        const pending = this.pendingHighlight;
        this.pendingHighlight = null;

        if (
          !this.highlightEnabled
          || pending.generation !== this.generation
          || pending.tabId !== this.snapshot.tabId
          || this.snapshot.status !== "ready"
        ) {
          continue;
        }

        try {
          await this.adapter.highlightSentence(
            pending.tabId,
            pending.anchor,
          );

          if (
            !this.highlightEnabled
            || pending.generation !== this.generation
            || pending.tabId !== this.snapshot.tabId
            || this.snapshot.status !== "ready"
          ) {
            continue;
          }

          this.lastHighlightedAnchor = { ...pending.anchor };

          if (this.snapshot.follow) {
            await this.adapter.scrollToSentence(
              pending.tabId,
              pending.anchor,
            );
          }
        } catch {
          // Highlighting is visual feedback. A remote repaint or highlight RPC
          // failure must not tear down otherwise valid document playback.
        }
      }
    } finally {
      this.highlightRunning = false;

      if (this.pendingHighlight) {
        void this.flushHighlightQueue();
      }
    }
  }

  private isCurrent(
    generation: number,
    tabId: string,
    manifest: ReadingManifestSummary,
  ): boolean {
    return (
      generation === this.generation
      && this.snapshot.status === "ready"
      && this.snapshot.tabId === tabId
      && this.manifest === manifest
      && this.source !== null
    );
  }

  private clearPlaybackIfOwned(
    manifest: ReadingManifestSummary | null,
  ): void {
    if (!manifest) return;

    const playback = this.playback.getSnapshot();
    if (
      playback.documentId === manifest.documentId
      && playback.revisionId === manifest.revisionId
    ) {
      this.playback.clear();
    }
  }

  private detachCurrent(): {
    tabId: string | null;
    manifest: ReadingManifestSummary | null;
    unsubscribeRevision: (() => void) | null;
  } {
    const detached = {
      tabId: this.snapshot.tabId,
      manifest: this.manifest,
      unsubscribeRevision: this.revisionUnsubscribe,
    };

    this.revisionUnsubscribe = null;
    this.source = null;
    this.manifest = null;
    this.lastHighlightedAnchor = null;
    this.pendingHighlight = null;
    detached.unsubscribeRevision?.();

    return detached;
  }

  private releaseDetached(detached: {
    tabId: string | null;
    manifest: ReadingManifestSummary | null;
  }): void {
    if (detached.tabId) {
      void this.adapter
        .clearHighlights(detached.tabId)
        .catch(() => undefined);
    }

    if (detached.manifest) {
      void this.core
        .dropManifest(detached.manifest.handle)
        .catch(() => undefined);
    }
  }

  private replaceSnapshot(
    patch: Partial<BrowserReadingBridgeSnapshot>,
  ): void {
    this.snapshot = { ...this.snapshot, ...patch };

    for (const listener of this.listeners) {
      listener();
    }
  }
}
