import { describe, expect, it } from "vitest";
import { BrowserV2VncAttachGate } from "./BrowserV2VncAttachGate";

describe("Browser V2 VNC attach gate", () => {
  it("invalidates an in-flight attach across a hidden route transition", () => {
    const gate = new BrowserV2VncAttachGate(true);
    const first = gate.begin();

    expect(first).not.toBeNull();
    expect(gate.isCurrent(first!)).toBe(true);

    gate.setActive(false);
    expect(gate.isCurrent(first!)).toBe(false);

    gate.setActive(true);
    expect(gate.isCurrent(first!)).toBe(false);
    expect(gate.begin()).toBeNull();

    gate.finish(first!);

    const second = gate.begin();
    expect(second).not.toBeNull();
    expect(gate.isCurrent(second!)).toBe(true);
    expect(second?.routeEpoch).toBeGreaterThan(first!.routeEpoch);
  });

  it("allows only one attach or ticket refresh to be in flight", () => {
    const gate = new BrowserV2VncAttachGate(true);
    const first = gate.begin();

    expect(first).not.toBeNull();
    expect(gate.hasInFlight).toBe(true);
    expect(gate.begin()).toBeNull();

    gate.finish(first!);

    expect(gate.hasInFlight).toBe(false);
    expect(gate.begin()).not.toBeNull();
  });

  it("does not begin an attach while Browser is hidden", () => {
    const gate = new BrowserV2VncAttachGate(false);

    expect(gate.begin()).toBeNull();

    gate.setActive(true);

    expect(gate.begin()).not.toBeNull();
  });

  it("resets the bounded retry budget after a hide/show cycle", () => {
    const gate = new BrowserV2VncAttachGate(true);

    for (let attempt = 0; attempt < 3; attempt += 1) {
      const token = gate.begin();
      expect(token).not.toBeNull();
      gate.finish(token!);
    }

    expect(gate.attemptsSinceReset).toBe(3);
    expect(gate.begin()).toBeNull();

    gate.setActive(false);
    expect(gate.attemptsSinceReset).toBe(0);

    gate.setActive(true);
    expect(gate.begin()).not.toBeNull();
  });

  it("can reset retries immediately after a verified frame", () => {
    const gate = new BrowserV2VncAttachGate(true);
    const token = gate.begin();

    expect(token).not.toBeNull();
    gate.finish(token!);
    expect(gate.attemptsSinceReset).toBe(1);

    gate.resetRetryBudget();

    expect(gate.attemptsSinceReset).toBe(0);
  });
});
