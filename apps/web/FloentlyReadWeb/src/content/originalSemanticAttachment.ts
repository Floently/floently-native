import {
  beginFileIngestion,
  type UnifiedIngestionResult,
} from "./unifiedDocumentIngestion";
import {
  linkLocalOriginalToProject,
  type LocalOriginalDocumentRecord,
} from "./localOriginalDocuments";
import { accountScopedLocalName } from "./localOwnerScope";
import type { ContentProject } from "./projectApi";

const activeAttachments = new Map<string, Promise<ContentProject>>();

function attachmentKey(
  ownerId: string,
  localDocumentId: string,
): string {
  const normalizedId = localDocumentId.trim();
  if (!normalizedId) {
    throw new Error("The original document id is missing.");
  }

  return accountScopedLocalName(
    `floently-read-local-attachment:${encodeURIComponent(normalizedId)}`,
    ownerId,
  );
}

export function fileFromLocalOriginal(
  record: LocalOriginalDocumentRecord,
): File {
  return new File(
    [record.blob],
    record.name || "document",
    {
      type:
        record.type
        || record.blob.type
        || "application/octet-stream",
      lastModified: record.lastModified || Date.now(),
    },
  );
}

export function trackCanonicalIngestionForLocalOriginal(
  ownerId: string,
  localDocumentId: string,
  canonical: Promise<UnifiedIngestionResult>,
): Promise<ContentProject> {
  let key: string;
  try {
    key = attachmentKey(ownerId, localDocumentId);
  } catch (error) {
    return Promise.reject(error);
  }

  const normalizedId = localDocumentId.trim();
  const existing = activeAttachments.get(key);
  if (existing) return existing;

  const task = canonical
    .then(async (result) => {
      await linkLocalOriginalToProject(
        ownerId,
        normalizedId,
        result.project.id,
      );
      return result.project;
    })
    .finally(() => {
      if (activeAttachments.get(key) === task) {
        activeAttachments.delete(key);
      }
    });

  activeAttachments.set(key, task);
  return task;
}

/**
 * Ensures one canonical semantic extraction/upload owns a local original at a
 * time. The original visual file may already be visible; therefore this path
 * intentionally disables the foreground 1.8s fast-open budget.
 */
export function attachSemanticProjectToLocalOriginal(
  ownerId: string,
  localDocumentId: string,
  file: File,
): Promise<ContentProject> {
  let key: string;
  try {
    key = attachmentKey(ownerId, localDocumentId);
  } catch (error) {
    return Promise.reject(error);
  }

  const existing = activeAttachments.get(key);
  if (existing) return existing;

  const canonical = beginFileIngestion(file, {
    ownerId,
    fastOpen: false,
  }).canonical;

  return trackCanonicalIngestionForLocalOriginal(
    ownerId,
    localDocumentId,
    canonical,
  );
}

export function semanticAttachmentInFlight(
  ownerId: string,
  localDocumentId: string,
): boolean {
  try {
    return activeAttachments.has(
      attachmentKey(ownerId, localDocumentId),
    );
  } catch {
    return false;
  }
}
