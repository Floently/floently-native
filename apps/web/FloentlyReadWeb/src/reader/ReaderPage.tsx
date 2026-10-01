import { useEffect, useState } from "react";
import { getLibraryDocument } from "../library/documentRepository";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { ReaderSurface } from "./ReaderSurface";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useReadDocumentSnapshot } from "../runtime/useReadDocumentSnapshot";
import { navigateTo } from "../routing/navigation";

export function ReaderPage({
  documentId,
}: {
  documentId: string | null;
}) {
  const runtime = useReadRuntime();
  const documentSnapshot = useReadDocumentSnapshot(runtime?.documents);
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (!runtime || !documentId) return;

    let cancelled = false;
    setLoadError(null);

    void getLibraryDocument(documentId)
      .then(async (document) => {
        if (cancelled) return;

        if (!document) {
          throw new Error("This document is not in the browser library.");
        }

        await runtime.documents.load({
          id: document.id,
          revisionId: document.updatedAt,
          title: document.title,
          language: document.language,
          text: document.text,
        });
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setLoadError(
            reason instanceof Error ? reason.message : String(reason),
          );
        }
      });

    return () => {
      cancelled = true;
    };
  }, [documentId, runtime]);

  if (!runtime) {
    return (
      <section className="product-page">
        <div className="page-loading">Starting the Read engine…</div>
      </section>
    );
  }

  const activeManifest =
    documentSnapshot.status === "ready"
    && (
      !documentId
      || documentSnapshot.documentId === documentId
    )
      ? documentSnapshot.manifest
      : null;

  return (
    <section className="product-page reader-page">
      <header className="product-page-header reader-page-header">
        <div>
          <p className="eyebrow">Reader</p>
          <h1>{documentSnapshot.title ?? "Open a document"}</h1>
          <p>
            Text follows the same logical timeline as audio. You can browse
            away from playback and rejoin it without changing the media item.
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

      {loadError || documentSnapshot.error ? (
        <div className="reader-empty-state">
          <p className="page-error" role="alert">
            {loadError ?? documentSnapshot.error}
          </p>
          <button
            type="button"
            className="page-primary-action"
            onClick={() => navigateTo("/app/library")}
          >
            Return to library
          </button>
        </div>
      ) : documentId && !requestedDocumentReady ? (
        <div className="page-loading">
          Building the document timeline in the Rust worker…
        </div>
      ) : requestedDocumentReady ? (
        <ReaderSurface
          core={runtime.core}
          manifest={documentSnapshot.manifest}
          session={runtime.playback}
          snapshot={playback}
        />
      ) : (
        <div className="reader-empty-state">
          <span aria-hidden="true">⌁</span>
          <h2>No document is open yet.</h2>
          <p>
            Choose something from your library or import a document to start
            reading.
          </p>
          <div>
            <button
              type="button"
              className="card-secondary"
              onClick={() => navigateTo("/app/library")}
            >
              Open library
            </button>
            <button
              type="button"
              className="page-primary-action"
              onClick={() => navigateTo("/app/import")}
            >
              Import document
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
