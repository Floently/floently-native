import { useEffect, useMemo, useState } from "react";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import type { ReadVoice } from "../tts/readTtsProvider";
import {
  effectiveReadHighlightMode,
  normalizeReadLanguage,
  setReadHighlightMode,
  setReadReaderFont,
  setReadReaderLineSpacing,
  setReadReaderTextSize,
  setReadSpeedPreference,
  setReadThemePreference,
  setReadVoicePreference,
} from "./readPreferencesStore";
import { useReadPreferences } from "./useReadPreferences";

function voiceLanguage(voice: ReadVoice): string | null {
  return normalizeReadLanguage(voice.language || voice.locale);
}

function languageLabel(language: string): string {
  const labels: Record<string, string> = {
    en: "English",
    fi: "Finnish",
    sv: "Swedish",
    de: "German",
    fr: "French",
    es: "Spanish",
    it: "Italian",
    pt: "Portuguese",
    nl: "Dutch",
    no: "Norwegian",
    da: "Danish",
    pl: "Polish",
  };

  return labels[language] ?? language.toUpperCase();
}

export function PreferencesPage() {
  const runtime = useReadRuntime();
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const preferences = useReadPreferences();
  const [voices, setVoices] = useState<ReadVoice[]>([]);
  const [voiceStatus, setVoiceStatus] =
    useState<"idle" | "loading" | "ready" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const listVoices = runtime?.tts.listVoices?.bind(runtime.tts);
    if (!listVoices) return;

    let cancelled = false;
    setVoiceStatus("loading");
    setError(null);

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

  const languages = useMemo(
    () =>
      Array.from(
        new Set(
          voices
            .map(voiceLanguage)
            .filter((language): language is string => Boolean(language)),
        ),
      ).sort((left, right) =>
        languageLabel(left).localeCompare(languageLabel(right)),
      ),
    [voices],
  );

  const currentVoice = useMemo(
    () => voices.find((voice) => voice.id === playback.voiceId) ?? null,
    [playback.voiceId, voices],
  );

  const selectedLanguage =
    preferences.selectedVoiceLanguage
    ?? (currentVoice ? voiceLanguage(currentVoice) : null)
    ?? languages[0]
    ?? "";

  const scopedVoices = useMemo(
    () =>
      selectedLanguage
        ? voices.filter(
            (voice) => voiceLanguage(voice) === selectedLanguage,
          )
        : voices,
    [selectedLanguage, voices],
  );

  const effectiveHighlight = effectiveReadHighlightMode(
    preferences.highlightMode,
    false,
  );

  function chooseVoice(voiceId: string): void {
    const voice = voices.find((candidate) => candidate.id === voiceId);
    const language = voice ? voiceLanguage(voice) : selectedLanguage || null;

    setReadVoicePreference(voiceId, language);
    void runtime?.playback.setVoice(
      voiceId,
      { language },
    );
  }

  function chooseLanguage(language: string): void {
    const nextVoice =
      voices.find((voice) => voiceLanguage(voice) === language)
      ?? null;

    if (!nextVoice) return;
    setReadVoicePreference(nextVoice.id, language);
    void runtime?.playback.setVoice(
      nextVoice.id,
      { language },
    );
  }

  function chooseSpeed(speed: number): void {
    setReadSpeedPreference(speed);
    runtime?.playback.setSpeed(speed);
  }

  return (
    <section className="product-page settings-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Reading behavior</p>
          <h1>Preferences</h1>
          <p>
            These defaults belong to Read as an app, not to a paragraph or
            hidden audio chunk. They persist in this browser and apply to new
            reading sessions. A document's saved resume state can still restore
            its own position, voice and speed without rewriting your defaults.
          </p>
        </div>
      </header>

      <div className="settings-grid preference-grid">
        <article className="settings-card">
          <div className="preference-card-heading">
            <span aria-hidden="true">◐</span>
            <div>
              <h2>Appearance</h2>
              <p>
                Choose Read's chrome theme. System follows your device live.
                Original PDFs and remote web pages keep their own appearance.
              </p>
            </div>
          </div>

          <label className="settings-field">
            Theme
            <select
              value={preferences.themePreference}
              onChange={(event) =>
                setReadThemePreference(
                  event.target.value as "light" | "dark" | "system",
                )
              }
            >
              <option value="dark">Dark</option>
              <option value="light">Light</option>
              <option value="system">System</option>
            </select>
          </label>
          <span className="preference-meta">
            Active: {preferences.resolvedTheme}
          </span>
        </article>

        <article className="settings-card">
          <div className="preference-card-heading">
            <span aria-hidden="true">1×</span>
            <div>
              <h2>Reading speed</h2>
              <p>
                Your default speed survives app reloads and document changes.
              </p>
            </div>
          </div>

          <label className="settings-field">
            Speed
            <select
              value={playback.speed}
              disabled={!runtime}
              onChange={(event) => chooseSpeed(Number(event.target.value))}
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
          <span className="preference-meta">
            Persisted default: {preferences.speed}×
          </span>
        </article>

        <article className="settings-card preference-card-wide">
          <div className="preference-card-heading">
            <span aria-hidden="true">◉</span>
            <div>
              <h2>Voice</h2>
              <p>
                Voice and language persist across compatible new documents.
                Changing voice keeps your logical reading position.
              </p>
            </div>
          </div>

          <div className="preference-inline-fields">
            <label className="settings-field">
              Language
              <select
                value={selectedLanguage}
                disabled={voiceStatus === "loading" || languages.length === 0}
                onChange={(event) => chooseLanguage(event.target.value)}
              >
                {languages.length === 0 ? (
                  <option value="">
                    {voiceStatus === "loading" ? "Loading…" : "No languages"}
                  </option>
                ) : (
                  languages.map((language) => (
                    <option key={language} value={language}>
                      {languageLabel(language)}
                    </option>
                  ))
                )}
              </select>
            </label>

            <label className="settings-field">
              Voice
              <select
                value={playback.voiceId}
                disabled={
                  !runtime
                  || voiceStatus === "loading"
                  || scopedVoices.length === 0
                }
                onChange={(event) => chooseVoice(event.target.value)}
              >
                {!scopedVoices.some(
                  (voice) => voice.id === playback.voiceId,
                ) ? (
                  <option value={playback.voiceId}>
                    {voiceStatus === "loading"
                      ? "Loading voices…"
                      : currentVoice?.name ?? "Current voice"}
                  </option>
                ) : null}
                {scopedVoices.map((voice) => (
                  <option key={voice.id} value={voice.id}>
                    {voice.name}
                    {voice.locale ? ` · ${voice.locale}` : ""}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {error ? <p className="page-error" role="alert">{error}</p> : null}
        </article>

        <article className="settings-card">
          <div className="preference-card-heading">
            <span aria-hidden="true">▤</span>
            <div>
              <h2>Reading highlight</h2>
              <p>
                Sentence highlighting uses verified document/Browser anchors.
              </p>
            </div>
          </div>

          <label className="settings-field">
            Highlight mode
            <select
              value={preferences.highlightMode}
              onChange={(event) =>
                setReadHighlightMode(
                  event.target.value as "none" | "sentence" | "word",
                )
              }
            >
              <option value="none">None</option>
              <option value="sentence">Sentence</option>
              <option value="word" disabled>
                Word · verified timings required
              </option>
            </select>
          </label>

          {preferences.highlightMode === "word" ? (
            <p className="preference-capability-note">
              Your stored word preference is preserved, but this build uses
              sentence highlighting until the TTS backend exposes verified word
              timings mapped to canonical scalar offsets.
            </p>
          ) : (
            <span className="preference-meta">
              Active: {effectiveHighlight}
            </span>
          )}
        </article>

        <article className="settings-card preference-card-wide">
          <div className="preference-card-heading">
            <span aria-hidden="true">Aa</span>
            <div>
              <h2>Document reader</h2>
              <p>
                Typography applies only to Floently's semantic document reader.
                It never restyles a live website or original PDF.
              </p>
            </div>
          </div>

          <div className="preference-inline-fields preference-three-fields">
            <label className="settings-field">
              Typeface
              <select
                value={preferences.readerFont}
                onChange={(event) =>
                  setReadReaderFont(event.target.value as "serif" | "sans")
                }
              >
                <option value="serif">Editorial serif</option>
                <option value="sans">Clean sans</option>
              </select>
            </label>

            <label className="settings-field">
              Text size
              <select
                value={preferences.readerTextSize}
                onChange={(event) =>
                  setReadReaderTextSize(
                    event.target.value as "compact" | "comfortable" | "large",
                  )
                }
              >
                <option value="compact">Compact</option>
                <option value="comfortable">Comfortable</option>
                <option value="large">Large</option>
              </select>
            </label>

            <label className="settings-field">
              Line spacing
              <select
                value={preferences.readerLineSpacing}
                onChange={(event) =>
                  setReadReaderLineSpacing(
                    event.target.value as "compact" | "comfortable" | "spacious",
                  )
                }
              >
                <option value="compact">Compact</option>
                <option value="comfortable">Comfortable</option>
                <option value="spacious">Spacious</option>
              </select>
            </label>
          </div>

          <div
            className="preference-reader-preview"
            data-reader-font={preferences.readerFont}
            data-reader-size={preferences.readerTextSize}
            data-reader-spacing={preferences.readerLineSpacing}
          >
            <span>Preview</span>
            <p>
              Read keeps the source intact while giving semantic documents a
              calm, adjustable surface for focused learning.
            </p>
          </div>
        </article>
      </div>
    </section>
  );
}
