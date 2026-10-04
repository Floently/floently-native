import type {
  BrowserBackend, BrowserCapabilities, BrowserEvent, BrowserSnapshot, BrowserStartInput,
  BrowserViewport, CreateTabInput, FindResult, NavigationResult, ReadingAdapter,
  ReadingAnchor, ReadingDocument, ReadingSelection, SelectionResult, TabSnapshot, ViewportPoint,
} from "./transport/browserContracts";
import { createCloudBrowserSession } from "./transport/cloudSessionClient.mjs";
import { CloudSignalSocket } from "./transport/cloudSignalingSocket.mjs";
import { CloudWebRtcTransport } from "./transport/cloudWebRtcTransport.mjs";
import type { BrowserV2VncGrant } from "./BrowserV2VncDisplay";
import { CloudReaderDocumentChunks } from "./transport/cloudReaderDocumentChunks.mjs";
import { cloudMediaConnectivity } from "./transport/cloudMediaConnectivity.mjs";
import type { BrowserV2CloudViewport, BrowserV2SignalEnvelope } from "./transport/cloudWebRtcTransport.mjs";
import { getAuthAccessToken } from "../auth/authStore";
const getSystemTimeMs = (): number => Date.now();

// The V2 media endpoint must be a HTTPS/WSS origin, never a URL/query credential.
// On the Read Render static host, WebSocket proxying is not supported. Configure
// VITE_BROWSER_V2_ORIGIN to the separately secured OCI ingress hostname.
const V2_ORIGIN = (import.meta.env.VITE_BROWSER_V2_ORIGIN || "").trim() ||
  window.location.origin;

/** The B-owned BrowserBackend sends only owner-authorized E commands. All RTC,
 * ICE, decoded frame proof, mobile gesture and pixel mapping use X's accepted
 * browser/integration transport, not another B-owned peer/data channel. */
export const BROWSER_V2_CLOUD_SESSION_PATH = "/api/browser-v2/session";
export type BrowserV2SafeSignalProgress = {
  phase: "OFFER_RECEIVED" | "OFFER_PARSED" | "REMOTE_SDP_SET" |
    "ANSWER_CALLBACK" | "ANSWER_SENT" | "ANSWER_NODE_RECEIVED" |
    "ANSWER_WS_FORWARDED" | "ANSWER_WS_QUEUED" | "ANSWER_WS_FLUSHED" | null;
  answerReceived: boolean;
  iceReceived: boolean;
};
export type BrowserV2SafeMediaFailure = {
  code: "MEDIA_NEGOTIATION_FAILED" | "MEDIA_ANSWER_TIMEOUT" |
    "MEDIA_ANSWER_WS_OVERSIZE" | "MEDIA_IPC_OVERSIZE" |
    "ENGINE_START_FAILED" | "WORKER_UNAVAILABLE" | "WORKER_COMMAND_FAILED";
  detailCode: string | null;
  stage: string | null;
};
export type CloudUiEvent = {
  type: "connection" | "viewport" | "frame" | "signal" | "error";
  ready?: boolean;
  signal?: BrowserV2SafeSignalProgress;
  failure?: BrowserV2SafeMediaFailure;
};
// WebRTC sender and gateway phase events can arrive out of order:
 // Python may report ANSWER_SENT after Node has already forwarded its answer.
 // The owner's error display must preserve the furthest *observed* hop.
const SIGNAL_PHASE_ORDER: Record<NonNullable<BrowserV2SafeSignalProgress["phase"]>, number> = {
  OFFER_RECEIVED: 1, OFFER_PARSED: 2, REMOTE_SDP_SET: 3,
  ANSWER_CALLBACK: 4, ANSWER_SENT: 5, ANSWER_NODE_RECEIVED: 6,
  ANSWER_WS_FORWARDED: 7, ANSWER_WS_QUEUED: 8, ANSWER_WS_FLUSHED: 9,
};
// E maps private GStreamer failures to classified server.error fields. Only
// explicit known classifications may be displayed; never reflect arbitrary
// remote strings, SDP, ICE, page content, URLs or session credentials.
const SAFE_MEDIA_CODES = new Set([
  "MEDIA_NEGOTIATION_FAILED", "MEDIA_ANSWER_TIMEOUT",
  "MEDIA_ANSWER_WS_OVERSIZE", "MEDIA_IPC_OVERSIZE",
  "ENGINE_START_FAILED", "WORKER_UNAVAILABLE", "WORKER_COMMAND_FAILED",
]);
const SAFE_MEDIA_DETAIL_CODES = new Set([
  "MEDIA_ANSWER_IPC_FAILED", "MEDIA_ANSWER_SDP_TEXT_FAILED",
  "MEDIA_ANSWER_LOCAL_DESCRIPTION_FAILED", "MEDIA_ANSWER_OVERSIZE",
  "MEDIA_ANSWER_TIMEOUT", "ANSWER_MISSING", "ANSWER_FAILED",
  "REMOTE_SDP_FAILED", "OFFER_SDP_INVALID", "OFFER_INVALID",
  "GSTREAMER_ERROR", "MEDIA_JSONL_OVERSIZE", "MEDIA_WS_ENVELOPE_OVERSIZE",
  "MEDIA_COMMAND_FAILED", "MEDIA_MESSAGE_TOO_LARGE",
]);
const SAFE_MEDIA_STAGES = new Set([
  "ANSWER_CALLBACK", "ANSWER_SDP_TEXT", "ANSWER_LOCAL_DESCRIPTION",
  "ANSWER_IPC", "ANSWER_WS", "ANSWER_SENT", "UNKNOWN",
]);
const DISPLAY_RECOVERY_CODES = new Set([
  "DISPLAY_UNAVAILABLE", "DISPLAY_GRANT_INVALID",
]);
const fail = (code: string) => new Error(code);
const isRecord = (v: unknown): v is Record<string, unknown> =>
  Boolean(v && typeof v === "object" && !Array.isArray(v));
