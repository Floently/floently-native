import {
  updateContentProjectProgress,
  type ProjectProgress,
} from "./projectApi";

export type ProjectProgressPayload = Partial<ProjectProgress>;

export interface PlaybackProgressSnapshot {
  activeSegmentIndex: number | null;
  canonicalScalarCursor: number | null;
  durationMs: number;
  elapsedMs: number;
  speed: number;
  voiceId: string | null;
}

export type SaveProjectProgress = (
  projectId: string,
  progress: ProjectProgressPayload,
) => Promise<unknown>;

interface PendingProgressWrite {
  projectId: string;
  progress: ProjectProgressPayload;
  key: string;
}

function progressKey(
  projectId: string,
  progress: ProjectProgressPayload,
): string {
  return JSON.stringify([
    projectId,
    progress.currentSegmentIndex ?? null,
    progress.currentCharacterOffset ?? null,
    progress.progressPercent ?? null,
    progress.playbackRate ?? null,
    progress.voiceId ?? null,
  ]);
}

export function projectProgressFromPlayback(
  snapshot: PlaybackProgressSnapshot,
): ProjectProgressPayload {
  const durationMs =
    Number.isFinite(snapshot.durationMs) && snapshot.durationMs > 0
      ? snapshot.durationMs
      : 0;
  const elapsedMs =
    Number.isFinite(snapshot.elapsedMs) && snapshot.elapsedMs > 0
      ? snapshot.elapsedMs
      : 0;
  const progressPercent =
    durationMs > 0
      ? Math.min(100, Math.max(0, elapsedMs / durationMs * 100))
      : 0;

  return {
    currentSegmentIndex: Math.max(
      0,
      Math.trunc(snapshot.activeSegmentIndex ?? 0),
    ),
    currentCharacterOffset: Math.max(
      0,
      Math.trunc(snapshot.canonicalScalarCursor ?? 0),
    ),
    progressPercent,
    playbackRate:
      Number.isFinite(snapshot.speed) && snapshot.speed > 0
        ? snapshot.speed
        : 1,
    voiceId: snapshot.voiceId,
  };
}

/**
 * Keeps at most one progress PUT in flight and coalesces intermediate cursor
 * movement to the newest pending snapshot. This prevents a slow older request
 * from reaching the backend after a newer cursor and overwriting it.
 */
export function createProjectProgressWriter(
  save: SaveProjectProgress = updateContentProjectProgress,
) {
  let activeKey: string | null = null;
  let lastSavedKey: string | null = null;
  let inFlight: Promise<void> | null = null;
  let pending: PendingProgressWrite | null = null;

  const startNext = () => {
    if (inFlight || !pending) return;

    if (pending.key === lastSavedKey) {
      pending = null;
      return;
    }

    const next = pending;
    pending = null;
    activeKey = next.key;

    inFlight = Promise.resolve()
      .then(() => save(next.projectId, next.progress))
      .then(
        () => {
          lastSavedKey = next.key;
        },
        () => {
          // Progress is opportunistic. Keep playback responsive and allow the
          // same snapshot to be retried by a later playback/lifecycle event.
        },
      )
      .finally(() => {
        activeKey = null;
        inFlight = null;
        startNext();
      });
  };

  return {
    queue(projectId: string, progress: ProjectProgressPayload): void {
      const key = progressKey(projectId, progress);

      if (pending?.key === key) return;
      if (key === activeKey && !pending) return;
      if (key === lastSavedKey && !inFlight) return;

      pending = {
        projectId,
        progress,
        key,
      };
      startNext();
    },

    async flush(): Promise<void> {
      for (;;) {
        startNext();

        if (inFlight) {
          await inFlight;
          continue;
        }

        if (!pending) return;
      }
    },
  };
}
