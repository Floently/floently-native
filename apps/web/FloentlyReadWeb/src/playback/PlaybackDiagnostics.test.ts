import { describe, expect, it } from "vitest";
import type { WebPlaybackTelemetryEvent } from "./webPlaybackSession";
import { sanitizePlaybackTelemetryForQualification } from "./PlaybackDiagnostics";

describe("playback qualification telemetry privacy", () => {
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
