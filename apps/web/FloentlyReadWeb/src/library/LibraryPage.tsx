import { useEffect, useMemo, useRef, useState } from "react";
import {
  deleteLibraryDocument,
  listLibraryDocuments,
  type LibraryDocument,
} from "./documentRepository";
import {
  deleteContentProject,
  listContentProjects,
  type ContentProject,
} from "../content/projectApi";
import {
  listLocalOriginalDocuments,
  removeLocalOriginalDocument,
  type LocalOriginalDocumentRecord,
} from "../content/localOriginalDocuments";
import { navigateTo } from "../routing/navigation";
import { useAuthState } from "../auth/useAuthState";
import { LibraryOwnerGate } from "./libraryOwnerGate";

function formatUpdatedAt(value: string | number): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function sourceLabel(sourceType: string): string {
  const normalized = sourceType.trim().toLowerCase();
  if (normalized === "pdf") return "PDF";
  if (normalized === "epub") return "EPUB";
  if (normalized === "docx") return "DOCX";
  if (normalized === "html") return "HTML";
  if (normalized === "markdown" || normalized === "md") return "Markdown";
  if (normalized === "text" || normalized === "txt") return "Text";
  return sourceType || "Document";
}

function sourceGlyph(sourceType: string): string {
  const normalized = sourceType.trim().toLowerCase();
  if (normalized === "pdf") return "P";
  if (normalized === "epub") return "E";
  if (normalized === "docx") return "D";
  if (normalized === "markdown" || normalized === "md") return "M";
  if (normalized === "html") return "H";
  return "T";
}

function localOriginalType(record: LocalOriginalDocumentRecord): string {
  if (
    record.type === "application/pdf"
    || record.name.toLowerCase().endsWith(".pdf")
  ) {
    return "pdf";
  }

  const extension = record.name.split(".").pop()?.toLowerCase();
  return extension || "file";
}

