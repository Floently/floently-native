import { describe, expect, it } from "vitest";
import { loginPathForReturnTo, safeReturnTo } from "./navigation";

const ORIGIN = "https://read.floently.com";

describe("safeReturnTo", () => {
  it("accepts protected in-app routes", () => {
    expect(
      safeReturnTo("/app/reader/book-1?from=library#page", ORIGIN),
    ).toBe("/app/reader/book-1?from=library#page");
  });

  it("rejects external and protocol-relative redirects", () => {
    expect(safeReturnTo("https://evil.example/app", ORIGIN)).toBeNull();
    expect(safeReturnTo("//evil.example/app", ORIGIN)).toBeNull();
  });

  it("rejects public routes", () => {
    expect(safeReturnTo("/login", ORIGIN)).toBeNull();
    expect(safeReturnTo("/", ORIGIN)).toBeNull();
  });
});

describe("loginPathForReturnTo", () => {
  it("encodes the full protected location", () => {
    expect(loginPathForReturnTo("/app/import?source=file"))
      .toBe("/login?returnTo=%2Fapp%2Fimport%3Fsource%3Dfile");
  });
});
