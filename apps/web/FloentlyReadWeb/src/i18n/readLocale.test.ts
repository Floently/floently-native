import { describe, expect, it } from "vitest";
import {
  formatDocumentCount,
  normalizeReadLocale,
  readMessage,
} from "./readLocale";

describe("Read interface locale", () => {
  it("normalizes Finnish locale variants and falls back to English", () => {
    expect(normalizeReadLocale("fi-FI")).toBe("fi");
    expect(normalizeReadLocale("fi_FI")).toBe("fi");
    expect(normalizeReadLocale("sv-FI")).toBe("en");
    expect(normalizeReadLocale(null)).toBe("en");
  });

  it("provides localized shell messages", () => {
    expect(readMessage("en", "shell.library")).toBe("Library");
    expect(readMessage("fi", "shell.library")).toBe("Kirjasto");
    expect(readMessage("fi", "shell.skipToContent")).toBe("Siirry sisältöön");
  });

  it("uses locale-aware plural categories", () => {
    expect(formatDocumentCount("en", 1)).toBe("1 document");
    expect(formatDocumentCount("en", 2)).toBe("2 documents");
    expect(formatDocumentCount("fi", 1)).toBe("1 asiakirja");
    expect(formatDocumentCount("fi", 2)).toBe("2 asiakirjaa");
  });
});
