import { describe, expect, it } from "vitest";
import type { ReadAuthSession } from "./authStore";
import { getReadEntitlement } from "./readEntitlement";

function makeSession(user: Partial<ReadAuthSession["user"]>): ReadAuthSession {
  return {
    accessToken: "token",
    user: {
      id: "u1",
      email: "reader@example.com",
      name: null,
      plan: "free",
      readPlan: null,
      readAccess: null,
      readFullAccess: null,
      requiresUserApiKey: null,
      readPolicy: null,
      readStripeCustomerId: null,
      avatarDataUrl: null,
      ...user,
    },
  };
}

describe("getReadEntitlement", () => {
  it("prefers backend Read policy over legacy plan labels", () => {
    const entitlement = getReadEntitlement(makeSession({
      plan: "pro",
      readPolicy: {
        product: "read",
        tier: "paid",
        plan: "reader",
        legacyPlan: null,
        features: { canExportAudio: true, canTranslate: true },
        voiceAccess: {
          defaultTier: "standard_reader_plus",
          allowedTiers: ["standard_reader", "standard_reader_plus"],
          premiumStudioAllowed: false,
        },
        limits: {},
      },
    }));

    expect(entitlement).toMatchObject({
      isPaid: true,
      plan: "reader",
      canExportAudio: true,
      canRecord: false,
      allowedVoiceTiers: ["standard_reader", "standard_reader_plus"],
    });
  });

  it("does not grant paid access for an unknown free-tier policy plan", () => {
    const entitlement = getReadEntitlement(makeSession({
      readPolicy: {
        product: "read",
        tier: "free",
        plan: "future_unknown_plan",
        legacyPlan: null,
        features: {},
        voiceAccess: {
          defaultTier: "standard_reader",
          allowedTiers: ["standard_reader"],
          premiumStudioAllowed: false,
        },
        limits: {},
      },
    }));

    expect(entitlement).toMatchObject({
      isPaid: false,
      plan: "free",
      canExportAudio: false,
    });
  });

  it("supports legacy full-access sessions during migration", () => {
    expect(getReadEntitlement(makeSession({ readPlan: "full-access" })))
      .toMatchObject({ isPaid: true, plan: "full_access", canRecord: true });
  });

  it("does not infer paid Read access from a free session", () => {
    expect(getReadEntitlement(makeSession({})))
      .toMatchObject({ isPaid: false, plan: "free" });
  });
});
