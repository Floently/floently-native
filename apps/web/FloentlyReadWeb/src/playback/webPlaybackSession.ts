import type {
  LogicalTimePosition,
  ReadingManifestSummary,
  ReadingSegmentDescriptor,
} from "../readCore.types";
import {
  defaultVoiceIdForLanguage,
  type ReadTtsAsset,
  type ReadTtsProvider,
} from "../tts/readTtsProvider";
import {
  ReadAudioCache,
  type PlayableAudioAsset,
  type ReadAudioCachePort,
} from "./readAudioCache";
import {
  BrowserAudioEngine,
  type BrowserAudioEngineCallbacks,
  type ReadAudioEngine,
  type ReadAudioEngineFactory,
} from "./browserAudioEngine";

export type WebPlaybackStatus =
  | "idle"
  | "ready"
  | "preparing"
  | "playing"
  | "paused"
  | "buffering"
  | "ended"
  | "error";

export interface WebPlaybackSnapshot {
  status: WebPlaybackStatus;
  documentId: string | null;
  revisionId: string | null;
  title: string | null;
  author: string | null;
  durationMs: number;
  elapsedMs: number;
  bufferedAheadMs: number;
  activeSegmentIndex: number | null;
  canonicalScalarCursor: number | null;
  speed: number;
  voiceId: string;
  error: string | null;
}

export interface WebPlaybackTelemetryEvent {
  name:
    | "document_loaded"
    | "play_requested"
    | "pause"
    | "seek"
    | "tts_start"
    | "tts_ready"
    | "segment_started"
    | "segment_ended"
    | "segment_handoff"
    | "audio_waiting"
    | "audio_playing"
    | "playback_error";
  at: number;
  data?: Record<string, string | number | boolean | null>;
}

export type WebPlaybackTelemetrySink =
  (event: WebPlaybackTelemetryEvent) => void;

export interface ReadPlaybackCore {
  segmentForLogicalTime(
    handle: string,
    elapsedMs: number,
  ): Promise<LogicalTimePosition | null>;
  getSegment(
    handle: string,
    index: number,
  ): Promise<ReadingSegmentDescriptor>;
  prefetchIndexes(
    handle: string,
    activeIndex: number,
    horizonMs?: number,
    maxSegments?: number,
  ): Promise<number[]>;
}

interface RuntimeAudio {
  descriptor: ReadingSegmentDescriptor;
  tts: ReadTtsAsset;
  playable: PlayableAudioAsset;
  physicalDurationMs: number | null;
}

interface ResumeState {
  elapsedMs: number;
  speed: number;
  voiceId: string;
}

interface PendingSegmentHandoff {
  fromIndex: number;
  toIndex: number;
  logicalBoundaryMs: number;
  startedAtMs: number;
}

const MIN_SPEED = 0.5;
const MAX_SPEED = 3;
const DEFAULT_PREFETCH_HORIZON_MS = 120_000;
const DEFAULT_PREFETCH_SEGMENTS = 4;
const MAX_RECENT_TELEMETRY_EVENTS = 128;

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, value));
}

function safeNumber(value: unknown, fallback: number): number {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : fallback;
}

function baseLanguage(language: string | null | undefined): string | null {
  const normalized = language?.trim().toLowerCase();
  if (!normalized) return null;
  return normalized.split("-")[0] || null;
}

function sameLanguage(
  left: string | null | undefined,
  right: string | null | undefined,
): boolean {
  const normalizedLeft = baseLanguage(left);
  const normalizedRight = baseLanguage(right);
  return Boolean(
    normalizedLeft
    && normalizedRight
    && normalizedLeft === normalizedRight,
  );
}

function scalarCursorForDescriptor(
  descriptor: ReadingSegmentDescriptor,
  elapsedMs: number,
): number {
  const logicalDurationMs = Math.max(
    1,
    descriptor.logicalEndMs - descriptor.logicalStartMs,
  );
  const fraction = clamp(
    (elapsedMs - descriptor.logicalStartMs) / logicalDurationMs,
    0,
    1,
  );
  const scalarSpan = Math.max(
    0,
    descriptor.scalarEnd - descriptor.scalarStart,
  );

  return Math.round(
    descriptor.scalarStart + scalarSpan * fraction,
  );
}

