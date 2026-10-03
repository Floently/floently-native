/**
 * ReadingManifest coordinates are Unicode scalar/code-point counts.
 * JavaScript DOM/String offsets are UTF-16 code-unit offsets.
 */
export function scalarCount(text: string): number {
  return Array.from(text).length;
}

export function utf16OffsetForScalar(
  text: string,
  scalarOffset: number,
): number {
  const bounded = Math.min(
    Math.max(0, Math.round(scalarOffset)),
    scalarCount(text),
  );

  let scalar = 0;
  let utf16 = 0;
  for (const codePoint of text) {
    if (scalar >= bounded) break;
    utf16 += codePoint.length;
    scalar += 1;
  }
  return utf16;
}

export function scalarOffsetForUtf16(
  text: string,
  utf16Offset: number,
): number | null {
  const bounded = Math.round(utf16Offset);
  if (bounded < 0 || bounded > text.length) {
    return null;
  }

  // Reject a UTF-16 position that splits a surrogate pair.
  if (bounded > 0 && bounded < text.length) {
    const prior = text.charCodeAt(bounded - 1);
    const next = text.charCodeAt(bounded);
    const priorIsHigh = prior >= 0xd800 && prior <= 0xdbff;
    const nextIsLow = next >= 0xdc00 && next <= 0xdfff;
    if (priorIsHigh && nextIsLow) {
      return null;
    }
  }

  return Array.from(text.slice(0, bounded)).length;
}
