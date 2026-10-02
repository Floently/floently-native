import { describe, expect, it } from "vitest";
import {
  hasAppOnlyAccess,
  hasFullAccess,
  hasReaderAccess,
  normalizePlanName,
} from "./planAccess";

describe("Read plan access", () => {
  it("normalizes backend plan variants", () => {
    expect(normalizePlanName("Full-Access")).toBe("full_access");
    expect(normalizePlanName("Read Creator")).toBe("read_creator");
  });

  it("keeps Reader and Creator semantics distinct", () => {
    expect(hasReaderAccess("reader")).toBe(true);
    expect(hasAppOnlyAccess("reader")).toBe(true);
    expect(hasFullAccess("reader")).toBe(false);
    expect(hasFullAccess("creator")).toBe(true);
  });
});
