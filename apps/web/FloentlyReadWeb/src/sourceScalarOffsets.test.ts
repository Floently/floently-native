import { describe, expect, it } from "vitest";
import {
  scalarCount,
  scalarOffsetForUtf16,
  utf16OffsetForScalar,
} from "./sourceScalarOffsets";

const cases = [
  ["Floently Read", 13, 13],
  ["Hyvää päivää", 11, 11],
  ["e\u0301", 2, 2],
  ["🙂", 1, 2],
  ["👩‍💻", 3, 5],
  ["🇫🇮", 2, 4],
  ["مرحبا", 5, 5],
  ["日本語", 3, 3],
  ["A🙂e\u0301日本", 6, 7],
] as const;

describe("canonical source scalar offsets", () => {
  it.each(cases)(
    "%s uses Unicode scalars rather than UTF-16 units",
    (text, expectedScalars, expectedUtf16) => {
      expect(scalarCount(text)).toBe(expectedScalars);
      expect(text.length).toBe(expectedUtf16);

      for (let scalar = 0; scalar <= expectedScalars; scalar += 1) {
        const utf16 = utf16OffsetForScalar(text, scalar);
        expect(scalarOffsetForUtf16(text, utf16)).toBe(scalar);
      }
    },
  );

  it("rejects UTF-16 offsets that split surrogate pairs", () => {
    expect(scalarOffsetForUtf16("🙂", 1)).toBeNull();
    expect(scalarOffsetForUtf16("A🙂B", 2)).toBeNull();
  });
});
