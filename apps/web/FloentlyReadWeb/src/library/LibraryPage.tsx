import { useEffect, useState } from "react";
import {
  deleteLibraryDocument,
  listLibraryDocuments,
  type LibraryDocument,
} from "./documentRepository";
import { navigateTo } from "../routing/navigation";

function formatUpdatedAt(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function LibraryPage() {
  const [documents, setDocuments] = useState<LibraryDocument[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">(
    "loading",
  );
  const [error, setError] = useState<string | null>(null);

  async function refresh() {
    try {
      setStatus("loading");
      setError(null);
      setDocuments(await listLibraryDocuments());
      setStatus("ready");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
      setStatus("error");
    }
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function removeDocument(document: LibraryDocument) {
    if (!window.confirm(`Remove “${document.title}” from this browser?`)) {
      return;
    }

    try {
      await deleteLibraryDocument(document.id);
      await refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    }
  }

  return (
    <section className="product-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Your reading</p>
          <h1>Library</h1>
          <p>
            Documents imported into this next-generation build are stored
            locally in this browser while the cloud library API is migrated.
          </p>
        </div>
        <button
          className="page-primary-action"
          type="button"
          onClick={() => navigateTo("/app/import")}
        >
          Import document
        </button>
      </header>

      {error ? <p className="page-error" role="alert">{error}</p> : null}

      {status === "loading" ? (
        <div className="page-loading">Loading your browser library…</div>
      ) : documents.length === 0 ? (
        <div className="library-empty">
          <span aria-hidden="true">⌁</span>
          <h2>Your library is ready for its first document.</h2>
          <p>
            Import text or Markdown now. PDF, EPUB and web capture will use
            dedicated ingestion adapters in later migration slices.
          </p>
          <button
            type="button"
            className="page-primary-action"
            onClick={() => navigateTo("/app/import")}
          >
            Import your first document
          </button>
        </div>
      ) : (
        <div className="library-grid">
          {documents.map((document) => (
            <article className="library-card" key={document.id}>
              <div className="library-card-icon" aria-hidden="true">
                {document.sourceType === "markdown" ? "M" : "T"}
              </div>
              <div className="library-card-body">
                <p className="library-card-type">
                  {document.sourceType === "markdown"
                    ? "Markdown"
                    : "Text document"}
                </p>
                <h2>{document.title}</h2>
                <p>{document.text.slice(0, 180)}</p>
                <span>
                  Updated {formatUpdatedAt(document.updatedAt)}
                </span>
              </div>
              <div className="library-card-actions">
                <button
                  type="button"
                  className="card-primary"
                  onClick={() =>
                    navigateTo(
                      `/app/reader/${encodeURIComponent(document.id)}`,
                    )
                  }
                >
                  Read
                </button>
                <button
                  type="button"
                  className="card-secondary"
                  onClick={() => void removeDocument(document)}
                >
                  Remove
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
