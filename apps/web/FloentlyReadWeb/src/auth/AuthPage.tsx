import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import {
  clearAuthError,
  login,
  loginWithGoogleCredential,
  register,
} from "./authStore";
import { useAuthState } from "./useAuthState";
import {
  navigateTo,
  safeReturnTo,
} from "../routing/navigation";
import { GoogleSignInButton } from "./GoogleSignInButton";

export function AuthPage({
  mode,
  search,
}: {
  mode: "login" | "signup";
  search: string;
}) {
  const auth = useAuthState();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  const returnTo = useMemo(() => {
    const params = new URLSearchParams(search);
    return safeReturnTo(params.get("returnTo")) ?? "/app/library";
  }, [search]);

  useEffect(() => {
    clearAuthError();
    setLocalError(null);
  }, [mode]);

  useEffect(() => {
    if (auth.status === "authenticated") {
      navigateTo(returnTo, true);
    }
  }, [auth.status, returnTo]);

  const finishGoogleSignIn = useCallback(
    async (credential: string) => {
      setLocalError(null);

      try {
        await loginWithGoogleCredential(credential);
        navigateTo(returnTo, true);
      } catch {
        // The auth store publishes the normalized transport error.
      }
    },
    [returnTo],
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError(null);

    const normalizedEmail = email.trim();
    if (!normalizedEmail || !password) {
      setLocalError("Enter your email and password.");
      return;
    }

    if (mode === "signup") {
      if (password.length < 8) {
        setLocalError("Use at least 8 characters for your password.");
        return;
      }

      if (password !== confirmation) {
        setLocalError("The passwords do not match.");
        return;
      }
    }

    try {
      if (mode === "signup") {
        await register(normalizedEmail, password);
      } else {
        await login(normalizedEmail, password);
      }

      navigateTo(returnTo, true);
    } catch {
      // Error is exposed by the auth store.
    }
  }

  const isBusy = auth.status === "loading";

  return (
    <main className="auth-shell">
      <section className="auth-brand-panel">
        <a
          href="/"
          className="brand-link"
          onClick={(event) => {
            event.preventDefault();
            navigateTo("/");
          }}
        >
          <span className="brand-mark" aria-hidden="true">F</span>
          <span>Floently Read</span>
        </a>

        <div>
          <p className="eyebrow">One reading identity</p>
          <h1>
            {mode === "signup"
              ? "Create your Floently account."
              : "Continue where you left off."}
          </h1>
          <p>
            The next-generation Read web app uses the same Floently account
            authority as the existing products. Your account is not duplicated
            for this rebuild.
          </p>
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-card">
          <p className="eyebrow">
            {mode === "signup" ? "Create account" : "Welcome back"}
          </p>
          <h2>
            {mode === "signup" ? "Start with Read" : "Sign in to Read"}
          </h2>

          {auth.googleEnabled && auth.googleClientId ? (
            <>
              <GoogleSignInButton
                clientId={auth.googleClientId}
                disabled={isBusy}
                onCredential={finishGoogleSignIn}
              />
              <div className="auth-divider"><span>or</span></div>
            </>
          ) : null}

          <form onSubmit={submit} className="auth-form">
            <label>
              Email
              <input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                disabled={isBusy}
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                autoComplete={
                  mode === "signup" ? "new-password" : "current-password"
                }
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                disabled={isBusy}
                required
              />
            </label>

            {mode === "signup" ? (
              <label>
                Confirm password
                <input
                  type="password"
                  autoComplete="new-password"
                  value={confirmation}
                  onChange={(event) => setConfirmation(event.target.value)}
                  disabled={isBusy}
                  required
                />
              </label>
            ) : null}

            {localError || auth.error ? (
              <p className="auth-error" role="alert">
                {localError ?? auth.error}
              </p>
            ) : null}

            <button
              className="auth-submit"
              type="submit"
              disabled={isBusy}
            >
              {isBusy
                ? "Connecting…"
                : mode === "signup"
                  ? "Create account"
                  : "Sign in"}
            </button>
          </form>

          <p className="auth-switch">
            {mode === "signup"
              ? "Already have an account?"
              : "New to Floently?"}{" "}
            <a
              href={mode === "signup" ? "/login" : "/signup"}
              onClick={(event) => {
                event.preventDefault();
                const target = mode === "signup" ? "/login" : "/signup";
                navigateTo(
                  returnTo === "/app/library"
                    ? target
                    : `${target}?returnTo=${encodeURIComponent(returnTo)}`,
                );
              }}
            >
              {mode === "signup" ? "Sign in" : "Create one"}
            </a>
          </p>
        </div>
      </section>
    </main>
  );
}
