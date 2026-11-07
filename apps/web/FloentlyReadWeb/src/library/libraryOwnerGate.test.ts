import { describe, expect, it } from "vitest";
import { LibraryOwnerGate } from "./libraryOwnerGate";

describe("LibraryOwnerGate", () => {
  it("rejects a refresh that resolves after an account switch", () => {
    const gate = new LibraryOwnerGate("account-a");
    const accountA = gate.beginRefresh("account-a");

    expect(accountA).not.toBeNull();
    expect(gate.isRefreshCurrent(accountA!)).toBe(true);

    gate.setOwner("account-b");

    expect(gate.isRefreshCurrent(accountA!)).toBe(false);
    expect(gate.isOwnerCurrent(accountA!)).toBe(false);

    const accountB = gate.beginRefresh("account-b");
    expect(accountB).not.toBeNull();
    expect(gate.isRefreshCurrent(accountB!)).toBe(true);
  });

  it("lets only the newest refresh for one account commit", () => {
    const gate = new LibraryOwnerGate("account-a");
    const first = gate.beginRefresh("account-a");
    const second = gate.beginRefresh("account-a");

    expect(first).not.toBeNull();
    expect(second).not.toBeNull();
    expect(gate.isRefreshCurrent(first!)).toBe(false);
    expect(gate.isRefreshCurrent(second!)).toBe(true);
  });

  it("keeps an owner-bound mutation valid across same-owner refreshes", () => {
    const gate = new LibraryOwnerGate("account-a");
    const mutation = gate.capture("account-a");

    expect(mutation).not.toBeNull();

    gate.beginRefresh("account-a");

    expect(gate.isOwnerCurrent(mutation!)).toBe(true);

    gate.setOwner("account-b");

    expect(gate.isOwnerCurrent(mutation!)).toBe(false);
  });

  it("invalidates an in-flight refresh after a successful mutation", () => {
    const gate = new LibraryOwnerGate("account-a");
    const refresh = gate.beginRefresh("account-a");

    expect(refresh).not.toBeNull();
    gate.invalidateRefresh("account-a");

    expect(gate.isRefreshCurrent(refresh!)).toBe(false);
  });

  it("fails closed for missing or mismatched owners", () => {
    const gate = new LibraryOwnerGate("account-a");

    expect(gate.capture(null)).toBeNull();
    expect(gate.capture("account-b")).toBeNull();
    expect(gate.beginRefresh("")).toBeNull();
  });
});
