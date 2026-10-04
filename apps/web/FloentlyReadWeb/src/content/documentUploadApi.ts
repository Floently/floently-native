import { buildApiUrl } from "../config/runtime";
import { buildAuthorizedHeaders, readApiError } from "./apiClient";
import { normalizeProject, type ContentProject } from "./projectApi";

interface ProjectEnvelope {
  project?: unknown;
}

export class DocumentUploadError extends Error {
  readonly status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "DocumentUploadError";
    this.status = status;
  }
}

export function uploadBlobForFile(file: File): Blob {
  // Safari 26.5+ can hand the networking process a disk-backed File path it
  // cannot read, producing a multipart request with a zero-byte body. slice()
  // yields a Blob-backed view while preserving bytes/type and avoids passing
  // the original filesystem-backed File object to FormData.
  return file.slice(
    0,
    file.size,
    file.type || "application/octet-stream",
  );
}

export async function uploadContentProject(
  file: File,
  options: { title?: string } = {},
): Promise<ContentProject> {
  if (!(file instanceof File) || file.size <= 0) {
    throw new DocumentUploadError("Choose a readable document file.");
  }

  const body = new FormData();
  body.append(
    "file",
    uploadBlobForFile(file),
    file.name || "document",
  );

  const title = options.title?.trim();
  if (title) body.append("title", title);

  let response: Response;
  try {
    response = await fetch(buildApiUrl("/api/v1/projects/upload"), {
      method: "POST",
      body,
      headers: buildAuthorizedHeaders(),
    });
  } catch {
    throw new DocumentUploadError(
      "The document service could not be reached.",
    );
  }

  if (!response.ok) {
    throw new DocumentUploadError(
      await readApiError(
        response,
        "Could not import this document into Floently Read.",
      ),
      response.status,
    );
  }

  const payload = await response.json() as ProjectEnvelope | unknown;
  const candidate =
    payload
    && typeof payload === "object"
    && "project" in payload
      ? (payload as ProjectEnvelope).project
      : payload;

  const project = normalizeProject(candidate);
  if (!project || typeof project.rawText !== "string" || !project.rawText.trim()) {
    throw new DocumentUploadError(
      "The imported document did not contain readable text.",
      response.status,
    );
  }

  return project;
}
