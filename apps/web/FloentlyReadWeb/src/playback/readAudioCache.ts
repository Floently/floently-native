import type { ReadTtsAsset } from "../tts/readTtsProvider";
import {
  DEFAULT_READ_AUDIO_CACHE_BUDGET,
  planReadAudioCacheEvictions,
  planReadAudioCachePressureEvictions,
  resolveReadAudioCacheKey,
} from "./readAudioCachePolicy";

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
  byteSize?: number;
}

const CACHE_NAME = "floently-read-audio-v1";
const DB_NAME = "floently-read-audio-v1";
const STORE_NAME = "assets";
const MIB = 1024 * 1024;
const QUOTA_PRESSURE_MIN_HEADROOM_BYTES = 4 * MIB;
const QUOTA_PRESSURE_MAX_HEADROOM_BYTES = 16 * MIB;

function hasCacheStorage(): boolean {
  return typeof caches !== "undefined";
}

function isQuotaExceededError(error: unknown): boolean {
  if (!error || typeof error !== "object") return false;

  const value = error as {
    name?: unknown;
    code?: unknown;
  };
  const name =
    typeof value.name === "string"
      ? value.name
      : "";
  const code =
    typeof value.code === "number"
      ? value.code
      : null;

  return (
    name === "QuotaExceededError"
    || name === "NS_ERROR_DOM_QUOTA_REACHED"
    || code === 22
    || code === 1014
  );
}

