import {
  beginFileIngestion,
} from "./unifiedDocumentIngestion";
import {
  linkLocalOriginalToProject,
  type LocalOriginalDocumentRecord,
} from "./localOriginalDocuments";
import type { ContentProject } from "./projectApi";

const activeAttachments = new Map<string, Promise<ContentProject>>();

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

/**
 * Ensures one canonical semantic extraction/upload owns a local original at a
 * time. The original visual file may already be visible; therefore this path
 * intentionally disables the foreground 1.8s fast-open budget.
 */
export function attachSemanticProjectToLocalOriginal(
  localDocumentId: string,
  file: File,
): Promise<ContentProject> {
  const normalizedId = localDocumentId.trim();
  if (!normalizedId) {
    return Promise.reject(
      new Error("The original document id is missing."),
    );
  }

  const existing = activeAttachments.get(normalizedId);
  if (existing) return existing;

  const task = beginFileIngestion(file, {
    fastOpen: false,
  }).canonical
    .then(async (result) => {
      await linkLocalOriginalToProject(
        normalizedId,
        result.project.id,
      );
      return result.project;
    })
    .finally(() => {
      if (activeAttachments.get(normalizedId) === task) {
        activeAttachments.delete(normalizedId);
      }
    });

  activeAttachments.set(normalizedId, task);
  return task;
}

export function semanticAttachmentInFlight(
  localDocumentId: string,
): boolean {
  return activeAttachments.has(localDocumentId.trim());
}
