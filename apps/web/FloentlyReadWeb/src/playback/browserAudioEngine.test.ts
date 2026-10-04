import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import {
  BrowserAudioEngine,
  type BrowserAudioEngineCallbacks,
} from "./browserAudioEngine";

class FakeAudioElement extends EventTarget {
  static instances: FakeAudioElement[] = [];

  preload = "";
  src = "";
  paused = true;
  playbackRate = 1;
  duration = 10;
  currentTime = 0;
  readyState = 1;
  error: MediaError | null = null;
  loadCount = 0;
  playCount = 0;
  pauseCount = 0;

  constructor() {
    super();
    FakeAudioElement.instances.push(this);
  }

  load(): void {
    this.loadCount += 1;
  }

  async play(): Promise<void> {
    this.playCount += 1;
    this.paused = false;
    this.dispatchEvent(new Event("playing"));
  }

  pause(): void {
    this.pauseCount += 1;
    this.paused = true;
  }
}

const originalAudioDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "Audio");
const originalHtmlMediaElementDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "HTMLMediaElement");

function restoreGlobal(
  name: "Audio" | "HTMLMediaElement",
  descriptor: PropertyDescriptor | undefined,
): void {
  if (descriptor) {
    Object.defineProperty(globalThis, name, descriptor);
  } else {
    Reflect.deleteProperty(globalThis, name);
  }
}

function installFakeMediaGlobals(): void {
  FakeAudioElement.instances = [];

  Object.defineProperty(globalThis, "Audio", {
    configurable: true,
    value: FakeAudioElement,
  });
  Object.defineProperty(globalThis, "HTMLMediaElement", {
    configurable: true,
    value: {
      HAVE_METADATA: 1,
    },
  });
}

function callbacks(): BrowserAudioEngineCallbacks {
  return {
    onTime: vi.fn(),
    onEnded: vi.fn(),
    onWaiting: vi.fn(),
    onPlaying: vi.fn(),
    onError: vi.fn(),
  };
}

beforeEach(() => {
  installFakeMediaGlobals();
});

afterEach(() => {
  restoreGlobal("Audio", originalAudioDescriptor);
  restoreGlobal("HTMLMediaElement", originalHtmlMediaElementDescriptor);
});

describe("BrowserAudioEngine prefetched handoff", () => {
  it("promotes a primed element without loading that URL twice", async () => {
    const cb = callbacks();
    const engine = new BrowserAudioEngine(cb);

    engine.prime(["https://audio.invalid/next.mp3"]);

    expect(FakeAudioElement.instances).toHaveLength(2);
    const initial = FakeAudioElement.instances[0];
    const prepared = FakeAudioElement.instances[1];

    expect(prepared.src).toBe("https://audio.invalid/next.mp3");
    expect(prepared.loadCount).toBe(1);

    const duration = await engine.load(
      "https://audio.invalid/next.mp3",
      2_500,
      1.5,
    );

    expect(duration).toBe(10_000);
    expect(FakeAudioElement.instances).toHaveLength(2);
    expect(prepared.loadCount).toBe(1);
    expect(prepared.currentTime).toBe(2.5);
    expect(prepared.playbackRate).toBe(1.5);
    expect(initial.src).toBe("");

    await engine.play();
    expect(prepared.playCount).toBe(1);

    engine.destroy();
  });

  it("loads a non-primed URL into the active element normally", async () => {
    const engine = new BrowserAudioEngine(callbacks());
    const active = FakeAudioElement.instances[0];

    await engine.load(
      "https://audio.invalid/current.mp3",
      0,
      1.25,
    );

    expect(FakeAudioElement.instances).toHaveLength(1);
    expect(active.src).toBe("https://audio.invalid/current.mp3");
    expect(active.loadCount).toBe(1);
    expect(active.playbackRate).toBe(1.25);

    engine.destroy();
  });

  it("detaches timeline callbacks from the retired active element", async () => {
    const cb = callbacks();
    const engine = new BrowserAudioEngine(cb);

    await engine.load(
      "https://audio.invalid/current.mp3",
      0,
      1,
    );
    const retired = FakeAudioElement.instances[0];

    engine.prime(["https://audio.invalid/next.mp3"]);
    const promoted = FakeAudioElement.instances[1];

    await engine.load(
      "https://audio.invalid/next.mp3",
      0,
      1,
    );

    vi.mocked(cb.onTime).mockClear();

    retired.currentTime = 4;
    retired.dispatchEvent(new Event("timeupdate"));
    expect(cb.onTime).not.toHaveBeenCalled();

    promoted.currentTime = 3;
    promoted.dispatchEvent(new Event("timeupdate"));
    expect(cb.onTime).toHaveBeenCalledWith(3_000, 10_000);

    engine.destroy();
  });

  it("releases the active and remaining prefetched elements on destroy", () => {
    const engine = new BrowserAudioEngine(callbacks());

    engine.prime([
      "https://audio.invalid/one.mp3",
      "https://audio.invalid/two.mp3",
    ]);

    expect(FakeAudioElement.instances).toHaveLength(3);

    engine.destroy();

    for (const element of FakeAudioElement.instances) {
      expect(element.paused).toBe(true);
      expect(element.src).toBe("");
      expect(element.loadCount).toBeGreaterThanOrEqual(1);
    }
  });
});
