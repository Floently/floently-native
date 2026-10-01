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
  const response = await fetch(url, {
    credentials: "include",
    ...init,
  });

  if (!response.ok) {
    throw new Error(await readApiError(response, fallbackError));
  }

  return await response.json() as T;
}
