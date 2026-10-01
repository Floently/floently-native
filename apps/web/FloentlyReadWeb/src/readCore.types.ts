export interface ReadingSegmentSummary {
  id: string;
  index: number;
  scalarStart: number;
  scalarEnd: number;
  wordStart: number;
  wordEnd: number;
  wordCount: number;
  estimatedSourceDurationMs: number;
  logicalStartMs: number;
  logicalEndMs: number;
}

export interface ReadingSegmentDescriptor extends ReadingSegmentSummary {
  text: string;
}

export interface ReadingSegmentWindow {
  centerIndex: number;
  startIndex: number;
  endIndexExclusive: number;
  totalSegments: number;
  segments: ReadingSegmentDescriptor[];
}

export interface ReadingManifestSummary {
  handle: string;
  schemaVersion: number;
  documentId: string;
  revisionId: string;
  title: string;
  language: string;
  wordCount: number;
  textScalarLength: number;
  estimatedSourceDurationMs: number;
  segmentCount: number;
  firstSegments: ReadingSegmentSummary[];
  buildMs: number;
}

export interface SegmentPosition {
  index: number;
  fraction: number;
}

export interface LogicalTimePosition {
  index: number;
  localOffsetMs: number;
}

export type ReadCoreRequest =
  | {
      id: number;
      type: "buildManifest";
      payload: {
        documentId: string;
        revisionId: string;
        title: string;
        language: string;
        text: string;
        maxScalars: number;
      };
    }
  | {
      id: number;
      type: "positionForProgress";
      payload: {
        handle: string;
        progress: number;
      };
    }
  | {
      id: number;
      type: "segmentForLogicalTime";
      payload: {
        handle: string;
        elapsedMs: number;
      };
    }
  | {
      id: number;
      type: "logicalTimeForScalar";
      payload: {
        handle: string;
        scalarOffset: number;
      };
    }
  | {
      id: number;
      type: "prefetchIndexes";
      payload: {
        handle: string;
        activeIndex: number;
        horizonMs: number;
        maxSegments: number;
      };
    }
  | {
      id: number;
      type: "getSegment";
      payload: {
        handle: string;
        index: number;
      };
    }
  | {
      id: number;
      type: "getSegmentWindow";
      payload: {
        handle: string;
        centerIndex: number;
        radius: number;
      };
    }
  | {
      id: number;
      type: "dropManifest";
      payload: {
        handle: string;
      };
    };

export type ReadCoreRequestWithoutId =
  ReadCoreRequest extends infer Request
    ? Request extends { id: number }
      ? Omit<Request, "id">
      : never
    : never;

export type ReadCoreSuccess =
  | ReadingManifestSummary
  | ReadingSegmentDescriptor
  | ReadingSegmentWindow
  | SegmentPosition
  | LogicalTimePosition
  | { elapsedMs: number }
  | number[]
  | null
  | { dropped: boolean };

export interface ReadCoreResponse {
  id: number;
  ok: boolean;
  result?: ReadCoreSuccess;
  error?: string;
}
