import { useEffect, useRef, useState } from "react";
import {
  getContentProject,
  type ContentProject,
} from "../content/projectApi";
import {
  projectProgressFromPlayback,
  type ProjectProgressPayload,
} from "../content/projectProgressWriter";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useReadDocumentSnapshot } from "../runtime/useReadDocumentSnapshot";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { ReaderSurface } from "./ReaderSurface";
import { navigateTo } from "../routing/navigation";

function progressPercent(elapsedMs: number, durationMs: number): number {
  if (durationMs <= 0) return 0;
  return Math.min(100, Math.max(0, elapsedMs / durationMs * 100));
}

export function ProjectReaderPage({
  projectId,
}: {
  projectId: string;
}) {
  const runtime = useReadRuntime();
  const documentSnapshot = useReadDocumentSnapshot(runtime?.documents);
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const [project, setProject] = useState<ContentProject | null>(null);
  const [error, setError] = useState<string | null>(null);
  const restoredProjectRef = useRef<string | null>(null);
  const lastSavedAtRef = useRef(0);
  const latestProgressRef = useRef<{
    projectId: string;
    progress: ProjectProgressPayload;
  } | null>(null);
  const progressWriter = runtime?.progressWriter ?? null;

  useEffect(() => {
    if (!runtime || !projectId) return;

    let cancelled = false;
    runtime.documents.clear();
    restoredProjectRef.current = null;
    lastSavedAtRef.current = 0;
    latestProgressRef.current = null;
    setProject(null);
    setError(null);

    void getContentProject(projectId)
      .then(async (nextProject) => {
        if (cancelled) return;
        if (!nextProject.rawText?.trim()) {
          throw new Error("This project does not contain readable text.");
        }

        setProject(nextProject);

        const manifest = await runtime.documents.load({
          id: `project:${nextProject.id}`,
          revisionId:
            nextProject.textHash
            || nextProject.updatedAt
            || nextProject.id,
          title: nextProject.title,
          language: nextProject.language?.trim() || "en",
          text: nextProject.rawText,
        });

        if (cancelled) return;

        const progress = nextProject.progress;
        if (progress?.playbackRate) {
          runtime.playback.setSpeed(
            progress.playbackRate,
            { updatePreference: false },
          );
        }
        if (progress?.voiceId) {
          await runtime.playback.setVoice(
            progress.voiceId,
            { updatePreference: false },
          );
        }

        if (
          progress
          && progress.currentCharacterOffset > 0
          && restoredProjectRef.current !== nextProject.id
        ) {
          const elapsedMs = await runtime.core.logicalTimeForScalar(
            manifest.handle,
            progress.currentCharacterOffset,
          );
          if (!cancelled && elapsedMs !== null) {
            await runtime.playback.seek(elapsedMs);
          }
        }

        restoredProjectRef.current = nextProject.id;
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      });

    return () => {
      cancelled = true;
      const latest = latestProgressRef.current;
      if (latest) {
        runtime.progressWriter.queue(
          latest.projectId,
          latest.progress,
        );
        void runtime.progressWriter.flush();
      }
    };
  }, [projectId, runtime]);

  useEffect(() => {
    if (
      !runtime
      || !progressWriter
      || !project
      || playback.documentId !== `project:${project.id}`
    ) {
      return;
    }

    const progress = projectProgressFromPlayback(playback);
    latestProgressRef.current = {
      projectId: project.id,
      progress,
    };

    const now = Date.now();
    const terminal =
      playback.status === "paused"
      || playback.status === "ended"
      || playback.status === "error";

    if (!terminal && now - lastSavedAtRef.current < 5_000) {
      return;
    }

    lastSavedAtRef.current = now;
    progressWriter.queue(project.id, progress);
  }, [
    playback.activeSegmentIndex,
    playback.canonicalScalarCursor,
    playback.documentId,
    playback.durationMs,
    playback.elapsedMs,
    playback.speed,
    playback.status,
    playback.voiceId,
    progressWriter,
    project,
    runtime,
  ]);

  useEffect(() => {
    if (!progressWriter) return;

    const flushLatestProgress = () => {
      const latest = latestProgressRef.current;
      if (!latest) return;

      progressWriter.queue(latest.projectId, latest.progress);
      void progressWriter.flush();
    };
    const flushWhenHidden = () => {
      if (document.visibilityState === "hidden") {
        flushLatestProgress();
      }
    };

    window.addEventListener("pagehide", flushLatestProgress);
    document.addEventListener("visibilitychange", flushWhenHidden);

    return () => {
      window.removeEventListener("pagehide", flushLatestProgress);
      document.removeEventListener("visibilitychange", flushWhenHidden);
      flushLatestProgress();
    };
  }, [progressWriter]);

  if (!runtime) {
    return (
      <section className="product-page">
        <div className="page-loading" role="status" aria-live="polite">Starting the Read engine…</div>
      </section>
    );
  }

  const manifest =
    documentSnapshot.status === "ready"
    && documentSnapshot.documentId === `project:${projectId}`
      ? documentSnapshot.manifest
      : null;

  return (
    <section className="product-page reader-page">
      <header className="product-page-header reader-page-header">
        <div>
          <p className="eyebrow">Synced document</p>
          <h1>{project?.title ?? "Opening document…"}</h1>
          <p>
            Reading position, speed and voice are restored from your synced
            project while the Rust worker keeps one logical document timeline.
          </p>
        </div>
        <div className="page-header-actions">
          <button
            type="button"
            className="card-secondary"
            onClick={() => navigateTo("/app/library")}
          >
            Library
          </button>
          <button
            type="button"
            className="page-primary-action"
            onClick={() => navigateTo("/app/import")}
          >
            Import
          </button>
        </div>
      </header>

      {error || documentSnapshot.error ? (
        <div className="reader-empty-state">
          <p className="page-error" role="alert">
            {error ?? documentSnapshot.error}
          </p>
          <button
            type="button"
            className="page-primary-action"
            onClick={() => navigateTo("/app/library")}
          >
            Return to library
          </button>
        </div>
      ) : manifest ? (
        <ReaderSurface
          core={runtime.core}
          manifest={manifest}
          session={runtime.playback}
          snapshot={playback}
        />
      ) : (
        <div className="page-loading" role="status" aria-live="polite">
          Loading the synced document and building its reading timeline…
        </div>
      )}
    </section>
  );
}
