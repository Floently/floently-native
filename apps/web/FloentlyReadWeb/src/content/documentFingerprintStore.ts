import { accountScopedLocalName } from "./localOwnerScope";

const STORAGE_KEY_PREFIX =
  "floently.read.web.vnext.file-project-index.v2";
const SAMPLE_BYTES = 64 * 1024;
const MAX_ENTRIES = 80;

interface FingerprintEntry {
  projectId: string;
  updatedAt: number;
}

interface FingerprintEnvelope {
  version: 2;
  entries: Record<string, FingerprintEntry>;
}

export function documentFingerprintStorageKey(ownerId: string): string {
  return accountScopedLocalName(STORAGE_KEY_PREFIX, ownerId);
}

function storage(): Storage | null {
  if (
    typeof window === "undefined"
    || typeof window.localStorage === "undefined"
  ) {
    return null;
  }

  return window.localStorage;
}

function readEnvelope(ownerId: string): FingerprintEnvelope {
  const target = storage();
  if (!target) return { version: 2, entries: {} };

  try {
    const raw = target.getItem(
      documentFingerprintStorageKey(ownerId),
    );
    if (!raw) return { version: 2, entries: {} };

    const parsed = JSON.parse(raw) as { entries?: unknown };
    if (
      !parsed.entries
      || typeof parsed.entries !== "object"
      || Array.isArray(parsed.entries)
    ) {
      return { version: 2, entries: {} };
    }

    const entries: Record<string, FingerprintEntry> = {};
    for (
      const [fingerprint, value]
      of Object.entries(parsed.entries as Record<string, unknown>)
    ) {
      if (!value || typeof value !== "object" || Array.isArray(value)) {
        continue;
      }

      const candidate = value as Record<string, unknown>;
      const projectId =
        typeof candidate.projectId === "string"
          ? candidate.projectId.trim()
          : "";
      const updatedAt =
        typeof candidate.updatedAt === "number"
        && Number.isFinite(candidate.updatedAt)
          ? candidate.updatedAt
          : 0;

      if (fingerprint && projectId) {
        entries[fingerprint] = { projectId, updatedAt };
      }
    }

    return { version: 2, entries };
  } catch {
    return { version: 2, entries: {} };
  }
}

function writeEnvelope(
  ownerId: string,
  envelope: FingerprintEnvelope,
): void {
  const target = storage();
  if (!target) return;

  try {
    const ordered = Object.entries(envelope.entries)
      .sort(([, left], [, right]) => right.updatedAt - left.updatedAt)
      .slice(0, MAX_ENTRIES);

    target.setItem(
      documentFingerprintStorageKey(ownerId),
      JSON.stringify({
        version: 2,
        entries: Object.fromEntries(ordered),
      }),
    );
  } catch {
    // Re-importing still works if browser storage is blocked/full.
  }
}

function toHex(buffer: ArrayBuffer): string {
  return Array.from(
    new Uint8Array(buffer),
    (byte) => byte.toString(16).padStart(2, "0"),
  ).join("");
}

export async function fingerprintDocumentFile(file: File): Promise<string> {
  const first = new Uint8Array(
    await file.slice(0, SAMPLE_BYTES).arrayBuffer(),
  );
  const tailStart = Math.max(0, file.size - SAMPLE_BYTES);
  const last = new Uint8Array(
    await file.slice(tailStart).arrayBuffer(),
  );
  const metadata = new TextEncoder().encode(
    `${file.size}|${file.type || "application/octet-stream"}`,
  );

  const merged = new Uint8Array(
    metadata.byteLength + first.byteLength + last.byteLength,
  );
  merged.set(metadata, 0);
  merged.set(first, metadata.byteLength);
  merged.set(last, metadata.byteLength + first.byteLength);

  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest(
      "SHA-256",
      merged,
    );
    return `sha256-sample:${toHex(digest)}`;
  }

  return [
    "meta",
    file.name.toLowerCase(),
    file.size,
    file.lastModified || 0,
    file.type || "application/octet-stream",
  ].join(":");
}

export function getProjectForFileFingerprint(
  ownerId: string,
  fingerprint: string,
): string | null {
  const normalized = fingerprint.trim();
  if (!normalized) return null;

  return readEnvelope(ownerId).entries[normalized]?.projectId ?? null;
}

export function rememberProjectForFileFingerprint(
  ownerId: string,
  fingerprint: string,
  projectId: string,
): void {
  const normalizedFingerprint = fingerprint.trim();
  const normalizedProjectId = projectId.trim();
  if (!normalizedFingerprint || !normalizedProjectId) return;

  const envelope = readEnvelope(ownerId);
  envelope.entries[normalizedFingerprint] = {
    projectId: normalizedProjectId,
    updatedAt: Date.now(),
  };
  writeEnvelope(ownerId, envelope);
}

export function forgetProjectForFileFingerprint(
  ownerId: string,
  fingerprint: string,
): void {
  const normalized = fingerprint.trim();
  if (!normalized) return;

  const envelope = readEnvelope(ownerId);
  if (!(normalized in envelope.entries)) return;

  delete envelope.entries[normalized];
  writeEnvelope(ownerId, envelope);
}
