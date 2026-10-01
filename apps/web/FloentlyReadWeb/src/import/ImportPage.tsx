import {
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
} from "react";
import {
  saveLibraryDocument,
  type LibraryDocument,
} from "../library/documentRepository";
import { navigateTo } from "../routing/navigation";

function sourceTypeForFile(fileName: string): LibraryDocument["sourceType"] {
  return fileName.toLowerCase().endsWith(".md") ? "markdown" : "text";
}

function titleForFile(fileName: string): string {
  return fileName.replace(/\.(txt|md|markdown)$/i, "") || fileName;
}

export function ImportPage() {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [title, setTitle] = useState("");
  const [language, setLanguage] = useState("en");
  const [text, setText] = useState("");
  const [sourceType, setSourceType] =
    useState<LibraryDocument["sourceType"]>("text");
  const [fileName, setFileName] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function acceptFile(file: File) {
    try {
      setError(null);

      if (
        !file.name.toLowerCase().endsWith(".txt")
        && !file.name.toLowerCase().endsWith(".md")
        && !file.name.toLowerCase().endsWith(".markdown")
        && !file.type.startsWith("text/")
      ) {
        throw new Error(
          "This migration slice currently accepts text and Markdown files.",
        );
      }

      const value = await file.text();
      setText(value);
      setTitle(titleForFile(file.name));
      setSourceType(sourceTypeForFile(file.name));
      setFileName(file.name);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    }
  }

  function onFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) void acceptFile(file);
    event.target.value = "";
  }

  function onDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    const file = event.dataTransfer.files?.[0];
    if (file) void acceptFile(file);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!text.trim()) {
      setError("Add or import document text first.");
      return;
    }

    setIsSaving(true);
    setError(null);

    try {
      const document = await saveLibraryDocument({
        title,
        language,
        text,
        sourceType,
      });

      navigateTo(
        `/app/reader/${encodeURIComponent(document.id)}`,
      );
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="product-page import-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Add reading material</p>
          <h1>Import</h1>
          <p>
            Text and Markdown are wired end-to-end in this first product shell.
            Additional file and web adapters can plug into the same document
            contract without changing the reader.
          </p>
        </div>
      </header>

      <form className="import-layout" onSubmit={save}>
        <div
          className="import-dropzone"
          onDragOver={(event) => event.preventDefault()}
          onDrop={onDrop}
        >
          <span className="import-drop-icon" aria-hidden="true">↥</span>
          <h2>Drop a text or Markdown file</h2>
          <p>
            {fileName
              ? `${fileName} is loaded and ready to review.`
              : "Or choose a file from this device."}
          </p>
          <button
            type="button"
            className="card-secondary"
            onClick={() => fileInputRef.current?.click()}
          >
            Choose file
          </button>
          <input
            ref={fileInputRef}
            className="visually-hidden"
            type="file"
            accept=".txt,.md,.markdown,text/plain,text/markdown"
            onChange={onFileChange}
          />
        </div>

        <div className="import-editor-card">
          <label>
            Document title
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Untitled document"
            />
          </label>

          <label>
            Reading language
            <select
              value={language}
              onChange={(event) => setLanguage(event.target.value)}
            >
              <option value="en">English</option>
              <option value="fi">Finnish</option>
              <option value="sv">Swedish</option>
              <option value="de">German</option>
              <option value="fr">French</option>
              <option value="es">Spanish</option>
              <option value="auto">Auto detect</option>
            </select>
          </label>

          <label>
            Document text
            <textarea
              value={text}
              onChange={(event) => {
                setText(event.target.value);
                if (!fileName) setSourceType("text");
              }}
              placeholder="Paste the text you want to read…"
            />
          </label>

          <div className="import-editor-footer">
            <span>{text.length.toLocaleString()} characters</span>
            <button
              type="submit"
              className="page-primary-action"
              disabled={isSaving || !text.trim()}
            >
              {isSaving ? "Saving…" : "Save and open reader"}
            </button>
          </div>

          {error ? <p className="page-error" role="alert">{error}</p> : null}
        </div>
      </form>
    </section>
  );
}
