import { describe, expect, it, vi } from "vitest";
import {
  createProjectProgressWriter,
  projectProgressFromPlayback,
} from "./projectProgressWriter";

describe("project progress writer", () => {
  it("maps playback state to canonical synced progress", () => {
    expect(
      projectProgressFromPlayback({
        activeSegmentIndex: 4,
        canonicalScalarCursor: 218,
        durationMs: 10_000,
        elapsedMs: 2_500,
        speed: 1.75,
        voiceId: "voice-fi",
      }),
    ).toEqual({
      currentSegmentIndex: 4,
      currentCharacterOffset: 218,
      progressPercent: 25,
      playbackRate: 1.75,
      voiceId: "voice-fi",
    });
  });

  it("serializes writes and coalesces cursor movement to the newest snapshot", async () => {
    const firstWrite: { release: () => void } = {
      release: () => {},
    };
    const save = vi.fn(
      async (
        _projectId: string,
        progress: { currentCharacterOffset?: number },
      ) => {
        if (progress.currentCharacterOffset === 10) {
          await new Promise<void>((resolve) => {
            firstWrite.release = resolve;
          });
        }
      },
    );
    const writer = createProjectProgressWriter(save);

    writer.queue("p1", { currentCharacterOffset: 10 });
    writer.queue("p1", { currentCharacterOffset: 20 });
    writer.queue("p1", { currentCharacterOffset: 30 });

    await Promise.resolve();
    expect(save).toHaveBeenCalledTimes(1);

    firstWrite.release();
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(2);
    expect(save.mock.calls[0]?.[1].currentCharacterOffset).toBe(10);
    expect(save.mock.calls[1]?.[1].currentCharacterOffset).toBe(30);
  });

  it("keeps the newest queued cursor even when it returns to the active value", async () => {
    const firstWrite: { release: () => void } = {
      release: () => {},
    };
    const save = vi.fn(
      async (
        _projectId: string,
        progress: { currentCharacterOffset?: number },
      ) => {
        if (progress.currentCharacterOffset === 10) {
          await new Promise<void>((resolve) => {
            firstWrite.release = resolve;
          });
        }
      },
    );
    const writer = createProjectProgressWriter(save);

    writer.queue("p1", { currentCharacterOffset: 10 });
    writer.queue("p1", { currentCharacterOffset: 30 });

    // A real backward seek may return to the same cursor that is still being
    // written. Last queued state wins; treating this as a duplicate would let
    // the stale pending 30 overwrite the user's newer position.
    writer.queue("p1", { currentCharacterOffset: 10 });

    await Promise.resolve();
    firstWrite.release();
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(1);
    expect(save.mock.calls[0]?.[1].currentCharacterOffset).toBe(10);
  });

  it("does not resend an identical snapshot after it is saved", async () => {
    const save = vi.fn(async () => undefined);
    const writer = createProjectProgressWriter(save);
    const progress = {
      currentCharacterOffset: 42,
      progressPercent: 12,
    };

    writer.queue("p1", progress);
    await writer.flush();
    writer.queue("p1", progress);
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(1);
  });

  it("does not start a queued write after its owner becomes inactive", async () => {
    let ownerActive = true;
    const save = vi.fn(async () => undefined);
    const writer = createProjectProgressWriter(save, {
      canWrite: () => ownerActive,
    });

    writer.queue("p1", { currentCharacterOffset: 12 });
    ownerActive = false;

    await writer.flush();

    expect(save).toHaveBeenCalledTimes(0);
  });

  it("does not start a pending second write after the owner changes", async () => {
    let ownerActive = true;
    let releaseFirst!: () => void;
    const save = vi.fn(
      async (
        _projectId: string,
        progress: { currentCharacterOffset?: number },
      ) => {
        if (progress.currentCharacterOffset === 10) {
          await new Promise<void>((resolve) => {
            releaseFirst = resolve;
          });
        }
      },
    );
    const writer = createProjectProgressWriter(save, {
      canWrite: () => ownerActive,
    });

    writer.queue("p1", { currentCharacterOffset: 10 });
    writer.queue("p1", { currentCharacterOffset: 20 });

    await Promise.resolve();
    expect(save).toHaveBeenCalledTimes(1);

    ownerActive = false;
    releaseFirst();
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(1);
    expect(save.mock.calls[0]?.[1].currentCharacterOffset).toBe(10);
  });

  it("drops pending work when the runtime invalidates the writer", async () => {
    let releaseFirst!: () => void;
    const save = vi.fn(
      async (
        _projectId: string,
        progress: { currentCharacterOffset?: number },
      ) => {
        if (progress.currentCharacterOffset === 10) {
          await new Promise<void>((resolve) => {
            releaseFirst = resolve;
          });
        }
      },
    );
    const writer = createProjectProgressWriter(save);

    writer.queue("p1", { currentCharacterOffset: 10 });
    writer.queue("p1", { currentCharacterOffset: 20 });

    await Promise.resolve();
    expect(save).toHaveBeenCalledTimes(1);

    writer.invalidate();
    releaseFirst();
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(1);

    writer.queue("p1", { currentCharacterOffset: 30 });
    await writer.flush();
    expect(save).toHaveBeenCalledTimes(1);
  });

  it("allows a failed snapshot to be retried later", async () => {
    let attempts = 0;
    const save = vi.fn(async () => {
      attempts += 1;
      if (attempts === 1) {
        throw new Error("offline");
      }
    });
    const writer = createProjectProgressWriter(save);
    const progress = { currentCharacterOffset: 84 };

    writer.queue("p1", progress);
    await writer.flush();
    writer.queue("p1", progress);
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(2);
  });
});
