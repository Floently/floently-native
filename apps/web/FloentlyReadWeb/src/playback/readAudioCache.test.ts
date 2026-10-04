import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import type { ReadTtsAsset } from "../tts/readTtsProvider";
import {
  ReadAudioCache,
  readAudioCacheStorageNames,
} from "./readAudioCache";

const originalCachesDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "caches");
const originalFetchDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "fetch");
const originalIndexedDbDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "indexedDB");
const originalCreateObjectUrl = URL.createObjectURL;
const originalRevokeObjectUrl = URL.revokeObjectURL;

function restoreGlobal(
  name: "caches" | "fetch" | "indexedDB",
  descriptor: PropertyDescriptor | undefined,
): void {
  if (descriptor) {
    Object.defineProperty(globalThis, name, descriptor);
  } else {
    Reflect.deleteProperty(globalThis, name);
  }
}

function asset(): ReadTtsAsset {
  return {
    audioUrl: "https://audio.invalid/segment.mp3",
    cacheKey: "segment-cache-key",
    contentHash: "sha256:segment",
    cacheHit: false,
    voiceId: "voice:test",
    rawWordTimings: [],
  };
}

function quotaError(): Error {
  const error = new Error("origin quota full");
  error.name = "QuotaExceededError";
  return error;
}

function installCacheHarness(
  put: (request: RequestInfo | URL, response: Response) => Promise<void>,
) {
  const cache = {
    match: vi.fn(async () => undefined),
    put: vi.fn(put),
    delete: vi.fn(async () => true),
  } as unknown as Cache;

  const open = vi.fn(async () => cache);
  Object.defineProperty(globalThis, "caches", {
    configurable: true,
    value: { open },
  });
  Object.defineProperty(globalThis, "indexedDB", {
    configurable: true,
    value: undefined,
  });

  const fetchMock = vi.fn(async () =>
    new Response(new Blob(["already downloaded audio bytes"]), {
      status: 200,
      headers: {
        "Content-Type": "audio/mpeg",
      },
    }));
  Object.defineProperty(globalThis, "fetch", {
    configurable: true,
    value: fetchMock,
  });

  let objectUrlSequence = 0;
  URL.createObjectURL = vi.fn(
    () => `blob:read-cache-${++objectUrlSequence}`,
  );
  URL.revokeObjectURL = vi.fn();

  return {
    cache,
    open,
    fetchMock,
  };
}

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  restoreGlobal("caches", originalCachesDescriptor);
  restoreGlobal("fetch", originalFetchDescriptor);
  restoreGlobal("indexedDB", originalIndexedDbDescriptor);
  URL.createObjectURL = originalCreateObjectUrl;
  URL.revokeObjectURL = originalRevokeObjectUrl;
});

describe("ReadAudioCache account storage isolation", () => {
  it("derives distinct durable storage namespaces per authenticated account", () => {
    const first = readAudioCacheStorageNames("account-a");
    const second = readAudioCacheStorageNames("account-b");

    expect(first).not.toEqual(second);
    expect(first.cacheName).toContain("account-a");
    expect(first.dbName).toContain("account-a");
    expect(second.cacheName).toContain("account-b");
    expect(second.dbName).toContain("account-b");
  });

  it("opens only the current account cache namespace", async () => {
    const { open } = installCacheHarness(async () => undefined);
    const audioCache = new ReadAudioCache({ ownerId: "account-a" });

    const playable = await audioCache.resolve(asset());

    expect(open).toHaveBeenCalled();
    expect(
      open.mock.calls.every(
        ([name]) =>
          name === readAudioCacheStorageNames("account-a").cacheName,
      ),
    ).toBe(true);

    playable.release();
    audioCache.dispose();
  });
});

describe("ReadAudioCache quota-pressure recovery", () => {
  it("retries one cache write after a quota-exceeded failure", async () => {
    let attempts = 0;
    const { cache, fetchMock } = installCacheHarness(async () => {
      attempts += 1;
      if (attempts === 1) {
        throw quotaError();
      }
    });

    const audioCache = new ReadAudioCache();
    const playable = await audioCache.resolve(asset());

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(cache.put).toHaveBeenCalledTimes(2);
    expect(playable.url).toBe("blob:read-cache-1");
    expect(playable.fromBrowserCache).toBe(false);

    playable.release();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(
      "blob:read-cache-1",
    );
    audioCache.dispose();
  });

  it("recognizes legacy browser quota exceptions as storage pressure", async () => {
    let attempts = 0;
    const { cache } = installCacheHarness(async () => {
      attempts += 1;
      if (attempts === 1) {
        const error = new Error("legacy quota full");
        error.name = "NS_ERROR_DOM_QUOTA_REACHED";
        throw error;
      }
    });

    const audioCache = new ReadAudioCache();
    const playable = await audioCache.resolve(asset());

    expect(cache.put).toHaveBeenCalledTimes(2);

    playable.release();
    audioCache.dispose();
  });

  it("keeps already-fetched bytes playable when quota retry also fails", async () => {
    const { cache, fetchMock } = installCacheHarness(async () => {
      throw quotaError();
    });

    const audioCache = new ReadAudioCache();
    const playable = await audioCache.resolve(asset());

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(cache.put).toHaveBeenCalledTimes(2);
    expect(playable).toMatchObject({
      url: "blob:read-cache-1",
      cacheKey: "segment-cache-key",
      contentHash: "sha256:segment",
      fromBrowserCache: false,
    });

    playable.release();
    audioCache.dispose();
  });

  it("does not pressure-retry unrelated cache write failures", async () => {
    const { cache, fetchMock } = installCacheHarness(async () => {
      throw new Error("cache backend unavailable");
    });

    const audioCache = new ReadAudioCache();
    const playable = await audioCache.resolve(asset());

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(cache.put).toHaveBeenCalledTimes(1);
    expect(playable.url).toBe("blob:read-cache-1");

    playable.release();
    audioCache.dispose();
  });
});
