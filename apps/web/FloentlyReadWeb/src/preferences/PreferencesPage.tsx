import { useEffect, useState } from "react";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import type { ReadVoice } from "../tts/readTtsProvider";

export function PreferencesPage() {
  const runtime = useReadRuntime();
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const [voices, setVoices] = useState<ReadVoice[]>([]);
  const [voiceStatus, setVoiceStatus] =
    useState<"idle" | "loading" | "ready" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const listVoices = runtime?.tts.listVoices?.bind(runtime.tts);
    if (!listVoices) return;

    let cancelled = false;
    setVoiceStatus("loading");

    void listVoices()
      .then((catalog) => {
        if (cancelled) return;
        setVoices(catalog.voices);
        setVoiceStatus("ready");
      })
      .catch((reason: unknown) => {
        if (cancelled) return;
        setVoiceStatus("error");
        setError(reason instanceof Error ? reason.message : String(reason));
      });

    return () => {
      cancelled = true;
    };
  }, [runtime]);

  return (
    <section className="product-page settings-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Reading behavior</p>
          <h1>Preferences</h1>
          <p>
            These controls apply to the active document session today. Global
            account-synced preferences will be migrated behind the same
            playback interface.
          </p>
        </div>
      </header>

      <div className="settings-grid">
        <article className="settings-card">
          <h2>Reading speed</h2>
          <p>
            Speed belongs to the document-wide session and remains unchanged
            when playback crosses hidden audio boundaries.
          </p>
          <label className="settings-field">
            Speed
            <select
              value={playback.speed}
              disabled={!runtime}
              onChange={(event) =>
                runtime?.playback.setSpeed(Number(event.target.value))
              }
            >
              {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3].map(
                (speed) => (
                  <option key={speed} value={speed}>
                    {speed}×
                  </option>
                ),
              )}
            </select>
          </label>
        </article>

        <article className="settings-card">
          <h2>Voice</h2>
          <p>
            Voice changes invalidate only the internal audio cache needed for
            the new voice and preserve the logical reading position.
          </p>

          <label className="settings-field">
            Voice
            <select
              value={playback.voiceId}
              disabled={
                !runtime
                || voiceStatus === "loading"
                || voices.length === 0
              }
              onChange={(event) =>
                void runtime?.playback.setVoice(event.target.value)
              }
            >
              {voices.length === 0 ? (
                <option value={playback.voiceId}>
                  {voiceStatus === "loading"
                    ? "Loading voices…"
                    : playback.voiceId || "Default voice"}
                </option>
              ) : (
                voices.map((voice) => (
                  <option key={voice.id} value={voice.id}>
                    {voice.name}
                    {voice.locale ? ` · ${voice.locale}` : ""}
                  </option>
                ))
              )}
            </select>
          </label>

          {error ? <p className="page-error" role="alert">{error}</p> : null}
        </article>
      </div>
    </section>
  );
}
