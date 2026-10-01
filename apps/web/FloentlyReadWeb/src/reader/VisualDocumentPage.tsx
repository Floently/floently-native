import { useEffect, useMemo, useRef, useState } from "react";
import {
  getLocalOriginalDocument,
  type LocalOriginalDocumentRecord,
} from "../content/localOriginalDocuments";
import {
  attachSemanticProjectToLocalOriginal,
  fileFromLocalOriginal,
} from "../content/originalSemanticAttachment";
import {
  getContentProject,
  type ContentProject,
} from "../content/projectApi";
import {
  createProjectProgressWriter,
  projectProgressFromPlayback,
  type ProjectProgressPayload,
} from "../content/projectProgressWriter";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { navigateTo } from "../routing/navigation";

function isPdf(record: LocalOriginalDocumentRecord): boolean {
  return (
    record.type === "application/pdf"
    || record.name.toLowerCase().endsWith(".pdf")
  );
}

function mergeOriginalSnapshot(
  current: LocalOriginalDocumentRecord | null,
  next: LocalOriginalDocumentRecord,
): LocalOriginalDocumentRecord {
  if (!current || current.id !== next.id) return next;

  const sameVisualFile =
    current.name === next.name
    && current.type === next.type
    && current.size === next.size
    && current.lastModified === next.lastModified
    && current.quickSignature === next.quickSignature;

  return sameVisualFile
    ? {
        ...next,
        // IndexedDB may materialize a fresh Blob object on every read. Keep
        // the already-rendered Blob so semantic polling cannot reload the PDF.
        blob: current.blob,
      }
    : next;
}