function quotaPressureBytesToFree(incomingBytes: number): number {
  const incoming = Math.max(
    1,
    Number.isFinite(incomingBytes)
      ? Math.floor(incomingBytes)
      : 1,
  );
  const safetyHeadroom = Math.min(
    QUOTA_PRESSURE_MAX_HEADROOM_BYTES,
    Math.max(
      QUOTA_PRESSURE_MIN_HEADROOM_BYTES,
      incoming,
    ),
  );

  return incoming + safetyHeadroom;
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

function hasKnownByteSize(
  metadata: AudioCacheMetadata,
): metadata is AudioCacheMetadata & { byteSize: number } {
  return (
    Number.isFinite(metadata.byteSize)
    && Number(metadata.byteSize) >= 0
  );
}

export class ReadAudioCache implements ReadAudioCachePort {
  private readonly objectUrls = new Map<string, string>();
  private readonly activeLeaseCounts = new Map<string, number>();
  private pruneInProgress = false;
  private pruneRequested = false;
  private disposed = false;

  async resolve(asset: ReadTtsAsset): Promise<PlayableAudioAsset> {
    const cacheKey = resolveReadAudioCacheKey(
      asset.cacheKey,
      asset.contentHash,
    );

    if (!cacheKey) {
      return {
        url: asset.audioUrl,
        cacheKey: "",
        contentHash: asset.contentHash,
        fromBrowserCache: false,
        release: () => undefined,
      };
    }

    const cacheUrl = syntheticCacheUrl(cacheKey);

    if (hasCacheStorage()) {
      try {
        const cache = await caches.open(CACHE_NAME);
        const cached = await cache.match(cacheUrl);
        if (cached) {
          const blob = await cached.blob();
          const objectUrl = this.createLeasedObjectUrl(blob, cacheKey);
          const now = Date.now();

          void writeMetadata({
            cacheKey,
            contentHash: asset.contentHash,
            sourceUrl: asset.audioUrl,
            cachedAt: now,
            lastAccessedAt: now,
            byteSize: blob.size,
          }).finally(() => this.schedulePrune());

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

    const stored = await this.tryStore(asset, cacheUrl, cacheKey);
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
    this.disposed = true;
    this.pruneRequested = false;

    for (const url of this.objectUrls.keys()) {
      URL.revokeObjectURL(url);
    }

    this.objectUrls.clear();
    this.activeLeaseCounts.clear();
  }

  private async tryStore(
    asset: ReadTtsAsset,
    cacheUrl: string,
    cacheKey: string,
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

      const firstCacheResponse = response.clone();
      const blob = await response.blob();

      let cache: Cache | null = null;
      try {
        cache = await caches.open(CACHE_NAME);
      } catch {
        return this.transientPlayable(blob, asset, cacheKey);
      }

      let persisted = false;

      try {
        await cache.put(cacheUrl, firstCacheResponse);
        persisted = true;
      } catch (error) {
        if (isQuotaExceededError(error)) {
          await this.pruneForQuotaPressure(
            cache,
            quotaPressureBytesToFree(blob.size),
          );

          try {
            await cache.put(
              cacheUrl,
              new Response(blob, {
                status: 200,
                headers: {
                  "Content-Type":
                    blob.type
                    || response.headers.get("Content-Type")
                    || "application/octet-stream",
                },
              }),
            );
            persisted = true;
          } catch {
            // A single pressure-recovery retry is enough. The fetched bytes
            // remain playable below even when persistence is still denied.
          }
        }
      }

      const playable = this.transientPlayable(
        blob,
        asset,
        cacheKey,
      );

      if (persisted) {
        const now = Date.now();
        await writeMetadata({
          cacheKey,
          contentHash: asset.contentHash,
          sourceUrl: asset.audioUrl,
          cachedAt: now,
          lastAccessedAt: now,
          byteSize: blob.size,
        });
        this.schedulePrune();
      }

      return playable;
    } catch {
      return null;
    }
  }

  private transientPlayable(
    blob: Blob,
    asset: ReadTtsAsset,
    cacheKey: string,
  ): PlayableAudioAsset {
    const objectUrl = this.createLeasedObjectUrl(blob, cacheKey);

    return {
      url: objectUrl,
      cacheKey,
      contentHash: asset.contentHash,
      fromBrowserCache: false,
      release: () => this.releaseObjectUrl(objectUrl),
    };
  }

  private createLeasedObjectUrl(
    blob: Blob,
    cacheKey: string,
  ): string {
    const objectUrl = URL.createObjectURL(blob);
    this.objectUrls.set(objectUrl, cacheKey);
    this.activeLeaseCounts.set(
      cacheKey,
      (this.activeLeaseCounts.get(cacheKey) ?? 0) + 1,
    );
    return objectUrl;
  }

  private releaseObjectUrl(url: string): void {
    const cacheKey = this.objectUrls.get(url);
    if (!cacheKey) return;

    this.objectUrls.delete(url);
    URL.revokeObjectURL(url);

    const remaining = (this.activeLeaseCounts.get(cacheKey) ?? 1) - 1;
    if (remaining > 0) {
      this.activeLeaseCounts.set(cacheKey, remaining);
    } else {
      this.activeLeaseCounts.delete(cacheKey);
    }

    this.schedulePrune();
  }

  private schedulePrune(): void {
    if (this.disposed) return;

    this.pruneRequested = true;
    if (this.pruneInProgress) return;

    this.pruneInProgress = true;
    void (async () => {
      while (this.pruneRequested && !this.disposed) {
        this.pruneRequested = false;
        await this.prune();
      }
    })().finally(() => {
      this.pruneInProgress = false;
      if (this.pruneRequested && !this.disposed) {
        this.schedulePrune();
      }
    });
  }

  private async hydrateLegacyByteSizes(
    cache: Cache,
    entries: readonly AudioCacheMetadata[],
  ): Promise<Array<AudioCacheMetadata & { byteSize: number }>> {
    const hydrated: Array<AudioCacheMetadata & { byteSize: number }> = [];

    for (const entry of entries) {
      if (hasKnownByteSize(entry)) {
        hydrated.push({
          ...entry,
          byteSize: Math.floor(entry.byteSize),
        });
        continue;
      }

      try {
        const response = await cache.match(
          syntheticCacheUrl(entry.cacheKey),
        );

        if (!response) {
          await deleteMetadata(entry.cacheKey);
          continue;
        }

        const blob = await response.blob();
        const next = {
          ...entry,
          byteSize: blob.size,
        };

        hydrated.push(next);
        await writeMetadata(next);
      } catch {
        // Unknown legacy size must not make the cache appear artificially
        // small. Treat it as one full budget so an idle entry is eligible
        // for conservative eviction while active leases remain protected.
        hydrated.push({
          ...entry,
          byteSize: DEFAULT_READ_AUDIO_CACHE_BUDGET.maxBytes,
        });
      }
    }

    return hydrated;
  }

  private async pruneForQuotaPressure(
    cache: Cache,
    bytesToFree: number,
  ): Promise<void> {
    const entries = await readAllMetadata();
    if (entries.length === 0) return;

    const hydrated = await this.hydrateLegacyByteSizes(
      cache,
      entries,
    );
    const activeCacheKeys = new Set(
      this.activeLeaseCounts.keys(),
    );
    const evictions = planReadAudioCachePressureEvictions(
      hydrated.map((entry) => ({
        cacheKey: entry.cacheKey,
        byteSize: entry.byteSize,
        lastAccessedAt: entry.lastAccessedAt,
      })),
      activeCacheKeys,
      bytesToFree,
    );

    for (const cacheKey of evictions) {
      await cache
        .delete(syntheticCacheUrl(cacheKey))
        .catch(() => false);
      await deleteMetadata(cacheKey);
    }
  }

  private async prune(): Promise<void> {
    if (!hasCacheStorage()) return;

    const cache = await caches.open(CACHE_NAME).catch(() => null);
    if (!cache) return;

    const entries = await readAllMetadata();
    if (entries.length === 0) return;

    const hydrated = await this.hydrateLegacyByteSizes(cache, entries);
    const activeCacheKeys = new Set(this.activeLeaseCounts.keys());
    const evictions = planReadAudioCacheEvictions(
      hydrated.map((entry) => ({
        cacheKey: entry.cacheKey,
        byteSize: entry.byteSize,
        lastAccessedAt: entry.lastAccessedAt,
      })),
      activeCacheKeys,
    );

    for (const cacheKey of evictions) {
      await cache
        .delete(syntheticCacheUrl(cacheKey))
        .catch(() => false);
      await deleteMetadata(cacheKey);
    }
  }
}
