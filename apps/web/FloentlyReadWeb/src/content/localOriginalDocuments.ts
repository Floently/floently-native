import { accountScopedLocalName } from "./localOwnerScope";

const DB_NAME_PREFIX = "floently-read-web-vnext-original-documents-v2";
const DB_VERSION = 1;
const STORE_NAME = "documents";
const QUICK_INDEX = "quickSignature";
const HASH_INDEX = "contentHash";
const SAMPLE_BYTES = 64 * 1024;

export interface LocalOriginalDocumentRecord {
  id: string;
  blob: Blob;
  name: string;
  type: string;
  size: number;
  lastModified: number;
  quickSignature: string;
  contentHash?: string | null;
  projectId: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface StoredLocalOriginalDocumentRecord
  extends Omit<LocalOriginalDocumentRecord, "blob"> {
  // Current storage format. Raw bytes avoid WebKit Blob/File structured-clone
  // failures while the app-facing contract remains Blob-based.
  bytes?: ArrayBuffer;
  // Legacy format retained for backward-compatible reads.
  blob?: Blob;
}

export function localOriginalDatabaseName(ownerId: string): string {
  return accountScopedLocalName(DB_NAME_PREFIX, ownerId);
}

function openDatabase(ownerId: string): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(
      localOriginalDatabaseName(ownerId),
      DB_VERSION,
    );

    request.onupgradeneeded = () => {
      const database = request.result;
      const store = database.objectStoreNames.contains(STORE_NAME)
        ? request.transaction!.objectStore(STORE_NAME)
        : database.createObjectStore(STORE_NAME, { keyPath: "id" });

      if (!store.indexNames.contains(QUICK_INDEX)) {
        store.createIndex(QUICK_INDEX, QUICK_INDEX, { unique: false });
      }
      if (!store.indexNames.contains(HASH_INDEX)) {
        store.createIndex(HASH_INDEX, HASH_INDEX, { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () =>
      reject(
        request.error
        ?? new Error("Could not open local Read document storage."),
      );
  });
}

function requestResult<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () =>
      reject(
        request.error
        ?? new Error("Local Read document storage request failed."),
      );
  });
}

function transactionComplete(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onerror = () =>
      reject(
        transaction.error
        ?? new Error("Could not save the original document."),
      );
    transaction.onabort = () =>
      reject(
        transaction.error
        ?? new Error("Original document save was interrupted."),
      );
  });
}

function randomId(): string {
  return typeof crypto.randomUUID === "function"
    ? crypto.randomUUID()
    : `doc_${Date.now().toString(36)}_${Math.random()
        .toString(36)
        .slice(2, 10)}`;
}

export function storageBlobForFile(file: File): Blob {
  const type = file.type || "application/octet-stream";
  return file.slice(0, file.size, type);
}

export async function originalRecordForStorage(
  record: LocalOriginalDocumentRecord,
): Promise<StoredLocalOriginalDocumentRecord> {
  const {
    blob,
    contentHash,
    ...metadata
  } = record;

  const stored: StoredLocalOriginalDocumentRecord = {
    ...metadata,
    bytes: await blob.arrayBuffer(),
  };

  // A missing index key means "not indexed yet". A present null is not a
  // valid IndexedDB key in WebKit and can abort the entire write transaction.
  if (contentHash) {
    stored.contentHash = contentHash;
  }

  return stored;
}

export function localOriginalFromStorage(
  stored: StoredLocalOriginalDocumentRecord | null | undefined,
): LocalOriginalDocumentRecord | null {
  if (!stored) return null;

  const {
    bytes,
    blob: legacyBlob,
    ...metadata
  } = stored;

  const blob =
    bytes instanceof ArrayBuffer
      ? new Blob(
          [bytes],
          {
            type:
              metadata.type
              || legacyBlob?.type
              || "application/octet-stream",
          },
        )
      : legacyBlob instanceof Blob
        ? legacyBlob
        : null;

  if (!blob) return null;

  return {
    ...metadata,
    blob,
  };
}

function bytesToHex(buffer: ArrayBuffer): string {
  return Array.from(
    new Uint8Array(buffer),
    (byte) => byte.toString(16).padStart(2, "0"),
  ).join("");
}

