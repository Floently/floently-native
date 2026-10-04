import { describe, expect, it } from "vitest";
import {
  planReadAudioCacheEvictions,
  planReadAudioCachePressureEvictions,
  resolveReadAudioCacheKey,
  type ReadAudioCacheBudget,
  type ReadAudioCacheBudgetEntry,
} from "./readAudioCachePolicy";

const budget: ReadAudioCacheBudget = {
  maxBytes: 100,
  targetBytes: 60,
  maxEntries: 10,
  targetEntries: 8,
};

function entry(
  cacheKey: string,
  byteSize: number,
  lastAccessedAt: number,
): ReadAudioCacheBudgetEntry {
  return { cacheKey, byteSize, lastAccessedAt };
}

describe("Read audio cache key normalization", () => {
  it("prefers the provider cache key and falls back to content hash", () => {
    expect(resolveReadAudioCacheKey("  provider-key  ", "content-hash")).toBe(
      "provider-key",
    );
    expect(resolveReadAudioCacheKey("", "  content-hash  ")).toBe(
      "content-hash",
    );
    expect(resolveReadAudioCacheKey("   ", "   ")).toBe("");
  });
});

describe("Read audio cache eviction policy", () => {
  it("keeps an under-budget cache intact", () => {
    expect(
      planReadAudioCacheEvictions(
        [
          entry("a", 20, 1),
          entry("b", 30, 2),
        ],
        new Set(),
        budget,
      ),
    ).toEqual([]);
  });

  it("evicts oldest unleased assets until the byte target is reached", () => {
    expect(
      planReadAudioCacheEvictions(
        [
          entry("oldest", 40, 1),
          entry("middle", 30, 2),
          entry("newest", 40, 3),
        ],
        new Set(),
        budget,
      ),
    ).toEqual(["oldest", "middle"]);
  });

  it("protects active cache keys even when they are oldest", () => {
    expect(
      planReadAudioCacheEvictions(
        [
          entry("active", 50, 1),
          entry("old-idle", 40, 2),
          entry("new-idle", 30, 3),
        ],
        new Set(["active"]),
        budget,
      ),
    ).toEqual(["old-idle", "new-idle"]);
  });

  it("also bounds metadata cardinality without returning to a tiny fixed file cap", () => {
    expect(
      planReadAudioCacheEvictions(
        [
          entry("a", 1, 1),
          entry("b", 1, 2),
          entry("c", 1, 3),
          entry("d", 1, 4),
        ],
        new Set(),
        {
          maxBytes: 100,
          targetBytes: 80,
          maxEntries: 3,
          targetEntries: 2,
        },
      ),
    ).toEqual(["a", "b"]);
  });

  it("fails safe when the active lease set alone exceeds the budget", () => {
    expect(
      planReadAudioCacheEvictions(
        [
          entry("active-a", 80, 1),
          entry("active-b", 80, 2),
        ],
        new Set(["active-a", "active-b"]),
        budget,
      ),
    ).toEqual([]);
  });
});


describe("Read audio cache quota-pressure eviction policy", () => {
  it("evicts oldest unleased assets until requested headroom is freed", () => {
    expect(
      planReadAudioCachePressureEvictions(
        [
          entry("oldest", 20, 1),
          entry("middle", 30, 2),
          entry("newest", 40, 3),
        ],
        new Set(),
        45,
      ),
    ).toEqual(["oldest", "middle"]);
  });

  it("never pressure-evicts active leased assets", () => {
    expect(
      planReadAudioCachePressureEvictions(
        [
          entry("active-oldest", 80, 1),
          entry("idle-middle", 30, 2),
          entry("idle-newest", 40, 3),
        ],
        new Set(["active-oldest"]),
        50,
      ),
    ).toEqual(["idle-middle", "idle-newest"]);
  });

  it("returns every available idle entry when the requested headroom cannot be reached", () => {
    expect(
      planReadAudioCachePressureEvictions(
        [
          entry("idle", 10, 1),
          entry("active", 100, 2),
        ],
        new Set(["active"]),
        80,
      ),
    ).toEqual(["idle"]);
  });
});
