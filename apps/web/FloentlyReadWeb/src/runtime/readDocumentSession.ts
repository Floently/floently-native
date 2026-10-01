import type {
  ReadingManifestSummary,
} from "../readCore.types";
import type { ReadCoreWorkerClient } from "../readCore.client";
import type { WebPlaybackSession } from "../playback/webPlaybackSession";

export interface ReadDocumentSource {
  id: string;
  revisionId: string;
  title: string;
  language: string;
  text: string;
}

export interface ReadDocumentSessionSnapshot {
  status: "idle" | "loading" | "ready" | "error";
  documentId: string | null;
  revisionId: string | null;
  title: string | null;
  manifest: ReadingManifestSummary | null;
  error: string | null;
}

type Listener = () => void;

const EMPTY_SNAPSHOT: ReadDocumentSessionSnapshot = {
  status: "idle",
  documentId: null,
  revisionId: null,
  title: null,
  manifest: null,
  error: null,
};

export class ReadDocumentSession {
  private readonly listeners = new Set<Listener>();
  private generation = 0;
  private snapshot: ReadDocumentSessionSnapshot = EMPTY_SNAPSHOT;

  constructor(
    private readonly core: ReadCoreWorkerClient,
    private readonly playback: WebPlaybackSession,
  ) {}

  readonly subscribe = (listener: Listener): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  readonly getSnapshot = (): ReadDocumentSessionSnapshot => this.snapshot;

  async load(source: ReadDocumentSource): Promise<ReadingManifestSummary> {
    const normalizedId = source.id.trim();
    const normalizedRevision = source.revisionId.trim();

    if (!normalizedId || !normalizedRevision || !source.text.trim()) {
      throw new Error("The document source is incomplete.");
    }

    if (
      this.snapshot.status === "ready"
      && this.snapshot.documentId === normalizedId
      && this.snapshot.revisionId === normalizedRevision
      && this.snapshot.manifest
    ) {
      return this.snapshot.manifest;
    }

    const generation = ++this.generation;
    const previousManifest = this.snapshot.manifest;

    this.replaceSnapshot({
      status: "loading",
      documentId: normalizedId,
      revisionId: normalizedRevision,
      title: source.title.trim() || "Untitled document",
      manifest: previousManifest,
      error: null,
    });

    try {
      const manifest = await this.core.buildManifest({
        documentId: normalizedId,
        revisionId: normalizedRevision,
        title: source.title.trim() || "Untitled document",
        language: source.language.trim() || "en",
        text: source.text,
      });

      if (generation !== this.generation) {
        void this.core.dropManifest(manifest.handle).catch(() => undefined);
        throw new Error("Document load was superseded.");
      }

      this.playback.loadDocument(manifest);

      this.replaceSnapshot({
        status: "ready",
        documentId: normalizedId,
        revisionId: normalizedRevision,
        title: manifest.title,
        manifest,
        error: null,
      });

      if (
        previousManifest
        && previousManifest.handle !== manifest.handle
      ) {
        void this.core
          .dropManifest(previousManifest.handle)
          .catch(() => undefined);
      }

      return manifest;
    } catch (reason) {
      if (generation !== this.generation) throw reason;

      const message =
        reason instanceof Error ? reason.message : String(reason);

      this.replaceSnapshot({
        status: "error",
        documentId: normalizedId,
        revisionId: normalizedRevision,
        title: source.title.trim() || "Untitled document",
        manifest: previousManifest,
        error: message,
      });

      throw reason;
    }
  }

  clear(): void {
    this.generation += 1;
    const previousManifest = this.snapshot.manifest;

    this.playback.clear();
    this.replaceSnapshot(EMPTY_SNAPSHOT);

    if (previousManifest) {
      void this.core
        .dropManifest(previousManifest.handle)
        .catch(() => undefined);
    }
  }

  destroy(): void {
    this.clear();
    this.listeners.clear();
  }

  private replaceSnapshot(
    patch: Partial<ReadDocumentSessionSnapshot>,
  ): void {
    this.snapshot = {
      ...this.snapshot,
      ...patch,
    };

    for (const listener of this.listeners) listener();
  }
}