export class WebPlaybackSession {
  private readonly listeners = new Set<() => void>();
  private readonly core: ReadPlaybackCore;
  private readonly tts: ReadTtsProvider;
  private readonly cache: ReadAudioCachePort;
  private readonly engine: ReadAudioEngine;
  private readonly telemetry?: WebPlaybackTelemetrySink;
  private readonly monotonicNow: () => number;
  private readonly recentTelemetry: WebPlaybackTelemetryEvent[] = [];

  private manifest: ReadingManifestSummary | null = null;
  private active: RuntimeAudio | null = null;
  private readonly audioByIndex = new Map<number, RuntimeAudio>();
  private readonly inFlightAudio = new Map<number, Promise<RuntimeAudio>>();
  private wantsPlayback = false;
  private generation = 0;
  private playbackIntentGeneration = 0;
  private preferredSpeed = 1;
  private preferredVoiceId = defaultVoiceIdForLanguage("en");
  private preferredVoiceLanguage: string | null = "en";
  private lastPersistedAt = 0;
  private lastTransitionAt = 0;
  private pendingSegmentHandoff: PendingSegmentHandoff | null = null;
  private readonly lifecycleDisposers: Array<() => void> = [];

  private snapshot: WebPlaybackSnapshot = {
    status: "idle",
    documentId: null,
    revisionId: null,
    title: null,
    author: null,
    durationMs: 0,
    elapsedMs: 0,
    bufferedAheadMs: 0,
    activeSegmentIndex: null,
    canonicalScalarCursor: null,
    speed: 1,
    voiceId: defaultVoiceIdForLanguage("en"),
    error: null,
  };

  constructor(options: {
    core: ReadPlaybackCore;
    tts: ReadTtsProvider;
    cache?: ReadAudioCachePort;
    engineFactory?: ReadAudioEngineFactory;
    telemetry?: WebPlaybackTelemetrySink;
    monotonicNow?: () => number;
    initialPreferences?: {
      speed?: number;
      voiceId?: string | null;
      voiceLanguage?: string | null;
    };
  }) {
    this.core = options.core;
    this.tts = options.tts;
    this.cache = options.cache ?? new ReadAudioCache();
    this.telemetry = options.telemetry;
    this.monotonicNow =
      options.monotonicNow
      ?? (() => (
        typeof performance !== "undefined"
          ? performance.now()
          : Date.now()
      ));

    const callbacks: BrowserAudioEngineCallbacks = {
      onTime: (currentTimeMs, physicalDurationMs) =>
        this.handlePhysicalTime(currentTimeMs, physicalDurationMs),
      onEnded: () => this.handleSegmentEnded(),
      onWaiting: () => this.handleWaiting(),
      onPlaying: () => this.handlePlaying(),
      onError: (message) => this.fail(message),
    };
    const engineFactory =
      options.engineFactory
      ?? ((engineCallbacks) => new BrowserAudioEngine(engineCallbacks));

    this.engine = engineFactory(callbacks);

    const initialSpeed = clamp(
      safeNumber(options.initialPreferences?.speed, 1),
      MIN_SPEED,
      MAX_SPEED,
    );
    const initialVoiceLanguage =
      baseLanguage(options.initialPreferences?.voiceLanguage);
    const initialVoiceId =
      options.initialPreferences?.voiceId?.trim()
      || defaultVoiceIdForLanguage(initialVoiceLanguage ?? "en");

    this.preferredSpeed = initialSpeed;
    this.preferredVoiceId = initialVoiceId;
    this.preferredVoiceLanguage = initialVoiceLanguage;
    this.snapshot = {
      ...this.snapshot,
      speed: initialSpeed,
      voiceId: initialVoiceId,
    };
    this.engine.setRate(initialSpeed);

    this.installMediaSessionHandlers();
    this.installPageLifecycleHandlers();
  }

  readonly subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  readonly getSnapshot = (): WebPlaybackSnapshot => this.snapshot;

  readonly getRecentTelemetry = (): WebPlaybackTelemetryEvent[] =>
    this.recentTelemetry.map((event) => ({
      ...event,
      ...(event.data ? { data: { ...event.data } } : {}),
    }));

