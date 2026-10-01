import {
  fetchGoogleConfig,
  fetchSession,
  loginWithGoogleCredential as requestGoogleLogin,
  loginWithPassword as requestPasswordLogin,
  logoutSession,
  registerWithPassword as requestRegistration,
} from "./authClient";

const TOKEN_STORAGE_KEY = "floently.read.web.vnext.auth-token";

export interface ReadAuthUser {
  id: string;
  email: string;
  name: string | null;
  plan: string;
  readPlan: string | null;
  avatarDataUrl: string | null;
}

export interface ReadAuthSession {
  accessToken: string | null;
  user: ReadAuthUser;
}

export interface ReadAuthState {
  initialized: boolean;
  status: "loading" | "anonymous" | "authenticated";
  session: ReadAuthSession | null;
  error: string | null;
  googleEnabled: boolean;
  googleClientId: string | null;
}

type Listener = () => void;

const listeners = new Set<Listener>();
let initializePromise: Promise<void> | null = null;

let state: ReadAuthState = {
  initialized: false,
  status: "loading",
  session: null,
  error: null,
  googleEnabled: false,
  googleClientId: null,
};

function record(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

function firstString(...values: unknown[]): string | null {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return null;
}

function unwrapData(value: unknown): Record<string, unknown> {
  let current = record(value);

  for (let depth = 0; depth < 3; depth += 1) {
    const nested = record(current.data);
    if (Object.keys(nested).length === 0) break;
    current = nested;
  }

  return current;
}

export function normalizeAuthSession(
  value: unknown,
  fallbackToken: string | null = null,
): ReadAuthSession | null {
  const source = unwrapData(value);
  const legacyUser = record(source.user);
  const learnUser = record(source.auth_user);
  const user = Object.keys(learnUser).length > 0 ? learnUser : legacyUser;
  const tokens = record(source.tokens);

  const id = firstString(
    user.user_id,
    user.id,
    source.user_id,
    source.id,
  );
  const email = firstString(user.email, source.email);

  if (!id || !email) {
    return null;
  }

  const accessToken = firstString(
    source.apiKey,
    source.api_key,
    user.apiKey,
    user.api_key,
    tokens.access_token,
    tokens.accessToken,
    fallbackToken,
  );

  return {
    accessToken,
    user: {
      id,
      email,
      name: firstString(user.name, user.displayName, user.display_name),
      plan:
        firstString(
          user.plan,
          user.subscription_tier,
          source.plan,
          source.subscription_tier,
        ) ?? "free",
      readPlan: firstString(user.readPlan, user.read_plan, source.readPlan),
      avatarDataUrl: firstString(
        user.avatarDataUrl,
        user.avatar_data_url,
        source.avatarDataUrl,
      ),
    },
  };
}

function emit(): void {
  for (const listener of listeners) listener();
}

function replaceState(patch: Partial<ReadAuthState>): void {
  state = { ...state, ...patch };
  emit();
}

function readStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY)?.trim() || null;
  } catch {
    return null;
  }
}

function persistToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch {
    // Cookie sessions still work when local storage is unavailable.
  }
}

function acceptSession(session: ReadAuthSession): ReadAuthSession {
  persistToken(session.accessToken);
  replaceState({
    initialized: true,
    status: "authenticated",
    session,
    error: null,
  });
  return session;
}

async function loadGoogleAvailability(): Promise<void> {
  try {
    const payload = unwrapData(await fetchGoogleConfig());
    const clientId = firstString(payload.clientId, payload.client_id);
    replaceState({
      googleClientId: clientId,
      googleEnabled: payload.enabled === true && Boolean(clientId),
    });
  } catch {
    replaceState({
      googleClientId: null,
      googleEnabled: false,
    });
  }
}

export function getAuthState(): ReadAuthState {
  return state;
}

export function subscribeAuthState(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getAuthAccessToken(): string | null {
  return state.session?.accessToken ?? readStoredToken();
}

export async function initializeAuth(): Promise<void> {
  if (state.initialized) return;
  if (initializePromise) return initializePromise;

  initializePromise = (async () => {
    replaceState({ status: "loading", error: null });

    await loadGoogleAvailability();

    try {
      const cookiePayload = await fetchSession();
      const cookieSession = normalizeAuthSession(cookiePayload);

      if (cookieSession) {
        acceptSession(cookieSession);
        return;
      }
    } catch {
      // A cookie session is optional; bearer fallback is handled below.
    }

    const storedToken = readStoredToken();
    if (storedToken) {
      try {
        const bearerPayload = await fetchSession(storedToken);
        const bearerSession = normalizeAuthSession(
          bearerPayload,
          storedToken,
        );

        if (bearerSession) {
          acceptSession(bearerSession);
          return;
        }
      } catch {
        persistToken(null);
      }
    }

    replaceState({
      initialized: true,
      status: "anonymous",
      session: null,
      error: null,
    });
  })().finally(() => {
    initializePromise = null;
  });

  return initializePromise;
}

async function authenticate(
  request: () => Promise<unknown>,
): Promise<ReadAuthSession> {
  replaceState({ status: "loading", error: null });

  try {
    const payload = await request();
    const session = normalizeAuthSession(payload);

    if (!session) {
      throw new Error("Authentication response was incomplete.");
    }

    return acceptSession(session);
  } catch (reason) {
    const message =
      reason instanceof Error ? reason.message : "Authentication failed.";

    replaceState({
      initialized: true,
      status: "anonymous",
      session: null,
      error: message,
    });
    throw reason;
  }
}

export function login(
  email: string,
  password: string,
): Promise<ReadAuthSession> {
  return authenticate(() => requestPasswordLogin(email, password));
}

export function register(
  email: string,
  password: string,
): Promise<ReadAuthSession> {
  return authenticate(() => requestRegistration(email, password));
}

export function loginWithGoogleCredential(
  credential: string,
): Promise<ReadAuthSession> {
  return authenticate(() => requestGoogleLogin(credential));
}

export async function logout(): Promise<void> {
  try {
    await logoutSession();
  } catch {
    // Local authority is cleared even when the remote logout transport fails.
  }

  persistToken(null);
  replaceState({
    initialized: true,
    status: "anonymous",
    session: null,
    error: null,
  });
}

export function clearAuthError(): void {
  replaceState({ error: null });
}
