import { buildAuthApiUrl } from "../config/runtime";

const LOGIN_ENDPOINT = buildAuthApiUrl("/api/v1/auth/login/password");
const REGISTER_ENDPOINT = buildAuthApiUrl("/api/v1/auth/register/password");
const SESSION_ENDPOINT = buildAuthApiUrl("/api/v1/auth/session");
const LOGOUT_ENDPOINT = buildAuthApiUrl("/api/v1/auth/logout");
const GOOGLE_CONFIG_ENDPOINT = buildAuthApiUrl("/api/v1/auth/google/config");
const GOOGLE_LOGIN_ENDPOINT = buildAuthApiUrl("/api/v1/auth/login/google");

async function readPayload(response: Response): Promise<unknown> {
  const raw = await response.text();
  if (!raw.trim()) return null;

  try {
    return JSON.parse(raw);
  } catch {
    return { message: raw };
  }
}

function errorMessage(payload: unknown, fallback: string): string {
  if (!payload || typeof payload !== "object") return fallback;
  const record = payload as Record<string, unknown>;

  for (const key of ["detail", "message", "error"]) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }

  return fallback;
}

async function requestJson(
  endpoint: string,
  init: RequestInit,
  fallbackError: string,
): Promise<unknown> {
  const response = await fetch(endpoint, {
    credentials: "include",
    ...init,
  });
  const payload = await readPayload(response);

  if (!response.ok) {
    throw new Error(errorMessage(payload, fallbackError));
  }

  return payload;
}

export function loginWithPassword(
  email: string,
  password: string,
): Promise<unknown> {
  return requestJson(
    LOGIN_ENDPOINT,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    },
    "Sign in failed.",
  );
}

export function registerWithPassword(
  email: string,
  password: string,
): Promise<unknown> {
  return requestJson(
    REGISTER_ENDPOINT,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    },
    "Account creation failed.",
  );
}

export function fetchSession(accessToken?: string | null): Promise<unknown> {
  const headers = new Headers();
  if (accessToken?.trim()) {
    headers.set("Authorization", `Bearer ${accessToken.trim()}`);
  }

  return requestJson(
    SESSION_ENDPOINT,
    { method: "GET", headers },
    "Your saved session is no longer valid.",
  );
}

export async function logoutSession(): Promise<void> {
  await requestJson(
    LOGOUT_ENDPOINT,
    { method: "POST" },
    "Logout failed.",
  );
}

export function fetchGoogleConfig(): Promise<unknown> {
  return requestJson(
    GOOGLE_CONFIG_ENDPOINT,
    { method: "GET" },
    "Google sign-in configuration could not be loaded.",
  );
}

export function loginWithGoogleCredential(
  credential: string,
): Promise<unknown> {
  return requestJson(
    GOOGLE_LOGIN_ENDPOINT,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential }),
    },
    "Google sign in failed.",
  );
}
