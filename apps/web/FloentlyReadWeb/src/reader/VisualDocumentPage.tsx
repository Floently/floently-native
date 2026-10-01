import { useEffect, useMemo, useState } from "react";
import {
  getLocalOriginalDocument,
  type LocalOriginalDocumentRecord,
} from "../content/localOriginalDocuments";
import {
  getContentProject,
  type ContentProject,
} from "../content/projectApi";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { navigateTo } from "../routing/navigation";

function isPdf(record: LocalOriginalDocumentRecord): boolean {
  return (
    record.type === "application/pdf"
    || record.name.toLowerCase().endsWith(".pdf")
  );
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

  const objectUrl = useMemo(
    () => record ? URL.createObjectURL(record.blob) : null,
    [record],
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

        setRecord(next);

        if (!next.projectId && attempts < 20) {
          attempts += 1;
          timer = window.setTimeout(() => {
            void refresh();
          }, 1_500);
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
          runtime.playback.setSpeed(nextProject.progress.playbackRate);
        }
        if (nextProject.progress?.voiceId) {
          await runtime.playback.setVoice(nextProject.progress.voiceId);
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
  }, [record?.projectId, runtime]);

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
                : record?.projectId
                  ? "Original pages · preparing reading layer"
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
