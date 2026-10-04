import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import type {
  LogicalTimePosition,
  ReadingManifestSummary,
  ReadingSegmentDescriptor,
} from "../readCore.types";
import type {
  ReadTtsAsset,
  ReadTtsInput,
  ReadTtsProvider,
} from "../tts/readTtsProvider";
import type {
  BrowserAudioEngineCallbacks,
  ReadAudioEngine,
} from "./browserAudioEngine";
import type {
  PlayableAudioAsset,
  ReadAudioCachePort,
} from "./readAudioCache";
import {
  WebPlaybackSession,
  type ReadPlaybackCore,
} from "./webPlaybackSession";

const SEGMENT_DURATION_MS = 10_000;

function makeSegments(): ReadingSegmentDescriptor[] {
  return [0, 1, 2].map((index) => ({
    id: `segment-${index}`,
    index,
    text: `Hidden text for segment ${index}.`,
    scalarStart: index * 100,
    scalarEnd: (index + 1) * 100,
    wordStart: index * 10,
    wordEnd: (index + 1) * 10,
    wordCount: 10,
    estimatedSourceDurationMs: SEGMENT_DURATION_MS,
    logicalStartMs: index * SEGMENT_DURATION_MS,
    logicalEndMs: (index + 1) * SEGMENT_DURATION_MS,
  }));
}

function makeManifest(): ReadingManifestSummary {
  const segments = makeSegments();

  return {
    handle: "doc:rev",
    schemaVersion: 1,
    documentId: "doc",
    revisionId: "rev",
    title: "Thirty second test book",
    language: "en",
    wordCount: 30,
    textScalarLength: 300,
    estimatedSourceDurationMs: 30_000,
    segmentCount: 3,
    firstSegments: segments,
    buildMs: 1,
  };
}

class FakeCore implements ReadPlaybackCore {
  readonly segments = makeSegments();
  prefetchResult: number[] = [];

  async segmentForLogicalTime(
    _handle: string,
    elapsedMs: number,
  ): Promise<LogicalTimePosition | null> {
    const bounded = Math.min(29_999, Math.max(0, elapsedMs));
    const index = Math.floor(bounded / SEGMENT_DURATION_MS);

    return {
      index,
      localOffsetMs: bounded - index * SEGMENT_DURATION_MS,
    };
  }

  async getSegment(
    _handle: string,
    index: number,
  ): Promise<ReadingSegmentDescriptor> {
    const segment = this.segments[index];
    if (!segment) {
      throw new Error(`Missing fake segment ${index}`);
    }
    return segment;
  }

  async prefetchIndexes(): Promise<number[]> {
    return [...this.prefetchResult];
  }
}

class FakeTts implements ReadTtsProvider {
  readonly id = "fake-tts";
  readonly calls: ReadTtsInput[] = [];
  readonly failures = new Set<string>();

  async synthesize(input: ReadTtsInput): Promise<ReadTtsAsset> {
    this.calls.push({ ...input });

    if (this.failures.has(input.segmentId)) {
      throw new Error(`Synthetic TTS failure for ${input.segmentId}`);
    }

    return {
      audioUrl: `https://audio.invalid/${input.segmentId}.mp3`,
      cacheKey: `cache:${input.voiceId}:${input.segmentId}`,
      contentHash: `hash:${input.voiceId}:${input.segmentId}`,
      cacheHit: false,
      voiceId: input.voiceId,
      durationMs: SEGMENT_DURATION_MS,
      rawWordTimings: [],
    };
  }
}

class FakeCache implements ReadAudioCachePort {
  readonly released: string[] = [];

  async resolve(asset: ReadTtsAsset): Promise<PlayableAudioAsset> {
    return {
      url: asset.audioUrl,
      cacheKey: asset.cacheKey,
      contentHash: asset.contentHash,
      fromBrowserCache: false,
      release: () => {
        this.released.push(asset.cacheKey);
      },
    };
  }

  dispose(): void {
    // Nothing to dispose in the deterministic fake.
  }
}

class FakeEngine implements ReadAudioEngine {
  paused = true;
  readonly loads: Array<{
    url: string;
    localOffsetMs: number;
    playbackRate: number;
  }> = [];
  readonly rates: number[] = [];
  readonly primed: string[][] = [];
  destroyed = false;

  constructor(
    private readonly callbacks: BrowserAudioEngineCallbacks,
  ) {}

