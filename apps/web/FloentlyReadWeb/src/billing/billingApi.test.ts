import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createReadCheckout,
  fetchReadPricing,
  normalizeReadPricePreview,
  normalizeReadUsageQuota,
} from "./billingApi";

const originalFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = originalFetch;
  vi.restoreAllMocks();
});

describe("Read billing API", () => {
  it("normalizes backend regional pricing", () => {
    expect(normalizeReadPricePreview({
      amount_display: "€11.99",
      amount_minor: 1199,
      band: "D",
      country: "Finland",
      country_code: "FI",
      currency: "EUR",
      economy_hours: 75,
      interval: "monthly",
      natural_hours: 2,
      plan: "reader",
      premium_hours: 0,
    })).toMatchObject({
      amountDisplay: "€11.99",
      amountMinor: 1199,
      band: "D",
      countryCode: "FI",
      plan: "reader",
      economyHours: 75,
    });
  });

  it("filters malformed quota counters", () => {
    expect(normalizeReadUsageQuota({
      periodKey: "2026-10",
      plan: "reader",
      usage: { standardReadingSeconds: 120, bad: "2" },
      limits: { standardReadingSeconds: 1000 },
      remaining: { standardReadingSeconds: 880 },
    })).toEqual({
      periodKey: "2026-10",
      plan: "reader",
      usage: { standardReadingSeconds: 120 },
      limits: { standardReadingSeconds: 1000 },
      remaining: { standardReadingSeconds: 880 },
      updatedAt: null,
    });
  });

  it("requests pricing from the backend contract", async () => {
    globalThis.fetch = vi.fn(async (input) => {
      expect(String(input)).toContain(
        "/api/v1/billing/read-pricing?country=FI&plan=reader&interval=monthly",
      );
      return new Response(JSON.stringify({
        ok: true,
        data: {
          amount_display: "€11.99",
          amount_minor: 1199,
          band: "D",
          country: "Finland",
          country_code: "FI",
          currency: "EUR",
          economy_hours: 75,
          interval: "monthly",
          natural_hours: 2,
          plan: "reader",
          premium_hours: 0,
        },
      }), { status: 200 });
    }) as typeof fetch;

    await expect(fetchReadPricing("FI", "reader", "monthly")).resolves
      .toMatchObject({ amountDisplay: "€11.99", plan: "reader" });
  });

  it("accepts only secure backend-issued checkout URLs", async () => {
    globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
      ok: true,
      data: { checkoutUrl: "https://checkout.stripe.com/c/pay/test" },
    }), { status: 200 })) as typeof fetch;

    await expect(createReadCheckout("FI", "reader", "monthly"))
      .resolves.toBe("https://checkout.stripe.com/c/pay/test");

    globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
      ok: true,
      data: { checkoutUrl: "http://example.com/not-secure" },
    }), { status: 200 })) as typeof fetch;

    await expect(createReadCheckout("FI", "reader", "monthly"))
      .rejects.toThrow("insecure");
  });
});
