import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type KeyboardEvent,
  type PointerEvent,
} from "react";
import type { ReadingManifestSummary } from "./readCore.types";
import { ReaderSurface } from "./reader/ReaderSurface";
import { useWebPlaybackSnapshot } from "./playback/useWebPlaybackSnapshot";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "./playback/webPlaybackSession";
import { useReadRuntime } from "./runtime/ReadRuntimeContext";
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
  const runtime = useReadRuntime();
  const playback = useWebPlaybackSnapshot(runtime?.playback);

  const [text, setText] = useState(SAMPLE_TEXT);
  const [title, setTitle] = useState("Floently Read sample");
  const [manifest, setManifest] = useState<ReadingManifestSummary | null>(null);
  const [status, setStatus] = useState("Ready to index");
  const [error, setError] = useState<string | null>(null);
  const [isBuilding, setIsBuilding] = useState(false);
  const activeManifestHandle = useRef<string | null>(null);

  useEffect(() => {
    return () => {
      const handle = activeManifestHandle.current;
      if (!runtime || !handle) return;

      runtime.playback.clear();
      void runtime.core.dropManifest(handle).catch(() => undefined);
      activeManifestHandle.current = null;
    };
  }, [runtime]);

  function releaseCurrentDocument(): void {
    const handle = activeManifestHandle.current;

    if (runtime) {
      runtime.playback.clear();
      if (handle) {
        void runtime.core.dropManifest(handle).catch(() => undefined);
      }
    }

    activeManifestHandle.current = null;
    setManifest(null);
  }

  async function buildManifest(): Promise<void> {
    if (!runtime) {
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
      const result = await runtime.core.buildManifest({
        documentId: "read-web-local",
        revisionId: createRevisionId(),
        title: title.trim() || "Untitled document",
        language: "en",
        text,
      });

      if (previousHandle && previousHandle !== result.handle) {
        void runtime.core.dropManifest(previousHandle).catch(() => undefined);
      }

      activeManifestHandle.current = result.handle;
      setManifest(result);
      runtime.playback.loadDocument(result);

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
      releaseCurrentDocument();
      setText(value);
      setTitle(file.name.replace(/\.[^.]+$/, "") || file.name);
      setStatus(`Loaded ${file.name}. Ready to index.`);
      setError(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      event.target.value = "";
    }
  }

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
          Core through WebAssembly. A durable browser PlaybackSession now owns
          media state above the screen tree.
        </p>

        <div className="engine-strip" aria-label="Current web architecture">
          <span>React + TypeScript</span>
          <span className="engine-arrow">→</span>
          <span>PlaybackSession + Worker</span>
          <span className="engine-arrow">→</span>
          <span>Rust/WASM + browser media</span>
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
              releaseCurrentDocument();
              setText(event.target.value);
              setStatus("Text changed. Re-index to refresh the manifest.");
            }}
          />

          <div className="editor-footer">
            <span>{text.length.toLocaleString()} browser characters</span>
            <button
              className="primary-button"
              disabled={isBuilding || !runtime}
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
                  label="Core"
                  value="Rust/WASM"
                />
                <Metric
                  label="Index build"
                  value={`${manifest.buildMs.toFixed(1)} ms`}
                />
              </div>

              <div className="mapping-card">
                <p>
                  The document is exposed as one continuous reading timeline.
                  Internal transport regions stay hidden from the public media
                  identity while the nearby reading surface is loaded on demand.
                </p>
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

      {runtime && manifest ? (
        <ReaderSurface
          core={runtime.core}
          manifest={manifest}
          session={runtime.playback}
          snapshot={playback}
        />
      ) : null}

      {runtime && manifest ? (
        <PlaybackDock
          session={runtime.playback}
          snapshot={playback}
        />
      ) : null}
    </main>
  );
}

function PlaybackDock({
  session,
  snapshot,
}: {
  session: WebPlaybackSession;
  snapshot: WebPlaybackSnapshot;
}) {
  const [seekDraftMs, setSeekDraftMs] = useState<number | null>(null);
  const displayedPositionMs = seekDraftMs ?? snapshot.elapsedMs;
  const isTransportActive = [
    "preparing",
    "buffering",
    "playing",
  ].includes(snapshot.status);

  function commitSeek(): void {
    if (seekDraftMs === null) return;
    const target = seekDraftMs;
    setSeekDraftMs(null);
    void session.seek(target);
  }

  function onSliderPointerUp(
    _event: PointerEvent<HTMLInputElement>,
  ): void {
    commitSeek();
  }

  function onSliderKeyUp(
    event: KeyboardEvent<HTMLInputElement>,
  ): void {
    if (
      event.key.startsWith("Arrow")
      || event.key === "Home"
      || event.key === "End"
      || event.key === "PageUp"
      || event.key === "PageDown"
    ) {
      commitSeek();
    }
  }

  return (
    <section
      className="player-dock"
      aria-label="Floently Read document player"
    >
      <div className="player-document">
        <p className="panel-kicker">Document playback</p>
        <strong>{snapshot.title ?? "Floently Read"}</strong>
        <span>
          {snapshot.status}
          {" · "}
          {snapshot.voiceId}
          {snapshot.bufferedAheadMs > 0
            ? ` · ${formatDuration(snapshot.bufferedAheadMs)} ready ahead`
            : ""}
        </span>
      </div>

      <div className="player-center">
        <div className="transport-row">
          <button
            className="transport-button secondary"
            onClick={() => void session.seekBy(-15_000)}
            aria-label="Back 15 seconds"
          >
            −15
          </button>
          <button
            className="transport-button primary"
            onClick={() => void session.togglePlayPause()}
            aria-label={isTransportActive ? "Pause" : "Play"}
          >
            {isTransportActive ? "Pause" : "Play"}
          </button>
          <button
            className="transport-button secondary"
            onClick={() => void session.seekBy(15_000)}
            aria-label="Forward 15 seconds"
          >
            +15
          </button>
        </div>

        <div className="player-timeline">
          <span>{formatDuration(displayedPositionMs)}</span>
          <input
            className="player-slider"
            type="range"
            min="0"
            max={Math.max(1, snapshot.durationMs)}
            step="1000"
            value={clampForInput(
              displayedPositionMs,
              snapshot.durationMs,
            )}
            onChange={(event) =>
              setSeekDraftMs(Number(event.target.value))
            }
            onPointerUp={onSliderPointerUp}
            onKeyUp={onSliderKeyUp}
            onBlur={commitSeek}
            aria-label="Document position"
          />
          <span>{formatDuration(snapshot.durationMs)}</span>
        </div>
      </div>

      <div className="player-options">
        <label htmlFor="playback-speed">Speed</label>
        <select
          id="playback-speed"
          value={snapshot.speed}
          onChange={(event) =>
            session.setSpeed(Number(event.target.value))
          }
        >
          {[0.75, 1, 1.25, 1.5, 2, 2.5, 3].map((speed) => (
            <option value={speed} key={speed}>
              {speed}×
            </option>
          ))}
        </select>
        {snapshot.error ? (
          <span className="player-error" title={snapshot.error}>
            Playback error
          </span>
        ) : null}
      </div>
    </section>
  );
}

function clampForInput(value: number, maximum: number): number {
  return Math.min(Math.max(0, value), Math.max(1, maximum));
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
