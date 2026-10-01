import { buildApiUrl } from "../config/runtime";
import {
  buildAuthorizedHeaders,
  requestApiJson,
} from "./apiClient";

export interface ProjectProgress {
  projectId: string;
  currentSegmentIndex: number;
  currentCharacterOffset: number;
  progressPercent: number;
  voiceId?: string | null;
  playbackRate?: number | null;
  updatedAt: string;
}

export interface ContentProject {
  id: string;
  title: string;
  kind: string;
  status: string;
  sourceType: string;
  sourceUrl?: string | null;
  language?: string | null;
  textHash: string;
  wordCount: number;
  characterCount: number;
  createdAt: string;
  updatedAt: string;
  lastOpenedAt?: string | null;
  progress?: ProjectProgress | null;
  rawText?: string;
}

interface ProjectEnvelope {
  project?: unknown;
}

interface ProjectListEnvelope {
  projects?: unknown;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function readString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function readNumber(value: unknown, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value)
    ? value
    : fallback;
}

export function normalizeProjectProgress(
  value: unknown,
): ProjectProgress | null {
  if (!isRecord(value)) return null;

  return {
    projectId: readString(value.projectId),
    currentSegmentIndex: readNumber(value.currentSegmentIndex),
    currentCharacterOffset: readNumber(value.currentCharacterOffset),
    progressPercent: readNumber(value.progressPercent),
    voiceId: typeof value.voiceId === "string" ? value.voiceId : null,
    playbackRate:
      typeof value.playbackRate === "number" ? value.playbackRate : null,
    updatedAt: readString(value.updatedAt),
  };
}

export function normalizeProject(value: unknown): ContentProject | null {
  if (!isRecord(value)) return null;

  const id = readString(value.id).trim();
  const title = readString(value.title).trim();
  if (!id || !title) return null;

  return {
    id,
    title,
    kind: readString(value.kind, "document"),
    status: readString(value.status, "ready"),
    sourceType: readString(value.sourceType, "text"),
    sourceUrl: typeof value.sourceUrl === "string" ? value.sourceUrl : null,
    language: typeof value.language === "string" ? value.language : null,
    textHash: readString(value.textHash),
    wordCount: readNumber(value.wordCount),
    characterCount: readNumber(value.characterCount),
    createdAt: readString(value.createdAt),
    updatedAt: readString(value.updatedAt),
    lastOpenedAt:
      typeof value.lastOpenedAt === "string" ? value.lastOpenedAt : null,
    progress: normalizeProjectProgress(value.progress),
    rawText: typeof value.rawText === "string" ? value.rawText : undefined,
  };
}

export async function listContentProjects(
  limit = 50,
  offset = 0,
): Promise<ContentProject[]> {
  const params = new URLSearchParams({
    limit: String(limit),
    offset: String(offset),
  });

  const payload = await requestApiJson<ProjectListEnvelope>(
    `${buildApiUrl("/api/v1/projects")}?${params.toString()}`,
    {
      method: "GET",
      headers: buildAuthorizedHeaders(),
    },
    "Could not load your library.",
  );

  if (!Array.isArray(payload.projects)) return [];

  return payload.projects
    .map(normalizeProject)
    .filter((project): project is ContentProject => Boolean(project));
}

export async function getContentProject(
  projectId: string,
): Promise<ContentProject> {
  const payload = await requestApiJson<ProjectEnvelope>(
    buildApiUrl(`/api/v1/projects/${encodeURIComponent(projectId)}`),
    {
      method: "GET",
      headers: buildAuthorizedHeaders(),
    },
    "Could not open this project.",
  );
  const project = normalizeProject(payload.project);

  if (!project || typeof project.rawText !== "string") {
    throw new Error("The project did not include readable text.");
  }

  return project;
}

export async function createProjectFromText(payload: {
  text: string;
  title?: string;
  sourceType?: string;
}): Promise<ContentProject> {
  const result = await requestApiJson<ProjectEnvelope>(
    buildApiUrl("/api/v1/projects/from-text"),
    {
      method: "POST",
      headers: buildAuthorizedHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(payload),
    },
    "Could not save this project.",
  );

  const project = normalizeProject(result.project);
  if (!project || typeof project.rawText !== "string") {
    throw new Error("The saved project did not include readable text.");
  }

  return project;
}

export async function deleteContentProject(projectId: string): Promise<void> {
  await requestApiJson(
    buildApiUrl(`/api/v1/projects/${encodeURIComponent(projectId)}`),
    {
      method: "DELETE",
      headers: buildAuthorizedHeaders(),
    },
    "Could not delete this project.",
  );
}

export async function updateContentProjectProgress(
  projectId: string,
  progress: Partial<ProjectProgress>,
): Promise<ProjectProgress | null> {
  const result = await requestApiJson<{ progress?: unknown }>(
    buildApiUrl(
      `/api/v1/projects/${encodeURIComponent(projectId)}/progress`,
    ),
    {
      method: "PUT",
      headers: buildAuthorizedHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify(progress),
    },
    "Could not save reading progress.",
  );

  return normalizeProjectProgress(result.progress);
}