  loadDocument(
    manifest: ReadingManifestSummary,
    options: {
      author?: string | null;
      voiceId?: string | null;
    } = {},
  ): void {
    this.generation += 1;
    this.playbackIntentGeneration += 1;
    this.wantsPlayback = false;
    this.clearPendingSegmentHandoff();
    this.engine.pause();
    this.releaseRuntimeAudio();

    this.manifest = manifest;
    const resume = this.readResumeState(manifest);
    const speed = clamp(
      resume?.speed ?? this.preferredSpeed,
      MIN_SPEED,
      MAX_SPEED,
    );
    const voiceId =
      options.voiceId?.trim()
      || resume?.voiceId?.trim()
      || (
        sameLanguage(
          this.preferredVoiceLanguage,
          manifest.language,
        )
          ? this.preferredVoiceId
          : defaultVoiceIdForLanguage(manifest.language)
      );
    const elapsedMs = clamp(
      resume?.elapsedMs ?? 0,
      0,
      manifest.estimatedSourceDurationMs,
    );

    this.preferredSpeed = speed;
    this.preferredVoiceId = voiceId;
    this.preferredVoiceLanguage = manifest.language;
    this.engine.setRate(speed);

    this.replaceSnapshot({
      status: "ready",
      documentId: manifest.documentId,
      revisionId: manifest.revisionId,
      title: manifest.title,
      author: options.author?.trim() || null,
      durationMs: manifest.estimatedSourceDurationMs,
      elapsedMs,
      bufferedAheadMs: 0,
      activeSegmentIndex: null,
      canonicalScalarCursor: elapsedMs === 0 ? 0 : null,
      speed,
      voiceId,
      error: null,
    });

    this.publishMediaMetadata();
    this.publishMediaState();
    this.emit("document_loaded", {
      durationMs: manifest.estimatedSourceDurationMs,
      segmentCount: manifest.segmentCount,
      resumed: elapsedMs > 0,
    });
  }

  clear(): void {
    this.generation += 1;
    this.playbackIntentGeneration += 1;
    this.wantsPlayback = false;
    this.clearPendingSegmentHandoff();
    this.engine.pause();
    this.releaseRuntimeAudio();
    this.manifest = null;
    this.replaceSnapshot({
      status: "idle",
      documentId: null,
      revisionId: null,
      title: null,
      author: null,
      durationMs: 0,
      elapsedMs: 0,
      bufferedAheadMs: 0,
      activeSegmentIndex: null,
      canonicalScalarCursor: null,
      error: null,
    });
    this.clearMediaSession(false);
  }

  async play(): Promise<void> {
    if (!this.manifest) return;

    const intentGeneration = ++this.playbackIntentGeneration;
    this.clearPendingSegmentHandoff();
    this.wantsPlayback = true;
    this.emit("play_requested");
    await this.startAtDocumentTime(
      this.snapshot.elapsedMs,
      true,
      intentGeneration,
    );
  }

  pause(): void {
    if (!this.manifest) return;

    this.playbackIntentGeneration += 1;
    this.wantsPlayback = false;
    this.clearPendingSegmentHandoff();
    this.engine.pause();
    this.replaceSnapshot({
      status: "paused",
      error: null,
    });
    this.persistResumeState(true);
    this.publishMediaState();
    this.emit("pause");
  }

  togglePlayPause(): Promise<void> | void {
    if (this.snapshot.status === "playing" || this.wantsPlayback) {
      this.pause();
      return;
    }
    return this.play();
  }

  async seek(documentTimeMs: number): Promise<void> {
    if (!this.manifest) return;

    const target = clamp(
      documentTimeMs,
      0,
      this.snapshot.durationMs,
    );
    const resumeAfterSeek = this.wantsPlayback
      || this.snapshot.status === "playing";
    const intentGeneration = ++this.playbackIntentGeneration;
    this.clearPendingSegmentHandoff();
    this.wantsPlayback = resumeAfterSeek;

    this.emit("seek", {
      fromMs: Math.round(this.snapshot.elapsedMs),
      toMs: Math.round(target),
    });

    await this.startAtDocumentTime(
      target,
      resumeAfterSeek,
      intentGeneration,
    );
  }

  seekBy(deltaMs: number): Promise<void> {
    return this.seek(this.snapshot.elapsedMs + deltaMs);
  }

  setSpeed(
    speed: number,
    options: { updatePreference?: boolean } = {},
  ): void {
    const bounded = clamp(
      safeNumber(speed, this.snapshot.speed),
      MIN_SPEED,
      MAX_SPEED,
    );
    if (options.updatePreference !== false) {
      this.preferredSpeed = bounded;
    }
    this.engine.setRate(bounded);
    this.replaceSnapshot({ speed: bounded });
    this.persistResumeState(true);
    this.publishMediaState();
  }

