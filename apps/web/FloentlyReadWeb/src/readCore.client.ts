import type {
  LogicalTimePosition,
  ReadCoreRequest,
  ReadCoreRequestWithoutId,
  ReadCoreResponse,
  ReadingManifestSummary,
  ReadingSegmentDescriptor,
  ReadingSegmentWindow,
  SegmentPosition,
} from "./readCore.types";

type PendingRequest = {
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
};

export class ReadCoreWorkerClient {
  private readonly worker: Worker;
  private readonly pending = new Map<number, PendingRequest>();
  private nextId = 1;

  constructor() {
    this.worker = new Worker(new URL("./readCore.worker.ts", import.meta.url), {
      type: "module",
      name: "floently-read-core",
    });

    this.worker.onmessage = (event: MessageEvent<ReadCoreResponse>) => {
      const pending = this.pending.get(event.data.id);
      if (!pending) return;
      this.pending.delete(event.data.id);

      if (event.data.ok) {
        pending.resolve(event.data.result);
      } else {
        pending.reject(new Error(event.data.error ?? "Read Core worker failed"));
      }
    };

    this.worker.onerror = (event) => {
      const error = new Error(event.message || "Read Core worker crashed");
      for (const pending of this.pending.values()) {
        pending.reject(error);
      }
      this.pending.clear();
    };
  }

  buildManifest(payload: {
    documentId: string;
    revisionId: string;
    title: string;
    language: string;
    text: string;
    maxScalars?: number;
  }): Promise<ReadingManifestSummary> {
    return this.request<ReadingManifestSummary>({
      type: "buildManifest",
      payload: {
        ...payload,
        maxScalars: payload.maxScalars ?? 3_600,
      },
    });
  }

  positionForProgress(
    handle: string,
    progress: number,
  ): Promise<SegmentPosition> {
    return this.request<SegmentPosition>({
      type: "positionForProgress",
      payload: { handle, progress },
    });
  }

  segmentForLogicalTime(
    handle: string,
    elapsedMs: number,
  ): Promise<LogicalTimePosition | null> {
    return this.request<LogicalTimePosition | null>({
      type: "segmentForLogicalTime",
      payload: { handle, elapsedMs },
    });
  }

  logicalTimeForScalar(
    handle: string,
    scalarOffset: number,
  ): Promise<number | null> {
    return this.request<{ elapsedMs: number } | null>({
      type: "logicalTimeForScalar",
      payload: { handle, scalarOffset },
    }).then((result) => result?.elapsedMs ?? null);
  }

  getSegment(
    handle: string,
    index: number,
  ): Promise<ReadingSegmentDescriptor> {
    return this.request<ReadingSegmentDescriptor>({
      type: "getSegment",
      payload: { handle, index },
    });
  }

  getSegmentWindow(
    handle: string,
    centerIndex: number,
    radius = 2,
  ): Promise<ReadingSegmentWindow> {
    return this.request<ReadingSegmentWindow>({
      type: "getSegmentWindow",
      payload: { handle, centerIndex, radius },
    });
  }

  prefetchIndexes(
    handle: string,
    activeIndex: number,
    horizonMs = 120_000,
    maxSegments = 4,
  ): Promise<number[]> {
    return this.request<number[]>({
      type: "prefetchIndexes",
      payload: { handle, activeIndex, horizonMs, maxSegments },
    });
  }

  async dropManifest(handle: string): Promise<void> {
    await this.request({
      type: "dropManifest",
      payload: { handle },
    });
  }

  terminate(): void {
    this.worker.terminate();
    const error = new Error("Read Core worker client terminated");
    for (const pending of this.pending.values()) {
      pending.reject(error);
    }
    this.pending.clear();
  }

  private request<T>(
    request: ReadCoreRequestWithoutId,
  ): Promise<T> {
    const id = this.nextId++;
    const message = { id, ...request } as ReadCoreRequest;

    return new Promise<T>((resolve, reject) => {
      this.pending.set(id, {
        resolve: resolve as (value: unknown) => void,
        reject,
      });
      this.worker.postMessage(message);
    });
  }
}