const capabilities: BrowserCapabilities = {
  tabs: false, downloads: false, uploads: false, clipboardRead: false, clipboardWrite: false,
  permissions: false, popups: false, persistentProfile: false, privateProfile: false,
  pageZoom: false, find: false, reader: true, readFromPoint: true, wordHighlight: false,
  cloudAutomation: false, mediaAudio: false,
};
function placeholderTab(url = "", documentRevision = "cloud-start"): TabSnapshot {
  return { id: "cloud-tab-1", lifecycle: url ? "LOADING" : "INTERACTIVE", url,
    displayUrl: url, title: url ? "Loading webpage" : "New tab", faviconUrl: null,
    loading: Boolean(url), progress: null, canGoBack: false, canGoForward: false,
    securityState: url.startsWith("https:") ? "secure" : "unknown",
    crashed: false, audible: false, muted: false, documentRevision };
}
function safeTarget(value: string): string {
  const url = new URL(value);
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password ||
      value.length > 2048) throw fail("NAVIGATION_FAILED");
  return url.href;
}
function safeViewport(v: BrowserViewport) {
  const cssWidth = Math.max(320, Math.min(2560, Math.round(v.width)));
  const cssHeight = Math.max(360, Math.min(1600, Math.round(v.height)));
  const devicePixelRatio = Math.max(1, Math.min(2, v.deviceScaleFactor || 1,
    2560 / cssWidth, 1600 / cssHeight));
  return { cssWidth, cssHeight, devicePixelRatio };
}
type Reply = { type: string; payload: Record<string, unknown> };
type VncInputAck = {
  clickDispatched: boolean;
  interactiveHit: boolean | null;
  textInserted: boolean | null;
  textInputFocused: boolean | null;
  touchDispatched: boolean | null;
};
type ReaderCommandAck = { ok: boolean; code: string | null };
type Waiter = { expected: string; resolve(v: unknown): void; reject(e: Error): void; timer: number };
type BrowserV2OptionalDisplayAllocation = {
  displayTransport?: "vnc" | "webrtc";
  resumed?: boolean;
  resumeSequence?: number;
  displayOrigin?: string;
  displayPath?: string;
  displayTicket?: string;
  displayExpiresInSeconds?: number;
};
const DISPLAY_PATH_ID = /^[A-Za-z0-9_.:-]{1,128}$/;
const DISPLAY_TICKET = /^[A-Za-z0-9_-]{43}$/;
const RELOAD_HINT_KEY = "iloadi.browser-v2.same-tab-session.v1";
const RELOAD_HINT_TTL_MS = 9 * 60 * 1000;
type BrowserV2ReloadHint = {
  version: 1;
  profileId: string;
  sessionId: string;
  expiresAtMs: number;
};
function readReloadHint(profileId: string): string | undefined {
  try {
    const raw=sessionStorage.getItem(RELOAD_HINT_KEY);
    if(!raw)return undefined;
    const value=JSON.parse(raw) as Partial<BrowserV2ReloadHint>;
    if(value.version!==1||value.profileId!==profileId||
       typeof value.sessionId!=="string"||
       !DISPLAY_PATH_ID.test(value.sessionId)||
       !Number.isFinite(value.expiresAtMs)||
       Number(value.expiresAtMs)<=Date.now()){
      sessionStorage.removeItem(RELOAD_HINT_KEY);
      return undefined;
    }
    return value.sessionId;
  } catch { return undefined; }
}
function writeReloadHint(profileId: string, sessionId: string): void {
  try {
    if(!profileId||!DISPLAY_PATH_ID.test(sessionId))return;
    const value:BrowserV2ReloadHint={
      version:1,profileId,sessionId,
      expiresAtMs:Date.now()+RELOAD_HINT_TTL_MS,
    };
    sessionStorage.setItem(RELOAD_HINT_KEY,JSON.stringify(value));
  } catch {}
}
function clearReloadHint(sessionId?: string): void {
  try {
    if(sessionId){
      const raw=sessionStorage.getItem(RELOAD_HINT_KEY);
      const value=raw?JSON.parse(raw) as Partial<BrowserV2ReloadHint>:null;
      if(value?.sessionId!==sessionId)return;
    }
    sessionStorage.removeItem(RELOAD_HINT_KEY);
  } catch {}
}

export class BrowserV2CloudClient implements BrowserBackend {
  private signal: CloudSignalSocket | null = null;
  private transport: CloudWebRtcTransport | null = null;
  private allocation: Awaited<ReturnType<typeof createCloudBrowserSession>> | null = null;
  private snapshot: BrowserSnapshot | null = null;
  private initialTarget: string | undefined;
  private events = new Set<(e: BrowserEvent) => void>();
  private media = new Set<(e: CloudUiEvent) => void>();
  private offSignal: (() => void) | null = null;
  private sequence = 0;
  private eventSequence = 0;
  private lastRemoteSequence = 0;
  private signalReadyGeneration = 0;
  private navigationRevision = 0;
  private readerChunks = new CloudReaderDocumentChunks();
  // Only protocol TYPES and public stage NAMES are retained. Never store,
  // display, log or report answer SDP, ICE candidates, auth tickets or URLs.
  private signalProgress: BrowserV2SafeSignalProgress = {
    phase: null, answerReceived: false, iceReceived: false,
  };
  private safeMediaFailure: BrowserV2SafeMediaFailure | null = null;
  private vncFrameReady = false;
  private vncDisplayGrant: BrowserV2VncGrant | null = null;
  private stopped = false;
  private connected = false;
  private workerReady = false;
  private waiter: Waiter | null = null;
  private displayGrantWaiter: Waiter | null = null;
  private navigationWait: { url: string;
    expected: "navigation.committed" | "navigation.completed";
    resolve(v: NavigationResult): void;
    reject(e: Error): void; timer: number } | null = null;
  private heartbeatTimer: number | null = null;
  private inputAckSeen = new Map<number,VncInputAck>();
  private inputAckWaiters = new Map<number, {
    resolve(value: VncInputAck): void; reject(e: Error): void; timer: number;
  }>();
  private readerAckSeen = new Map<number,ReaderCommandAck>();
  private readerAckWaiters = new Map<number, {
    resolve(value: ReaderCommandAck): void; reject(e: Error): void; timer: number;
  }>();
  private profileId: string | null = null;
  private preserveForReload = false;

