import { useEffect, useMemo, useState } from "react";
import { getReadEntitlement } from "../auth/readEntitlement";
import { useAuthState } from "../auth/useAuthState";
import { getReadBillingPortalUrl } from "../config/runtime";
import { navigateTo } from "../routing/navigation";
import {
  createReadCheckout,
  fetchReadPricing,
  type BillingInterval,
  type ReadPlanId,
  type ReadPricePreview,
} from "./billingApi";

const COUNTRY_OPTIONS = [
  { code: "FI", label: "Finland" },
  { code: "US", label: "United States" },
  { code: "CA", label: "Canada" },
  { code: "GB", label: "United Kingdom" },
  { code: "NG", label: "Nigeria" },
  { code: "IN", label: "India" },
  { code: "BR", label: "Brazil" },
  { code: "ZA", label: "South Africa" },
] as const;

const PLAN_FEATURES: Record<ReadPlanId, string[]> = {
  reader: [
    "Persistent synced reading library",
    "PDF, document and live-web reading",
    "Saved cross-session reading progress",
    "Standard and Plus reader voices",
  ],
  creator: [
    "Everything in Reader",
    "Expanded reading and provider quotas",
    "Natural and premium voice allowance where available",
    "Creator-oriented recording, transcription and export entitlements",
  ],
};

function planIsCurrent(current: string, plan: ReadPlanId): boolean {
  return plan === "creator"
    ? current === "creator" || current === "full_access"
    : current === "reader" || current === "app";
}

function priceLabel(preview: ReadPricePreview | undefined): string {
  return preview?.amountDisplay || "Loading…";
}

export function SubscriptionPage() {
  const auth = useAuthState();
  const entitlement = getReadEntitlement(auth.session);
  const [country, setCountry] = useState("FI");
  const [interval, setInterval] = useState<BillingInterval>("monthly");
  const [pricing, setPricing] =
    useState<Partial<Record<ReadPlanId, ReadPricePreview>>>({});
  const [pricingStatus, setPricingStatus] =
    useState<"loading" | "ready" | "error">("loading");
  const [pricingError, setPricingError] = useState<string | null>(null);
  const [checkoutPlan, setCheckoutPlan] = useState<ReadPlanId | null>(null);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const billingPortalUrl = getReadBillingPortalUrl();

  useEffect(() => {
    let cancelled = false;
    setPricingStatus("loading");
    setPricingError(null);

    void Promise.all([
      fetchReadPricing(country, "reader", interval),
      fetchReadPricing(country, "creator", interval),
    ]).then(([reader, creator]) => {
      if (cancelled) return;
      setPricing({ reader, creator });
      setPricingStatus("ready");
    }).catch((reason: unknown) => {
      if (cancelled) return;
      setPricing({});
      setPricingStatus("error");
      setPricingError(
        reason instanceof Error
          ? reason.message
          : "Could not load regional pricing.",
      );
    });

    return () => {
      cancelled = true;
    };
  }, [country, interval]);

  const plans = useMemo(
    () => [
      {
        id: "reader" as const,
        title: "Reader",
        description:
          "For focused reading, listening and understanding across documents and live websites.",
      },
      {
        id: "creator" as const,
        title: "Creator",
        description:
          "For heavier reading workflows plus creation, recording, transcription and export capability.",
      },
    ],
    [],
  );

  async function choosePlan(plan: ReadPlanId): Promise<void> {
    if (!auth.session || checkoutPlan) return;
    setCheckoutPlan(plan);
    setCheckoutError(null);

    try {
      const checkoutUrl = await createReadCheckout(country, plan, interval);
      window.location.assign(checkoutUrl);
    } catch (reason) {
      setCheckoutPlan(null);
      setCheckoutError(
        reason instanceof Error ? reason.message : "Could not start checkout.",
      );
    }
  }

  return (
    <section className="product-page subscription-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Read subscription</p>
          <h1>Plan and billing</h1>
          <p>
            Regional prices and checkout sessions come from the Read billing
            backend. The browser never grants itself paid access.
          </p>
        </div>
        <button
          type="button"
          className="card-secondary"
          onClick={() => navigateTo("/app/account")}
        >
          Account
        </button>
      </header>

      <div className="subscription-controls" aria-label="Billing options">
        <label>
          <span>Country</span>
          <select
            value={country}
            onChange={(event) => setCountry(event.target.value)}
          >
            {COUNTRY_OPTIONS.map((option) => (
              <option value={option.code} key={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <div
          className="subscription-interval"
          role="group"
          aria-label="Billing interval"
        >
          {(["monthly", "yearly"] as const).map((value) => (
            <button
              type="button"
              key={value}
              aria-pressed={interval === value}
              onClick={() => setInterval(value)}
            >
              {value === "monthly" ? "Monthly" : "Yearly"}
            </button>
          ))}
        </div>
        <span className="subscription-current">
          Current: <strong>{entitlement.plan.replaceAll("_", " ")}</strong>
        </span>
      </div>

      {pricingError ? (
        <p className="page-error" role="alert">{pricingError}</p>
      ) : null}
      {checkoutError ? (
        <p className="page-error" role="alert">{checkoutError}</p>
      ) : null}

      <div
        className="subscription-grid"
        aria-busy={pricingStatus === "loading"}
      >
        {plans.map((plan) => {
          const preview = pricing[plan.id];
          const active = planIsCurrent(entitlement.plan, plan.id);
          const busy = checkoutPlan === plan.id;

          return (
            <article
              className={
                active ? "subscription-card current" : "subscription-card"
              }
              key={plan.id}
            >
              <div className="subscription-card-heading">
                <div>
                  <p>{active ? "Current access" : "Available plan"}</p>
                  <h2>{plan.title}</h2>
                </div>
                {preview ? <span>Band {preview.band}</span> : null}
              </div>

              <p className="subscription-description">{plan.description}</p>

              <div className="subscription-price">
                <strong>{priceLabel(preview)}</strong>
                <span>/ {interval === "monthly" ? "month" : "year"}</span>
              </div>

              {preview ? (
                <p className="subscription-region">
                  {preview.country} · {preview.currency} ·{" "}
                  {preview.economyHours}h economy
                  {preview.naturalHours > 0
                    ? ` · ${preview.naturalHours}h natural`
                    : ""}
                </p>
              ) : null}

              <ul>
                {PLAN_FEATURES[plan.id].map((feature) => (
                  <li key={feature}>{feature}</li>
                ))}
              </ul>

              <button
                type="button"
                className={active ? "card-secondary" : "page-primary-action"}
                disabled={
                  active
                  || pricingStatus !== "ready"
                  || Boolean(checkoutPlan)
                }
                onClick={() => void choosePlan(plan.id)}
              >
                {active
                  ? "Current plan"
                  : busy
                    ? "Opening secure checkout…"
                    : `Choose ${plan.title}`}
              </button>
            </article>
          );
        })}
      </div>

      {billingPortalUrl ? (
        <div className="subscription-manage">
          <div>
            <strong>Already subscribed?</strong>
            <span>
              Payment method and cancellation are managed through the
              configured billing portal.
            </span>
          </div>
          <a className="card-secondary" href={billingPortalUrl}>
            Manage billing
          </a>
        </div>
      ) : null}
    </section>
  );
}
