export interface ReaderWindowRange {
  centerIndex: number;
  startIndex: number;
  endIndexExclusive: number;
}

export const MAX_READER_WINDOW_RADIUS = 4;

export function boundedReaderWindowRange(
  totalSegments: number,
  centerIndex: number,
  requestedRadius: number,
): ReaderWindowRange {
  const count = Math.max(0, Math.floor(totalSegments));

  if (count === 0) {
    return {
      centerIndex: 0,
      startIndex: 0,
      endIndexExclusive: 0,
    };
  }

  const center = Math.min(
    count - 1,
    Math.max(0, Math.round(centerIndex)),
  );
  const radius = Math.min(
    MAX_READER_WINDOW_RADIUS,
    Math.max(0, Math.round(requestedRadius)),
  );

  return {
    centerIndex: center,
    startIndex: Math.max(0, center - radius),
    endIndexExclusive: Math.min(count, center + radius + 1),
  };
}
