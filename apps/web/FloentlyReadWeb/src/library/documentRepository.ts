const DATABASE_NAME = "floently-read-web-vnext";
const DATABASE_VERSION = 1;
const DOCUMENT_STORE = "documents";

export interface LibraryDocument {
  id: string;
  title: string;
  language: string;
  text: string;
  sourceType: "text" | "markdown";
  createdAt: string;
  updatedAt: string;
}

export interface SaveLibraryDocumentInput {
  id?: string;
  title: string;
  language?: string;
  text: string;
  sourceType?: LibraryDocument["sourceType"];
}

let databasePromise: Promise<IDBDatabase> | null = null;

function openDatabase(): Promise<IDBDatabase> {
  if (databasePromise) return databasePromise;

  databasePromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);

    request.onupgradeneeded = () => {
      const database = request.result;

      if (!database.objectStoreNames.contains(DOCUMENT_STORE)) {
        const store = database.createObjectStore(DOCUMENT_STORE, {
          keyPath: "id",
        });
        store.createIndex("updatedAt", "updatedAt");
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () =>
      reject(request.error ?? new Error("Document database failed to open."));
  });

  return databasePromise;
}

function requestResult<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () =>
      reject(request.error ?? new Error("Browser storage request failed."));
  });
}

function transactionComplete(transaction: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onabort = () =>
      reject(transaction.error ?? new Error("Browser storage was aborted."));
    transaction.onerror = () =>
      reject(transaction.error ?? new Error("Browser storage failed."));
  });
}

function makeDocumentId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `read-${Date.now().toString(36)}-${Math.random()
    .toString(36)
    .slice(2, 10)}`;
}

export async function listLibraryDocuments(): Promise<LibraryDocument[]> {
  const database = await openDatabase();
  const transaction = database.transaction(DOCUMENT_STORE, "readonly");
  const records = await requestResult(
    transaction.objectStore(DOCUMENT_STORE).getAll(),
  );

  return (records as LibraryDocument[]).sort((left, right) =>
    right.updatedAt.localeCompare(left.updatedAt),
  );
}

export async function getLibraryDocument(
  id: string,
): Promise<LibraryDocument | null> {
  const database = await openDatabase();
  const transaction = database.transaction(DOCUMENT_STORE, "readonly");
  const record = await requestResult(
    transaction.objectStore(DOCUMENT_STORE).get(id),
  );

  return (record as LibraryDocument | undefined) ?? null;
}

export async function saveLibraryDocument(
  input: SaveLibraryDocumentInput,
): Promise<LibraryDocument> {
  const text = input.text.trim();

  if (!text) {
    throw new Error("Add document text before saving.");
  }

  const database = await openDatabase();
  const id = input.id?.trim() || makeDocumentId();
  const existing = input.id ? await getLibraryDocument(id) : null;
  const timestamp = new Date().toISOString();

  const document: LibraryDocument = {
    id,
    title: input.title.trim() || "Untitled document",
    language: input.language?.trim() || "en",
    text,
    sourceType: input.sourceType ?? "text",
    createdAt: existing?.createdAt ?? timestamp,
    updatedAt: timestamp,
  };

  const transaction = database.transaction(DOCUMENT_STORE, "readwrite");
  transaction.objectStore(DOCUMENT_STORE).put(document);
  await transactionComplete(transaction);

  return document;
}

export async function deleteLibraryDocument(id: string): Promise<void> {
  const database = await openDatabase();
  const transaction = database.transaction(DOCUMENT_STORE, "readwrite");
  transaction.objectStore(DOCUMENT_STORE).delete(id);
  await transactionComplete(transaction);
}