  capabilities() { return capabilities; }
  getBrowserSnapshot() { return this.snapshot; }
  getTransport() { return this.transport; }
  getDisplayTransport(): "webrtc" | "vnc" {
    const optional = this.allocation as
      (typeof this.allocation & BrowserV2OptionalDisplayAllocation);
    return optional?.displayTransport === "vnc" ? "vnc" : "webrtc";
  }
  isDisplayWorkerReady(): boolean { return this.workerReady && !this.stopped; }
  takeVncDisplayGrant(): BrowserV2VncGrant | null {
    if (!this.vncDisplayGrant) return null;
    const grant = { ...this.vncDisplayGrant };
    // Display tickets are one-use transport credentials. Once handed to the
    // noVNC adapter they must never be retried from cached client state.
    this.vncDisplayGrant = null;
    return grant;
  }
  getVncViewport(): {
    cssWidth: number; cssHeight: number;
    renderWidth: number; renderHeight: number; revision: number;
  } | null {
    if (this.getDisplayTransport() !== "vnc" ||
        !this.allocation?.initialViewport) return null;
    const v = this.allocation.initialViewport;
    return {
      cssWidth: v.cssWidth, cssHeight: v.cssHeight,
      renderWidth: v.renderWidth, renderHeight: v.renderHeight,
      revision: v.revision,
    };
  }
  /**
   * RFB handshake is never a frame. BrowserV2VncDisplay invokes this only
   * after sampling a real nonblank noVNC-rendered canvas.
   */
  setVncFrameReady(ready: boolean): void {
    if (this.stopped || this.getDisplayTransport() !== "vnc") return;
    const next = ready && this.workerReady;
    if (this.vncFrameReady === next) return;
    this.vncFrameReady = next;
    this.connected = next;
    if (this.snapshot) this.change({
      ...this.snapshot,
      lifecycle: next ? "READY" : this.workerReady ? "RECOVERING" : "STARTING",
      connectivity: next ? "connected" : "reconnecting",
    });
    if (next && this.initialTarget) {
      const target = this.initialTarget;
      this.initialTarget = undefined;
      void this.open(target).catch(() => {});
    }
    this.publish("frame");
  }
  getViewportRevision(): number | null {
    return this.getDisplayTransport() === "vnc"
      ? this.allocation?.initialViewport?.revision ?? null
      : this.transport?.appliedViewport?.revision ?? null;
  }
  getSignalProgress(): BrowserV2SafeSignalProgress { return { ...this.signalProgress }; }
  getSignalReadyGeneration(): number { return this.signalReadyGeneration; }
  getSafeMediaFailure(): BrowserV2SafeMediaFailure | null {
    return this.safeMediaFailure ? { ...this.safeMediaFailure } : null;
  }
  isMediaReady(): boolean {
    return this.getDisplayTransport() === "vnc"
      ? this.connected && this.workerReady && this.vncFrameReady && !this.stopped
      : Boolean(this.connected && this.transport?.decodedFrameReady);
  }
  isNarrationServiceReady(): boolean { return false; } // E audio is not implemented.
  subscribeMedia(fn: (e: CloudUiEvent) => void): () => void {
    this.media.add(fn); return () => this.media.delete(fn);
  }
  private publish(type: CloudUiEvent["type"]) {
    const e: CloudUiEvent = { type, ready: this.isMediaReady(),
      ...(type === "signal" ? { signal: this.getSignalProgress() } : {}),
      ...(type === "error" ? { failure: this.getSafeMediaFailure() ?? undefined } : {}),
    };
    for (const listener of this.media) listener(e);
  }
  private change(next: BrowserSnapshot) {
    const prior = this.snapshot;
    this.snapshot = next;
    const emit = (type: BrowserEvent["type"], payload: unknown, tabId?: string) => {
      const sequence = ++this.eventSequence;
      const e: BrowserEvent = { eventId: "cloud-" + sequence, backendId: next.backendId,
        sequence, timestamp: new Date(getSystemTimeMs()).toISOString(), type, tabId, payload };
      for (const listener of this.events) listener(e);
    };
    if (!prior || prior.lifecycle !== next.lifecycle)
      emit("lifecycle.changed", { lifecycle: next.lifecycle });
    if (!prior || prior.connectivity !== next.connectivity)
      emit("connectivity.changed", { connectivity: next.connectivity });
    const current = next.tabs[0], previous = prior?.tabs[0];
    if (current && (!previous || current.url !== previous.url || current.loading !== previous.loading ||
        current.title !== previous.title)) emit("tab.updated", { tab: current }, current.id);
    if (current && previous && current.documentRevision !== previous.documentRevision)
      emit("document.revision", { revision: current.documentRevision }, current.id);
    this.publish("connection");
  }
  private degrade() {
    if (!this.snapshot || this.stopped) return;
    this.connected = false;
    this.change({ ...this.snapshot, lifecycle: "RECOVERING", connectivity: "reconnecting" });
  }
  private expireSignalRecovery() {
    if (this.stopped) return;
    this.workerReady = false;
    this.connected = false;
    this.vncFrameReady = false;
    this.vncDisplayGrant = null;
    this.degrade();
  }
  private finish(expected: string, payload: unknown) {
    const wait = this.waiter;
    if (!wait || wait.expected !== expected) return;
    this.waiter = null;
    window.clearTimeout(wait.timer);
    wait.resolve(payload);
  }
  private acknowledgeInput(sequence: number, ack: VncInputAck) {
    const wait = this.inputAckWaiters.get(sequence);
    if (wait) {
      this.inputAckWaiters.delete(sequence);
      window.clearTimeout(wait.timer);
      wait.resolve(ack);
      return;
    }
    this.inputAckSeen.set(sequence,ack);
    while (this.inputAckSeen.size > 64) {
      const first = this.inputAckSeen.keys().next().value;
      if (typeof first !== "number") break;
      this.inputAckSeen.delete(first);
    }
  }
  private waitForInputAck(sequence: number): Promise<VncInputAck> {
    const seen=this.inputAckSeen.get(sequence);
    if (seen) {
      this.inputAckSeen.delete(sequence);
      return Promise.resolve(seen);
    }
    return new Promise((resolve,reject) => {
      const timer=window.setTimeout(() => {
        this.inputAckWaiters.delete(sequence);
        reject(fail("INPUT_NOT_ACKNOWLEDGED"));
      },1800);
      this.inputAckWaiters.set(sequence,{resolve,reject,timer});
    });
  }
  private acknowledgeReader(sequence: number, ack: ReaderCommandAck) {
    const wait=this.readerAckWaiters.get(sequence);
    if(wait){
      this.readerAckWaiters.delete(sequence);
      window.clearTimeout(wait.timer);
      wait.resolve(ack);return;
    }
    this.readerAckSeen.set(sequence,ack);
    while(this.readerAckSeen.size>64){
      const first=this.readerAckSeen.keys().next().value;
      if(typeof first!=="number")break;
      this.readerAckSeen.delete(first);
    }
  }
  private waitForReaderAck(sequence: number): Promise<void> {
    const resolveAck=(ack:ReaderCommandAck)=>{
      if(!ack.ok)throw fail(ack.code??"STALE_DOCUMENT");
    };
    const seen=this.readerAckSeen.get(sequence);
    if(seen){
      this.readerAckSeen.delete(sequence);
      try{resolveAck(seen);return Promise.resolve();}
      catch(error){return Promise.reject(error);}
    }
    return new Promise((resolve,reject)=>{
      const timer=window.setTimeout(()=>{
        this.readerAckWaiters.delete(sequence);
        reject(fail("CLOUD_SIGNALING_FAILED"));
      },6000);
      this.readerAckWaiters.set(sequence,{
        resolve:ack=>{
          try{resolveAck(ack);resolve();}
          catch(error){reject(error instanceof Error?error:fail("STALE_DOCUMENT"));}
        },
        reject,timer,
      });
    });
  }
  private remoteError(code: string, detailCode?: unknown, stage?: unknown) {
    const name = /^[A-Z_]{2,64}$/.test(code) ? code : "CLOUD_SIGNALING_FAILED";
    if (DISPLAY_RECOVERY_CODES.has(name)) {
      const waiting=this.displayGrantWaiter;
      if (waiting) {
        this.displayGrantWaiter=null;
        window.clearTimeout(waiting.timer);
        waiting.reject(fail(name));
      }
      return;
    }
    if (SAFE_MEDIA_CODES.has(name)) {
      this.safeMediaFailure = {
        code: name as BrowserV2SafeMediaFailure["code"],
        detailCode: typeof detailCode === "string" &&
          SAFE_MEDIA_DETAIL_CODES.has(detailCode) ? detailCode : null,
        stage: typeof stage === "string" &&
          SAFE_MEDIA_STAGES.has(stage) ? stage : null,
      };
      this.publish("error");
    }
    this.readerChunks.reset();
    if (name === "NAVIGATION_FAILED" && this.snapshot && this.navigationWait) {
      // Chromium can reject a DNS/TLS/proxy/navigation request while the
      // authenticated browser process remains perfectly healthy. Do not leave
      // Floently chrome stuck in LOADING or tear down the same logged-in
      // session. The failed navigation is a new error document, so advance the
      // opaque revision to invalidate Reader state from the previous page.
      const current=this.snapshot.tabs[0]??placeholderTab();
      const failed={...current,loading:false,progress:null,
        lifecycle:"ERROR" as const,
        securityState:current.url.startsWith("https:")?"secure" as const:
          current.url.startsWith("http:")?"insecure" as const:"unknown" as const,
        documentRevision:"cloud-nav-" + (++this.navigationRevision)};
      this.change({...this.snapshot,activeTabId:failed.id,tabs:[failed]});
      const sequence=++this.eventSequence;
      const event:BrowserEvent={eventId:"cloud-"+sequence,
        backendId:this.snapshot.backendId,sequence,
        timestamp:new Date(getSystemTimeMs()).toISOString(),
        type:"navigation.failed",tabId:failed.id,
        payload:{code:"NAVIGATION_FAILED"}};
      for(const listener of this.events)listener(event);
    }
    if (name === "WORKER_COMMAND_FAILED") {
      for (const [sequence,wait] of this.inputAckWaiters) {
        window.clearTimeout(wait.timer);
        wait.reject(fail(name));
        this.inputAckWaiters.delete(sequence);
      }
      for (const [sequence,wait] of this.readerAckWaiters) {
        window.clearTimeout(wait.timer);
        wait.reject(fail(name));
        this.readerAckWaiters.delete(sequence);
      }
    }
    if (this.waiter) {
      const waiting = this.waiter; this.waiter = null;
      window.clearTimeout(waiting.timer); waiting.reject(fail(name));
    }
    if (this.displayGrantWaiter &&
        (name === "ENGINE_START_FAILED" || name === "WORKER_UNAVAILABLE" ||
         name === "CLOUD_SIGNALING_FAILED" || name === "SIGNAL_PROTOCOL_ERROR")) {
      const waiting = this.displayGrantWaiter; this.displayGrantWaiter = null;
      window.clearTimeout(waiting.timer); waiting.reject(fail(name));
    }
    if (this.navigationWait) {
      const waiting = this.navigationWait; this.navigationWait = null;
      window.clearTimeout(waiting.timer); waiting.reject(fail(name));
    }
    if (name === "ENGINE_START_FAILED" || name === "WORKER_UNAVAILABLE" ||
        name === "VIEWPORT_RESIZE_NOT_IMPLEMENTED") this.degrade();
  }
  private receive(m: BrowserV2SignalEnvelope) {
    if (this.stopped || !this.allocation || m.sessionId !== this.allocation.sessionId ||
        m.sequence <= this.lastRemoteSequence) return;
    this.lastRemoteSequence = m.sequence;
    const p = m.payload;
    // Safe progress reports the furthest completed signaling hop only.
    // ANSWER_SENT does not prove browser receipt and must never overwrite
    // ANSWER_WS_FORWARDED even when these server events arrive out of order.
    if (m.type === "server.event" && p.event === "media.phase" &&
        ["OFFER_RECEIVED", "OFFER_PARSED", "REMOTE_SDP_SET",
         "ANSWER_CALLBACK", "ANSWER_SENT", "ANSWER_NODE_RECEIVED",
         "ANSWER_WS_FORWARDED", "ANSWER_WS_QUEUED",
         "ANSWER_WS_FLUSHED"].includes(String(p.stage))) {
      const phase=p.stage as NonNullable<BrowserV2SafeSignalProgress["phase"]>;
      const previous=this.signalProgress.phase;
      if(!previous || SIGNAL_PHASE_ORDER[phase]>SIGNAL_PHASE_ORDER[previous]) {
        this.signalProgress = { ...this.signalProgress, phase };
        this.publish("signal");
      }
    } else if (m.type === "server.answer") {
      this.signalProgress = { ...this.signalProgress, answerReceived: true };
      this.publish("signal");
    } else if (m.type === "server.ice") {
      if (!this.signalProgress.iceReceived) {
        this.signalProgress = { ...this.signalProgress, iceReceived: true };
        this.publish("signal");
      }
    }
    if (m.type === "server.state" &&
        (p.state === "NEGOTIATING" || p.state === "DISPLAY_WAITING")) {
      this.workerReady = true;
      this.signalReadyGeneration++;
      if (this.snapshot) this.change({ ...this.snapshot,
        lifecycle: p.state === "DISPLAY_WAITING" ? "STARTING" : "READY" });
      this.publish("connection");
    } else if (m.type === "server.error") {
      this.remoteError(typeof p.code === "string" ? p.code : "CLOUD_SIGNALING_FAILED",
        p.detailCode, p.stage);
    } else if (m.type === "server.viewportApplied") {
      this.publish("viewport");
    } else if (m.type === "server.event" && p.event === "browser.event" &&
               p.type === "navigation.started" && this.snapshot) {
      const current=this.snapshot.tabs[0]??placeholderTab();
      let nextUrl=current.url;
      const candidate=typeof p.url==="string"?p.url:this.navigationWait?.url;
      if(candidate){
        try{nextUrl=safeTarget(candidate);}catch{}
      }
      const next={...current,url:nextUrl,displayUrl:nextUrl,
        title:current.title||"Loading webpage",loading:true,progress:null,
        lifecycle:"LOADING" as const};
      this.change({...this.snapshot,activeTabId:next.id,tabs:[next]});
    } else if (m.type === "server.event" && p.event === "browser.event" &&
               p.type === "navigation.failed" && this.snapshot) {
      const current=this.snapshot.tabs[0]??placeholderTab();
      let nextUrl=current.url;
      if(typeof p.url==="string"){
        try{nextUrl=safeTarget(p.url);}catch{}
      }
      const next={...current,
        url:nextUrl,displayUrl:nextUrl,
        title:typeof p.title==="string"&&p.title.length<=160&&p.title.trim()&&
          !/[\r\n\0]/.test(p.title)?p.title:current.title,
        loading:false,progress:null,lifecycle:"ERROR" as const,
        canGoBack:p.canGoBack===true,canGoForward:p.canGoForward===true,
        securityState:nextUrl.startsWith("https:")?"secure" as const:
          nextUrl.startsWith("http:")?"insecure" as const:"unknown" as const,
        documentRevision:"cloud-nav-" + (++this.navigationRevision),
      };
      this.change({...this.snapshot,activeTabId:next.id,tabs:[next]});
      const waiting=this.navigationWait;
      if(waiting){
        this.navigationWait=null;window.clearTimeout(waiting.timer);
        waiting.reject(fail("NAVIGATION_FAILED"));
      }
      const sequence=++this.eventSequence;
      const event:BrowserEvent={eventId:"cloud-"+sequence,
        backendId:this.snapshot.backendId,sequence,
        timestamp:new Date(getSystemTimeMs()).toISOString(),
        type:"navigation.failed",tabId:next.id,
        payload:{code:"NAVIGATION_FAILED"}};
      for(const listener of this.events)listener(event);
    } else if (m.type === "server.event" && p.event === "browser.event" &&
               (p.type === "navigation.committed" ||
                p.type === "navigation.completed" ||
                p.type === "navigation.observed") && this.snapshot) {
      const current=this.snapshot.tabs[0]??placeholderTab();
      let nextUrl=current.url,hasPublicUrl=false;
      if(typeof p.url==="string"){
        try{nextUrl=safeTarget(p.url);hasPublicUrl=true;}catch{}
      }
      const committed=p.type==="navigation.committed";
      const observed=p.type==="navigation.observed";
      // Chromium represents DNS/TLS/proxy failures with an internal
      // chrome-error:// document. The worker intentionally strips that URL.
      // Keep the explicit failed-navigation state until a real public
      // navigation starts/commits; background observation may still refresh
      // Back/Forward capability without making the failed page look healthy.
      const preserveFailedDocument=observed&&
        current.lifecycle==="ERROR"&&!hasPublicUrl;
      const documentChanged=!preserveFailedDocument&&(committed||
        (observed&&(nextUrl!==current.url||p.documentChanged===true)));
      const next={...current,
        url:nextUrl,displayUrl:nextUrl,
        title:preserveFailedDocument?current.title:
          typeof p.title==="string"&&p.title.length<=160&&p.title.trim()&&
          !/[\r\n\0]/.test(p.title)?p.title:current.title,
        loading:preserveFailedDocument?false:p.loading===true,
        progress:null,
        lifecycle:preserveFailedDocument?"ERROR" as const:
          (p.loading===true?"LOADING":"COMPLETE") as "LOADING"|"COMPLETE",
        canGoBack:p.canGoBack===true,
        canGoForward:p.canGoForward===true,
        securityState:nextUrl.startsWith("https:")?"secure" as const:
          nextUrl.startsWith("http:")?"insecure" as const:"unknown" as const,
        documentRevision:documentChanged?
          "cloud-nav-" + (++this.navigationRevision):current.documentRevision,
      };
      if(documentChanged&&this.waiter){
        const reader=this.waiter;this.waiter=null;
        window.clearTimeout(reader.timer);
        reader.reject(fail("STALE_DOCUMENT"));
      }
      this.change({...this.snapshot,activeTabId:next.id,tabs:[next]});
      const waiting=this.navigationWait;
      if(waiting&&waiting.expected===p.type){
        this.navigationWait=null;window.clearTimeout(waiting.timer);
        waiting.resolve({tab:next});
      }
    } else if (m.type === "server.event" && p.event === "reader.document") {
      // Older service images may still send a short single-frame article.
      this.finish("reader.document", p.document);
    } else if (m.type === "server.event" && p.event === "reader.documentChunk") {
      if (this.waiter?.expected !== "reader.document") return;
      try {
        const document = this.readerChunks.append(p);
        if (document) this.finish("reader.document", document);
      } catch {
        this.remoteError("READER_DOCUMENT_TRANSFER_FAILED");
      }
    } else if (m.type === "server.event" && p.event === "reader.point") {
      this.finish("reader.point", p.anchor);
    } else if (m.type === "server.event" && p.event === "reader.commandAck" &&
               Number.isSafeInteger(p.clientSequence) &&
               typeof p.ok === "boolean") {
      this.acknowledgeReader(p.clientSequence as number,{
        ok:p.ok as boolean,
        code:typeof p.code==="string"&&/^[A-Z_]{2,64}$/.test(p.code)?
          p.code:null,
      });
    } else if (m.type === "server.event" && p.event === "input.ack" &&
               Number.isSafeInteger(p.clientSequence) &&
               ["input.pointer","input.wheel","input.touch","input.key","input.text"].includes(
                 String(p.inputType))) {
      this.acknowledgeInput(p.clientSequence as number,{
        clickDispatched:p.clickDispatched === true,
        interactiveHit:typeof p.interactiveHit === "boolean" ?
          p.interactiveHit as boolean : null,
        textInserted:typeof p.textInserted === "boolean" ?
          p.textInserted as boolean : null,
        textInputFocused:typeof p.textInputFocused === "boolean" ?
          p.textInputFocused as boolean : null,
        touchDispatched:typeof p.touchDispatched === "boolean" ?
          p.touchDispatched as boolean : null,
      });
    } else if (m.type === "server.event" && p.event === "display.ticket") {
      const validated = this.checkedVncGrant(p);
      if (validated) {
        this.vncDisplayGrant = validated;
        const waiting=this.displayGrantWaiter;
        if (waiting) {
          this.displayGrantWaiter=null;
          window.clearTimeout(waiting.timer);
          waiting.resolve(validated);
        }
      } else this.remoteError("DISPLAY_GRANT_INVALID");
    }
  }
  private checkedVncGrant(value: Record<string, unknown>): BrowserV2VncGrant | null {
    if (!this.allocation || this.getDisplayTransport() !== "vnc" ||
        !DISPLAY_PATH_ID.test(this.allocation.sessionId) ||
        typeof value.displayPath !== "string" ||
        value.displayPath !==
          "/api/browser-v2/cloud/sessions/" +
          encodeURIComponent(this.allocation.sessionId) + "/display" ||
        typeof value.displayTicket !== "string" ||
        !DISPLAY_TICKET.test(value.displayTicket) ||
        typeof value.displayExpiresInSeconds !== "number" ||
        !Number.isInteger(value.displayExpiresInSeconds) ||
        (value.displayExpiresInSeconds as number) < 1 ||
        (value.displayExpiresInSeconds as number) > 90) return null;
    let displayOrigin=V2_ORIGIN;
    if(typeof value.displayOrigin==="string"&&value.displayOrigin){
      try{
        const parsed=new URL(value.displayOrigin);
        if(parsed.protocol!=="https:"||parsed.username||parsed.password||
           parsed.search||parsed.hash||parsed.pathname!=="/")
          return null;
        displayOrigin=parsed.origin;
      }catch{return null;}
    }
    return { origin: displayOrigin, sessionId: this.allocation.sessionId,
      displayPath: value.displayPath,
      displayTicket: value.displayTicket,
      displayExpiresInSeconds: value.displayExpiresInSeconds as number };
  }
  /** Refresh display only; old owner signal/Chromium/CDP/Reader stay intact.
   * X must allowlist client.displayTicket on canonical CloudSignalSocket. */
  async refreshVncDisplayGrant(): Promise<BrowserV2VncGrant> {
    if (!this.workerReady || !this.signal || this.getDisplayTransport() !== "vnc" ||
        this.displayGrantWaiter) throw fail("DISPLAY_UNAVAILABLE");
    return new Promise((resolve, reject) => {
      const timer = window.setTimeout(() => {
        if (this.displayGrantWaiter?.expected === "display.ticket")
          this.displayGrantWaiter = null;
        reject(fail("DISPLAY_TICKET_TIMEOUT"));
      }, 7000);
      this.displayGrantWaiter = { expected: "display.ticket",
        resolve: result => resolve(result as BrowserV2VncGrant), reject, timer };
      void this.command("client.displayTicket", {}).catch(error => {
        if (this.displayGrantWaiter?.expected === "display.ticket") {
          this.displayGrantWaiter = null; window.clearTimeout(timer);
          reject(fail("DISPLAY_UNAVAILABLE"));
        }
      });
    });
  }
  private async command(type: string, payload: Record<string, unknown>): Promise<number> {
    if (!this.signal || !this.allocation || this.stopped ||
        (!this.workerReady && type !== "client.ping"))
      throw fail("CLOUD_RECONNECTING");
    // X CloudSignalSocket owns the canonical on-wire sequence because media
    // and BrowserBackend share this one authenticated socket.
    const envelope: BrowserV2SignalEnvelope = {
      version: "0.1", sessionId: this.allocation.sessionId,
      sequence: ++this.sequence, type, payload,
    };
    try {
      return await this.signal.sendWithSequence(envelope);
    } catch (error) {
      if (error instanceof Error && error.message === "SIGNAL_RESUME_EXPIRED")
        this.expireSignalRecovery();
      throw error;
    }
  }
  async start(input: BrowserStartInput): Promise<BrowserSnapshot> {
    if (this.allocation || this.stopped) throw fail("BROWSER_V2_SESSION_ALREADY_STARTED");
    const viewport = safeViewport(input.viewport ?? {
      width: 390, height: 792, deviceScaleFactor: window.devicePixelRatio || 1,
    });
    this.initialTarget = input.initialTarget ? safeTarget(input.initialTarget) : undefined;
    this.profileId=input.profileId;
    this.preserveForReload=false;
    const resumeSessionId=input.privateMode?undefined:readReloadHint(input.profileId);
    const session = await createCloudBrowserSession({
      origin: V2_ORIGIN, viewport,
      ...(resumeSessionId ? { resumeSessionId } : {}),
      ...(getAuthAccessToken() ? { authorization: "Bearer " + getAuthAccessToken() } : {}),
    });
    if (this.stopped) throw fail("SESSION_GONE");
    this.allocation = session;
    const optional = session as typeof session & BrowserV2OptionalDisplayAllocation;
    if (optional.displayTransport === "vnc") {
      // Fresh allocations include the first one-use display grant. A same-
      // Chromium reattach intentionally does not: after the owner signal is
      // restored, the page asks for a fresh display ticket on that live worker.
      const grant = this.checkedVncGrant(optional as unknown as Record<string, unknown>);
      if (!grant && optional.resumed !== true) {
        await this.stop(); throw fail("DISPLAY_GRANT_INVALID");
      }
      this.vncDisplayGrant = grant;
    }
    const socket = new CloudSignalSocket({
      origin: V2_ORIGIN, sessionId: session.sessionId,
      socketTicket: session.socketTicket,
      initialSequence: optional.resumed===true ? Number(optional.resumeSequence??0) : 0,
    });
    this.signal = socket;
    this.offSignal = socket.subscribe(m => this.receive(m));
    this.snapshot = {
      backendId: "cloud-v2-" + session.sessionId, lifecycle: "STARTING",
      activeTabId: "cloud-tab-1", tabs: [placeholderTab()],
      profile: { id: input.profileId, mode: input.privateMode ? "private" : "persistent" },
      capabilities, connectivity: "reconnecting",
    };
    try { await socket.connect(); }
    catch (e) { await this.stop(); throw e; }
    if(!input.privateMode)writeReloadHint(input.profileId,session.sessionId);
    // Browser WebSockets cannot emit protocol ping frames themselves. Keep
    // the authenticated owner channel active so reverse proxies and the
    // gateway's sliding idle timeout do not destroy a live reading session.
    this.heartbeatTimer=window.setInterval(() => {
      if (this.stopped || this.signal !== socket) return;
      void this.command("client.ping", {}).catch(() => {});
    },20_000);
    if (!this.snapshot) throw fail("SESSION_GONE");
    return this.snapshot;
  }
  async attachVideo(video: HTMLVideoElement, onFrame: () => void, onStatus: (s: string) => void) {
    if (this.transport || !this.signal || !this.allocation || this.stopped)
      throw fail("CLOUD_SIGNALING_FAILED");
    const transport = new CloudWebRtcTransport({
      sessionId: this.allocation.sessionId, signaling: this.signal, video,
      // The gateway's authenticated allocation already supplies short-lived
      // TURN credentials. Forward them to the *actual* RTCPeerConnection.
      // Omitting these forced host-only ICE (private Docker/mDNS addresses)
      // even when coturn was configured correctly on the server.
      iceServers: this.allocation.iceServers,
      // A single backend-authenticated TURN/TCP endpoint is an explicit
      // external packet-loss recovery mode. Forbid local/direct ICE so a
      // lossy peer-reflexive UDP path cannot win candidate nomination.
      relayOnly: this.allocation.iceServers.length===1 &&
        typeof this.allocation.iceServers[0].urls==="string" &&
        /^turn:[A-Za-z0-9.-]+:[0-9]{2,5}\?transport=tcp$/.test(
          this.allocation.iceServers[0].urls),
      onDecodedFrame: () => { this.publish("frame"); onFrame(); },
      // Use the EXISTING authenticated, one-owner WSS command path when
      // GStreamer SCTP browser.control/motion is closed or never opens.
      // The private worker already supports these exact commands. No public
      // CDP, alternate session, new port or media freshness bypass.
      onInputFallback: async (type, payload, viewportRevision) => {
        let commandType: "input.pointer" | "input.touch" | "input.wheel";
        let action: string | undefined;
        switch (type) {
          case "click": commandType = "input.pointer"; action = "click"; break;
          case "pointerButton":
            commandType = "input.pointer";
            action = payload.state === "down" ? "down" :
              payload.state === "up" ? "up" : undefined;
            break;
          case "wheel": commandType = "input.wheel"; break;
          case "touchStart": commandType = "input.touch"; action = "start"; break;
          case "touchMove": commandType = "input.touch"; action = "move"; break;
          case "touchEnd": commandType = "input.touch"; action = "end"; break;
          default: throw fail("INPUT_FALLBACK_UNSUPPORTED");
        }
        if (type === "pointerButton" && !action)
          throw fail("INPUT_FALLBACK_UNSUPPORTED");
        await this.command(commandType, {
          ...payload, ...(action ? {action} : {}), viewportRevision,
        });
      },
      onStatus: status => {
        const mediaReady = cloudMediaConnectivity(status);
        // WARNING: ICE_STATE_CONNECTED, REMOTE_DESCRIPTION_APPLIED and all
        // media.phase telemetry can arrive AFTER real decoded playback.
        // Emitting connectivity=reconnecting for mere progress causes the
        // UI adapter to stop speech, mark Reader STALE and hide controls.
        // Only a genuine change in the transport's media-ready state may
        // update BrowserBackend connectivity.
        if (mediaReady === true) {
          this.connected = true;
          if (this.safeMediaFailure) {
            this.safeMediaFailure = null;
            this.publish("error");
          }
        } else if (mediaReady === false) {
          this.connected = false;
        }
        if (mediaReady !== null && this.snapshot && !this.stopped) this.change({
          ...this.snapshot,
          lifecycle: mediaReady ? "READY" :
                     this.workerReady ? "RECOVERING" : "STARTING",
          connectivity: mediaReady ? "connected" : "reconnecting",
        });
        onStatus(status);
        if (status === "MEDIA_CONNECTED" && this.initialTarget && this.isMediaReady()) {
          const target = this.initialTarget;
          this.initialTarget = undefined;
          void this.open(target).catch(() => {});
        }
      },
    });
    this.transport = transport;
    try { await transport.start(); }
    catch (e) { this.degrade(); throw e; }
    if (this.stopped) await transport.stop();
  }
  stopForPageHide(): void {
    if (this.stopped || !this.allocation || !this.profileId) return;
    // Preserve the SAME live Chromium across an outer Read-page reload.
    // sessionStorage contains only an opaque session id; the next page must
    // authenticate normally before E will mint a fresh one-use reattach ticket.
    this.preserveForReload=true;
    writeReloadHint(this.profileId,this.allocation.sessionId);
  }
  async stop(): Promise<void> {
    if (this.stopped) return;
    const signal = this.signal, allocation = this.allocation;
    const preserve=this.preserveForReload;
    if (signal && allocation && !preserve) {
      try { await this.command("session.stop", {}); } catch {}
      clearReloadHint(allocation.sessionId);
    } else if (allocation && preserve && this.profileId) {
      writeReloadHint(this.profileId,allocation.sessionId);
    }
    this.stopped = true;
    this.connected = false;
    this.vncFrameReady = false;
    this.vncDisplayGrant = null;
    this.signalProgress = { phase: null, answerReceived: false, iceReceived: false };
    this.signalReadyGeneration = 0;
    this.safeMediaFailure = null;
    if (this.heartbeatTimer !== null) window.clearInterval(this.heartbeatTimer);
    this.heartbeatTimer = null;
    for (const [sequence,wait] of this.inputAckWaiters) {
      window.clearTimeout(wait.timer);
      wait.reject(fail("SESSION_GONE"));
      this.inputAckWaiters.delete(sequence);
    }
    this.inputAckSeen.clear();
    for (const [sequence,wait] of this.readerAckWaiters) {
      window.clearTimeout(wait.timer);
      wait.reject(fail("SESSION_GONE"));
      this.readerAckWaiters.delete(sequence);
    }
    this.readerAckSeen.clear();
    await this.transport?.stop();
    this.offSignal?.(); signal?.close();
    this.transport = null; this.offSignal = null; this.signal = null;
    this.allocation = null; this.snapshot = null;
    this.profileId=null;
    this.preserveForReload=false;
    this.readerChunks.reset();
    if (this.waiter) { const w = this.waiter; this.waiter = null;
      window.clearTimeout(w.timer); w.reject(fail("SESSION_GONE")); }
    if (this.displayGrantWaiter) { const w = this.displayGrantWaiter;
      this.displayGrantWaiter = null;
      window.clearTimeout(w.timer); w.reject(fail("SESSION_GONE")); }
    if (this.navigationWait) { const w = this.navigationWait; this.navigationWait = null;
      window.clearTimeout(w.timer); w.reject(fail("SESSION_GONE")); }
  }
  /**
   * Re-arm this app-owned client only after a completed stop.
   *
   * Browser V2 deliberately has one owner session at a time. Retry therefore
   * destroys the old authenticated allocation first, then explicitly reuses
   * this JavaScript owner object with fresh tickets/socket/transport. This is
   * never called while an allocation or media transport is still attached.
   */
  prepareRestart(): void {
    if (
      this.allocation
      || this.signal
      || this.transport
      || this.offSignal
      || this.heartbeatTimer !== null
    ) {
      throw fail("BROWSER_V2_SESSION_STILL_ACTIVE");
    }

    this.stopped = false;
    this.connected = false;
    this.workerReady = false;
    this.vncFrameReady = false;
    this.vncDisplayGrant = null;
    this.signalProgress = {
      phase: null,
      answerReceived: false,
      iceReceived: false,
    };
    this.signalReadyGeneration = 0;
    this.safeMediaFailure = null;
    this.sequence = 0;
    this.lastRemoteSequence = 0;
    this.navigationRevision = 0;
    this.preserveForReload = false;
  }