  async load(
    url: string,
    localOffsetMs: number,
    playbackRate: number,
  ): Promise<number> {
    this.loads.push({
      url,
      localOffsetMs,
      playbackRate,
    });
    this.rates.push(playbackRate);
    return SEGMENT_DURATION_MS;
  }

  async play(): Promise<void> {
    this.paused = false;
    this.callbacks.onPlaying();
  }

  pause(): void {
    this.paused = true;
  }

  setRate(rate: number): void {
    this.rates.push(rate);
  }

  prime(urls: string[]): void {
    this.primed.push([...urls]);
  }

  destroy(): void {
    this.destroyed = true;
    this.paused = true;
  }

  emitTime(
    currentTimeMs: number,
    physicalDurationMs = SEGMENT_DURATION_MS,
  ): void {
    this.callbacks.onTime(currentTimeMs, physicalDurationMs);
  }

  emitEnded(): void {
    this.paused = true;
    this.callbacks.onEnded();
  }
}

interface Harness {
  session: WebPlaybackSession;
  core: FakeCore;
  tts: FakeTts;
  cache: FakeCache;
  engine: FakeEngine;
}

function createHarness(
  initialPreferences?: {
    speed?: number;
    voiceId?: string | null;
    voiceLanguage?: string | null;
  },
): Harness {
  const core = new FakeCore();
  const tts = new FakeTts();
  const cache = new FakeCache();
  let engine: FakeEngine | null = null;

  const session = new WebPlaybackSession({
    core,
    tts,
    cache,
    initialPreferences,
    engineFactory: (callbacks) => {
      engine = new FakeEngine(callbacks);
      return engine;
    },
  });

  if (!engine) {
    throw new Error("Fake audio engine was not created.");
  }

  return {
    session,
    core,
    tts,
    cache,
    engine,
  };
}

const originalNavigatorDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "navigator");

beforeEach(() => {
  vi.restoreAllMocks();

  if (originalNavigatorDescriptor) {
    Object.defineProperty(
      globalThis,
      "navigator",
      originalNavigatorDescriptor,
    );
  } else {
    Reflect.deleteProperty(globalThis, "navigator");
  }
});

afterEach(() => {
  if (originalNavigatorDescriptor) {
    Object.defineProperty(
      globalThis,
      "navigator",
      originalNavigatorDescriptor,
    );
  } else {
    Reflect.deleteProperty(globalThis, "navigator");
  }
});

