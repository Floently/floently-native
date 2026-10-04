export interface BrowserAudioEngineCallbacks {
  onTime: (currentTimeMs: number, physicalDurationMs: number | null) => void;
  onEnded: () => void;
  onWaiting: () => void;
  onPlaying: () => void;
  onError: (message: string) => void;
}

export interface ReadAudioEngine {
  readonly paused: boolean;
  load(
    url: string,
    localOffsetMs: number,
    playbackRate: number,
  ): Promise<number | null>;
  play(): Promise<void>;
  pause(): void;
  setRate(rate: number): void;
  prime(urls: string[]): void;
  destroy(): void;
}

export type ReadAudioEngineFactory = (
  callbacks: BrowserAudioEngineCallbacks,
) => ReadAudioEngine;

export class BrowserAudioEngine implements ReadAudioEngine {
  private audio: HTMLAudioElement;
  private readonly preloaded = new Map<string, HTMLAudioElement>();
  private readonly callbacks: BrowserAudioEngineCallbacks;
  private currentUrl: string | null = null;
  private cancelMetadataWait: (() => void) | null = null;

  constructor(callbacks: BrowserAudioEngineCallbacks) {
    this.callbacks = callbacks;
    this.audio = this.createAudioElement();
    this.attachActiveListeners(this.audio);
  }

  get paused(): boolean {
    return this.audio.paused;
  }

  async load(
    url: string,
    localOffsetMs: number,
    playbackRate: number,
  ): Promise<number | null> {
    this.cancelPendingMetadataWait();

    if (this.currentUrl !== url) {
      const previous = this.audio;
      const prepared = this.preloaded.get(url) ?? null;

      previous.pause();

      if (prepared) {
        this.preloaded.delete(url);
        this.detachActiveListeners(previous);
        this.retireElement(previous);
        this.audio = prepared;
        this.attachActiveListeners(this.audio);
      } else {
        this.audio.src = url;
        this.audio.load();
      }

      this.currentUrl = url;
    }

    this.audio.playbackRate = playbackRate;
    const audio = this.audio;
    const durationMs = await this.waitForMetadata(audio);

    const maxTime = Number.isFinite(audio.duration)
      ? Math.max(0, audio.duration - 0.01)
      : Number.POSITIVE_INFINITY;
    const requestedSeconds = Math.max(0, localOffsetMs / 1_000);
    audio.currentTime = Math.min(requestedSeconds, maxTime);

    return durationMs;
  }

  async play(): Promise<void> {
    await this.audio.play();
  }

  pause(): void {
    this.audio.pause();
  }

  setRate(rate: number): void {
    this.audio.playbackRate = rate;
  }

  prime(urls: string[]): void {
    const desired = new Set(urls.slice(0, 4));

    for (const [url, element] of this.preloaded) {
      if (desired.has(url)) continue;
      this.retireElement(element);
      this.preloaded.delete(url);
    }

    for (const url of desired) {
      if (url === this.currentUrl || this.preloaded.has(url)) continue;
      const element = this.createAudioElement();
      element.src = url;
      element.load();
      this.preloaded.set(url, element);
    }
  }

  destroy(): void {
    this.cancelPendingMetadataWait();
    this.detachActiveListeners(this.audio);
    this.retireElement(this.audio);
    this.currentUrl = null;

    for (const element of this.preloaded.values()) {
      this.retireElement(element);
    }
    this.preloaded.clear();
  }

  private createAudioElement(): HTMLAudioElement {
    const element = new Audio();
    element.preload = "auto";
    return element;
  }

  private attachActiveListeners(element: HTMLAudioElement): void {
    element.addEventListener("timeupdate", this.handleTime);
    element.addEventListener("durationchange", this.handleTime);
    element.addEventListener("ended", this.handleEnded);
    element.addEventListener("waiting", this.handleWaiting);
    element.addEventListener("stalled", this.handleWaiting);
    element.addEventListener("playing", this.handlePlaying);
    element.addEventListener("error", this.handleError);
  }

  private detachActiveListeners(element: HTMLAudioElement): void {
    element.removeEventListener("timeupdate", this.handleTime);
    element.removeEventListener("durationchange", this.handleTime);
    element.removeEventListener("ended", this.handleEnded);
    element.removeEventListener("waiting", this.handleWaiting);
    element.removeEventListener("stalled", this.handleWaiting);
    element.removeEventListener("playing", this.handlePlaying);
    element.removeEventListener("error", this.handleError);
  }

  private retireElement(element: HTMLAudioElement): void {
    element.pause();
    element.src = "";
    element.load();
  }

  private cancelPendingMetadataWait(): void {
    const cancel = this.cancelMetadataWait;
    this.cancelMetadataWait = null;
    cancel?.();
  }

  private waitForMetadata(
    element: HTMLAudioElement,
  ): Promise<number | null> {
    if (element.readyState >= HTMLMediaElement.HAVE_METADATA) {
      return Promise.resolve(this.physicalDurationMs(element));
    }

    return new Promise((resolve, reject) => {
      let settled = false;

      const cleanup = () => {
        element.removeEventListener("loadedmetadata", onLoaded);
        element.removeEventListener("error", onError);
        if (this.cancelMetadataWait === cancel) {
          this.cancelMetadataWait = null;
        }
      };
      const finish = (
        action: () => void,
      ) => {
        if (settled) return;
        settled = true;
        cleanup();
        action();
      };
      const onLoaded = () => {
        finish(() => resolve(this.physicalDurationMs(element)));
      };
      const onError = () => {
        finish(() => reject(
          new Error("Browser media failed while loading audio metadata."),
        ));
      };
      const cancel = () => {
        finish(() => reject(
          new Error("Browser media load superseded."),
        ));
      };

      this.cancelMetadataWait = cancel;
      element.addEventListener("loadedmetadata", onLoaded, { once: true });
      element.addEventListener("error", onError, { once: true });
    });
  }

  private physicalDurationMs(
    element: HTMLAudioElement = this.audio,
  ): number | null {
    return Number.isFinite(element.duration) && element.duration >= 0
      ? element.duration * 1_000
      : null;
  }

  private handleTime = () => {
    this.callbacks.onTime(
      Math.max(0, this.audio.currentTime * 1_000),
      this.physicalDurationMs(),
    );
  };

  private handleEnded = () => {
    this.callbacks.onEnded();
  };

  private handleWaiting = () => {
    this.callbacks.onWaiting();
  };

  private handlePlaying = () => {
    this.callbacks.onPlaying();
  };

  private handleError = () => {
    const mediaError = this.audio.error;
    this.callbacks.onError(
      mediaError
        ? `Browser media error ${mediaError.code}`
        : "Browser media playback failed.",
    );
  };
}