async function digest(parts: Uint8Array[]): Promise<string | null> {
  try {
    if (!crypto.subtle) return null;

    const total = parts.reduce(
      (sum, part) => sum + part.byteLength,
      0,
    );
    const merged = new Uint8Array(total);
    let offset = 0;

    for (const part of parts) {
      merged.set(part, offset);
      offset += part.byteLength;
    }

    return bytesToHex(await crypto.subtle.digest("SHA-256", merged));
  } catch {
    return null;
  }
}

async function buildQuickSignature(file: File): Promise<string> {
  try {
    const first = new Uint8Array(
      await file.slice(0, SAMPLE_BYTES).arrayBuffer(),
    );
    const tailStart = Math.max(0, file.size - SAMPLE_BYTES);
    const last = new Uint8Array(
      await file.slice(tailStart, file.size).arrayBuffer(),
    );
    const metadata = new TextEncoder().encode(
      `${file.size}|${file.type || "application/octet-stream"}`,
    );
    const sampled = await digest([metadata, first, last]);

    if (sampled) return `sample:${sampled}`;
  } catch {
    // Metadata fallback below.
  }

  return [
    "meta",
    file.name.toLowerCase(),
    file.size,
    file.lastModified || 0,
    file.type || "application/octet-stream",
  ].join(":");
}

async function calculateContentHash(file: File): Promise<string | null> {
  try {
    if (!crypto.subtle) return null;
    return bytesToHex(
      await crypto.subtle.digest("SHA-256", await file.arrayBuffer()),
    );
  } catch {
    return null;
  }
}

async function getByIndex(
  ownerId: string,
  indexName: string,
  value: IDBValidKey,
): Promise<LocalOriginalDocumentRecord | null> {
  const database = await openDatabase(ownerId);

  try {
    const transaction = database.transaction(STORE_NAME, "readonly");
    const record = await requestResult(
      transaction.objectStore(STORE_NAME).index(indexName).get(value),
    );

    return localOriginalFromStorage(
      record as StoredLocalOriginalDocumentRecord | undefined,
    );
  } finally {
    database.close();
  }
}

export function withCompletedContentHash(
  record: LocalOriginalDocumentRecord,
  contentHash: string,
  updatedAt = Date.now(),
  fallbackProjectId: string | null = null,
): LocalOriginalDocumentRecord {
  return {
    ...record,
    contentHash,
    projectId: record.projectId ?? fallbackProjectId,
    updatedAt,
  };
}

async function putRecord(
  ownerId: string,
  record: LocalOriginalDocumentRecord,
): Promise<void> {
  // Materialize bytes before opening the write transaction. IndexedDB
  // transactions may auto-close while unrelated async Blob work is pending.
  const stored = await originalRecordForStorage(record);
  const database = await openDatabase(ownerId);

  try {
    const transaction = database.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).put(stored);
    await transactionComplete(transaction);
  } finally {
    database.close();
  }
}

export async function getLocalOriginalDocument(
  ownerId: string,
  id: string,
): Promise<LocalOriginalDocumentRecord | null> {
  const database = await openDatabase(ownerId);

  try {
    const transaction = database.transaction(STORE_NAME, "readonly");
    const record = await requestResult(
      transaction.objectStore(STORE_NAME).get(id),
    );

    return localOriginalFromStorage(
      record as StoredLocalOriginalDocumentRecord | undefined,
    );
  } finally {
    database.close();
  }
}

async function getAllLocalOriginalRecords(
  ownerId: string,
): Promise<LocalOriginalDocumentRecord[]> {
  const database = await openDatabase(ownerId);

  try {
    const transaction = database.transaction(STORE_NAME, "readonly");
    const stored = await requestResult(
      transaction.objectStore(STORE_NAME).getAll(),
    ) as StoredLocalOriginalDocumentRecord[];

    return stored
      .map(localOriginalFromStorage)
      .filter(
        (record): record is LocalOriginalDocumentRecord =>
          record !== null,
      );
  } finally {
    database.close();
  }
}

