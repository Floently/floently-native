import { describe, expect, it } from "vitest";
import {
  MAX_READER_WINDOW_RADIUS,
  boundedReaderWindowRange,
} from "./readerWindow";

describe("boundedReaderWindowRange", () => {
  it("returns an empty bounded range for an empty document", () => {
    expect(boundedReaderWindowRange(0, 40, 4)).toEqual({
      centerIndex: 0,
      startIndex: 0,
      endIndexExclusive: 0,
    });
  });

  it("caps the reader window independently of document size", () => {
    const range = boundedReaderWindowRange(10_000, 5_000, 100);

    expect(MAX_READER_WINDOW_RADIUS).toBe(4);
    expect(range).toEqual({
      centerIndex: 5_000,
      startIndex: 4_996,
      endIndexExclusive: 5_005,
    });
    expect(range.endIndexExclusive - range.startIndex).toBeLessThanOrEqual(9);
  });

  it("clamps a requested center at the beginning", () => {
    expect(boundedReaderWindowRange(100, -50, 2)).toEqual({
      centerIndex: 0,
      startIndex: 0,
      endIndexExclusive: 3,
    });
  });

  it("keeps the window bounded at the end", () => {
    expect(boundedReaderWindowRange(5, 4, 2)).toEqual({
      centerIndex: 4,
      startIndex: 2,
      endIndexExclusive: 5,
    });
  });
});