  async setVoice(
    voiceId: string,
    options: {
      updatePreference?: boolean;
      language?: string | null;
    } = {},
  ): Promise<void> {
    const normalized = voiceId.trim();
    if (!normalized || normalized === this.snapshot.voiceId) {
      if (normalized && options.updatePreference !== false) {
        this.preferredVoiceId = normalized;
        this.preferredVoiceLanguage = baseLanguage(
          options.language ?? this.manifest?.language,
        );
      }
      return;
    }

    const wasPlaying = this.wantsPlayback
      || this.snapshot.status === "playing";
    const cursor = this.snapshot.elapsedMs;
    const intentGeneration = ++this.playbackIntentGeneration;
    this.clearPendingSegmentHandoff();

    if (options.updatePreference !== false) {
      this.preferredVoiceId = normalized;
      this.preferredVoiceLanguage = baseLanguage(
        options.language ?? this.manifest?.language,
      );
    }

    if (!this.manifest) {
      this.replaceSnapshot({
        voiceId: normalized,
        error: null,
      });
      return;
    }

    this.generation += 1;
    this.wantsPlayback = wasPlaying;
    this.engine.pause();
    this.releaseRuntimeAudio();
    this.replaceSnapshot({
      voiceId: normalized,
      status: wasPlaying ? "preparing" : "paused",
      bufferedAheadMs: 0,
      error: null,
    });
    this.persistResumeState(true);

    if (wasPlaying) {
      await this.startAtDocumentTime(
        cursor,
        true,
        intentGeneration,
      );
    }
  }

  destroy(): void {
    this.generation += 1;
    this.playbackIntentGeneration += 1;
    this.wantsPlayback = false;
    this.clearPendingSegmentHandoff();
    this.persistResumeState(true);
    this.releaseRuntimeAudio();
    this.removePageLifecycleHandlers();
    this.engine.destroy();
    this.cache.dispose();
    this.clearMediaSession(true);
    this.listeners.clear();
  }

  private async startAtDocumentTime(
    targetMs: number,
    autoplay: boolean,
    intentGeneration = this.playbackIntentGeneration,
  ): Promise<void> {
    const manifest = this.manifest;
    if (
      !manifest
      || intentGeneration !== this.playbackIntentGeneration
    ) {
      return;
    }

    const generation = this.generation;
    const target = clamp(targetMs, 0, manifest.estimatedSourceDurationMs);

    if (target >= manifest.estimatedSourceDurationMs) {
      this.wantsPlayback = false;
      this.engine.pause();
      this.replaceSnapshot({
        status: "ended",
        elapsedMs: manifest.estimatedSourceDurationMs,
        activeSegmentIndex:
          manifest.segmentCount > 0 ? manifest.segmentCount - 1 : null,
        canonicalScalarCursor: manifest.textScalarLength,
      });
      this.persistResumeState(true);
      this.publishMediaState();
      return;
    }

    this.prepareRuntimeTransition(target);

    try {
      const position = await this.core.segmentForLogicalTime(
        manifest.handle,
        target,
      );
      if (
        !position
        || generation !== this.generation
        || intentGeneration !== this.playbackIntentGeneration
      ) {
        return;
      }

      const runtime = await this.ensureAudio(position.index, generation);
      if (generation !== this.generation) {
        runtime.playable.release();
        return;
      }
      if (intentGeneration !== this.playbackIntentGeneration) {
        return;
      }

      await this.activateRuntimeAudio(
        runtime,
        target,
        position.localOffsetMs,
        autoplay,
        generation,
        intentGeneration,
      );
    } catch (error) {
      if (
        generation !== this.generation
        || intentGeneration !== this.playbackIntentGeneration
      ) {
        return;
      }
      this.fail(
        error instanceof Error
          ? error.message
          : String(error),
      );
    }
  }

  private prepareRuntimeTransition(target: number): void {
    // Freeze the previous physical source while the destination is resolved.
    // Otherwise old timeupdate events could move the public document clock
    // after a seek, voice change, or hidden-segment transition has begun.
    this.engine.pause();
    this.active = null;
    this.replaceSnapshot({
      status: "preparing",
      elapsedMs: target,
      error: null,
    });
    this.publishMediaState();
  }

