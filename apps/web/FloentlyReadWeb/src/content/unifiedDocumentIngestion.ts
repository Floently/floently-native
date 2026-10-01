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

  let timerId: number | null = null;
  const timeout = new Promise<never>((_, reject) => {
    timerId = window.setTimeout(() => {
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
      window.clearTimeout(timerId);
    }
  }
}

export async function ingestFileIntoReader(
  file: File,
  options: { title?: string } = {},
): Promise<UnifiedIngestionResult> {
  let fingerprint: string | null = null;

  try {
    fingerprint = await fingerprintDocumentFile(file);
  } catch {
    fingerprint = null;
  }

  if (fingerprint) {
    const existingProjectId = getProjectForFileFingerprint(fingerprint);

    if (existingProjectId) {
      try {
        const project = await withinFastOpenBudget(
          getContentProject(existingProjectId),
          file,
        );
        rememberProjectForFileFingerprint(fingerprint, project.id);

        return {
          kind: "file",
          project,
          reused: true,
        };
      } catch {
        // Stale fingerprint mappings and slow cloud reads are recoverable:
        // upload the file again instead of blocking the person.
      }
    }
  }

  const project = await withinFastOpenBudget(
    uploadContentProject(file, options),
    file,
  );

  if (fingerprint) {
    rememberProjectForFileFingerprint(fingerprint, project.id);
  }

  return {
    kind: "file",
    project,
    reused: false,
  };
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
