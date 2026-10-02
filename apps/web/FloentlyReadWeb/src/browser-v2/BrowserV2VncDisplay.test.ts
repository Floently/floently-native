import { describe, expect, it } from "vitest";
import { browserV2VncDisplayUrl } from "./BrowserV2VncDisplay";

const sessionId = "session-123";
const displayTicket = "A".repeat(43);
const displayPath =
  `/api/browser-v2/cloud/sessions/${sessionId}/display`;

function grant(overrides: Partial<{
  origin: string;
  sessionId: string;
  displayPath: string;
  displayTicket: string;
  displayExpiresInSeconds: number;
}> = {}) {
  return {
    origin: "https://browser.example.test",
    sessionId,
    displayPath,
    displayTicket,
    displayExpiresInSeconds: 30,
    ...overrides,
  };
}

describe("Browser V2 VNC display grant safety", () => {
  it("builds the canonical secure display socket without leaking the ticket into the URL", () => {
    const url = browserV2VncDisplayUrl(grant());

    expect(url).toBe(
      `wss://browser.example.test${displayPath}`,
    );
    expect(url).not.toContain(displayTicket);
    expect(new URL(url).search).toBe("");
  });

  it("rejects a display path issued for a different owner session", () => {
    expect(() =>
      browserV2VncDisplayUrl(
        grant({
          displayPath:
            "/api/browser-v2/cloud/sessions/older-session/display",
        }),
      ),
    ).toThrow("DISPLAY_PATH_INVALID");
  });

  it("rejects insecure non-local origins and URL credential/query smuggling", () => {
    expect(() =>
      browserV2VncDisplayUrl(grant({ origin: "http://browser.example.test" })),
    ).toThrow("DISPLAY_ORIGIN_INVALID");

    expect(() =>
      browserV2VncDisplayUrl(
        grant({ origin: "https://user:pass@browser.example.test/" }),
      ),
    ).toThrow("DISPLAY_ORIGIN_INVALID");

    expect(() =>
      browserV2VncDisplayUrl(
        grant({ origin: "https://browser.example.test/?ticket=secret" }),
      ),
    ).toThrow("DISPLAY_ORIGIN_INVALID");
  });

  it("allows loopback HTTP only for local development and upgrades it to ws", () => {
    expect(
      browserV2VncDisplayUrl(grant({ origin: "http://127.0.0.1/" })),
    ).toBe(`ws://127.0.0.1${displayPath}`);
  });

  it("rejects malformed or overlong-lived display grants", () => {
    expect(() =>
      browserV2VncDisplayUrl(grant({ displayTicket: "short" })),
    ).toThrow("DISPLAY_GRANT_INVALID");

    expect(() =>
      browserV2VncDisplayUrl(grant({ displayExpiresInSeconds: 91 })),
    ).toThrow("DISPLAY_GRANT_INVALID");

    expect(() =>
      browserV2VncDisplayUrl(grant({ sessionId: "bad/session" })),
    ).toThrow("DISPLAY_GRANT_INVALID");
  });
});