  private async activateRuntimeAudio(
    runtime: RuntimeAudio,
    target: number,
    localLogicalOffsetMs: number,
    autoplay: boolean,
    generation: number,
    intentGeneration: number,
  ): Promise<void> {
    const logicalDurationMs = Math.max(
      1,
      runtime.descriptor.logicalEndMs
        - runtime.descriptor.logicalStartMs,
    );

    let physicalDurationMs =
      runtime.physicalDurationMs
      ?? runtime.tts.durationMs
      ?? null;

    if (!physicalDurationMs || physicalDurationMs <= 0) {
      physicalDurationMs = await this.engine.load(
        runtime.playable.url,
        0,
        this.snapshot.speed,
      );
      runtime.physicalDurationMs = physicalDurationMs;
    }

    const localLogicalMs = clamp(
      localLogicalOffsetMs,
      0,
      logicalDurationMs,
    );
    const physicalOffsetMs =
      physicalDurationMs && physicalDurationMs > 0
        ? (localLogicalMs / logicalDurationMs) * physicalDurationMs
        : localLogicalMs;

    const loadedDuration = await this.engine.load(
      runtime.playable.url,
      physicalOffsetMs,
      this.snapshot.speed,
    );
    runtime.physicalDurationMs =
      loadedDuration
      ?? physicalDurationMs
      ?? runtime.physicalDurationMs;

    if (
      generation !== this.generation
      || intentGeneration !== this.playbackIntentGeneration
    ) {
      return;
    }

    this.active = runtime;
    this.lastTransitionAt = this.monotonicNow();
    this.replaceSnapshot({
      status: autoplay ? "preparing" : "paused",
      elapsedMs: target,
      activeSegmentIndex: runtime.descriptor.index,
      canonicalScalarCursor: scalarCursorForDescriptor(
        runtime.descriptor,
        target,
      ),
      error: null,
    });
    this.publishMediaState();

    void this.prefetch(runtime.descriptor.index, generation);

    if (autoplay) {
      if (
        !this.wantsPlayback
        || intentGeneration !== this.playbackIntentGeneration
      ) {
        return;
      }

      await this.engine.play();

      if (
        !this.wantsPlayback
        || intentGeneration !== this.playbackIntentGeneration
      ) {
        this.engine.pause();
        return;
      }
    }

    this.emit("segment_started", {
      index: runtime.descriptor.index,
      logicalStartMs: runtime.descriptor.logicalStartMs,
      logicalEndMs: runtime.descriptor.logicalEndMs,
    });
  }

  private async startReadySequentialRuntime(
    runtime: RuntimeAudio,
    target: number,
    intentGeneration: number,
  ): Promise<void> {
    const generation = this.generation;
    this.prepareRuntimeTransition(target);

    try {
      await this.activateRuntimeAudio(
        runtime,
        target,
        0,
        true,
        generation,
        intentGeneration,
      );
    } catch (error) {
      if (
        generation !== this.generation
        || intentGeneration !== this.playbackIntentGeneration
      ) {
        return;
      }
      this.fail(
        error instanceof Error
          ? error.message
          : String(error),
      );
    }
  }

  private async ensureAudio(
    index: number,
    generation: number,
  ): Promise<RuntimeAudio> {
    const existing = this.audioByIndex.get(index);
    if (existing) return existing;

    const inFlight = this.inFlightAudio.get(index);
    if (inFlight) return inFlight;

    const promise = this.createRuntimeAudio(index, generation);
    this.inFlightAudio.set(index, promise);

    try {
      const result = await promise;
      if (generation !== this.generation) {
        result.playable.release();
        throw new Error("Stale playback generation");
      }
      this.audioByIndex.set(index, result);
      return result;
    } finally {
      if (this.inFlightAudio.get(index) === promise) {
        this.inFlightAudio.delete(index);
      }
    }
  }

  private async createRuntimeAudio(
    index: number,
    generation: number,
  ): Promise<RuntimeAudio> {
    const manifest = this.manifest;
    if (!manifest) {
      throw new Error("No ReadingManifest is loaded.");
    }

    const descriptor = await this.core.getSegment(
      manifest.handle,
      index,
    );
    if (generation !== this.generation) {
      throw new Error("Stale playback generation");
    }

    this.emit("tts_start", {
      index,
      wordCount: descriptor.wordCount,
    });

    const tts = await this.tts.synthesize({
      text: descriptor.text,
      language: manifest.language,
      voiceId: this.snapshot.voiceId,
      speed: this.snapshot.speed,
      segmentId: descriptor.id,
    });

    if (generation !== this.generation) {
      throw new Error("Stale playback generation");
    }

    const playable = await this.cache.resolve(tts);

    this.emit("tts_ready", {
      index,
      cacheHit: tts.cacheHit || playable.fromBrowserCache,
    });

    return {
      descriptor,
      tts,
      playable,
      physicalDurationMs: tts.durationMs ?? null,
    };
  }

