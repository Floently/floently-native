import { describe, expect, it } from "vitest";
import {
  anchorSpanForScalar,
  buildBrowserReadingSource,
  sameBrowserReadingIdentity,
  scalarStartForAnchor,
  unicodeScalarLength,
} from "./browserReadingSource";
import type { BrowserReadingDocument } from "./browserContracts";

function document(
  overrides: Partial<BrowserReadingDocument> = {},
): BrowserReadingDocument {
  return {
    documentId: "page-1",
    revision: "rev-1",
    title: "Article",
    language: "en",
    canonicalUrl: "https://example.com/article",
    sentences: [
      {
        id: "s1",
        text: "Hello 👋 world",
        order: 1,
        wordCount: 3,
        anchor: { id: "a1", revision: "rev-1" },
      },
      {
        id: "s2",
        text: "Second sentence.",
        order: 2,
        wordCount: 2,
        anchor: { id: "a2", revision: "rev-1" },
      },
    ],
    ...overrides,
  };
}

describe("browserReadingSource", () => {
  it("counts Unicode scalars instead of UTF-16 code units", () => {
    expect("👋".length).toBe(2);
    expect(unicodeScalarLength("Hello 👋 world")).toBe(13);

    const source = buildBrowserReadingSource(document());

    expect(source.spans[0]).toMatchObject({
      scalarStart: 0,
      scalarEnd: 13,
    });
    expect(source.spans[1].scalarStart).toBe(15);
    expect(source.scalarLength).toBe(
      unicodeScalarLength("Hello 👋 world\n\nSecond sentence."),
    );
  });

  it("maps separator scalars forward to the next page sentence", () => {
    const source = buildBrowserReadingSource(document());

    expect(anchorSpanForScalar(source, 12)?.anchor.id).toBe("a1");
    expect(anchorSpanForScalar(source, 13)?.anchor.id).toBe("a2");
    expect(anchorSpanForScalar(source, 14)?.anchor.id).toBe("a2");
  });

  it("maps the document end to the last anchor", () => {
    const source = buildBrowserReadingSource(document());

    expect(
      anchorSpanForScalar(source, source.scalarLength)?.anchor.id,
    ).toBe("a2");
  });

  it("requires both opaque anchor id and revision for reverse mapping", () => {
    const source = buildBrowserReadingSource(document());

    expect(
      scalarStartForAnchor(source, { id: "a2", revision: "rev-1" }),
    ).toBe(15);
    expect(
      scalarStartForAnchor(source, { id: "a2", revision: "rev-old" }),
    ).toBeNull();
  });

  it("sorts sentence order stably and ignores empty extracted nodes", () => {
    const source = buildBrowserReadingSource(
      document({
        sentences: [
          {
            id: "empty",
            text: "   ",
            order: 0,
            wordCount: 0,
            anchor: { id: "empty", revision: "rev-1" },
          },
          {
            id: "later",
            text: "Later",
            order: 2,
            wordCount: 1,
            anchor: { id: "later", revision: "rev-1" },
          },
          {
            id: "first-a",
            text: "First A",
            order: 1,
            wordCount: 2,
            anchor: { id: "first-a", revision: "rev-1" },
          },
          {
            id: "first-b",
            text: "First B",
            order: 1,
            wordCount: 2,
            anchor: { id: "first-b", revision: "rev-1" },
          },
        ],
      }),
    );

    expect(source.text).toBe("First A\n\nFirst B\n\nLater");
    expect(source.spans.map((span) => span.sentenceId)).toEqual([
      "first-a",
      "first-b",
      "later",
    ]);
  });

  it("treats same revision with changed canonical text as a different page proof", () => {
    const original = buildBrowserReadingSource(document());
    const changed = buildBrowserReadingSource(
      document({
        sentences: [
          ...document().sentences.slice(0, 1),
          {
            ...document().sentences[1],
            text: "Changed sentence.",
          },
        ],
      }),
    );

    expect(sameBrowserReadingIdentity(original, changed)).toBe(false);
  });
});