  subscribe(fn: (e: BrowserEvent) => void) { this.events.add(fn); return () => this.events.delete(fn); }
  private unsupported(): Promise<never> { return Promise.reject(fail("UNSUPPORTED_CAPABILITY")); }
  private navigate(type:"browser.open"|"browser.back"|"browser.forward"|"browser.reload"|"browser.stop",
                   payload:Record<string,unknown>,fallbackUrl:string,
                   expected:"navigation.committed"|"navigation.completed"="navigation.committed"):
                   Promise<NavigationResult> {
    if(!this.isMediaReady())return Promise.reject(fail("CLOUD_RECONNECTING"));
    if(this.navigationWait)return Promise.reject(fail("BROWSER_V2_RPC_BACKPRESSURE"));
    return new Promise((resolve,reject)=>{
      const timeout=window.setTimeout(()=>{
        if(this.navigationWait?.timer===timeout)this.navigationWait=null;
        reject(fail("NAVIGATION_FAILED"));
      },20000);
      this.navigationWait={url:fallbackUrl,expected,resolve,reject,timer:timeout};
      void this.command(type,payload).catch(error=>{
        if(this.navigationWait?.timer!==timeout)return;
        this.navigationWait=null;window.clearTimeout(timeout);
        reject(error instanceof Error?error:fail("NAVIGATION_FAILED"));
      });
    });
  }
  async open(target: string, tabId?: string): Promise<NavigationResult> {
    if(tabId&&tabId!=="cloud-tab-1")return this.unsupported();
    const url=safeTarget(target);
    return this.navigate("browser.open",{url},url);
  }
  back(id: string): Promise<NavigationResult> {
    if(id!=="cloud-tab-1")return this.unsupported();
    return this.navigate("browser.back",{},this.snapshot?.tabs[0]?.url??"");
  }
  forward(id: string): Promise<NavigationResult> {
    if(id!=="cloud-tab-1")return this.unsupported();
    return this.navigate("browser.forward",{},this.snapshot?.tabs[0]?.url??"");
  }
  reload(id: string): Promise<NavigationResult> {
    if(id!=="cloud-tab-1")return this.unsupported();
    return this.navigate("browser.reload",{},this.snapshot?.tabs[0]?.url??"");
  }
  async stopLoading(id: string): Promise<void> {
    if(id!=="cloud-tab-1")return this.unsupported();
    await this.navigate("browser.stop",{},this.snapshot?.tabs[0]?.url??"",
      "navigation.completed");
  }
  createTab(_input: CreateTabInput = {}): Promise<TabSnapshot> { return this.unsupported(); }
  closeTab(_id: string): Promise<void> { return this.unsupported(); }
  activateTab(_id: string): Promise<TabSnapshot> { return this.unsupported(); }
  duplicateTab(_id: string): Promise<TabSnapshot> { return this.unsupported(); }
  restoreClosedTab(): Promise<TabSnapshot | null> { return this.unsupported(); }
  find(_id: string, _query: string, _direction: "next" | "previous"): Promise<FindResult> {
    return this.unsupported();
  }
  setZoom(_id: string, _factor: number): Promise<void> { return this.unsupported(); }
  getSelection(_id: string): Promise<SelectionResult | null> { return this.unsupported(); }
  async updateViewport(v: BrowserViewport) {
    if (!this.allocation) return;
    const requested = safeViewport(v), existing = this.allocation.initialViewport;
    if (existing.cssWidth !== requested.cssWidth || existing.cssHeight !== requested.cssHeight ||
        Math.abs(existing.devicePixelRatio - requested.devicePixelRatio) > 0.01) {
      // Current E worker cannot renegotiate its Xvfb/VP8 viewport. Preserve
      // the actual negotiated frame and original CSS coordinate system while
      // the page surface reflows (Reader bar, drawer, iOS bars, rotation).
      // X browserVideoPoint maps the contained video to those original CSS
      // coordinates. Never send an unsupported resize and kill a live session.
      return;
    }
    await this.transport?.requestViewport(requested);
  }
  async sendKey(action: "down" | "up", key: string): Promise<void> {
    if (!this.isMediaReady() || key.length > 32) return;
    await this.command("input.key", {
      action, key, viewportRevision: this.getViewportRevision(),
    });
  }