  private async prefetch(
    activeIndex: number,
    generation: number,
  ): Promise<void> {
    const manifest = this.manifest;
    if (!manifest || generation !== this.generation) return;

    try {
      const indexes = await this.core.prefetchIndexes(
        manifest.handle,
        activeIndex,
        DEFAULT_PREFETCH_HORIZON_MS,
        DEFAULT_PREFETCH_SEGMENTS,
      );

      const results = await Promise.allSettled(
        indexes.map((index) => this.ensureAudio(index, generation)),
      );

      if (generation !== this.generation) return;

      const ready = results
        .filter(
          (result): result is PromiseFulfilledResult<RuntimeAudio> =>
            result.status === "fulfilled",
        )
        .map((result) => result.value);

      this.engine.prime(
        ready.map((runtime) => runtime.playable.url),
      );

      const bufferedAheadMs = ready.reduce(
        (total, runtime) =>
          total
          + Math.max(
            0,
            runtime.descriptor.logicalEndMs
              - runtime.descriptor.logicalStartMs,
          ),
        0,
      );

      this.replaceSnapshot({ bufferedAheadMs });
      this.pruneRuntimeAudio(
        new Set([
          activeIndex,
          ...indexes,
        ]),
      );
    } catch {
      // Prefetch failure is non-fatal. The active segment may still play and
      // the next segment will retry on demand.
    }
  }

  private pruneRuntimeAudio(keep: Set<number>): void {
    for (const [index, runtime] of this.audioByIndex) {
      if (keep.has(index)) continue;
      if (this.active?.descriptor.index === index) continue;
      runtime.playable.release();
      this.audioByIndex.delete(index);
    }
  }

  private handlePhysicalTime(
    currentTimeMs: number,
    physicalDurationMs: number | null,
  ): void {
    const manifest = this.manifest;
    const active = this.active;
    if (!manifest || !active) return;

    if (physicalDurationMs && physicalDurationMs > 0) {
      active.physicalDurationMs = physicalDurationMs;
    }

    const physicalDuration =
      active.physicalDurationMs
      ?? active.tts.durationMs
      ?? 0;

    const logicalDuration = Math.max(
      0,
      active.descriptor.logicalEndMs
        - active.descriptor.logicalStartMs,
    );

    const fraction =
      physicalDuration > 0
        ? clamp(currentTimeMs / physicalDuration, 0, 1)
        : 0;

    const elapsedMs = clamp(
      active.descriptor.logicalStartMs
        + logicalDuration * fraction,
      0,
      manifest.estimatedSourceDurationMs,
    );

    this.replaceSnapshot({
      elapsedMs,
      activeSegmentIndex: active.descriptor.index,
      canonicalScalarCursor: scalarCursorForDescriptor(
        active.descriptor,
        elapsedMs,
      ),
      status:
        this.wantsPlayback && !this.engine.paused
          ? "playing"
          : this.snapshot.status,
    });
    this.persistResumeState(false);
    this.publishMediaState();
  }

  private handleSegmentEnded(): void {
    const manifest = this.manifest;
    const active = this.active;
    if (!manifest || !active) return;

    const nextIndex = active.descriptor.index + 1;
    const endedAt = this.monotonicNow();
    const transitionFrom = this.lastTransitionAt;

    this.emit("segment_ended", {
      index: active.descriptor.index,
      playedMs: Math.round(endedAt - transitionFrom),
    });

    if (nextIndex >= manifest.segmentCount) {
      this.clearPendingSegmentHandoff();
      this.wantsPlayback = false;
      this.replaceSnapshot({
        status: "ended",
        elapsedMs: manifest.estimatedSourceDurationMs,
        bufferedAheadMs: 0,
        activeSegmentIndex: active.descriptor.index,
        canonicalScalarCursor: manifest.textScalarLength,
      });
      this.persistResumeState(true);
      this.publishMediaState();
      return;
    }

    if (this.wantsPlayback) {
      this.pendingSegmentHandoff = {
        fromIndex: active.descriptor.index,
        toIndex: nextIndex,
        logicalBoundaryMs: active.descriptor.logicalEndMs,
        startedAtMs: endedAt,
      };
    } else {
      this.clearPendingSegmentHandoff();
    }

    const prefetchedNext = this.audioByIndex.get(nextIndex) ?? null;
    if (this.wantsPlayback && prefetchedNext) {
      void this.startReadySequentialRuntime(
        prefetchedNext,
        active.descriptor.logicalEndMs,
        this.playbackIntentGeneration,
      );
      return;
    }

    void this.startAtDocumentTime(
      active.descriptor.logicalEndMs,
      this.wantsPlayback,
      this.playbackIntentGeneration,
    );
  }

