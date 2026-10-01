import { describe, expect, it } from "vitest";
import { normalizeAuthSession } from "./authStore";

describe("normalizeAuthSession", () => {
  it("accepts the legacy READ session shape", () => {
    const session = normalizeAuthSession({
      apiKey: "legacy-token",
      user: {
        id: "user-1",
        email: "reader@example.com",
        name: "Reader",
        plan: "full",
      },
    });

    expect(session).toEqual({
      accessToken: "legacy-token",
      user: {
        id: "user-1",
        email: "reader@example.com",
        name: "Reader",
        plan: "full",
        readPlan: null,
        avatarDataUrl: null,
      },
    });
  });

  it("accepts the Learn auth envelope", () => {
    const session = normalizeAuthSession({
      data: {
        auth_user: {
          user_id: "user-2",
          email: "learn@example.com",
          subscription_tier: "pro",
          readPlan: "full",
        },
        tokens: {
          access_token: "learn-token",
        },
      },
    });

    expect(session?.accessToken).toBe("learn-token");
    expect(session?.user).toMatchObject({
      id: "user-2",
      email: "learn@example.com",
      plan: "pro",
      readPlan: "full",
    });
  });

  it("uses a bearer fallback when the refreshed session omits a token", () => {
    const session = normalizeAuthSession(
      {
        user: {
          id: "user-3",
          email: "resume@example.com",
          plan: "app",
        },
      },
      "stored-token",
    );

    expect(session?.accessToken).toBe("stored-token");
  });

  it("rejects incomplete identity payloads", () => {
    expect(normalizeAuthSession({ user: { email: "missing-id@example.com" } }))
      .toBeNull();
  });
});