export function sameOriginalIdentity(
  left: LocalOriginalDocumentRecord,
  right: LocalOriginalDocumentRecord,
): boolean {
  if (left.contentHash && right.contentHash) {
    return left.contentHash === right.contentHash;
  }

  return Boolean(
    left.quickSignature
    && right.quickSignature
    && left.quickSignature === right.quickSignature,
  );
}

export async function listLocalOriginalDocuments(
  ownerId: string,
  limit = 80,
): Promise<LocalOriginalDocumentRecord[]> {
  const records = await getAllLocalOriginalRecords(ownerId);
  const canonical = new Map<string, LocalOriginalDocumentRecord>();

  for (const record of records) {
    const identity =
      record.contentHash
      || record.quickSignature
      || record.id;
    const existing = canonical.get(identity);

    if (
      !existing
      || Number(record.updatedAt || 0) > Number(existing.updatedAt || 0)
    ) {
      canonical.set(identity, record);
    }
  }

  return Array.from(canonical.values())
    .sort((left, right) =>
      Number(right.updatedAt || 0) - Number(left.updatedAt || 0),
    )
    .slice(0, Math.max(1, limit));
}

export async function removeLocalOriginalDocument(
  ownerId: string,
  id: string,
): Promise<void> {
  const records = await getAllLocalOriginalRecords(ownerId);
  const target = records.find((record) => record.id === id) ?? null;
  const idsToDelete = target
    ? records
        .filter((record) => sameOriginalIdentity(target, record))
        .map((record) => record.id)
    : [id];

  const database = await openDatabase(ownerId);

  try {
    const transaction = database.transaction(STORE_NAME, "readwrite");
    const store = transaction.objectStore(STORE_NAME);

    for (const recordId of idsToDelete) {
      store.delete(recordId);
    }

    await transactionComplete(transaction);
  } finally {
    database.close();
  }
}

export async function handoffOriginalDocument(
  ownerId: string,
  file: File,
): Promise<{ id: string; duplicate: boolean }> {
  const quickSignature = await buildQuickSignature(file);
  const existingQuick = await getByIndex(
    ownerId,
    QUICK_INDEX,
    quickSignature,
  );

  if (existingQuick) {
    const refreshed: LocalOriginalDocumentRecord = {
      ...existingQuick,
      blob: storageBlobForFile(file),
      name: file.name,
      type: file.type || existingQuick.type,
      size: file.size,
      lastModified: file.lastModified || existingQuick.lastModified,
      updatedAt: Date.now(),
    };
    await putRecord(ownerId, refreshed);

    return {
      id: refreshed.id,
      duplicate: true,
    };
  }

  const now = Date.now();
  const record: LocalOriginalDocumentRecord = {
    id: randomId(),
    blob: storageBlobForFile(file),
    name: file.name,
    type: file.type || "application/octet-stream",
    size: file.size,
    lastModified: file.lastModified || 0,
    quickSignature,
    projectId: null,
    createdAt: now,
    updatedAt: now,
  };
  await putRecord(ownerId, record);

  void (async () => {
    const hash = await calculateContentHash(file);
    if (!hash) return;

    const exact = await getByIndex(ownerId, HASH_INDEX, hash);
    const latest = await getLocalOriginalDocument(ownerId, record.id);
    if (!latest) return;

    // Never delete the just-opened id during background hashing: another route
    // or tab may already be rendering it. listLocalOriginalDocuments() dedupes
    // identical hashes for presentation while both ids remain addressable.
    await putRecord(
      ownerId,
      withCompletedContentHash(
        latest,
        hash,
        Date.now(),
        exact && exact.id !== latest.id
          ? exact.projectId
          : null,
      ),
    );
  })();

  return {
    id: record.id,
    duplicate: false,
  };
}

export async function linkLocalOriginalToProject(
  ownerId: string,
  id: string,
  projectId: string,
): Promise<void> {
  const record = await getLocalOriginalDocument(ownerId, id);
  if (!record) return;

  await putRecord(ownerId, {
    ...record,
    projectId: projectId.trim() || null,
    updatedAt: Date.now(),
  });
}
