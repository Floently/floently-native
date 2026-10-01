import { describe, expect, it } from "vitest";
import {
  effectiveReadHighlightMode,
  normalizeReadHighlightMode,
  normalizeReadLanguage,
  normalizeReadReaderFont,
  normalizeReadReaderLineSpacing,
  normalizeReadReaderTextSize,
  normalizeReadSpeed,
  normalizeReadThemePreference,
  resolveReadTheme,
  sameReadLanguage,
} from "./readPreferencesStore";

describe("Read preference normalization", () => {
  it("validates stored theme, reading and typography values", () => {
    expect(normalizeReadThemePreference("system")).toBe("system");
    expect(normalizeReadThemePreference("sepia")).toBe("dark");

    expect(normalizeReadHighlightMode("none")).toBe("none");
    expect(normalizeReadHighlightMode("word")).toBe("word");
    expect(normalizeReadHighlightMode("paragraph")).toBe("sentence");

    expect(normalizeReadReaderFont("sans")).toBe("sans");
    expect(normalizeReadReaderFont("comic")).toBe("serif");

    expect(normalizeReadReaderTextSize("large")).toBe("large");
    expect(normalizeReadReaderTextSize("huge")).toBe("comfortable");

    expect(normalizeReadReaderLineSpacing("spacious")).toBe("spacious");
    expect(normalizeReadReaderLineSpacing("double")).toBe("comfortable");
  });

  it("bounds persisted playback speed to the supported contract", () => {
    expect(normalizeReadSpeed("2.5")).toBe(2.5);
    expect(normalizeReadSpeed(0.5)).toBe(0.5);
    expect(normalizeReadSpeed(3)).toBe(3);
    expect(normalizeReadSpeed(4)).toBe(1);
    expect(normalizeReadSpeed("not-a-number")).toBe(1);
  });

  it("resolves system theme without changing the stored preference", () => {
    expect(resolveReadTheme("system", true)).toBe("dark");
    expect(resolveReadTheme("system", false)).toBe("light");
    expect(resolveReadTheme("light", true)).toBe("light");
    expect(resolveReadTheme("dark", false)).toBe("dark");
  });

  it("matches voices and documents by base language", () => {
    expect(normalizeReadLanguage("EN-us")).toBe("en");
    expect(sameReadLanguage("en", "en-US")).toBe(true);
    expect(sameReadLanguage("fi-FI", "fi")).toBe(true);
    expect(sameReadLanguage("en", "fi")).toBe(false);
  });

  it("never fabricates word highlighting when verified timing is absent", () => {
    expect(effectiveReadHighlightMode("word", false)).toBe("sentence");
    expect(effectiveReadHighlightMode("word", true)).toBe("word");
    expect(effectiveReadHighlightMode("sentence", false)).toBe("sentence");
    expect(effectiveReadHighlightMode("none", false)).toBe("none");
  });
});
