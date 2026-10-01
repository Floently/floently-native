declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: GoogleIdentityApi;
      };
    };
  }
}

interface GooglePromptNotification {
  getNotDisplayedReason?: () => string;
  getSkippedReason?: () => string;
  isDismissedMoment?: () => boolean;
  isNotDisplayed?: () => boolean;
  isSkippedMoment?: () => boolean;
}

interface GoogleCredentialResponse {
  credential?: string;
}

interface GoogleIdentityApi {
  initialize: (config: {
    auto_select?: boolean;
    callback: (response: GoogleCredentialResponse) => void;
    client_id: string;
    ux_mode?: "popup" | "redirect";
  }) => void;
  prompt: (listener?: (notification: GooglePromptNotification) => void) => void;
  renderButton: (
    parent: HTMLElement,
    options: {
      locale?: string;
      logo_alignment?: "left" | "center";
      shape?: "rectangular" | "pill" | "circle" | "square";
      size?: "large" | "medium" | "small";
      text?: "signin_with" | "signup_with" | "continue_with" | "signin";
      theme?: "outline" | "filled_blue" | "filled_black";
      type?: "standard" | "icon";
      width?: number;
    },
  ) => void;
}

const GOOGLE_IDENTITY_SCRIPT_ID = "floently-google-identity-script";
const GOOGLE_IDENTITY_SCRIPT_SRC = "https://accounts.google.com/gsi/client?hl=en";
const GOOGLE_IDENTITY_READY_TIMEOUT_MS = 4000;

let configuredClientId = "";
let configuredHandler: ((credential: string) => void) | null = null;
let scriptLoadPromise: Promise<void> | null = null;

function getGoogleIdentity(): GoogleIdentityApi | null {
  return window.google?.accounts?.id ?? null;
}

function hasGoogleIdentityLoaded(): boolean {
  return Boolean(getGoogleIdentity());
}

function waitForGoogleIdentityReady(timeoutMs = GOOGLE_IDENTITY_READY_TIMEOUT_MS): Promise<void> {
  if (hasGoogleIdentityLoaded()) {
    return Promise.resolve();
  }

  return new Promise<void>((resolve, reject) => {
    const startedAt = window.performance?.now() ?? 0;

    const check = () => {
      if (hasGoogleIdentityLoaded()) {
        resolve();
        return;
      }

      const elapsedMs = (window.performance?.now() ?? 0) - startedAt;

      if (elapsedMs >= timeoutMs) {
        reject(new Error("Google Identity Services loaded, but the sign-in API is still unavailable."));
        return;
      }

      window.setTimeout(check, 50);
    };

    check();
  });
}

export function getGoogleUnavailableMessage(isConfigured: boolean): string {
  if (isConfigured) {
    return "Google sign-in could not start. Refresh the page and try again.";
  }

  return "Google sign-in is temporarily unavailable. You can still sign in with email and password.";
}

export function ensureGoogleIdentityScript(): Promise<void> {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return Promise.reject(new Error("Google sign-in can only run in the browser."));
  }

  if (hasGoogleIdentityLoaded()) {
    return Promise.resolve();
  }

  if (scriptLoadPromise) {
    return scriptLoadPromise;
  }

  scriptLoadPromise = new Promise<void>((resolve, reject) => {
    const handleSuccess = () => {
      void waitForGoogleIdentityReady().then(resolve).catch(reject);
    };

    const handleFailure = () => {
      reject(new Error("Google Identity Services did not load."));
    };

    const existingScript = document.getElementById(GOOGLE_IDENTITY_SCRIPT_ID) as HTMLScriptElement | null;

    if (existingScript) {
      existingScript.addEventListener("load", handleSuccess, { once: true });
      existingScript.addEventListener("error", handleFailure, { once: true });
      void waitForGoogleIdentityReady().then(resolve).catch(() => {
        // Wait for the current script listeners above.
      });
      return;
    }

    const script = document.createElement("script");
    script.id = GOOGLE_IDENTITY_SCRIPT_ID;
    script.src = GOOGLE_IDENTITY_SCRIPT_SRC;
    script.async = true;
    script.defer = true;
    script.addEventListener("load", handleSuccess, { once: true });
    script.addEventListener("error", handleFailure, { once: true });
    document.head.appendChild(script);
  }).catch((error): never => {
    scriptLoadPromise = null;
    throw error;
  });

  return scriptLoadPromise;
}

export async function configureGoogleSignIn(
  clientId: string,
  onCredential: (credential: string) => void,
): Promise<void> {
  await ensureGoogleIdentityScript();
  const googleIdentity = getGoogleIdentity();

  if (!googleIdentity) {
    throw new Error("Google Identity Services did not load.");
  }

  if (configuredClientId === clientId && configuredHandler === onCredential) {
    return;
  }

  googleIdentity.initialize({
    auto_select: false,
    callback: (response) => {
      if (typeof response.credential === "string" && response.credential.length > 0) {
        onCredential(response.credential);
      }
    },
    client_id: clientId,
    ux_mode: "popup",
  });

  configuredClientId = clientId;
  configuredHandler = onCredential;
}

export async function mountGoogleSignInButton(
  container: HTMLElement,
  clientId: string,
  onCredential: (credential: string) => void,
  options: {
    text?: "signin_with" | "signup_with" | "continue_with" | "signin";
    width?: number;
  } = {},
): Promise<void> {
  await configureGoogleSignIn(clientId, onCredential);
  const googleIdentity = getGoogleIdentity();

  if (!googleIdentity) {
    throw new Error("Google Identity Services did not load.");
  }

  container.innerHTML = "";
  googleIdentity.renderButton(container, {
    locale: "en",
    logo_alignment: "left",
    shape: "pill",
    size: "large",
    text: options.text ?? "continue_with",
    theme: "outline",
    type: "standard",
    width: options.width ?? Math.max(240, Math.floor(container.clientWidth || 320)),
  });
}

export async function requestGoogleSignIn(): Promise<void> {
  await ensureGoogleIdentityScript();
  const googleIdentity = getGoogleIdentity();

  if (!googleIdentity) {
    throw new Error("Google Identity Services did not load.");
  }

  googleIdentity.prompt();
}