  private handleWaiting(): void {
    if (!this.wantsPlayback) return;
    this.replaceSnapshot({ status: "buffering" });
    this.publishMediaState();
    this.emit("audio_waiting", {
      elapsedMs: Math.round(this.snapshot.elapsedMs),
    });
  }

  private handlePlaying(): void {
    if (!this.wantsPlayback) return;

    const pending = this.pendingSegmentHandoff;
    const activeIndex = this.active?.descriptor.index ?? null;
    if (pending) {
      this.pendingSegmentHandoff = null;

      if (activeIndex === pending.toIndex) {
        this.emit("segment_handoff", {
          fromIndex: pending.fromIndex,
          toIndex: pending.toIndex,
          logicalBoundaryMs: Math.round(pending.logicalBoundaryMs),
          mediaStartLatencyMs: Math.max(
            0,
            this.monotonicNow() - pending.startedAtMs,
          ),
        });
      }
    }

    this.replaceSnapshot({ status: "playing" });
    this.publishMediaState();
    this.emit("audio_playing", {
      elapsedMs: Math.round(this.snapshot.elapsedMs),
    });
  }

  private fail(message: string): void {
    this.playbackIntentGeneration += 1;
    this.wantsPlayback = false;
    this.clearPendingSegmentHandoff();
    this.engine.pause();
    this.replaceSnapshot({
      status: "error",
      error: message,
    });
    this.publishMediaState();
    this.emit("playback_error", { message });
  }

  private releaseRuntimeAudio(): void {
    this.active = null;
    this.engine.prime([]);

    for (const runtime of this.audioByIndex.values()) {
      runtime.playable.release();
    }

    this.audioByIndex.clear();
    this.inFlightAudio.clear();
  }

  private replaceSnapshot(
    patch: Partial<WebPlaybackSnapshot>,
  ): void {
    this.snapshot = {
      ...this.snapshot,
      ...patch,
    };

    for (const listener of this.listeners) {
      listener();
    }
  }

  private resumeStorageKey(manifest: ReadingManifestSummary): string {
    return [
      "floently-read-resume-v1",
      manifest.documentId,
      manifest.revisionId,
    ].join(":");
  }

  private readResumeState(
    manifest: ReadingManifestSummary,
  ): ResumeState | null {
    try {
      const raw = localStorage.getItem(
        this.resumeStorageKey(manifest),
      );
      if (!raw) return null;

      const value = JSON.parse(raw) as Partial<ResumeState>;
      return {
        elapsedMs: safeNumber(value.elapsedMs, 0),
        speed: safeNumber(value.speed, 1),
        voiceId:
          typeof value.voiceId === "string"
            ? value.voiceId
            : defaultVoiceIdForLanguage(manifest.language),
      };
    } catch {
      return null;
    }
  }

  private persistResumeState(force: boolean): void {
    const manifest = this.manifest;
    if (!manifest) return;

    const now = Date.now();
    if (!force && now - this.lastPersistedAt < 1_000) return;
    this.lastPersistedAt = now;

    try {
      const value: ResumeState = {
        elapsedMs: this.snapshot.elapsedMs,
        speed: this.snapshot.speed,
        voiceId: this.snapshot.voiceId,
      };
      localStorage.setItem(
        this.resumeStorageKey(manifest),
        JSON.stringify(value),
      );
    } catch {
      // Resume storage is best-effort; playback remains functional without it.
    }
  }

  private installPageLifecycleHandlers(): void {
    if (
      typeof window === "undefined"
      || typeof document === "undefined"
    ) {
      return;
    }

    const onVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        this.persistResumeState(true);
        return;
      }

