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
});
