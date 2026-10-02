export interface ReadAudioCacheBudgetEntry {
  cacheKey: string;
  byteSize: number;
  lastAccessedAt: number;
}

export interface ReadAudioCacheBudget {
  maxBytes: number;
  targetBytes: number;
  maxEntries: number;
  targetEntries: number;
}

const MIB = 1024 * 1024;

export const DEFAULT_READ_AUDIO_CACHE_BUDGET: ReadAudioCacheBudget = {
  maxBytes: 96 * MIB,
  targetBytes: 72 * MIB,
  maxEntries: 256,
  targetEntries: 192,
};

export function resolveReadAudioCacheKey(
  cacheKey: string | null | undefined,
  contentHash: string | null | undefined,
): string {
  return String(cacheKey || "").trim()
    || String(contentHash || "").trim();
}

function safeSize(value: number): number {
  return Number.isFinite(value) && value > 0
    ? Math.floor(value)
    : 0;
}

function validBudget(budget: ReadAudioCacheBudget): ReadAudioCacheBudget {
  const maxBytes = Math.max(1, Math.floor(budget.maxBytes));
  const maxEntries = Math.max(1, Math.floor(budget.maxEntries));

  return {
    maxBytes,
    targetBytes: Math.min(
      maxBytes,
      Math.max(0, Math.floor(budget.targetBytes)),
    ),
    maxEntries,
    targetEntries: Math.min(
      maxEntries,
      Math.max(0, Math.floor(budget.targetEntries)),
    ),
  };
}

export function planReadAudioCacheEvictions(
  entries: readonly ReadAudioCacheBudgetEntry[],
  activeCacheKeys: ReadonlySet<string>,
  budget: ReadAudioCacheBudget = DEFAULT_READ_AUDIO_CACHE_BUDGET,
): string[] {
  const limits = validBudget(budget);
  let remainingBytes = entries.reduce(
    (sum, entry) => sum + safeSize(entry.byteSize),
    0,
  );
  let remainingEntries = entries.length;

  if (
    remainingBytes <= limits.maxBytes
    && remainingEntries <= limits.maxEntries
  ) {
    return [];
  }

  const oldestFirst = [...entries].sort(
    (left, right) =>
      left.lastAccessedAt - right.lastAccessedAt
      || left.cacheKey.localeCompare(right.cacheKey),
  );

  const evictions: string[] = [];

  for (const entry of oldestFirst) {
    if (
      remainingBytes <= limits.targetBytes
      && remainingEntries <= limits.targetEntries
    ) {
      break;
    }

    if (activeCacheKeys.has(entry.cacheKey)) {
      continue;
    }

    evictions.push(entry.cacheKey);
    remainingBytes = Math.max(
      0,
      remainingBytes - safeSize(entry.byteSize),
    );
    remainingEntries = Math.max(0, remainingEntries - 1);
  }

  return evictions;
}
