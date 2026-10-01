import { useEffect, useRef, useState } from "react";
import { ensureGoogleIdentityScript } from "./googleIdentity";

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

    void ensureGoogleIdentityScript()
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