export function VisualDocumentPage({
  localDocumentId,
}: {
  localDocumentId: string;
}) {
  const runtime = useReadRuntime();
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const [record, setRecord] =
    useState<LocalOriginalDocumentRecord | null>(null);
  const [project, setProject] = useState<ContentProject | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [semanticBusy, setSemanticBusy] = useState(false);
  const [semanticWaitExpired, setSemanticWaitExpired] = useState(false);
  const [semanticLoadRevision, setSemanticLoadRevision] = useState(0);
  const lastSavedAtRef = useRef(0);
  const latestProgressRef = useRef<{
    projectId: string;
    progress: ProjectProgressPayload;
  } | null>(null);
  const progressWriterRef = useRef<
    ReturnType<typeof createProjectProgressWriter> | null
  >(null);

  if (!progressWriterRef.current) {
    progressWriterRef.current = createProjectProgressWriter();
  }
  const progressWriter = progressWriterRef.current;

  const objectUrl = useMemo(
    () => record ? URL.createObjectURL(record.blob) : null,
    [record?.blob],
  );

  useEffect(() => {
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [objectUrl]);

  useEffect(() => {
    let cancelled = false;
    let timer: number | null = null;
    let attempts = 0;

    const refresh = async () => {
      try {
        const next = await getLocalOriginalDocument(localDocumentId);
        if (cancelled) return;

        if (!next) {
          throw new Error(
            "This original document is no longer available on this device.",
          );
        }

        setRecord((current) =>
          mergeOriginalSnapshot(current, next),
        );

        if (next.projectId) {
          setSemanticWaitExpired(false);
        } else if (attempts < 40) {
          attempts += 1;
          timer = window.setTimeout(() => {
            void refresh();
          }, 1_500);
        } else {
          setSemanticWaitExpired(true);
        }
      } catch (reason) {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      }
    };

    void refresh();

    return () => {
      cancelled = true;
      if (timer !== null) window.clearTimeout(timer);
    };
  }, [localDocumentId]);

  useEffect(() => {
    if (!runtime || !record?.projectId) return;

    let cancelled = false;
    lastSavedAtRef.current = 0;
    latestProgressRef.current = null;

    void getContentProject(record.projectId)
      .then(async (nextProject) => {
        if (cancelled || !nextProject.rawText?.trim()) return;

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

        if (nextProject.progress?.playbackRate) {
          runtime.playback.setSpeed(
            nextProject.progress.playbackRate,
            { updatePreference: false },
          );
        }
        if (nextProject.progress?.voiceId) {
          await runtime.playback.setVoice(
            nextProject.progress.voiceId,
            { updatePreference: false },
          );
        }

        if (nextProject.progress?.currentCharacterOffset) {
          const elapsedMs = await runtime.core.logicalTimeForScalar(
            manifest.handle,
            nextProject.progress.currentCharacterOffset,
          );
          if (!cancelled && elapsedMs !== null) {
            await runtime.playback.seek(elapsedMs);
          }
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [record?.projectId, runtime, semanticLoadRevision]);

  useEffect(() => {
    if (
      !project
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
  ]);

  useEffect(() => {
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

  async function retrySemanticLayer(): Promise<void> {
    if (!record || semanticBusy) return;

    setSemanticBusy(true);
    setError(null);

    try {
      const nextProject =
        await attachSemanticProjectToLocalOriginal(
          record.id,
          fileFromLocalOriginal(record),
        );
      const refreshed =
        await getLocalOriginalDocument(record.id);

      setProject(null);
      setRecord((current) =>
        mergeOriginalSnapshot(
          current,
          refreshed ?? {
            ...record,
            projectId: nextProject.id,
            updatedAt: Date.now(),
          },
        ),
      );
      setSemanticWaitExpired(false);
      setSemanticLoadRevision((value) => value + 1);
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "The reading layer could not be prepared.",
      );
      setSemanticWaitExpired(true);
    } finally {
      setSemanticBusy(false);
    }
  }

  const semanticReady = Boolean(
    project
    && playback.documentId === `project:${project.id}`,
  );

  return (
    <section className="visual-document-page">
      <header className="visual-document-toolbar">
        <div className="visual-document-title">
          <button
            type="button"
            className="card-secondary"
            onClick={() => navigateTo("/app/library")}
          >
            Library
          </button>
          <div>
            <strong>{record?.name ?? "Opening document…"}</strong>
            <span>
              {semanticReady
                ? "Original pages · reading layer ready"
                : semanticBusy
                  ? "Original pages · retrying reading layer"
                  : record?.projectId
                    ? "Original pages · preparing reading layer"
                    : semanticWaitExpired
                      ? "Original pages · reading layer needs attention"
                      : "Original pages · extracting reading layer"}
            </span>
          </div>
        </div>

        <div className="visual-document-actions">
          {objectUrl ? (
            <a
              className="card-secondary"
              href={objectUrl}
              download={record?.name ?? "document"}
            >
              Download original
            </a>
          ) : null}
          {!semanticReady && record && (semanticWaitExpired || error) ? (
            <button
              type="button"
              className="card-secondary"
              disabled={semanticBusy}
              onClick={() => void retrySemanticLayer()}
            >
              {semanticBusy ? "Retrying…" : "Retry reading layer"}
            </button>
          ) : null}
          <button
            type="button"
            className="page-primary-action"
            disabled={!semanticReady}
            onClick={() => void runtime?.playback.togglePlayPause()}
          >
            {["playing", "buffering", "preparing"].includes(playback.status)
              ? "Pause reading"
              : semanticReady
                ? "Read document"
                : "Preparing…"}
          </button>
        </div>
      </header>

      {error ? (
        <div className="browser-error" role="alert">
          <span>{error}</span>
        </div>
      ) : null}

      <div className="visual-document-frame">
        {!record || !objectUrl ? (
          <div className="browser-display-gate">
            <span className="browser-display-spinner" aria-hidden="true" />
            <strong>Opening the original document…</strong>
          </div>
        ) : isPdf(record) ? (
          <iframe
            src={objectUrl}
            title={record.name}
            className="visual-document-iframe"
          />
        ) : (
          <div className="visual-document-unsupported">
            <h2>{record.name}</h2>
            <p>
              The original file is preserved on this device. This browser
              cannot render the format inline, but the semantic reading layer
              can still be used when extraction completes.
            </p>
            <a
              className="page-primary-action"
              href={objectUrl}
              download={record.name}
            >
              Open original file
            </a>
          </div>
        )}
      </div>
    </section>
  );
}
