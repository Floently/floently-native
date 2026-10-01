import { getAuthAccessToken } from "../auth/authStore";

export function buildAuthorizedHeaders(
  initial: HeadersInit = {},
): Headers {
  const headers = new Headers(initial);
  const accessToken = getAuthAccessToken();

  if (accessToken?.trim()) {
    headers.set("Authorization", `Bearer ${accessToken.trim()}`);
  }

  return headers;
}

export async function readApiError(
  response: Response,
  fallback: string,
): Promise<string> {
  try {
    const payload = await response.json() as {
      detail?: unknown;
      error?: unknown;
      message?: unknown;
    };

    for (const value of [payload.detail, payload.error, payload.message]) {
      if (typeof value === "string" && value.trim()) {
        return value.trim();
      }
    }
  } catch {
    // Stable fallback below.
  }

  return fallback;
}

export async function requestApiJson<T>(
  url: string,
  init: RequestInit,
  fallbackError: string,
): Promise<T> {
  const response = await fetch(url, init);

  if (!response.ok) {
    throw new Error(await readApiError(response, fallbackError));
  }

  const raw = await response.text();
  if (!raw.trim()) {
    return undefined as T;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    throw new Error("The content service returned an invalid response.");
  }
}
