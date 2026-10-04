import { describe, expect, it } from "vitest";
import {
  accountScopedLocalName,
  localReadOwnerScope,
  requireLocalReadOwnerId,
} from "./localOwnerScope";

describe("local Read owner scope", () => {
  it("fails closed when authenticated ownership is missing", () => {
    expect(() => requireLocalReadOwnerId("")).toThrow(
      "Authenticated Read owner is required",
    );
    expect(() => localReadOwnerScope("   ")).toThrow(
      "Authenticated Read owner is required",
    );
  });

  it("keeps account namespaces distinct and URL-safe", () => {
    expect(accountScopedLocalName("read-local", "account-a")).toBe(
      "read-local:account-a",
    );
    expect(accountScopedLocalName("read-local", "account/b")).toBe(
      "read-local:account%2Fb",
    );
    expect(
      accountScopedLocalName("read-local", "account-a"),
    ).not.toBe(
      accountScopedLocalName("read-local", "account-b"),
    );
  });
});
