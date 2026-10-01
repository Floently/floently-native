/// <reference lib="webworker" />

import initReadCore, {
  build_manifest_json,
  logical_time_for_scalar_json,
  position_for_progress_json,
  prefetch_indexes_json,
  read_core_contract_version,
  segment_for_logical_time_json,
} from "./generated/read-core-wasm/floently_read_core_wasm.js";

import { boundedReaderWindowRange } from "./reader/readerWindow";

import type {
  ReadCoreRequest,
  ReadCoreResponse,
  ReadingManifestSummary,
  ReadingSegmentDescriptor,
  ReadingSegmentSummary,
  ReadingSegmentWindow,
} from "./readCore.types";

interface CoreSegment {
  id: string;
  text: string;
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

interface CoreManifest {
  schemaVersion: number;
  documentId: string;
  revisionId: string;
  title: string;
  language: string;
  wordCount: number;
  textScalarLength: number;
  estimatedSourceDurationMs: number;
  segments: CoreSegment[];
}

const manifestJsonByHandle = new Map<string, string>();
let wasmReady: Promise<unknown> | null = null;

async function ensureWasm(): Promise<void> {
  if (!wasmReady) {
    wasmReady = initReadCore();
  }
  await wasmReady;

  const version = read_core_contract_version();
  if (version !== 1) {
    throw new Error(`Unsupported Rust Read Core contract version: ${version}`);
  }
}

function makeHandle(documentId: string, revisionId: string): string {
  return `${documentId}:${revisionId}`;
}

function requireManifest(handle: string): string {
  const value = manifestJsonByHandle.get(handle);
  if (!value) {
    throw new Error(`Unknown or released ReadingManifest handle: ${handle}`);
  }
  return value;
}

function summarizeSegment(segment: CoreSegment): ReadingSegmentSummary {
  return {
    id: segment.id,
    index: segment.index,
    scalarStart: segment.scalarStart,
    scalarEnd: segment.scalarEnd,
    wordStart: segment.wordStart,
    wordEnd: segment.wordEnd,
    wordCount: segment.wordCount,
    estimatedSourceDurationMs: segment.estimatedSourceDurationMs,
    logicalStartMs: segment.logicalStartMs,
    logicalEndMs: segment.logicalEndMs,
  };
}

function describeSegment(segment: CoreSegment): ReadingSegmentDescriptor {
  return {
    ...summarizeSegment(segment),
    text: segment.text,
  };
}

async function dispatch(request: ReadCoreRequest): Promise<ReadCoreResponse> {
  try {
    await ensureWasm();

    switch (request.type) {
      case "buildManifest": {
        const started = performance.now();
        const {
          documentId,
          revisionId,
          title,
          language,
          text,
          maxScalars,
        } = request.payload;

        const manifestJson = build_manifest_json(
          documentId,
          revisionId,
          title,
          language,
          text,
          maxScalars,
        );
        const manifest = JSON.parse(manifestJson) as CoreManifest;
        const handle = makeHandle(documentId, revisionId);
        manifestJsonByHandle.set(handle, manifestJson);

        const result: ReadingManifestSummary = {
          handle,
          schemaVersion: manifest.schemaVersion,
          documentId: manifest.documentId,
          revisionId: manifest.revisionId,
          title: manifest.title,
          language: manifest.language,
          wordCount: manifest.wordCount,
          textScalarLength: manifest.textScalarLength,
          estimatedSourceDurationMs: manifest.estimatedSourceDurationMs,
          segmentCount: manifest.segments.length,
          firstSegments: manifest.segments.slice(0, 4).map(summarizeSegment),
          buildMs: performance.now() - started,
        };

        return { id: request.id, ok: true, result };
      }

      case "positionForProgress": {
        const manifestJson = requireManifest(request.payload.handle);
        const result = JSON.parse(
          position_for_progress_json(
            manifestJson,
            Math.min(1, Math.max(0, request.payload.progress)),
          ),
        );
        return { id: request.id, ok: true, result };
      }

      case "segmentForLogicalTime": {
        const manifestJson = requireManifest(request.payload.handle);
        const result = JSON.parse(
          segment_for_logical_time_json(
            manifestJson,
            Math.max(0, Math.round(request.payload.elapsedMs)),
          ),
        );
        return { id: request.id, ok: true, result };
      }

      case "logicalTimeForScalar": {
        const manifestJson = requireManifest(request.payload.handle);
        const manifest = JSON.parse(manifestJson) as CoreManifest;
        const scalarOffset = Math.min(
          manifest.textScalarLength,
          Math.max(0, Math.round(request.payload.scalarOffset)),
        );
        const result = JSON.parse(
          logical_time_for_scalar_json(
            manifestJson,
            scalarOffset,
          ),
        );
        return { id: request.id, ok: true, result };
      }

      case "prefetchIndexes": {
        const manifestJson = requireManifest(request.payload.handle);
        const result = JSON.parse(
          prefetch_indexes_json(
            manifestJson,
            Math.max(0, Math.round(request.payload.activeIndex)),
            Math.max(30_000, Math.round(request.payload.horizonMs)),
            Math.max(1, Math.round(request.payload.maxSegments)),
          ),
        );
        return { id: request.id, ok: true, result };
      }

      case "getSegment": {
        const manifestJson = requireManifest(request.payload.handle);
        const manifest = JSON.parse(manifestJson) as CoreManifest;
        const index = Math.max(0, Math.round(request.payload.index));
        const segment = manifest.segments[index];

        if (!segment || segment.index !== index) {
          throw new Error(`ReadingManifest segment ${index} is unavailable`);
        }

        return {
          id: request.id,
          ok: true,
          result: describeSegment(segment),
        };
      }

      case "getSegmentWindow": {
        const manifestJson = requireManifest(request.payload.handle);
        const manifest = JSON.parse(manifestJson) as CoreManifest;
        const range = boundedReaderWindowRange(
          manifest.segments.length,
          request.payload.centerIndex,
          request.payload.radius,
        );

        const result: ReadingSegmentWindow = {
          centerIndex: range.centerIndex,
          startIndex: range.startIndex,
          endIndexExclusive: range.endIndexExclusive,
          totalSegments: manifest.segments.length,
          segments: manifest.segments
            .slice(range.startIndex, range.endIndexExclusive)
            .map(describeSegment),
        };

        return {
          id: request.id,
          ok: true,
          result,
        };
      }

      case "dropManifest": {
        const dropped = manifestJsonByHandle.delete(request.payload.handle);
        return { id: request.id, ok: true, result: { dropped } };
      }
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return { id: request.id, ok: false, error: message };
  }
}

self.onmessage = async (event: MessageEvent<ReadCoreRequest>) => {
  const response = await dispatch(event.data);
  self.postMessage(response);
};

export {};
