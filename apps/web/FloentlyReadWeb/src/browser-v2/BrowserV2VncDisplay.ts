/**
 * Real browser display using the upstream MPL-2.0 noVNC RFB client.
 * This adapter never fetches a URL/DOM/cookie, creates another Chromium, or
 * creates another owner session. Desktop may use the authenticated RFB stream
 * for native mouse/keyboard input so Chromium-level dialogs remain usable;
 * mobile remains view-only here and uses the owner touch/IME channel. The
 * caller provides E's one-use display grant for its EXISTING authenticated
 * signal session.
 *
 * RFB "connect" is a protocol handshake, NOT evidence of a rendered page.
 * Mark usable only after a real noVNC canvas contains actual opaque
 * framebuffer pixels; a legitimate initial page may be uniformly white.
 * Never infer readiness from a socket, timeout or CSS box alone.
 */
export type BrowserV2VncGrant = {
  sessionId: string;
  origin: string;
  displayPath: string;
  displayTicket: string;
  displayExpiresInSeconds: number;
};
export type BrowserV2VncStage =
  "DISPLAY_CONNECTING" | "DISPLAY_RFB_CONNECTED" |
  "DISPLAY_FRAME_READY" | "DISPLAY_FRAME_TIMEOUT" |
  "DISPLAY_DISCONNECTED" | "DISPLAY_AUTH_FAILED" | "DISPLAY_UNAVAILABLE";

export type BrowserV2RfbLike = {
  scaleViewport: boolean;
  clipViewport: boolean;
  resizeSession: boolean;
  viewOnly: boolean;
  focusOnClick: boolean;
  addEventListener(type: string, callback: (event: Event) => void): void;
  removeEventListener(type: string, callback: (event: Event) => void): void;
  disconnect(): void;
  focus(): void;
};

type RfbConstructor = new (element: HTMLElement, url: string,
  options: { shared: false; wsProtocols: string[] }) => BrowserV2RfbLike;

const ticketPattern = /^[A-Za-z0-9_-]{43}$/;
const idPattern = /^[A-Za-z0-9_.:-]{1,128}$/;
let defaultRfbPromise: Promise<RfbConstructor> | null = null;
export function preloadBrowserV2VncRfb(): Promise<RfbConstructor> {
  if (!defaultRfbPromise)
    defaultRfbPromise = import("@novnc/novnc")
      .then(module => module.default as RfbConstructor);
  return defaultRfbPromise;
}

export function browserV2VncDisplayUrl(grant: BrowserV2VncGrant): string {
  if (!idPattern.test(grant.sessionId) ||
      !ticketPattern.test(grant.displayTicket) ||
      !Number.isInteger(grant.displayExpiresInSeconds) ||
      grant.displayExpiresInSeconds < 1 || grant.displayExpiresInSeconds > 90)
    throw Error("DISPLAY_GRANT_INVALID");
  const root = new URL(grant.origin);
  const local = ["localhost", "127.0.0.1", "[::1]"].includes(root.hostname);
  if (root.username || root.password || root.search || root.hash ||
      root.pathname !== "/" ||
      (root.protocol !== "https:" && !(root.protocol === "http:" && local)))
    throw Error("DISPLAY_ORIGIN_INVALID");
  if (grant.displayPath !==
      "/api/browser-v2/cloud/sessions/" +
        encodeURIComponent(grant.sessionId) + "/display")
    throw Error("DISPLAY_PATH_INVALID");
  root.protocol = root.protocol === "https:" ? "wss:" : "ws:";
  root.pathname = grant.displayPath;
  // A displayTicket is only a WebSocket SUBPROTOCOL, never in URL/query.
  return root.href;
}

export function browserV2ObservedRfbFrame(target: HTMLElement): boolean {
  const canvas = target.querySelector("canvas");
  if (!canvas || canvas.width < 2 || canvas.height < 2) return false;
  const context = canvas.getContext("2d", { willReadFrequently: true });
  if (!context) return false;
  try {
    // Never persist/log/emit pixels. Sampling the rendered noVNC display
    // distinguishes an RFB handshake and empty HTML canvas from an actual
    // article/new-tab image, without needing old RTP/DTLS/WebRTC statistics.
    let opaque = 0;
    for (let y = 1; y <= 9; y++) {
      for (let x = 1; x <= 9; x++) {
        const pixel = context.getImageData(
          Math.floor(canvas.width * x / 10),
          Math.floor(canvas.height * y / 10), 1, 1).data;
        if (pixel[3] < 250) continue;
        opaque++;
      }
    }
    // Canvas pixels are transparent before a real framebuffer paint. Once a
    // sufficiently broad sample is opaque, a real RFB frame has arrived even
    // if the legitimate page is uniformly white (about:blank/new tab).
    return opaque >= 12;
  } catch {
    // A blocked/tainted readback is UNKNOWN, not invented frame proof.
    return false;
  }
}

