export type BrowserV2VncAttachToken = {
  attempt: number;
  routeEpoch: number;
};

export class BrowserV2VncAttachGate {
  private active: boolean;
  private routeEpoch = 0;
  private nextAttempt = 0;
  private retryCount = 0;
  private inFlightAttempt: number | null = null;

  constructor(active: boolean) {
    this.active = active;
  }

  setActive(active: boolean): void {
    if (this.active === active) return;

    this.active = active;
    this.routeEpoch += 1;

    if (!active) {
      this.retryCount = 0;
    }
  }

  begin(maxAttempts = 3): BrowserV2VncAttachToken | null {
    if (
      !this.active
      || this.inFlightAttempt !== null
      || this.retryCount >= maxAttempts
    ) {
      return null;
    }

    const attempt = ++this.nextAttempt;
    this.retryCount += 1;
    this.inFlightAttempt = attempt;
    return {
      attempt,
      routeEpoch: this.routeEpoch,
    };
  }

  isCurrent(token: BrowserV2VncAttachToken): boolean {
    return (
      this.active
      && this.inFlightAttempt === token.attempt
      && this.routeEpoch === token.routeEpoch
    );
  }

  finish(token: BrowserV2VncAttachToken): void {
    if (this.inFlightAttempt === token.attempt) {
      this.inFlightAttempt = null;
    }
  }

  resetRetryBudget(): void {
    this.retryCount = 0;
  }

  get isActive(): boolean {
    return this.active;
  }

  get hasInFlight(): boolean {
    return this.inFlightAttempt !== null;
  }

  get attemptsSinceReset(): number {
    return this.retryCount;
  }
}