describe("WebPlaybackSession document-wide contract", () => {
  it("publishes the whole document duration before any TTS exists", () => {
    const { session, tts } = createHarness();

    session.loadDocument(makeManifest());

    expect(session.getSnapshot()).toMatchObject({
      documentId: "doc",
      revisionId: "rev",
      durationMs: 30_000,
      elapsedMs: 0,
      status: "ready",
    });
    expect(tts.calls).toHaveLength(0);

    session.destroy();
  });

  it("plays a hidden segment without changing public document identity", async () => {
    const { session, tts, engine } = createHarness();

    session.loadDocument(makeManifest());
    await session.play();

    expect(tts.calls[0]).toMatchObject({
      segmentId: "segment-0",
    });
    expect(engine.loads.at(-1)).toMatchObject({
      url: "https://audio.invalid/segment-0.mp3",
      localOffsetMs: 0,
      playbackRate: 1,
    });
    expect(session.getSnapshot()).toMatchObject({
      documentId: "doc",
      title: "Thirty second test book",
      durationMs: 30_000,
      status: "playing",
    });

    session.destroy();
  });

  it("maps a long document seek to hidden segment and local media time", async () => {
    const { session, tts, engine } = createHarness();

    session.loadDocument(makeManifest());
    await session.seek(25_000);

    expect(tts.calls.at(-1)).toMatchObject({
      segmentId: "segment-2",
    });
    expect(engine.loads.at(-1)).toMatchObject({
      url: "https://audio.invalid/segment-2.mp3",
      localOffsetMs: 5_000,
    });
    expect(session.getSnapshot()).toMatchObject({
      elapsedMs: 25_000,
      durationMs: 30_000,
      status: "paused",
    });

    session.destroy();
  });

  it("exposes the active reading region and canonical scalar cursor", async () => {
    const { session, engine } = createHarness();

    session.loadDocument(makeManifest());
    await session.play();
    engine.emitTime(5_000, 10_000);

    expect(session.getSnapshot()).toMatchObject({
      activeSegmentIndex: 0,
      canonicalScalarCursor: 50,
      elapsedMs: 5_000,
    });

    await session.seek(15_000);

    expect(session.getSnapshot()).toMatchObject({
      activeSegmentIndex: 1,
      canonicalScalarCursor: 150,
      elapsedMs: 15_000,
    });

    session.destroy();
  });

  it("can change the preferred voice before any document is loaded", async () => {
    const { session } = createHarness();

    await session.setVoice(
      "voice:future-en",
      { language: "en-US" },
    );

    expect(session.getSnapshot()).toMatchObject({
      status: "idle",
      documentId: null,
      voiceId: "voice:future-en",
    });

    session.loadDocument(makeManifest());
    expect(session.getSnapshot().voiceId).toBe("voice:future-en");

    session.destroy();
  });

  it("starts a fresh session from persisted speed and same-language voice defaults", () => {
    const { session } = createHarness({
      speed: 1.75,
      voiceId: "voice:persistent-en",
      voiceLanguage: "en-US",
    });

    session.loadDocument(makeManifest());

    expect(session.getSnapshot()).toMatchObject({
      speed: 1.75,
      voiceId: "voice:persistent-en",
    });

    session.destroy();
  });

  it("does not use a persisted voice for a different document language", () => {
    const { session } = createHarness({
      speed: 2,
      voiceId: "voice:persistent-en",
      voiceLanguage: "en",
    });

    session.loadDocument({
      ...makeManifest(),
      language: "fi-FI",
    });

    expect(session.getSnapshot().speed).toBe(2);
    expect(session.getSnapshot().voiceId).not.toBe("voice:persistent-en");

    session.destroy();
  });

  it("keeps a document-specific resume speed from replacing the session default", () => {
    const { session } = createHarness({
      speed: 1.5,
    });

    session.loadDocument(makeManifest());
    session.setSpeed(2.5, { updatePreference: false });
    session.clear();
    session.loadDocument({
      ...makeManifest(),
      documentId: "next",
      revisionId: "next-rev",
      handle: "next:next-rev",
    });

    expect(session.getSnapshot().speed).toBe(1.5);

    session.destroy();
  });

  it("keeps the selected speed across document replacement and clear", () => {
    const { session } = createHarness();

    session.loadDocument(makeManifest());
    session.setSpeed(2.5);
    session.clear();

    session.loadDocument({
      ...makeManifest(),
      handle: "doc-2:rev-2",
      documentId: "doc-2",
      revisionId: "rev-2",
      title: "Another article",
    });

    expect(session.getSnapshot()).toMatchObject({
      documentId: "doc-2",
      speed: 2.5,
    });

    session.destroy();
  });

  it("keeps a selected voice across same-language page replacement", async () => {
    const { session } = createHarness();

    session.loadDocument(makeManifest());
    await session.setVoice("voice:new");
    session.clear();

    session.loadDocument({
      ...makeManifest(),
      handle: "browser:page-2:rev-2",
      documentId: "browser:page-2",
      revisionId: "rev-2",
      title: "Next web page",
      language: "en",
    });

    expect(session.getSnapshot().voiceId).toBe("voice:new");

    session.loadDocument({
      ...makeManifest(),
      handle: "browser:page-fi:rev-1",
      documentId: "browser:page-fi",
      revisionId: "rev-1",
      title: "Finnish page",
      language: "fi",
    });

    expect(session.getSnapshot().voiceId).not.toBe("voice:new");

    session.destroy();
  });

  it("keeps speed across hidden segment transitions", async () => {
    const { session, tts, engine } = createHarness();

    session.loadDocument(makeManifest());
    session.setSpeed(2);
    await session.play();

    expect(session.getSnapshot().speed).toBe(2);
    engine.emitEnded();

    await vi.waitFor(() => {
      expect(
        tts.calls.some((call) => call.segmentId === "segment-1"),
      ).toBe(true);
      expect(engine.loads.at(-1)?.url).toContain("segment-1.mp3");
    });

    expect(engine.loads.at(-1)?.playbackRate).toBe(2);
    expect(session.getSnapshot().speed).toBe(2);

    session.destroy();
  });

  it("invalidates old-voice audio while preserving the logical cursor", async () => {
    const { session, tts, cache } = createHarness();

    session.loadDocument(makeManifest());
    await session.seek(15_000);

    const beforeVoiceChange = session.getSnapshot().elapsedMs;
    const oldCacheKey = tts.calls.at(-1)?.voiceId
      ? `cache:${tts.calls.at(-1)?.voiceId}:segment-1`
      : "";

    await session.setVoice("voice:new");

    expect(session.getSnapshot()).toMatchObject({
      elapsedMs: beforeVoiceChange,
      voiceId: "voice:new",
      status: "paused",
    });
    expect(cache.released).toContain(oldCacheKey);

    await session.play();

    expect(tts.calls.at(-1)).toMatchObject({
      segmentId: "segment-1",
      voiceId: "voice:new",
    });
    expect(session.getSnapshot().elapsedMs).toBe(beforeVoiceChange);

    session.destroy();
  });

  it("keeps current playback healthy when speculative prefetch fails", async () => {
    const { session, core, tts } = createHarness();

    core.prefetchResult = [1];
    tts.failures.add("segment-1");

    session.loadDocument(makeManifest());
    await session.play();

    await vi.waitFor(() => {
      expect(
        tts.calls.some((call) => call.segmentId === "segment-1"),
      ).toBe(true);
    });

    expect(session.getSnapshot()).toMatchObject({
      status: "playing",
      error: null,
      durationMs: 30_000,
    });

    session.destroy();
  });

  it("flushes resume state when the page becomes hidden without forcing pause", async () => {
    localStorage.clear();
    const { session, engine } = createHarness();

    session.loadDocument(makeManifest());
    await session.play();
    engine.emitTime(4_200, 10_000);

    const key = "floently-read-resume-v1:doc:rev";
    localStorage.removeItem(key);

    const visibilityDescriptor = Object.getOwnPropertyDescriptor(
      document,
      "visibilityState",
    );
    Object.defineProperty(document, "visibilityState", {
      configurable: true,
      value: "hidden",
    });

    document.dispatchEvent(new Event("visibilitychange"));

    expect(engine.paused).toBe(false);
    expect(JSON.parse(localStorage.getItem(key) ?? "{}")).toMatchObject({
      elapsedMs: 4_200,
      speed: 1,
    });

    if (visibilityDescriptor) {
      Object.defineProperty(
        document,
        "visibilityState",
        visibilityDescriptor,
      );
    }

    session.destroy();
  });

  it("republishes Media Session state after page restoration", () => {
    const fakeMediaSession = {
      metadata: null,
      playbackState: "none",
      setActionHandler: vi.fn(),
      setPositionState: vi.fn(),
    };

    Object.defineProperty(globalThis, "navigator", {
      configurable: true,
      value: {
        mediaSession: fakeMediaSession,
      },
    });

    const { session } = createHarness();
    session.loadDocument(makeManifest());

    fakeMediaSession.setPositionState.mockClear();
    fakeMediaSession.metadata = null;

    window.dispatchEvent(new Event("pageshow"));

    expect(fakeMediaSession.metadata).not.toBeNull();
    expect(fakeMediaSession.setPositionState).toHaveBeenCalledWith({
      duration: 30,
      playbackRate: 1,
      position: 0,
    });

    session.destroy();
  });

  it("removes page lifecycle listeners when destroyed", () => {
    localStorage.clear();
    const { session } = createHarness();

    session.loadDocument(makeManifest());
    session.destroy();

    const key = "floently-read-resume-v1:doc:rev";
    localStorage.removeItem(key);
    window.dispatchEvent(new Event("pagehide"));

    expect(localStorage.getItem(key)).toBeNull();
  });

  it("publishes document time to Media Session instead of clip duration", async () => {
    const positionStates: Array<{
      duration: number;
      playbackRate: number;
      position: number;
    }> = [];

    const fakeMediaSession = {
      metadata: null,
      playbackState: "none",
      setActionHandler: vi.fn(),
      setPositionState: vi.fn((value) => {
        positionStates.push({ ...value });
      }),
    };

    Object.defineProperty(globalThis, "navigator", {
      configurable: true,
      value: {
        mediaSession: fakeMediaSession,
      },
    });

    const { session, engine } = createHarness();

    session.loadDocument(makeManifest());
    await session.play();
    engine.emitTime(5_000, 10_000);

    expect(positionStates.at(-1)).toEqual({
      duration: 30,
      playbackRate: 1,
      position: 5,
    });
    expect(positionStates.at(-1)?.duration).not.toBe(10);

    session.destroy();
  });
});
