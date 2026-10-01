import { useEffect, useRef, useState } from "react";

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (options: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              type: "standard";
              theme: "outline";
              size: "large";
              shape: "pill";
              text: "continue_with";
              width: number;
            },
          ) => void;
        };
      };
    };
  }
}

let googleScriptPromise: Promise<void> | null = null;

function loadGoogleIdentity(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (googleScriptPromise) return googleScriptPromise;

  googleScriptPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      'script[data-floently-google-identity="true"]',
    );

    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener(
        "error",
        () => reject(new Error("Google sign in could not be loaded.")),
        { once: true },
      );
      return;
    }

    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.dataset.floentlyGoogleIdentity = "true";
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error("Google sign in could not be loaded."));
    document.head.append(script);
  });

  return googleScriptPromise;
}

export function GoogleSignInButton({
  clientId,
  onCredential,
  disabled,
}: {
  clientId: string;
  onCredential: (credential: string) => Promise<void>;
  disabled?: boolean;
}) {
  const targetRef = useRef<HTMLDivElement | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    void loadGoogleIdentity()
      .then(() => {
        if (cancelled || !targetRef.current) return;
        const identity = window.google?.accounts?.id;

        if (!identity) {
          throw new Error("Google sign in is unavailable.");
        }

        targetRef.current.replaceChildren();
        identity.initialize({
          client_id: clientId,
          callback: (response) => {
            const credential = response.credential?.trim();
            if (credential && !disabled) {
              void onCredential(credential);
            }
          },
        });
        identity.renderButton(targetRef.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          shape: "pill",
          text: "continue_with",
          width: 320,
        });
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [clientId, disabled, onCredential]);

  return (
    <div className={disabled ? "google-signin disabled" : "google-signin"}>
      <div ref={targetRef} />
      {error ? <p className="auth-inline-error">{error}</p> : null}
    </div>
  );
}