export function LibraryPage() {
  const auth = useAuthState();
  const ownerId = auth.session?.user.id ?? null;
  const ownerGateRef = useRef<LibraryOwnerGate | null>(null);
  // Advance the owner epoch during render so promises from the previous owner
  // are already stale before the new owner's effects start asynchronous work.
  if (!ownerGateRef.current) {
    ownerGateRef.current = new LibraryOwnerGate(ownerId);
  } else {
    ownerGateRef.current.setOwner(ownerId);
  }
  const ownerGate = ownerGateRef.current;

  const [projects, setProjects] = useState<ContentProject[]>([]);
  const [originals, setOriginals] =
    useState<LocalOriginalDocumentRecord[]>([]);
  const [legacyDocuments, setLegacyDocuments] =
    useState<LibraryDocument[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] =
    useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  async function refresh(): Promise<void> {
    const refreshToken = ownerGate.beginRefresh(ownerId);
    if (!refreshToken) return;

    setStatus("loading");
    setError(null);

    const [cloudResult, originalsResult, legacyResult] =
      await Promise.allSettled([
        listContentProjects(100, 0),
        listLocalOriginalDocuments(refreshToken.ownerId, 100),
        listLibraryDocuments(refreshToken.ownerId),
      ]);

    if (!ownerGate.isRefreshCurrent(refreshToken)) {
      return;
    }

    if (cloudResult.status === "fulfilled") {
      setProjects(cloudResult.value);
    }
    if (originalsResult.status === "fulfilled") {
      setOriginals(originalsResult.value);
    }
    if (legacyResult.status === "fulfilled") {
      setLegacyDocuments(legacyResult.value);
    }

    const failures = [
      cloudResult,
      originalsResult,
      legacyResult,
    ].filter((result) => result.status === "rejected");

    if (failures.length === 3) {
      setStatus("error");
      setError("Your library could not be loaded.");
      return;
    }

    if (failures.length > 0) {
      setError(
        "Some library information could not be refreshed. Available documents are still shown.",
      );
    }

    setStatus("ready");
  }

  useEffect(() => {
    setProjects([]);
    setOriginals([]);
    setLegacyDocuments([]);
    setError(null);
    setStatus(ownerId ? "loading" : "ready");

    if (ownerId) {
      void refresh();
    }
  }, [ownerId]);

  const normalizedQuery = query.trim().toLowerCase();

  const linkedProjectIds = useMemo(
    () =>
      new Set(
        originals
          .map((record) => record.projectId)
          .filter((id): id is string => Boolean(id)),
      ),
    [originals],
  );

  const filteredProjects = useMemo(
    () =>
      projects.filter(
        (project) =>
          !linkedProjectIds.has(project.id)
          && (
            !normalizedQuery
            || project.title.toLowerCase().includes(normalizedQuery)
          ),
      ),
    [linkedProjectIds, normalizedQuery, projects],
  );

  const filteredOriginals = useMemo(
    () =>
      originals.filter(
        (record) =>
          !normalizedQuery
          || record.name.toLowerCase().includes(normalizedQuery),
      ),
    [normalizedQuery, originals],
  );

  const filteredLegacy = useMemo(
    () =>
      legacyDocuments.filter(
        (document) =>
          !normalizedQuery
          || document.title.toLowerCase().includes(normalizedQuery),
      ),
    [legacyDocuments, normalizedQuery],
  );

  const continueProjects = useMemo(
    () =>
      projects
        .filter(
          (project) =>
            (project.progress?.progressPercent ?? 0) > 0
            && (project.progress?.progressPercent ?? 0) < 100,
        )
        .sort((left, right) =>
          (right.progress?.updatedAt ?? "").localeCompare(
            left.progress?.updatedAt ?? "",
          ),
        )
        .slice(0, 4),
    [projects],
  );

  const totalItems =
    filteredProjects.length
    + filteredOriginals.length
    + filteredLegacy.length;

  async function removeProject(project: ContentProject): Promise<void> {
    if (!window.confirm(`Delete “${project.title}” from your synced library?`)) {
      return;
    }

    const ownerToken = ownerGate.capture(ownerId);
    if (!ownerToken) return;

    try {
      await deleteContentProject(project.id);
      if (!ownerGate.isOwnerCurrent(ownerToken)) return;

      ownerGate.invalidateRefresh(ownerToken.ownerId);
      setProjects((current) =>
        current.filter((item) => item.id !== project.id),
      );
    } catch (reason) {
      if (ownerGate.isOwnerCurrent(ownerToken)) {
        setError(reason instanceof Error ? reason.message : String(reason));
      }
    }
  }

  async function removeOriginal(
    record: LocalOriginalDocumentRecord,
  ): Promise<void> {
    if (
      !window.confirm(
        `Remove the original “${record.name}” from this device?`,
      )
    ) {
      return;
    }

    const ownerToken = ownerGate.capture(ownerId);
    if (!ownerToken) return;

    try {
      await removeLocalOriginalDocument(ownerToken.ownerId, record.id);
      if (!ownerGate.isOwnerCurrent(ownerToken)) return;

      ownerGate.invalidateRefresh(ownerToken.ownerId);
      setOriginals((current) =>
        current.filter((item) => item.id !== record.id),
      );
    } catch (reason) {
      if (ownerGate.isOwnerCurrent(ownerToken)) {
        setError(reason instanceof Error ? reason.message : String(reason));
      }
    }
  }

  async function removeLegacy(document: LibraryDocument): Promise<void> {
    if (!window.confirm(`Remove “${document.title}” from this browser?`)) {
      return;
    }

    const ownerToken = ownerGate.capture(ownerId);
    if (!ownerToken) return;

    try {
      await deleteLibraryDocument(ownerToken.ownerId, document.id);
      if (!ownerGate.isOwnerCurrent(ownerToken)) return;

      ownerGate.invalidateRefresh(ownerToken.ownerId);
      setLegacyDocuments((current) =>
        current.filter((item) => item.id !== document.id),
      );
    } catch (reason) {
      if (ownerGate.isOwnerCurrent(ownerToken)) {
        setError(reason instanceof Error ? reason.message : String(reason));
      }
    }
  }

  return (
    <section className="product-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Your reading</p>
          <h1>Library</h1>
          <p>
            Synced projects and original visual files live together. PDFs keep
            their device copy for page-faithful viewing while extracted reading
            text stays in the linked cloud project.
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

      <div className="library-toolbar">
        <label>
          <span className="visually-hidden">Search library</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search library"
          />
        </label>
        <button
          type="button"
          className="card-secondary"
          disabled={status === "loading"}
          onClick={() => void refresh()}
        >
          Refresh
        </button>
      </div>

      {error ? <p className="page-error" role="alert">{error}</p> : null}

      {continueProjects.length > 0 && !normalizedQuery ? (
        <section className="library-continue-section">
          <div className="library-section-heading">
            <h2>Continue reading</h2>
          </div>
          <div className="library-continue-grid">
            {continueProjects.map((project) => (
              <button
                type="button"
                className="library-continue-card"
                key={project.id}
                onClick={() =>
                  navigateTo(
                    `/app/project/${encodeURIComponent(project.id)}`,
                  )
                }
              >
                <span>
                  {Math.round(project.progress?.progressPercent ?? 0)}%
                </span>
                <strong>{project.title}</strong>
                <small>{sourceLabel(project.sourceType)}</small>
                <i>
                  <b
                    style={{
                      width: `${Math.min(
                        100,
                        Math.max(0, project.progress?.progressPercent ?? 0),
                      )}%`,
                    }}
                  />
                </i>
              </button>
            ))}
          </div>
        </section>
      ) : null}

      <div className="library-section-heading">
        <h2>All documents</h2>
        <span>{totalItems} items</span>
      </div>

      {status === "loading" && totalItems === 0 ? (
        <div className="page-loading" role="status" aria-live="polite">Loading your library…</div>
      ) : totalItems === 0 && !normalizedQuery ? (
        <div className="library-empty">
          <span aria-hidden="true">⌁</span>
          <h2>Your library is ready for its first document.</h2>
          <p>
            Import PDF, DOCX, EPUB, HTML, Markdown or text—or open a live
            website in Browser V2.
          </p>
          <button
            type="button"
            className="page-primary-action"
            onClick={() => navigateTo("/app/import")}
          >
            Import your first document
          </button>
        </div>
      ) : totalItems === 0 ? (
        <div className="library-empty">
          <h2>No documents match “{query}”.</h2>
        </div>
      ) : (
        <div className="library-grid">
          {filteredOriginals.map((record) => {
            const type = localOriginalType(record);
            const linkedProject = record.projectId
              ? projects.find((project) => project.id === record.projectId)
              : null;

            return (
              <article className="library-card" key={`original:${record.id}`}>
                <div className="library-card-icon" aria-hidden="true">
                  {sourceGlyph(type)}
                </div>
                <div className="library-card-body">
                  <p className="library-card-type">
                    {sourceLabel(type)} · Original on this device
                  </p>
                  <h2>{record.name}</h2>
                  <p>
                    {linkedProject
                      ? `${linkedProject.wordCount.toLocaleString()} readable words · semantic layer synced`
                      : "Original source preserved · reading layer may still be preparing"}
                  </p>
                  <span>Updated {formatUpdatedAt(record.updatedAt)}</span>
                </div>
                <div className="library-card-actions">
                  <button
                    type="button"
                    className="card-primary"
                    onClick={() =>
                      navigateTo(
                        `/app/document/${encodeURIComponent(record.id)}`,
                      )
                    }
                  >
                    Open original
                  </button>
                  <button
                    type="button"
                    className="card-secondary"
                    onClick={() => void removeOriginal(record)}
                  >
                    Remove device copy
                  </button>
                </div>
              </article>
            );
          })}

          {filteredProjects.map((project) => (
            <article className="library-card" key={`project:${project.id}`}>
              <div className="library-card-icon" aria-hidden="true">
                {sourceGlyph(project.sourceType)}
              </div>
              <div className="library-card-body">
                <p className="library-card-type">
                  {sourceLabel(project.sourceType)} · Synced
                </p>
                <h2>{project.title}</h2>
                <p>
                  {project.wordCount.toLocaleString()} words
                  {project.progress
                    ? ` · ${Math.round(project.progress.progressPercent)}% read`
                    : ""}
                </p>
                <span>
                  Updated {formatUpdatedAt(project.updatedAt)}
                </span>
              </div>
              <div className="library-card-actions">
                <button
                  type="button"
                  className="card-primary"
                  onClick={() =>
                    navigateTo(
                      `/app/project/${encodeURIComponent(project.id)}`,
                    )
                  }
                >
                  Read
                </button>
                <button
                  type="button"
                  className="card-secondary"
                  onClick={() => void removeProject(project)}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}

          {filteredLegacy.map((document) => (
            <article className="library-card" key={`legacy:${document.id}`}>
              <div className="library-card-icon" aria-hidden="true">
                {document.sourceType === "markdown" ? "M" : "T"}
              </div>
              <div className="library-card-body">
                <p className="library-card-type">
                  {document.sourceType === "markdown"
                    ? "Markdown"
                    : "Text"} · Browser-local legacy
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
                  onClick={() => void removeLegacy(document)}
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
