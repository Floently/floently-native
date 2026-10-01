import {
  useState,
  type KeyboardEvent,
  type PointerEvent,
} from "react";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "../playback/webPlaybackSession";
import { setReadSpeedPreference } from "../preferences/readPreferencesStore";

function formatDuration(milliseconds: number): string {
  const totalSeconds = Math.max(0, Math.round(milliseconds / 1_000));
  const hours = Math.floor(totalSeconds / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return [hours, minutes, seconds]
      .map((value, index) =>
        index === 0 ? String(value) : String(value).padStart(2, "0"),
      )
      .join(":");
  }

  return [minutes, seconds]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");
}

function clampForInput(value: number, maximum: number): number {
  return Math.min(Math.max(0, value), Math.max(1, maximum));
}

export function PlaybackDock({
  session,
  snapshot,
}: {
  session: WebPlaybackSession;
  snapshot: WebPlaybackSnapshot;
}) {
  const [seekDraftMs, setSeekDraftMs] = useState<number | null>(null);
  const displayedPositionMs = seekDraftMs ?? snapshot.elapsedMs;
  const isTransportActive = [
    "preparing",
    "buffering",
    "playing",
  ].includes(snapshot.status);

  function commitSeek(): void {
    if (seekDraftMs === null) return;
    const target = seekDraftMs;
    setSeekDraftMs(null);
    void session.seek(target);
  }

  function onSliderPointerUp(
    _event: PointerEvent<HTMLInputElement>,
  ): void {
    commitSeek();
  }

  function onSliderKeyUp(
    event: KeyboardEvent<HTMLInputElement>,
  ): void {
    if (
      event.key.startsWith("Arrow")
      || event.key === "Home"
      || event.key === "End"
      || event.key === "PageUp"
      || event.key === "PageDown"
    ) {
      commitSeek();
    }
  }

  return (
    <section
      className="player-dock"
      aria-label="Floently Read document player"
    >
      <div className="player-document">
        <span className="player-eyebrow">Now reading</span>
        <strong>{snapshot.title ?? "Floently Read"}</strong>
        <span>
          {snapshot.status}
          {snapshot.bufferedAheadMs > 0
            ? ` · ${formatDuration(snapshot.bufferedAheadMs)} ready ahead`
            : ""}
        </span>
      </div>

      <div className="player-center">
        <div className="transport-row">
          <button
            className="transport-button secondary"
            onClick={() => void session.seekBy(-15_000)}
            aria-label="Back 15 seconds"
          >
            −15
          </button>
          <button
            className="transport-button primary"
            onClick={() => void session.togglePlayPause()}
            aria-label={isTransportActive ? "Pause" : "Play"}
          >
            {isTransportActive ? "Pause" : "Play"}
          </button>
          <button
            className="transport-button secondary"
            onClick={() => void session.seekBy(15_000)}
            aria-label="Forward 15 seconds"
          >
            +15
          </button>
        </div>

        <div className="player-timeline">
          <span>{formatDuration(displayedPositionMs)}</span>
          <input
            className="player-slider"
            type="range"
            min="0"
            max={Math.max(1, snapshot.durationMs)}
            step="1000"
            value={clampForInput(
              displayedPositionMs,
              snapshot.durationMs,
            )}
            onChange={(event) =>
              setSeekDraftMs(Number(event.target.value))
            }
            onPointerUp={onSliderPointerUp}
            onKeyUp={onSliderKeyUp}
            onBlur={commitSeek}
            aria-label="Document position"
          />
          <span>{formatDuration(snapshot.durationMs)}</span>
        </div>
      </div>

      <div className="player-options">
        <label htmlFor="playback-speed">Speed</label>
        <select
          id="playback-speed"
          value={snapshot.speed}
          onChange={(event) => {
            const speed = Number(event.target.value);
            setReadSpeedPreference(speed);
            session.setSpeed(speed);
          }}
        >
          {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3].map((speed) => (
            <option value={speed} key={speed}>
              {speed}×
            </option>
          ))}
        </select>
        {snapshot.error ? (
          <span className="player-error" title={snapshot.error}>
            Playback error
          </span>
        ) : null}
      </div>
    </section>
  );
}
