import { afterEach, describe, expect, it, vi } from "vitest";
import { getReadBillingPortalUrl } from "./runtime";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("getReadBillingPortalUrl", () => {
  it("accepts a configured HTTPS portal URL", () => {
    vi.stubEnv(
      "VITE_STRIPE_READ_BILLING_PORTAL_URL",
      "https://billing.example.com/session",
    );

    expect(getReadBillingPortalUrl()).toBe(
      "https://billing.example.com/session",
    );
  });

  it("rejects insecure or malformed portal URLs", () => {
    vi.stubEnv(
      "VITE_STRIPE_READ_BILLING_PORTAL_URL",
      "http://billing.example.com/session",
    );
    expect(getReadBillingPortalUrl()).toBeNull();

    vi.stubEnv("VITE_STRIPE_READ_BILLING_PORTAL_URL", "not a url");
    expect(getReadBillingPortalUrl()).toBeNull();
  });

  it("supports the legacy portal environment key", () => {
    vi.stubEnv("VITE_STRIPE_READ_BILLING_PORTAL_URL", "");
    vi.stubEnv(
      "VITE_STRIPE_BILLING_PORTAL_URL",
      "https://billing.example.com/legacy",
    );

    expect(getReadBillingPortalUrl()).toBe(
      "https://billing.example.com/legacy",
    );
  });
});
