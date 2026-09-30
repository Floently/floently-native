export interface BrowserAudioEngineCallbacks {
  onTime: (currentTimeMs: number, physicalDurationMs: number | null) => void;
  onEnded: () => void;
  onWaiting: () => void;
  onPlaying: () => void;
  onError: (message: string) => void;
}

export class BrowserAudioEngine {
  private readonly audio = new Audio();
  private readonly preloaded = new Map<string, HTMLAudioElement>();
  private readonly callbacks: BrowserAudioEngineCallbacks;
  private currentUrl: string | null = null;

  constructor(callbacks: BrowserAudioEngineCallbacks) {
    this.callbacks = callbacks;
    this.audio.preload = "auto";

    this.audio.addEventListener("timeupdate", this.handleTime);
    this.audio.addEventListener("durationchange", this.handleTime);
    this.audio.addEventListener("ended", this.handleEnded);
    this.audio.addEventListener("waiting", this.handleWaiting);
    this.audio.addEventListener("stalled", this.handleWaiting);
    this.audio.addEventListener("playing", this.handlePlaying);
    this.audio.addEventListener("error", this.handleError);
  }

  get paused(): boolean {
    return this.audio.paused;
  }

  async load(
    url: string,
    localOffsetMs: number,
    playbackRate: number,
  ): Promise<number | null> {
    if (this.currentUrl !== url) {
      this.audio.pause();
      this.audio.src = url;
      this.audio.load();
      this.currentUrl = url;
    }

    this.audio.playbackRate = playbackRate;
    const durationMs = await this.waitForMetadata();

    const maxTime = Number.isFinite(this.audio.duration)
      ? Math.max(0, this.audio.duration - 0.01)
      : Number.POSITIVE_INFINITY;
    const requestedSeconds = Math.max(0, localOffsetMs / 1_000);
    this.audio.currentTime = Math.min(requestedSeconds, maxTime);

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
      element.src = "";
      element.load();
      this.preloaded.delete(url);
    }

    for (const url of desired) {
      if (url === this.currentUrl || this.preloaded.has(url)) continue;
      const element = new Audio();
      element.preload = "auto";
      element.src = url;
      element.load();
      this.preloaded.set(url, element);
    }
  }

  destroy(): void {
    this.audio.pause();
    this.audio.removeEventListener("timeupdate", this.handleTime);
    this.audio.removeEventListener("durationchange", this.handleTime);
    this.audio.removeEventListener("ended", this.handleEnded);
    this.audio.removeEventListener("waiting", this.handleWaiting);
    this.audio.removeEventListener("stalled", this.handleWaiting);
    this.audio.removeEventListener("playing", this.handlePlaying);
    this.audio.removeEventListener("error", this.handleError);
    this.audio.src = "";
    this.audio.load();

    for (const element of this.preloaded.values()) {
      element.src = "";
      element.load();
    }
    this.preloaded.clear();
  }

  private waitForMetadata(): Promise<number | null> {
    if (this.audio.readyState >= HTMLMediaElement.HAVE_METADATA) {
      return Promise.resolve(this.physicalDurationMs());
    }

    return new Promise((resolve, reject) => {
      const cleanup = () => {
        this.audio.removeEventListener("loadedmetadata", onLoaded);
        this.audio.removeEventListener("error", onError);
      };
      const onLoaded = () => {
        cleanup();
        resolve(this.physicalDurationMs());
      };
      const onError = () => {
        cleanup();
        reject(new Error("Browser media failed while loading audio metadata."));
      };

      this.audio.addEventListener("loadedmetadata", onLoaded, { once: true });
      this.audio.addEventListener("error", onError, { once: true });
    });
  }

  private physicalDurationMs(): number | null {
    return Number.isFinite(this.audio.duration) && this.audio.duration >= 0
      ? this.audio.duration * 1_000
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