  async sendText(text: string): Promise<void> {
    if (!this.isMediaReady() || typeof text !== "string" || !text || text.length > 4096) return;
    const sequence=await this.command("input.text", {
      text, viewportRevision: this.getViewportRevision(),
    });
    const ack=await this.waitForInputAck(sequence);
    // Transitional compatibility: the currently-live backend predates the
    // explicit textInserted field. Reject only an explicit negative ack; once
    // the paired backend release is live every new worker returns true/false.
    if(ack.textInserted===false)throw fail("INPUT_NOT_ACKNOWLEDGED");
  }

  async sendVncTouch(action: "start" | "move" | "end",
                     x: number, y: number, touchId = 0): Promise<VncInputAck | undefined> {
    if (!this.isMediaReady() || this.getDisplayTransport() !== "vnc")
      throw fail("INPUT_NOT_READY");
    const viewport = this.getVncViewport();
    if (!viewport || !Number.isFinite(x) || !Number.isFinite(y) ||
        !Number.isInteger(touchId) || touchId < 0 || touchId > 31 ||
        x < 0 || y < 0 || x >= viewport.renderWidth || y >= viewport.renderHeight)
      throw fail("INPUT_POINT_UNAVAILABLE");
    const sequence=await this.command("input.touch", {
      action, x, y, touchId, viewportRevision: viewport.revision,
    });
    if(action!=="end")return;
    const ack=await this.waitForInputAck(sequence);
    if(ack.touchDispatched===false)throw fail("INPUT_NOT_ACKNOWLEDGED");
    return ack;
  }

