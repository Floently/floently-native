import { useEffect, useRef, useState } from "react";
import type { ChangeEvent } from "react";
import { ReadCoreWorkerClient } from "./readCore.client";
import type {
  ReadingManifestSummary,
  SegmentPosition,
} from "./readCore.types";
import "./styles.css";

const SAMPLE_TEXT = `Floently Read is being rebuilt around one logical document timeline.

The browser should understand an entire long document quickly, while rendering only what the person needs to see. Audio generation, buffering, highlighting, and playback can then advance independently without exposing hidden implementation segments.

This page is the first web consumer of the shared Rust Read Core. The same canonical document semantics will be used by Swift on iOS and Kotlin on Android.`;

function formatDuration(milliseconds: number): string {
  const totalSeconds = Math.max(0, Math.round(milliseconds / 1_000));
  const hours = Math.floor(totalSeconds / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return [hours, minutes, seconds]
      .map((value, index) =>
        index === 0 ? String(value) : String(value).padStart(2, "0"),
      )
      .join(":");
  }

  return [minutes, seconds]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");
}

function createRevisionId(): string {
  return `web-${Date.now().toString(36)}`;
}

export default function App() {
  const [client, setClient] = useState<ReadCoreWorkerClient | null>(null);
  const [text, setText] = useState(SAMPLE_TEXT);
  const [title, setTitle] = useState("Floently Read sample");
  const [manifest, setManifest] = useState<ReadingManifestSummary | null>(null);
  const [progress, setProgress] = useState(0.35);
  const [position, setPosition] = useState<SegmentPosition | null>(null);
  const [prefetch, setPrefetch] = useState<number[]>([]);
  const [status, setStatus] = useState("Ready to index");
  const [error, setError] = useState<string | null>(null);
  const [isBuilding, setIsBuilding] = useState(false);
  const activeManifestHandle = useRef<string | null>(null);

  useEffect(() => {
    const nextClient = new ReadCoreWorkerClient();
    setClient(nextClient);

    return () => {
      const handle = activeManifestHandle.current;
      if (handle) {
        void nextClient.dropManifest(handle);
      }
      nextClient.terminate();
    };
  }, []);

  useEffect(() => {
    if (!client || !manifest) {
      setPosition(null);
      setPrefetch([]);
      return;
    }

    let cancelled = false;

    Promise.all([
      client.positionForProgress(manifest.handle, progress),
      client.positionForProgress(manifest.handle, progress).then((mapped) =>
        client.prefetchIndexes(manifest.handle, mapped.index),
      ),
    ])
      .then(([mapped, indexes]) => {
        if (cancelled) return;
        setPosition(mapped);
        setPrefetch(indexes);
      })
      .catch((reason: unknown) => {
        if (cancelled) return;
        setError(reason instanceof Error ? reason.message : String(reason));
      });

    return () => {
      cancelled = true;
    };
  }, [client, manifest, progress]);

  async function buildManifest(): Promise<void> {
    if (!client) {
      setError("Read Core is still initializing.");
      return;
    }

    if (!text.trim()) {
      setError("Add text before indexing the document.");
      return;
    }

    setIsBuilding(true);
    setError(null);
    setStatus("Indexing in Rust/WASM worker…");

    const previousHandle = activeManifestHandle.current;

    try {
      const result = await client.buildManifest({
        documentId: "read-web-local",
        revisionId: createRevisionId(),
        title: title.trim() || "Untitled document",
        language: "en",
        text,
      });

      if (previousHandle && previousHandle !== result.handle) {
        void client.dropManifest(previousHandle);
      }

      activeManifestHandle.current = result.handle;
      setManifest(result);
      setProgress(0);
      setStatus(
        `Indexed ${result.wordCount.toLocaleString()} words in ${result.buildMs.toFixed(1)} ms`,
      );
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
      setStatus("Indexing failed");
    } finally {
      setIsBuilding(false);
    }
  }

  async function onFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const value = await file.text();
      const handle = activeManifestHandle.current;
      if (handle && client) {
        void client.dropManifest(handle);
        activeManifestHandle.current = null;
      }

      setText(value);
      setTitle(file.name.replace(/\.[^.]+$/, "") || file.name);
      setManifest(null);
      setStatus(`Loaded ${file.name}. Ready to index.`);
      setError(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      event.target.value = "";
    }
  }

  const logicalElapsedMs = manifest
    ? manifest.estimatedSourceDurationMs * progress
    : 0;

  return (
    <main className="page-shell">
      <section className="hero">
        <div className="brand-row">
          <span className="brand-mark" aria-hidden="true">F</span>
          <div>
            <p className="eyebrow">Floently Read · next generation</p>
            <h1>One document. One timeline.</h1>
          </div>
        </div>

        <p className="hero-copy">
          React renders the interface. A Web Worker runs the shared Rust Read
          Core through WebAssembly, so long-document indexing does not block the
          page.
        </p>

        <div className="engine-strip" aria-label="Current web architecture">
          <span>React + TypeScript</span>
          <span className="engine-arrow">→</span>
          <span>Web Worker</span>
          <span className="engine-arrow">→</span>
          <span>Rust/WASM Read Core</span>
        </div>
      </section>

      <section className="workspace" aria-label="Document indexing workspace">
        <div className="editor-panel">
          <div className="panel-header">
            <div>
              <p className="panel-kicker">Document source</p>
              <h2>Import or paste text</h2>
            </div>
            <label className="file-button">
              Open text file
              <input
                type="file"
                accept=".txt,.md,text/plain,text/markdown"
                onChange={onFileSelected}
              />
            </label>
          </div>

          <label className="field-label" htmlFor="document-title">
            Title
          </label>
          <input
            id="document-title"
            className="title-input"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />

          <label className="field-label" htmlFor="document-text">
            Text
          </label>
          <textarea
            id="document-text"
            className="document-input"
            value={text}
            onChange={(event) => {
              const handle = activeManifestHandle.current;
              if (handle && client) {
                void client.dropManifest(handle);
                activeManifestHandle.current = null;
              }

              setText(event.target.value);
              setManifest(null);
              setStatus("Text changed. Re-index to refresh the manifest.");
            }}
          />

          <div className="editor-footer">
            <span>{text.length.toLocaleString()} browser characters</span>
            <button
              className="primary-button"
              disabled={isBuilding || !client}
              onClick={() => void buildManifest()}
            >
              {isBuilding ? "Indexing…" : "Build ReadingManifest"}
            </button>
          </div>
        </div>

        <aside className="manifest-panel" aria-live="polite">
          <div className="panel-header">
            <div>
              <p className="panel-kicker">Shared engine result</p>
              <h2>ReadingManifest v1</h2>
            </div>
            <span className={manifest ? "status-dot ready" : "status-dot"} />
          </div>

          <p className="status-line">{status}</p>
          {error ? <p className="error-line">{error}</p> : null}

          {manifest ? (
            <>
              <div className="metrics-grid">
                <Metric label="Words" value={manifest.wordCount.toLocaleString()} />
                <Metric
                  label="Duration"
                  value={formatDuration(manifest.estimatedSourceDurationMs)}
                />
                <Metric
                  label="Segments"
                  value={manifest.segmentCount.toLocaleString()}
                />
                <Metric
                  label="Rust build"
                  value={`${manifest.buildMs.toFixed(1)} ms`}
                />
              </div>

              <div className="timeline-card">
                <div className="timeline-header">
                  <span>Logical document position</span>
                  <strong>{Math.round(progress * 100)}%</strong>
                </div>
                <input
                  className="progress-slider"
                  type="range"
                  min="0"
                  max="1"
                  step="0.001"
                  value={progress}
                  onChange={(event) => setProgress(Number(event.target.value))}
                />
                <div className="timeline-footer">
                  <span>{formatDuration(logicalElapsedMs)}</span>
                  <span>{formatDuration(manifest.estimatedSourceDurationMs)}</span>
                </div>
              </div>

              <div className="mapping-card">
                <p>
                  Rust maps that public position to hidden segment{" "}
                  <strong>{position?.index ?? "…"}</strong>
                  {position
                    ? ` at ${Math.round(position.fraction * 100)}% inside the segment`
                    : ""}
                  .
                </p>
                <p>
                  120-second prefetch plan:{" "}
                  <strong>
                    {prefetch.length > 0
                      ? prefetch.map((index) => `#${index}`).join(", ")
                      : "none needed"}
                  </strong>
                </p>
              </div>

              <div className="segment-list">
                <div className="segment-list-header">
                  <span>First hidden transport units</span>
                  <span>not user-visible media items</span>
                </div>
                {manifest.firstSegments.map((segment) => (
                  <div className="segment-row" key={segment.id}>
                    <span>#{segment.index}</span>
                    <span>{segment.wordCount} words</span>
                    <span>
                      {formatDuration(segment.estimatedSourceDurationMs)}
                    </span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="empty-state">
              <span className="empty-symbol">⌁</span>
              <p>
                Index the document to calculate its whole-document timeline
                without rendering or synthesizing all of it.
              </p>
            </div>
          )}
        </aside>
      </section>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
