import {
  afterEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import {
  RenderReadTtsProvider,
  type ReadTtsInput,
} from "./readTtsProvider";

function input(speed = 1): ReadTtsInput {
  return {
    text: "A deterministic sentence.",
    language: "en",
    voiceId: "voice:test",
    speed,
    segmentId: "segment-1",
  };
}

function jsonResponse(data: Record<string, unknown>): Response {
  return new Response(
    JSON.stringify({ data }),
    {
      status: 200,
      headers: { "Content-Type": "application/json" },
    },
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("RenderReadTtsProvider cache identity", () => {
  it("changes fallback identity when resolved provider or model changes", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        audioUrl: "https://audio.invalid/first.mp3",
        voiceId: "voice:test",
        provider: "azure",
        model: "neural-v1",
      }))
      .mockResolvedValueOnce(jsonResponse({
        audioUrl: "https://audio.invalid/second.mp3",
        voiceId: "voice:test",
        provider: "google",
        model: "neural-v2",
      }));

    vi.stubGlobal("fetch", fetchMock);

    const provider = new RenderReadTtsProvider({
      baseUrl: "https://tts.invalid",
    });

    const first = await provider.synthesize(input());
    const second = await provider.synthesize(input());

    expect(first.cacheKey).toMatch(/^read-tts:sha256:/);
    expect(second.cacheKey).toMatch(/^read-tts:sha256:/);
    expect(first.cacheKey).not.toBe(second.cacheKey);
    expect(first.contentHash).not.toBe(second.contentHash);
    expect(first).toMatchObject({
      provider: "azure",
      model: "neural-v1",
    });
    expect(second).toMatchObject({
      provider: "google",
      model: "neural-v2",
    });
  });

  it("keeps local playback speed outside the synthesis cache identity", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        audioUrl: "https://audio.invalid/slow.mp3",
        voiceId: "voice:test",
        provider: "azure",
        model: "neural-v1",
      }))
      .mockResolvedValueOnce(jsonResponse({
        audioUrl: "https://audio.invalid/fast.mp3",
        voiceId: "voice:test",
        provider: "azure",
        model: "neural-v1",
      }));

    vi.stubGlobal("fetch", fetchMock);

    const provider = new RenderReadTtsProvider({
      baseUrl: "https://tts.invalid",
    });

    const slow = await provider.synthesize(input(0.75));
    const fast = await provider.synthesize(input(2.5));

    expect(slow.cacheKey).toBe(fast.cacheKey);
    expect(slow.contentHash).toBe(fast.contentHash);
  });

  it("preserves an explicit backend cache key", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse({
        audioUrl: "https://audio.invalid/server-key.mp3",
        voiceId: "voice:test",
        provider: "azure",
        model: "neural-v3",
        cacheKey: "backend-authoritative-cache-key",
      })),
    );

    const provider = new RenderReadTtsProvider({
      baseUrl: "https://tts.invalid",
    });

    const asset = await provider.synthesize(input());

    expect(asset.cacheKey).toBe("backend-authoritative-cache-key");
    expect(asset.contentHash).toMatch(/^sha256:[0-9a-f]{64}$/);
  });
});