  async sendVncClick(x: number, y: number,
                     button: "left" | "middle" | "right" = "left"): Promise<VncInputAck> {
    if (!this.isMediaReady() || this.getDisplayTransport() !== "vnc")
      throw fail("INPUT_NOT_READY");
    const viewport = this.getVncViewport();
    if (!viewport || !Number.isFinite(x) || !Number.isFinite(y) ||
        x < 0 || y < 0 || x >= viewport.renderWidth || y >= viewport.renderHeight)
      throw fail("INPUT_POINT_UNAVAILABLE");
    const sequence=await this.command("input.pointer", {
      action: "click", x, y, button, viewportRevision: viewport.revision,
    });
    // Resolve only after gateway -> isolated worker -> CDP dispatch completes.
    const ack=await this.waitForInputAck(sequence);
    if(!ack.clickDispatched)throw fail("INPUT_NOT_ACKNOWLEDGED");
    return ack;
  }

  async sendVncWheel(deltaX: number, deltaY: number,
                     x?: number, y?: number): Promise<void> {
    if (!this.isMediaReady() || this.getDisplayTransport() !== "vnc")
      throw fail("INPUT_NOT_READY");
    const viewport = this.getVncViewport();
    if (!viewport || !Number.isFinite(deltaX) || !Number.isFinite(deltaY))
      throw fail("INPUT_POINT_UNAVAILABLE");
    const hasPoint=Number.isFinite(x)&&Number.isFinite(y);
    if (hasPoint && (Number(x)<0 || Number(y)<0 ||
        Number(x)>=viewport.renderWidth || Number(y)>=viewport.renderHeight))
      throw fail("INPUT_POINT_UNAVAILABLE");
    await this.command("input.wheel", {
      deltaX, deltaY, ...(hasPoint ? {x:Number(x),y:Number(y)} : {}),
      viewportRevision: viewport.revision,
    });
  }
  private request<T>(expected: string, type: string, payload: Record<string, unknown>): Promise<T> {
    if (!this.isMediaReady()) return Promise.reject(fail("CLOUD_RECONNECTING"));
    if (this.waiter) return Promise.reject(fail("BROWSER_V2_RPC_BACKPRESSURE"));
    if (expected === "reader.document") this.readerChunks.reset();
    // Full authenticated documents can be substantially slower than a point
    // lookup. Stay below the worker's 30s RPC ceiling, but do not abandon a
    // healthy extraction at the old 9s frontend boundary.
    const timeoutMs=expected==="reader.document"?25_000:9_000;
    return new Promise<T>((resolve, reject) => {
      const timer = window.setTimeout(() => { this.waiter = null;
        reject(fail("CLOUD_SIGNALING_FAILED")); }, timeoutMs);
      this.waiter = { expected, resolve: x => resolve(x as T), reject, timer };
      void this.command(type, payload).catch(e => {
        if (this.waiter) { this.waiter = null; window.clearTimeout(timer);
          reject(e instanceof Error ? e : fail("CLOUD_SIGNALING_FAILED")); }
      });
    });
  }
  reader(): ReadingAdapter {
    const only = (id: string) => { if (id !== "cloud-tab-1") throw fail("STALE_DOCUMENT"); };
    return {
      extractDocument: id => { only(id); return this.request<ReadingDocument>("reader.document", "reader.extract", {}); },
      resolvePoint: (id, p: ViewportPoint) => {
        only(id);
        if (p.viewportRevision !== undefined && p.viewportRevision !== this.getViewportRevision())
          return Promise.resolve(null);
        return this.request<ReadingAnchor | null>("reader.point", "reader.resolvePoint",
          { x: p.x, y: p.y, viewportRevision: this.getViewportRevision() });
      },
      getSelection: async id => { only(id); return null as ReadingSelection | null; },
      highlightSentence: async (id, anchor) => { only(id);
        const sequence=await this.command("reader.highlightSentence", { anchor });
        await this.waitForReaderAck(sequence); },
      highlightWord: async (id, anchor, wordIndex) => { only(id);
        const sequence=await this.command("reader.highlightWord", { anchor, wordIndex });
        await this.waitForReaderAck(sequence); },
      clearHighlights: async id => { only(id);
        const sequence=await this.command("reader.clearHighlights", {});
        await this.waitForReaderAck(sequence); },
      scrollToSentence: async (id, anchor) => { only(id);
        const sequence=await this.command("reader.scrollToSentence", { anchor });
        await this.waitForReaderAck(sequence); },
      subscribeRevision: (id, listener) => { only(id);
        const fn = (e: BrowserEvent) => {
          if (e.type === "document.revision" && e.tabId === id && isRecord(e.payload) &&
              typeof e.payload.revision === "string") listener(e.payload.revision);
        };
        return this.subscribe(fn);
      },
    };
  }
  audioPlay(_tabId: string, _sentenceId: string): Promise<void> { return this.unsupported(); }
  audioPause(): Promise<void> { return this.unsupported(); }
  audioStop(): Promise<void> { return this.unsupported(); }
}
