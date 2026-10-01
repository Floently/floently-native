import { useState, type FormEvent } from "react";

export function BrowserPage() {
  const [url, setUrl] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = url.trim();

    if (!value) return;

    try {
      const parsed = new URL(
        value.includes("://") ? value : `https://${value}`,
      );
      setMessage(
        `Saved target: ${parsed.hostname}. Live web capture is intentionally not enabled in this parallel build until the existing Browser V2 navigation and page-preserving reader contracts are ported.`,
      );
    } catch {
      setMessage("Enter a valid web address.");
    }
  }

  return (
    <section className="product-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Web reading</p>
          <h1>Browser</h1>
          <p>
            This route is reserved for the polyglot Browser V2 migration. The
            current production browser remains untouched while its navigation,
            page-preserving reading and remote-browser contracts are moved
            behind the new document timeline.
          </p>
        </div>
      </header>

      <div className="browser-migration-card">
        <div>
          <p className="browser-status-label">Migration boundary</p>
          <h2>Keep the web page visible while Read speaks it.</h2>
          <p>
            The new implementation will preserve the rendered page, map
            visible content into the Rust reading model, and let playback
            follow the page instead of converting it into a separate text
            screen.
          </p>
        </div>

        <form onSubmit={submit} className="browser-url-form">
          <label>
            Web address
            <div>
              <input
                type="text"
                inputMode="url"
                value={url}
                placeholder="example.com/article"
                onChange={(event) => setUrl(event.target.value)}
              />
              <button type="submit" className="page-primary-action">
                Check target
              </button>
            </div>
          </label>
        </form>

        {message ? <p className="browser-migration-note">{message}</p> : null}
      </div>
    </section>
  );
}