      this.publishMediaMetadata();
      this.publishMediaState();
    };
    const onPageHide = () => {
      this.persistResumeState(true);
    };
    const onPageShow = () => {
      this.publishMediaMetadata();
      this.publishMediaState();
    };

    document.addEventListener("visibilitychange", onVisibilityChange);
    window.addEventListener("pagehide", onPageHide);
    window.addEventListener("pageshow", onPageShow);

    this.lifecycleDisposers.push(
      () => document.removeEventListener(
        "visibilitychange",
        onVisibilityChange,
      ),
      () => window.removeEventListener("pagehide", onPageHide),
      () => window.removeEventListener("pageshow", onPageShow),
    );
  }

  private removePageLifecycleHandlers(): void {
    for (const dispose of this.lifecycleDisposers.splice(0)) {
      dispose();
    }
  }

  private installMediaSessionHandlers(): void {
    if (
      typeof navigator === "undefined"
      || !("mediaSession" in navigator)
    ) {
      return;
    }

    const mediaSession = navigator.mediaSession;

    const setHandler = (
      action: MediaSessionAction,
      handler: MediaSessionActionHandler | null,
    ) => {
      try {
        mediaSession.setActionHandler(action, handler);
      } catch {
        // Some browsers expose Media Session but not every action.
      }
    };

    setHandler("play", () => {
      void this.play();
    });
    setHandler("pause", () => this.pause());
    setHandler("seekbackward", (details) => {
      void this.seekBy((details.seekOffset ?? 15) * -1_000);
    });
    setHandler("seekforward", (details) => {
      void this.seekBy((details.seekOffset ?? 15) * 1_000);
    });
    setHandler("seekto", (details) => {
      if (typeof details.seekTime === "number") {
        void this.seek(details.seekTime * 1_000);
      }
    });
    setHandler("stop", () => this.pause());
  }

  private publishMediaMetadata(): void {
    if (
      typeof navigator === "undefined"
      || !("mediaSession" in navigator)
      || typeof MediaMetadata === "undefined"
    ) {
      return;
    }

    navigator.mediaSession.metadata = new MediaMetadata({
      title: this.snapshot.title ?? "Floently Read",
      artist: this.snapshot.author ?? "Floently Read",
      album: "Floently Read",
    });
  }

  private publishMediaState(): void {
    if (
      typeof navigator === "undefined"
      || !("mediaSession" in navigator)
    ) {
      return;
    }

    const session = navigator.mediaSession;
    const logicallyPlaying =
      this.wantsPlayback
      && (
        this.snapshot.status === "preparing"
        || this.snapshot.status === "buffering"
        || this.snapshot.status === "playing"
      );

    session.playbackState =
      logicallyPlaying
        ? "playing"
        : this.snapshot.status === "idle"
          ? "none"
          : "paused";

    const durationSeconds = this.snapshot.durationMs / 1_000;
    if (
      durationSeconds <= 0
      || !Number.isFinite(durationSeconds)
      || typeof session.setPositionState !== "function"
    ) {
      return;
    }

    const positionSeconds = clamp(
      this.snapshot.elapsedMs / 1_000,
      0,
      durationSeconds,
    );

    try {
      session.setPositionState({
        duration: durationSeconds,
        playbackRate: this.snapshot.speed,
        position: positionSeconds,
      });
    } catch {
      // Position publication is supplemental; transport control still works.
    }
  }

  private clearMediaSession(removeHandlers: boolean): void {
    if (
      typeof navigator === "undefined"
      || !("mediaSession" in navigator)
    ) {
      return;
    }

    const session = navigator.mediaSession;
    session.metadata = null;
    session.playbackState = "none";

    if (!removeHandlers) return;

    for (
      const action of [
        "play",
        "pause",
        "seekbackward",
        "seekforward",
        "seekto",
        "stop",
      ] as MediaSessionAction[]
    ) {
      try {
        session.setActionHandler(action, null);
      } catch {
        // Ignore unsupported actions.
      }
    }
  }

  private clearPendingSegmentHandoff(): void {
    this.pendingSegmentHandoff = null;
  }

  private emit(
    name: WebPlaybackTelemetryEvent["name"],
    data?: WebPlaybackTelemetryEvent["data"],
  ): void {
    const event: WebPlaybackTelemetryEvent = {
      name,
      at: Date.now(),
      data,
    };

    this.recentTelemetry.push(event);
    if (this.recentTelemetry.length > MAX_RECENT_TELEMETRY_EVENTS) {
      this.recentTelemetry.splice(
        0,
        this.recentTelemetry.length - MAX_RECENT_TELEMETRY_EVENTS,
      );
    }

    this.telemetry?.(event);
  }
}
