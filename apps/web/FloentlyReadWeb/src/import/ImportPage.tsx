import {
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
} from "react";
import {
  handoffOriginalDocument,
  linkLocalOriginalToProject,
} from "../content/localOriginalDocuments";
import {
  ingestFileIntoReader,
  ingestTextIntoReader,
} from "../content/unifiedDocumentIngestion";
import { navigateTo } from "../routing/navigation";
import { useAuthState } from "../auth/useAuthState";
import {
  GoogleDriveImportCancelledError,
  pickGoogleDriveFileAsFile,
} from "./googleDriveImport";

type ImportSource = "device" | "drive" | "paste" | "website";

const READ_FILE_ACCEPT = [
  ".pdf",
  ".docx",
  ".epub",
  ".html",
  ".htm",
  ".md",
  ".markdown",
  ".txt",
  "application/pdf",
  "application/epub+zip",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "text/html",
  "text/markdown",
  "text/plain",
].join(",");

function isPdfFile(file: File): boolean {
  return (
    file.type === "application/pdf"
    || file.name.toLowerCase().endsWith(".pdf")
  );
}

function normalizeWebsiteInput(value: string): string {
  const raw = value.trim();
  if (!raw) return "";

  const lower = raw.toLowerCase();
  if (lower.startsWith("http://") || lower.startsWith("https://")) {
    try {
      const parsed = new URL(raw);
      return parsed.protocol === "http:" || parsed.protocol === "https:"
        ? parsed.href
        : "";
    } catch {
      return "";
    }
  }

  if (!raw.includes(" ") && raw.includes(".")) {
    try {
      return new URL(`https://${raw}`).href;
    } catch {
      return "";
    }
  }

  return `https://www.google.com/search?q=${encodeURIComponent(raw)}`;
}

function titleForText(text: string): string {
  const firstLine = text
    .replaceAll("\r", "")
    .split("\n")
    .map((line) => line.trim())
    .find(Boolean);

  if (!firstLine) return "Pasted text";
  return firstLine.length <= 80
    ? firstLine
    : `${firstLine.slice(0, 77)}…`;
}

