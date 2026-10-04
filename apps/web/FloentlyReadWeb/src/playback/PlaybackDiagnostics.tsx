import {
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
  WebPlaybackTelemetryEvent,
} from "./webPlaybackSession";

const DIAGNOSTICS_SESSION_KEY =
  "floently-read-playback-diagnostics-v1";

type DiagnosticsStorage = Pick<
  Storage,
  "getItem" | "setItem" | "removeItem"
>;

export function resolvePlaybackDiagnosticsEnabled(
  search?: string,
  storage?: DiagnosticsStorage | null,
): boolean {
  const fallbackSearch =
    typeof window === "undefined"
      ? ""
      : window.location.search;
  const requested = new URLSearchParams(
    search ?? fallbackSearch,
  ).get("readDiagnostics");

  let sessionStoragePort = storage;
  if (sessionStoragePort === undefined) {
    try {
      sessionStoragePort =
        typeof window === "undefined"
          ? null
          : window.sessionStorage;
    } catch {
      sessionStoragePort = null;
    }
  }

  if (!sessionStoragePort) {
    return requested === "1";
  }

  try {
    if (requested === "1") {
      sessionStoragePort.setItem(
        DIAGNOSTICS_SESSION_KEY,
        "1",
      );
      return true;
    }

    if (requested === "0") {
      sessionStoragePort.removeItem(
        DIAGNOSTICS_SESSION_KEY,
      );
      return false;
    }

    return sessionStoragePort.getItem(
      DIAGNOSTICS_SESSION_KEY,
    ) === "1";
  } catch {
    return requested === "1";
  }
}

function finiteNumber(
  value: unknown,
): number | null {
  return typeof value === "number"
    && Number.isFinite(value)
    ? value
    : null;
}

export function sanitizePlaybackTelemetryForQualification(
  events: readonly WebPlaybackTelemetryEvent[],
): Array<{
  name: WebPlaybackTelemetryEvent["name"];
  at: number;
  data?: Record<string, number | boolean | null>;
}> {
  return events.map((event) => {
    const safeData = Object.fromEntries(
      Object.entries(event.data ?? {}).filter(
        (
          entry,
        ): entry is [
          string,
          number | boolean | null,
        ] => typeof entry[1] !== "string",
      ),
    );

    return {
      name: event.name,
      at: event.at,
      ...(Object.keys(safeData).length > 0
        ? { data: safeData }
        : {}),
    };
  });
}

function handoffLatencies(
  events: readonly WebPlaybackTelemetryEvent[],
): number[] {
  return events.flatMap((event) => {
    if (event.name !== "segment_handoff") return [];

    const latency = finiteNumber(
      event.data?.mediaStartLatencyMs,
    );

    return latency === null
      ? []
      : [Math.max(0, latency)];
  });
}

function metric(
  label: string,
  value: ReactNode,
): ReactNode {
  return (
    <div className="playback-diagnostics-metric">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function formatSeconds(milliseconds: number): string {
  const seconds = Math.max(0, milliseconds) / 1_000;
  return `${seconds.toFixed(seconds < 10 ? 2 : 1)}s`;
}

export function PlaybackDiagnostics({
  session,
  snapshot,
}: {
  session: WebPlaybackSession;
  snapshot: WebPlaybackSnapshot;
}) {
  const [copyState, setCopyState] = useState<
    "idle" | "copied" | "failed"
  >("idle");

  const events = session.getRecentTelemetry();
  const latencies = useMemo(
    () => handoffLatencies(events),
    [events],
  );
  const latestLatency =
    latencies.at(-1) ?? null;
  const averageLatency =
    latencies.length > 0
      ? latencies.reduce(
          (total, value) => total + value,
          0,
        ) / latencies.length
      : null;
  const maximumLatency =
    latencies.length > 0
      ? Math.max(...latencies)
      : null;
  const bufferingEvents = events.filter(
    (event) => event.name === "audio_waiting",
  ).length;

  const qualificationSnapshot = {
    capturedAt: new Date().toISOString(),
    playback: {
      status: snapshot.status,
      elapsedMs: snapshot.elapsedMs,
      durationMs: snapshot.durationMs,
      bufferedAheadMs: snapshot.bufferedAheadMs,
      speed: snapshot.speed,
      activeSegmentIndex: snapshot.activeSegmentIndex,
    },
    handoff: {
      samples: latencies.length,
      latestMediaStartLatencyMs: latestLatency,
      averageMediaStartLatencyMs: averageLatency,
      maximumMediaStartLatencyMs: maximumLatency,
      bufferingEvents,
    },
    recentTelemetry:
      sanitizePlaybackTelemetryForQualification(events),
  };

  async function copyDiagnostics(): Promise<void> {
    try {
      if (
        typeof navigator === "undefined"
        || !navigator.clipboard?.writeText
      ) {
        throw new Error("Clipboard unavailable");
      }

      await navigator.clipboard.writeText(
        JSON.stringify(
          qualificationSnapshot,
          null,
          2,
        ),
      );
      setCopyState("copied");
    } catch {
      setCopyState("failed");
    }
  }

  return (
    <details
      className="playback-diagnostics"
      aria-label="Read playback qualification diagnostics"
    >
      <summary>Playback diagnostics</summary>

      <p className="playback-diagnostics-note">
        Qualification data only. Handoff latency measures browser media start,
        not verified acoustic silence.
      </p>

      <dl className="playback-diagnostics-grid">
        {metric("State", snapshot.status)}
        {metric(
          "Logical time",
          `${formatSeconds(snapshot.elapsedMs)} / ${formatSeconds(snapshot.durationMs)}`,
        )}
        {metric(
          "Buffered ahead",
          formatSeconds(snapshot.bufferedAheadMs),
        )}
        {metric("Speed", `${snapshot.speed}x`)}
        {metric("Handoff samples", latencies.length)}
        {metric(
          "Latest handoff",
          latestLatency === null
            ? "-"
            : `${Math.round(latestLatency)} ms`,
        )}
        {metric(
          "Average handoff",
          averageLatency === null
            ? "-"
            : `${Math.round(averageLatency)} ms`,
        )}
        {metric(
          "Maximum handoff",
          maximumLatency === null
            ? "-"
            : `${Math.round(maximumLatency)} ms`,
        )}
        {metric("Buffering events", bufferingEvents)}
      </dl>

      <div className="playback-diagnostics-actions">
        <button
          type="button"
          onClick={() => void copyDiagnostics()}
        >
          Copy diagnostics JSON
        </button>
        <output
          aria-live="polite"
          className="playback-diagnostics-copy-state"
        >
          {copyState === "copied"
            ? "Copied"
            : copyState === "failed"
              ? "Clipboard unavailable"
              : ""}
        </output>
      </div>
    </details>
  );
}
