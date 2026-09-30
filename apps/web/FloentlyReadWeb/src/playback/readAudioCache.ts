import type { ReadTtsAsset } from "../tts/readTtsProvider";

export interface PlayableAudioAsset {
  url: string;
  cacheKey: string;
  contentHash: string;
  fromBrowserCache: boolean;
  release: () => void;
}

export interface ReadAudioCachePort {
  resolve(asset: ReadTtsAsset): Promise<PlayableAudioAsset>;
  dispose(): void;
}

interface AudioCacheMetadata {
  cacheKey: string;
  contentHash: string;
  sourceUrl: string;
  cachedAt: number;
  lastAccessedAt: number;
}

const CACHE_NAME = "floently-read-audio-v1";
const DB_NAME = "floently-read-audio-v1";
const STORE_NAME = "assets";
const MAX_CACHE_ENTRIES = 32;

function hasCacheStorage(): boolean {
  return typeof caches !== "undefined";
}

function syntheticCacheUrl(cacheKey: string): string {
  const origin =
    typeof location !== "undefined"
      ? location.origin
      : "https://read.floently.invalid";
  return `${origin}/__floently_read_audio_cache__/${encodeURIComponent(cacheKey)}`;
}

function openMetadataDb(): Promise<IDBDatabase | null> {
  if (typeof indexedDB === "undefined") {
    return Promise.resolve(null);
  }

  return new Promise((resolve) => {
    const request = indexedDB.open(DB_NAME, 1);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, {
          keyPath: "cacheKey",
        });
        store.createIndex("lastAccessedAt", "lastAccessedAt");
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => resolve(null);
  });
}

async function writeMetadata(metadata: AudioCacheMetadata): Promise<void> {
  const db = await openMetadataDb();
  if (!db) return;

  await new Promise<void>((resolve) => {
    const transaction = db.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).put(metadata);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => resolve();
    transaction.onabort = () => resolve();
  });

  db.close();
}

async function readAllMetadata(): Promise<AudioCacheMetadata[]> {
  const db = await openMetadataDb();
  if (!db) return [];

  const result = await new Promise<AudioCacheMetadata[]>((resolve) => {
    const transaction = db.transaction(STORE_NAME, "readonly");
    const request = transaction.objectStore(STORE_NAME).getAll();
    request.onsuccess = () =>
      resolve((request.result as AudioCacheMetadata[]) ?? []);
    request.onerror = () => resolve([]);
  });

  db.close();
  return result;
}

async function deleteMetadata(cacheKey: string): Promise<void> {
  const db = await openMetadataDb();
  if (!db) return;

  await new Promise<void>((resolve) => {
    const transaction = db.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).delete(cacheKey);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => resolve();
    transaction.onabort = () => resolve();
  });

  db.close();
}

export class ReadAudioCache implements ReadAudioCachePort {
  private readonly objectUrls = new Set<string>();

  async resolve(asset: ReadTtsAsset): Promise<PlayableAudioAsset> {
    const cacheKey = asset.cacheKey || asset.contentHash;
    const cacheUrl = syntheticCacheUrl(cacheKey);

    if (hasCacheStorage()) {
      try {
        const cache = await caches.open(CACHE_NAME);
        const cached = await cache.match(cacheUrl);
        if (cached) {
          const blob = await cached.blob();
          const objectUrl = URL.createObjectURL(blob);
          this.objectUrls.add(objectUrl);

          void writeMetadata({
            cacheKey,
            contentHash: asset.contentHash,
            sourceUrl: asset.audioUrl,
            cachedAt: Date.now(),
            lastAccessedAt: Date.now(),
          });

          return {
            url: objectUrl,
            cacheKey,
            contentHash: asset.contentHash,
            fromBrowserCache: true,
            release: () => this.releaseObjectUrl(objectUrl),
          };
        }
      } catch {
        // Cache APIs are an optimization. Direct media playback remains valid.
      }
    }

    const stored = await this.tryStore(asset, cacheUrl);
    if (stored) {
      return stored;
    }

    return {
      url: asset.audioUrl,
      cacheKey,
      contentHash: asset.contentHash,
      fromBrowserCache: false,
      release: () => undefined,
    };
  }

  dispose(): void {
    for (const url of this.objectUrls) {
      URL.revokeObjectURL(url);
    }
    this.objectUrls.clear();
  }

  private async tryStore(
    asset: ReadTtsAsset,
    cacheUrl: string,
  ): Promise<PlayableAudioAsset | null> {
    if (!hasCacheStorage()) return null;

    try {
      const response = await fetch(asset.audioUrl, {
        method: "GET",
        mode: "cors",
        credentials: "omit",
      });

      if (!response.ok || response.type === "opaque") {
        return null;
      }

      const cache = await caches.open(CACHE_NAME);
      await cache.put(cacheUrl, response.clone());

      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      this.objectUrls.add(objectUrl);

      await writeMetadata({
        cacheKey: asset.cacheKey,
        contentHash: asset.contentHash,
        sourceUrl: asset.audioUrl,
        cachedAt: Date.now(),
        lastAccessedAt: Date.now(),
      });

      void this.prune();

      return {
        url: objectUrl,
        cacheKey: asset.cacheKey,
        contentHash: asset.contentHash,
        fromBrowserCache: false,
        release: () => this.releaseObjectUrl(objectUrl),
      };
    } catch {
      return null;
    }
  }

  private releaseObjectUrl(url: string): void {
    if (!this.objectUrls.delete(url)) return;
    URL.revokeObjectURL(url);
  }

  private async prune(): Promise<void> {
    const entries = await readAllMetadata();
    if (entries.length <= MAX_CACHE_ENTRIES) return;

    const stale = entries
      .sort((a, b) => b.lastAccessedAt - a.lastAccessedAt)
      .slice(MAX_CACHE_ENTRIES);

    const cache = hasCacheStorage()
      ? await caches.open(CACHE_NAME).catch(() => null)
      : null;

    for (const entry of stale) {
      if (cache) {
        await cache
          .delete(syntheticCacheUrl(entry.cacheKey))
          .catch(() => false);
      }
      await deleteMetadata(entry.cacheKey);
    }
  }
}
