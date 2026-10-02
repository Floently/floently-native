import { useEffect, useState } from "react";
import { logout } from "../auth/authStore";
import { getReadEntitlement } from "../auth/readEntitlement";
import { useAuthState } from "../auth/useAuthState";
import {
  fetchReadUsageQuota,
  type ReadUsageQuota,
} from "../billing/billingApi";
import { navigateTo } from "../routing/navigation";

const QUOTA_ITEMS = [
  ["standardReadingSeconds", "Reading time", "seconds"],
  ["audioExportSeconds", "Audio exports", "seconds"],
  ["transcriptionSeconds", "Transcription", "seconds"],
  ["translationCharacters", "Translation", "characters"],
  ["providerRequests", "Provider runs", "requests"],
] as const;

function formatAmount(value: number, unit: string): string {
  const safe = Math.max(0, value || 0);

  if (unit === "seconds") {
    const minutes = Math.floor(safe / 60);
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const rest = minutes % 60;
      return rest ? `${hours}h ${rest}m` : `${hours}h`;
    }
    return `${minutes}m`;
  }

  if (unit === "characters") {
    if (safe >= 1_000_000) return `${(safe / 1_000_000).toFixed(1)}M`;
    if (safe >= 1_000) return `${Math.round(safe / 1_000)}k`;
  }

  return new Intl.NumberFormat().format(safe);
}

export function AccountPage() {
  const auth = useAuthState();
  const user = auth.session?.user;
  const entitlement = getReadEntitlement(auth.session);
  const [quota, setQuota] = useState<ReadUsageQuota | null>(null);
  const [quotaStatus, setQuotaStatus] =
    useState<"loading" | "ready" | "error">("loading");
  const [quotaError, setQuotaError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setQuota(null);
    setQuotaStatus("loading");
    setQuotaError(null);

    if (!user) {
      return () => {
        cancelled = true;
      };
    }

    void fetchReadUsageQuota()
      .then((next) => {
        if (cancelled) return;
        setQuota(next);
        setQuotaStatus("ready");
      })
      .catch((reason: unknown) => {
        if (cancelled) return;
        setQuotaStatus("error");
        setQuotaError(
          reason instanceof Error
            ? reason.message
            : "Could not load Read quota.",
        );
      });

    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  if (!user) return null;

  async function signOut() {
    await logout();
    navigateTo("/", true);
  }

  return (
    <section className="product-page settings-page account-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Floently account</p>
          <h1>Account</h1>
          <p>
            Shared Floently identity, Read-specific access and backend-owned
            usage limits are shown together without merging Learn billing into
            Read.
          </p>
        </div>
        <button
          type="button"
          className="page-primary-action"
          onClick={() => navigateTo("/app/subscription")}
        >
          Manage plan
        </button>
      </header>

      <div className="settings-grid account-summary-grid">
        <article className="settings-card">
          <h2>Profile</h2>
          <dl className="account-details">
            <div><dt>Email</dt><dd>{user.email}</dd></div>
            <div><dt>Name</dt><dd>{user.name || "Not set"}</dd></div>
            <div><dt>Floently plan</dt><dd>{user.plan}</dd></div>
            <div>
              <dt>Read plan</dt>
              <dd>{entitlement.plan.replaceAll("_", " ")}</dd>
            </div>
          </dl>
        </article>

        <article className="settings-card account-access-card">
          <h2>Read access</h2>
          <div className="account-access-pills">
            <span>
              {entitlement.isPaid ? "Paid Read access" : "Free Read access"}
            </span>
            <span>
              {entitlement.allowedVoiceTiers.length} voice tier
              {entitlement.allowedVoiceTiers.length === 1 ? "" : "s"}
            </span>
          </div>
          <p>
            {entitlement.canExportAudio
              ? "Audio export enabled."
              : "Audio export is not included in this access level."}
          </p>
          <button
            type="button"
            className="card-secondary"
            onClick={() => navigateTo("/app/subscription")}
          >
            Plan and billing
          </button>
        </article>
      </div>

      <section className="account-quota-section">
        <div className="account-section-heading">
          <h2>Read quota</h2>
          <span>{quota?.periodKey || "Current period"}</span>
        </div>

        {quotaStatus === "error" ? (
          <p className="page-error" role="alert">{quotaError}</p>
        ) : null}

        {quotaStatus === "loading" && !quota ? (
          <div className="page-loading account-quota-loading">
            Loading Read quota…
          </div>
        ) : null}

        {quota ? (
          <div className="account-quota-grid">
            {QUOTA_ITEMS.map(([key, label, unit]) => {
              const used = quota.usage[key] ?? 0;
              const limit = quota.limits[key] ?? 0;
              const remaining =
                quota.remaining[key] ?? Math.max(0, limit - used);
              const percent =
                limit > 0
                  ? Math.min(100, Math.max(0, used / limit * 100))
                  : 0;

              return (
                <article className="account-quota-card" key={key}>
                  <div>
                    <strong>{label}</strong>
                    <span>
                      {limit > 0 ? `${Math.round(percent)}%` : "Unavailable"}
                    </span>
                  </div>
                  <i aria-hidden="true">
                    <b style={{ width: `${percent}%` }} />
                  </i>
                  <p>
                    {formatAmount(used, unit)} used ·{" "}
                    {formatAmount(remaining, unit)} left
                  </p>
                </article>
              );
            })}
          </div>
        ) : null}
      </section>

      <article className="settings-card danger-card account-session-card">
        <h2>Session</h2>
        <p>
          Signing out clears this browser’s saved bearer fallback and asks the
          shared account service to end its session cookie.
        </p>
        <button
          type="button"
          className="card-secondary"
          onClick={() => void signOut()}
        >
          Sign out
        </button>
      </article>
    </section>
  );
}