export function ImportPage() {
  const auth = useAuthState();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [source, setSource] = useState<ImportSource>("device");
  const [pasteText, setPasteText] = useState("");
  const [website, setWebsite] = useState("");
  const [dragging, setDragging] = useState(false);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const sourceDescription = useMemo(() => {
    if (source === "paste") {
      return "Paste text and save it as a synced Read project.";
    }
    if (source === "website") {
      return "Open the real page in Floently Browser V2. It will not be copied into a text view.";
    }
    if (source === "drive") {
      return "Choose a supported file from Google Drive. It follows the same original-source and synced-project rules as a device file.";
    }

    return "PDFs keep their original pages. DOCX, EPUB, HTML, Markdown and TXT use the synced document pipeline.";
  }, [source]);

  async function openFile(file: File): Promise<void> {
    if (!file || busy) return;

    setBusy(true);
    setError(null);

    if (isPdfFile(file)) {
      try {
        setStatus("Opening the original PDF pages…");
        const local = await handoffOriginalDocument(file);

        navigateTo(
          `/app/document/${encodeURIComponent(local.id)}`,
        );

        // Extraction is deliberately background work after the original is
        // already visible. The original page view is not blocked by OCR/parser
        // latency or a cold backend.
        void ingestFileIntoReader(file)
          .then(async (result) => {
            await linkLocalOriginalToProject(
              local.id,
              result.project.id,
            );
          })
          .catch(() => {
            // The original PDF remains usable even if the semantic layer is
            // temporarily unavailable. The visual source is authoritative.
          });

        return;
      } catch (reason) {
        setBusy(false);
        setError(
          reason instanceof Error
            ? reason.message
            : "Could not open this PDF.",
        );
        return;
      }
    }

    setStatus(`Adding ${file.name} to your synced library…`);

    try {
      const result = await ingestFileIntoReader(file);
      navigateTo(
        `/app/project/${encodeURIComponent(result.project.id)}`,
      );
    } catch (reason) {
      // Preserve the original file when canonical extraction cannot finish.
      // This mirrors production's fail-open visual-source behavior.
      try {
        const local = await handoffOriginalDocument(file);
        navigateTo(
          `/app/document/${encodeURIComponent(local.id)}`,
        );
      } catch {
        setBusy(false);
        setStatus(null);
        setError(
          reason instanceof Error
            ? reason.message
            : "Could not import this document.",
        );
      }
    }
  }

  function onFileChange(event: ChangeEvent<HTMLInputElement>): void {
    const file = event.target.files?.[0];
    if (file) void openFile(file);
    event.target.value = "";
  }

  function onDrop(event: DragEvent<HTMLDivElement>): void {
    event.preventDefault();
    setDragging(false);

    const file = event.dataTransfer.files?.[0];
    if (file) {
      setSource("device");
      void openFile(file);
    }
  }

  async function savePastedText(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();
    const text = pasteText.trim();
    if (!text || busy) return;

    setBusy(true);
    setError(null);
    setStatus("Saving text to your synced library…");

    try {
      const result = await ingestTextIntoReader(text, {
        title: titleForText(text),
        sourceType: "text",
      });
      navigateTo(
        `/app/project/${encodeURIComponent(result.project.id)}`,
      );
    } catch (reason) {
      setBusy(false);
      setStatus(null);
      setError(
        reason instanceof Error
          ? reason.message
          : "Could not save this text.",
      );
    }
  }

  async function openGoogleDrive(): Promise<void> {
    if (busy) return;

    setBusy(true);
    setError(null);
    setStatus("Opening Google Drive…");

    try {
      const file = await pickGoogleDriveFileAsFile(
        auth.googleClientId,
        {
          onStatus: (message) => setStatus(message),
        },
      );

      setBusy(false);
      await openFile(file);
    } catch (reason) {
      setBusy(false);

      if (reason instanceof GoogleDriveImportCancelledError) {
        setStatus("Google Drive selection cancelled.");
        return;
      }

      setStatus(null);
      setError(
        reason instanceof Error
          ? reason.message
          : "Google Drive could not be opened.",
      );
    }
  }

  function openWebsite(
    event: FormEvent<HTMLFormElement>,
  ): void {
    event.preventDefault();
    const target = normalizeWebsiteInput(website);

    if (!target) {
      setError("Enter a valid web address or search.");
      return;
    }

    const params = new URLSearchParams({
      url: target,
      autostart: "1",
    });

    navigateTo(`/app/browser?${params.toString()}`);
  }

  return (
    <section className="product-page import-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Add reading material</p>
          <h1>Import</h1>
          <p>
            PDFs keep their original pages. Websites keep their live page.
            Floently adds a semantic reading layer without replacing the source
            you opened.
          </p>
        </div>
      </header>

      <div className="import-source-tabs" role="tablist" aria-label="Import source">
        {([
          ["device", "Device"],
          ["drive", "Google Drive"],
          ["paste", "Paste text"],
          ["website", "Website"],
        ] as const).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={source === id}
            className={source === id ? "active" : ""}
            onClick={() => {
              setSource(id);
              setError(null);
              setStatus(null);
            }}
          >
            {label}
          </button>
        ))}
      </div>

      <p className="import-source-description">{sourceDescription}</p>

      {source === "device" ? (
        <div
          className={
            dragging
              ? "import-dropzone import-dropzone-active"
              : "import-dropzone"
          }
          onDragEnter={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragLeave={(event) => {
            if (event.currentTarget === event.target) {
              setDragging(false);
            }
          }}
          onDragOver={(event) => event.preventDefault()}
          onDrop={onDrop}
        >
          <span className="import-drop-icon" aria-hidden="true">↥</span>
          <h2>{dragging ? "Drop to open" : "Drop a document here"}</h2>
          <p>
            PDF · DOCX · EPUB · HTML · Markdown · TXT
          </p>
          <button
            type="button"
            className="page-primary-action"
            disabled={busy}
            onClick={() => fileInputRef.current?.click()}
          >
            {busy ? "Opening…" : "Choose document"}
          </button>
          <input
            ref={fileInputRef}
            className="visually-hidden"
            type="file"
            accept={READ_FILE_ACCEPT}
            onChange={onFileChange}
          />
        </div>
      ) : null}

      {source === "drive" ? (
        <div className="import-dropzone">
          <span className="import-drop-icon" aria-hidden="true">G</span>
          <h2>Choose from Google Drive</h2>
          <p>
            PDF · Google Docs · Google Slides · Google Sheets · text and
            supported document files
          </p>
          <button
            type="button"
            className="page-primary-action"
            disabled={busy || !auth.googleClientId}
            onClick={() => void openGoogleDrive()}
          >
            {busy ? "Opening…" : "Choose from Drive"}
          </button>
          {!auth.googleClientId ? (
            <small className="import-drive-note">
              Google Drive will be available when Google sign-in is enabled for
              this deployment.
            </small>
          ) : null}
        </div>
      ) : null}

      {source === "paste" ? (
        <form className="import-editor-card import-single-card" onSubmit={savePastedText}>
          <label>
            Text
            <textarea
              value={pasteText}
              onChange={(event) => setPasteText(event.target.value)}
              placeholder="Paste the text you want to read…"
              autoFocus
            />
          </label>
          <div className="import-editor-footer">
            <span>{pasteText.length.toLocaleString()} characters</span>
            <button
              type="submit"
              className="page-primary-action"
              disabled={busy || !pasteText.trim()}
            >
              {busy ? "Saving…" : "Save and read"}
            </button>
          </div>
        </form>
      ) : null}

      {source === "website" ? (
        <form className="import-editor-card import-single-card" onSubmit={openWebsite}>
          <label>
            Website address or search
            <input
              type="text"
              inputMode="url"
              value={website}
              onChange={(event) => setWebsite(event.target.value)}
              placeholder="example.com/article or search terms"
              autoFocus
            />
          </label>
          <div className="import-editor-footer">
            <span>The site opens inside the authenticated live browser.</span>
            <button
              type="submit"
              className="page-primary-action"
              disabled={!website.trim()}
            >
              Open live page
            </button>
          </div>
        </form>
      ) : null}

      {status ? (
        <p className="import-status" role="status">{status}</p>
      ) : null}
      {error ? (
        <p className="page-error" role="alert">{error}</p>
      ) : null}
    </section>
  );
}
