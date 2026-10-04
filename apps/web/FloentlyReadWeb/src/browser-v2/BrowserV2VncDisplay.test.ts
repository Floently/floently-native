import { describe, expect, it } from "vitest";
import {
  BrowserV2VncDisplay,
  browserV2VncDisplayUrl,
  type BrowserV2RfbLike,
} from "./BrowserV2VncDisplay";

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


describe("Browser V2 VNC display lifecycle", () => {
  it("can disconnect a hidden display and reconnect without closing the owner adapter", async () => {
    const instances: FakeRfb[] = [];

    class FakeRfb implements BrowserV2RfbLike {
      scaleViewport = false;
      clipViewport = false;
      resizeSession = false;
      viewOnly = true;
      focusOnClick = false;
      disconnected = false;

      constructor() {
        instances.push(this);
      }

      addEventListener(): void {}
      removeEventListener(): void {}
      disconnect(): void {
        this.disconnected = true;
      }
      focus(): void {}
    }

    const target = {} as HTMLElement;
    const stages: string[] = [];
    const display = new BrowserV2VncDisplay(
      target,
      (stage) => stages.push(stage),
      () => undefined,
      async () => FakeRfb as unknown as new (
        element: HTMLElement,
        url: string,
        options: { shared: false; wsProtocols: string[] },
      ) => BrowserV2RfbLike,
    );

    await display.connect(grant());
    expect(instances).toHaveLength(1);

    display.disconnect();
    expect(instances[0].disconnected).toBe(true);
    expect(stages.at(-1)).toBe("DISPLAY_DISCONNECTED");

    await display.connect(grant({
      displayTicket: "B".repeat(43),
    }));
    expect(instances).toHaveLength(2);
    expect(instances[1].disconnected).toBe(false);

    display.close();
    await expect(
      display.connect(grant({
        displayTicket: "C".repeat(43),
      })),
    ).rejects.toThrow("DISPLAY_CLOSED");
  });
});
