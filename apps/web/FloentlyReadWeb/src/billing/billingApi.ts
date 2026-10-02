import { getAuthAccessToken } from "../auth/authStore";
import { buildApiUrl } from "../config/runtime";

export type BillingInterval = "monthly" | "yearly";
export type ReadPlanId = "reader" | "creator";

export interface ReadPricePreview {
  amountDisplay: string;
  amountMinor: number;
  band: string;
  country: string;
  countryCode: string;
  currency: string;
  economyHours: number;
  interval: BillingInterval;
  naturalHours: number;
  plan: ReadPlanId | "free";
  premiumHours: number;
  stripePriceLookupKey: string | null;
  stripeProductLookupKey: string | null;
}

export interface ReadUsageQuota {
  periodKey: string;
  plan: string;
  usage: Record<string, number>;
  limits: Record<string, number>;
  remaining: Record<string, number>;
  updatedAt: string | null;
}

function record(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {};
}

function textValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function numberValue(value: unknown, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function numberMap(value: unknown): Record<string, number> {
  return Object.fromEntries(
    Object.entries(record(value)).filter(
      (entry): entry is [string, number] =>
        typeof entry[1] === "number" && Number.isFinite(entry[1]),
    ),
  );
}

function authorizedHeaders(initial: HeadersInit = {}): Headers {
  const headers = new Headers(initial);
  const token = getAuthAccessToken();
  if (token?.trim()) headers.set("Authorization", `Bearer ${token.trim()}`);
  return headers;
}

async function responsePayload(response: Response): Promise<Record<string, unknown>> {
  try {
    return record(await response.json());
  } catch {
    return {};
  }
}

function apiError(payload: Record<string, unknown>, fallback: string): string {
  const nestedError = record(payload.error);
  for (const value of [payload.detail, nestedError.message, payload.message]) {
    if (typeof value === "string" && value.trim()) return value.trim();
  }
  return fallback;
}

export function normalizeReadPricePreview(value: unknown): ReadPricePreview {
  const source = record(value);
  const interval = textValue(source.interval) === "yearly" ? "yearly" : "monthly";
  const rawPlan = textValue(source.plan, "free");
  const plan: ReadPricePreview["plan"] =
    rawPlan === "reader" || rawPlan === "creator" ? rawPlan : "free";

  return {
    amountDisplay: textValue(source.amount_display),
    amountMinor: numberValue(source.amount_minor),
    band: textValue(source.band),
    country: textValue(source.country),
    countryCode: textValue(source.country_code),
    currency: textValue(source.currency),
    economyHours: numberValue(source.economy_hours),
    interval,
    naturalHours: numberValue(source.natural_hours),
    plan,
    premiumHours: numberValue(source.premium_hours),
    stripePriceLookupKey: typeof source.stripe_price_lookup_key === "string"
      ? source.stripe_price_lookup_key
      : null,
    stripeProductLookupKey: typeof source.stripe_product_lookup_key === "string"
      ? source.stripe_product_lookup_key
      : null,
  };
}

export function normalizeReadUsageQuota(value: unknown): ReadUsageQuota {
  const source = record(value);
  return {
    periodKey: textValue(source.periodKey),
    plan: textValue(source.plan, "free"),
    usage: numberMap(source.usage),
    limits: numberMap(source.limits),
    remaining: numberMap(source.remaining),
    updatedAt: typeof source.updatedAt === "string" ? source.updatedAt : null,
  };
}

export async function fetchReadPricing(
  country: string,
  plan: ReadPlanId,
  interval: BillingInterval,
): Promise<ReadPricePreview> {
  const params = new URLSearchParams({ country, plan, interval });
  const response = await fetch(
    `${buildApiUrl("/api/v1/billing/read-pricing")}?${params.toString()}`,
  );
  const payload = await responsePayload(response);

  if (!response.ok || payload.ok !== true) {
    throw new Error(apiError(payload, "Could not load regional pricing."));
  }

  return normalizeReadPricePreview(payload.data);
}

export async function createReadCheckout(
  country: string,
  plan: ReadPlanId,
  interval: BillingInterval,
): Promise<string> {
  const response = await fetch(buildApiUrl("/api/v1/billing/read-checkout"), {
    method: "POST",
    headers: authorizedHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({ country, plan, interval }),
  });
  const payload = await responsePayload(response);

  if (!response.ok || payload.ok !== true) {
    throw new Error(apiError(payload, "Could not start checkout."));
  }

  const checkoutUrl = textValue(record(payload.data).checkoutUrl).trim();
  if (!checkoutUrl) throw new Error("Checkout did not return a payment link.");

  let parsed: URL;
  try {
    parsed = new URL(checkoutUrl);
  } catch {
    throw new Error("Checkout returned an invalid payment link.");
  }
  if (parsed.protocol !== "https:") {
    throw new Error("Checkout returned an insecure payment link.");
  }

  return parsed.href;
}

export async function fetchReadUsageQuota(): Promise<ReadUsageQuota> {
  const response = await fetch(buildApiUrl("/api/v1/read/usage"), {
    method: "GET",
    headers: authorizedHeaders(),
  });
  const payload = await responsePayload(response);

  if (!response.ok) {
    throw new Error(apiError(payload, "Could not load Read usage quota."));
  }

  return normalizeReadUsageQuota(payload);
}