export class BrowserV2VncDisplay {
  private current: BrowserV2RfbLike | null = null;
  private stopped = false;
  private generation = 0;
  private readinessTimer: ReturnType<typeof setTimeout> | null = null;
  private pollTimer: ReturnType<typeof setTimeout> | null = null;
  private detach: (() => void) | null = null;
  private ready = false;
  constructor(private readonly target: HTMLElement,
    private readonly onStage: (stage: BrowserV2VncStage) => void,
    private readonly onFrame: () => void,
    private readonly loadRfb: () => Promise<RfbConstructor> =
      preloadBrowserV2VncRfb,
    private readonly nativeInput = false,
  ) {}
  get frameReady(): boolean { return this.ready && !this.stopped; }
  async connect(grant: BrowserV2VncGrant): Promise<void> {
    if (this.stopped) throw Error("DISPLAY_CLOSED");
    const url = browserV2VncDisplayUrl(grant);
    this.reset();
    const at = ++this.generation;
    this.onStage("DISPLAY_CONNECTING");
    let Rfb: RfbConstructor;
    try { Rfb = await this.loadRfb(); }
    catch {
      if (at === this.generation) this.onStage("DISPLAY_UNAVAILABLE");
      return;
    }
    if (this.stopped || at !== this.generation) return;
    let rfb: BrowserV2RfbLike;
    try {
      rfb = new Rfb(this.target, url, {
        shared: false,
        wsProtocols: ["floently.v2.display", "auth." + grant.displayTicket],
      });
    } catch {
      this.onStage("DISPLAY_UNAVAILABLE");
      return;
    }
    this.current = rfb;
    rfb.scaleViewport = true;
    rfb.clipViewport = false;
    // E's physical Chrome/Xvfb viewport is fixed per negotiated session.
    // Resizing after the fact would break CDP/private Reader point identity.
    rfb.resizeSession = false;
    // Desktop uses the RFB connection as the real remote desktop input
    // surface so Chromium-native dialogs (WebAuthn/passkeys, permission UI,
    // browser popups) receive mouse and keyboard events. Coarse-pointer mobile
    // stays view-only and uses Floently's explicit touch/IME owner channel.
    rfb.viewOnly = !this.nativeInput;
    rfb.focusOnClick = this.nativeInput;
    const active = () => !this.stopped && at === this.generation &&
      this.current === rfb;
    const poll = () => {
      if (!active() || this.ready) return;
      if (browserV2ObservedRfbFrame(this.target)) {
        this.ready = true;
        if (this.readinessTimer) clearTimeout(this.readinessTimer);
        this.readinessTimer = null;
        this.onStage("DISPLAY_FRAME_READY");
        this.onFrame();
        return;
      }
      this.pollTimer = setTimeout(poll, 80);
    };
    const onConnect = () => {
      if (!active()) return;
      this.onStage("DISPLAY_RFB_CONNECTED");
      poll();
    };
    const onDisconnect = () => {
      if (!active()) return;
      this.ready = false;
      this.onStage("DISPLAY_DISCONNECTED");
      if (this.pollTimer) clearTimeout(this.pollTimer);
      this.pollTimer = null;
    };
    const onCredentials = () => {
      // Protected Unix-only x11vnc MUST NOT challenge a browser for a raw
      // VNC password. Treat it as a service misconfiguration, not a prompt.
      if (active()) this.onStage("DISPLAY_AUTH_FAILED");
      this.reset();
    };
    rfb.addEventListener("connect", onConnect);
    rfb.addEventListener("disconnect", onDisconnect);
    rfb.addEventListener("credentialsrequired", onCredentials);
    rfb.addEventListener("securityfailure", onCredentials);
    this.detach = () => {
      rfb.removeEventListener("connect", onConnect);
      rfb.removeEventListener("disconnect", onDisconnect);
      rfb.removeEventListener("credentialsrequired", onCredentials);
      rfb.removeEventListener("securityfailure", onCredentials);
    };
    this.readinessTimer = setTimeout(() => {
      if (!active() || this.ready) return;
      this.onStage("DISPLAY_FRAME_TIMEOUT");
    }, 10_000);
  }
  focus(): void { if (this.frameReady) this.current?.focus(); }
  disconnect(): void {
    this.reset();
    this.onStage("DISPLAY_DISCONNECTED");
  }
  close(): void { this.stopped = true; this.reset(); }
  private reset(): void {
    this.generation++;
    this.ready = false;
    if (this.readinessTimer) clearTimeout(this.readinessTimer);
    if (this.pollTimer) clearTimeout(this.pollTimer);
    this.readinessTimer = null; this.pollTimer = null;
    this.detach?.(); this.detach = null;
    const prior = this.current; this.current = null;
    try { prior?.disconnect(); } catch {}
  }
}
