import { describe, expect, it } from "vitest";
import type { WebPlaybackTelemetryEvent } from "./webPlaybackSession";
import {
  resolvePlaybackDiagnosticsEnabled,
  sanitizePlaybackTelemetryForQualification,
} from "./PlaybackDiagnostics";

function memoryStorage() {
  const values = new Map<string, string>();

  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => {
      values.set(key, value);
    },
    removeItem: (key: string) => {
      values.delete(key);
    },
  };
}

describe("playback qualification diagnostics", () => {
  it("persists opt-in only in the supplied session storage and supports explicit disable", () => {
    const storage = memoryStorage();

    expect(
      resolvePlaybackDiagnosticsEnabled(
        "?readDiagnostics=1",
        storage,
      ),
    ).toBe(true);

    expect(
      resolvePlaybackDiagnosticsEnabled("", storage),
    ).toBe(true);

    expect(
      resolvePlaybackDiagnosticsEnabled(
        "?readDiagnostics=0",
        storage,
      ),
    ).toBe(false);

    expect(
      resolvePlaybackDiagnosticsEnabled("", storage),
    ).toBe(false);
  });

  it("drops every string-valued telemetry field from copied diagnostics", () => {
    const events: WebPlaybackTelemetryEvent[] = [
      {
        name: "playback_error",
        at: 1,
        data: {
          message: "provider echoed source text",
        },
      },
      {
        name: "seek",
        at: 2,
        data: {
          fromMs: 1000,
          toMs: 2500,
        },
      },
      {
        name: "tts_ready",
        at: 3,
        data: {
          index: 4,
          cacheHit: true,
          provider: "must-not-leave-the-session",
        },
      },
    ];

    expect(
      sanitizePlaybackTelemetryForQualification(events),
    ).toEqual([
      {
        name: "playback_error",
        at: 1,
      },
      {
        name: "seek",
        at: 2,
        data: {
          fromMs: 1000,
          toMs: 2500,
        },
      },
      {
        name: "tts_ready",
        at: 3,
        data: {
          index: 4,
          cacheHit: true,
        },
      },
    ]);
  });
});
