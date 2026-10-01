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

  it("does not let an active duplicate replace a newer pending cursor", async () => {
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
    writer.queue("p1", { currentCharacterOffset: 10 });

    await Promise.resolve();
    firstWrite.release();
    await writer.flush();

    expect(save).toHaveBeenCalledTimes(2);
    expect(save.mock.calls[0]?.[1].currentCharacterOffset).toBe(10);
    expect(save.mock.calls[1]?.[1].currentCharacterOffset).toBe(30);
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
