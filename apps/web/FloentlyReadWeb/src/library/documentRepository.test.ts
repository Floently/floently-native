import { describe, expect, it } from "vitest";
import { libraryDocumentDatabaseName } from "./documentRepository";

describe("legacy local document account isolation", () => {
  it("uses a distinct v2 IndexedDB namespace per authenticated owner", () => {
    const first = libraryDocumentDatabaseName("account-a");
    const second = libraryDocumentDatabaseName("account-b");

    expect(first).not.toBe(second);
    expect(first).toContain("floently-read-web-vnext-v2");
    expect(first).toContain("account-a");
    expect(second).toContain("account-b");
  });

  it("fails closed without an authenticated owner", () => {
    expect(() => libraryDocumentDatabaseName(" ")).toThrow(
      "Authenticated Read owner is required",
    );
  });
});
