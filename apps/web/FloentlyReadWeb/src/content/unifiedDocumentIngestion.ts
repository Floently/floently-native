import {
  fingerprintDocumentFile,
  getProjectForFileFingerprint,
  rememberProjectForFileFingerprint,
} from "./documentFingerprintStore";
import { uploadContentProject } from "./documentUploadApi";
import {
  createProjectFromText,
  getContentProject,
  type ContentProject,
} from "./projectApi";

export type UnifiedIngestionKind = "file" | "text" | "website";

const BOOK_SIZE_THRESHOLD_BYTES = 1_500_000;
const BOOK_FAST_OPEN_BUDGET_MS = 1_800;

export interface UnifiedIngestionResult {
  kind: UnifiedIngestionKind;
  project: ContentProject;
  reused: boolean;
}

async function withinFastOpenBudget<T>(
  operation: Promise<T>,
  file: File,
): Promise<T> {
  if (file.size < BOOK_SIZE_THRESHOLD_BYTES) {
    return operation;
  }

  let timerId: ReturnType<typeof globalThis.setTimeout> | null = null;
  const timeout = new Promise<never>((_, reject) => {
    timerId = globalThis.setTimeout(() => {
      reject(
        new Error(
          "Canonical import is continuing too slowly; open the original immediately.",
        ),
      );
    }, BOOK_FAST_OPEN_BUDGET_MS);
  });

  try {
    return await Promise.race([operation, timeout]);
  } finally {
    if (timerId !== null) {
      globalThis.clearTimeout(timerId);
    }
  }
}

export interface FileIngestionOptions {
  ownerId: string;
  title?: string;
  /**
   * Keep the production first-open budget for UI-blocking imports.
   * Set false only when the original source is already visible and canonical
   * extraction is allowed to finish fully in the background.
   */
  fastOpen?: boolean;
}

export interface FileIngestionHandle {
  /**
   * The complete canonical project operation. It is never cancelled by the
   * first-open UI budget and may finish after the original visual source opens.
   */
  canonical: Promise<UnifiedIngestionResult>;
  /**
   * The result appropriate for a foreground open. Large files may reject after
   * the first-open budget while canonical continues behind the preserved source.
   */
  immediate: Promise<UnifiedIngestionResult>;
}

async function canonicalFileIngestion(
  file: File,
  options: Pick<FileIngestionOptions, "ownerId" | "title">,
): Promise<UnifiedIngestionResult> {
  let fingerprint: string | null = null;

  try {
    fingerprint = await fingerprintDocumentFile(file);
  } catch {
    fingerprint = null;
  }

  if (fingerprint) {
    const existingProjectId = getProjectForFileFingerprint(
      options.ownerId,
      fingerprint,
    );

    if (existingProjectId) {
      try {
        const project = await getContentProject(existingProjectId);
        rememberProjectForFileFingerprint(
          options.ownerId,
          fingerprint,
          project.id,
        );

        return {
          kind: "file",
          project,
          reused: true,
        };
      } catch {
        // A stale mapping is recoverable. Continue to one canonical upload.
      }
    }
  }

  const project = await uploadContentProject(file, {
    title: options.title,
  });

  if (fingerprint) {
    rememberProjectForFileFingerprint(
      options.ownerId,
      fingerprint,
      project.id,
    );
  }

  return {
    kind: "file",
    project,
    reused: false,
  };
}

export function beginFileIngestion(
  file: File,
  options: FileIngestionOptions,
): FileIngestionHandle {
  const canonical = canonicalFileIngestion(file, options);
  const immediate =
    options.fastOpen === false
      ? canonical
      : withinFastOpenBudget(canonical, file);

  return {
    canonical,
    immediate,
  };
}

export async function ingestFileIntoReader(
  file: File,
  options: FileIngestionOptions,
): Promise<UnifiedIngestionResult> {
  return beginFileIngestion(file, options).immediate;
}

export async function ingestTextIntoReader(
  text: string,
  options: {
    title?: string;
    sourceType?: string;
  } = {},
): Promise<UnifiedIngestionResult> {
  const normalized = text.trim();
  if (!normalized) {
    throw new Error("Add text before importing it.");
  }

  const project = await createProjectFromText({
    text: normalized,
    title: options.title,
    sourceType: options.sourceType ?? "text",
  });

  return {
    kind: "text",
    project,
    reused: false,
  };
}

/**
 * Websites are Browser V2 sources, not copied document imports.
 * Authenticated state, forms, videos and navigation must remain live.
 */
export async function ingestWebsiteIntoReader(
  _url: string,
): Promise<UnifiedIngestionResult> {
  throw new Error(
    "Website sources must open as the live page; copied website ingestion is disabled.",
  );
}

export const ingestionBudgets = {
  bookSizeThresholdBytes: BOOK_SIZE_THRESHOLD_BYTES,
  bookFastOpenBudgetMs: BOOK_FAST_OPEN_BUDGET_MS,
} as const;
