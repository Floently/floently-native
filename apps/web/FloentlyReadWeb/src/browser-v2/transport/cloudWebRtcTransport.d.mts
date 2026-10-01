export type BrowserV2SignalEnvelope = {
  version: "0.1";
  sessionId: string;
  sequence: number;
  type: string;
  payload: Record<string, unknown>;
};

export type BrowserV2CloudViewport = {
  revision: number;
  cssWidth: number;
  cssHeight: number;
  mediaWidth: number;
  mediaHeight: number;
  effectiveDpr: number;
};

export type BrowserV2VideoPoint = { x: number; y: number };

export type BrowserV2Signaling = {
  send(envelope: BrowserV2SignalEnvelope): Promise<void> | void;
  subscribe(listener: (envelope: BrowserV2SignalEnvelope) => void): () => void;
  waitForReady?(timeoutMs?: number): Promise<void>;
};

export const BROWSER_V2_WIRE_VERSION: "0.1";
export function browserVideoPoint(
  video: HTMLVideoElement,
  viewport: BrowserV2CloudViewport | null,
  clientX: number,
  clientY: number,
): BrowserV2VideoPoint | null;

export class CloudWebRtcTransport {
  constructor(input: {
    sessionId: string;
    signaling: BrowserV2Signaling;
    video: HTMLVideoElement;
    iceServers?: RTCIceServer[];
    relayOnly?: boolean;
    rtcFactory?: (iceServers: RTCIceServer[], relayOnly?: boolean) => RTCPeerConnection;
    onStatus?: (status: string) => void;
    onDecodedFrame?: (frame: { width: number; height: number }) => void;
    /** Existing authorized WSS path; never a public CDP endpoint. */
    onInputFallback?: ((type: string, payload: Record<string, unknown>,
      viewportRevision: number) => Promise<void>) | null;
  });
  readonly sessionId: string;
  readonly decodedFrameReady: boolean;
  readonly appliedViewport: BrowserV2CloudViewport | null;
  readonly safePeerState: Readonly<{
    remoteAnswer: "not-received" | "applying" | "applied" | "failed";
    iceConnectionState: "new" | "checking" | "connected" | "completed" |
      "disconnected" | "failed" | "closed" | "unknown";
    connectionState: "new" | "connecting" | "connected" |
      "disconnected" | "failed" | "closed" | "unknown";
  }>;
  /** Safe public debugging for the current live click/scroll gate; no page or ICE data. */
  readonly safeInputState: Readonly<{
    browserControlOpen: boolean;
    browserMotionOpen: boolean;
    peerConnected: boolean;
    videoElementReady: boolean;
    videoProof: boolean;
    freshFrameRequired: boolean;
    viewportMatches: boolean;
    authenticatedFallback: boolean;
    fallbackFailed: boolean;
    inputReady: boolean;
  }>;
  safeMediaStats(): Promise<Readonly<{
    remoteAnswer: "not-received" | "applying" | "applied" | "failed";
    ice: string;
    connection: string;
    dtls: string;
    pair: string;
    localType: string;
    remoteType: string;
    rxPackets: number;
    rxBytes: number;
    rxLost: number;
    rxPli: number;
    rxNack: number;
    framesReceived: number;
    framesDecoded: number;
    keyFramesDecoded: number;
    jitterFrames: number;
    codec: "VP8" | "unknown";
    track: string;
    videoReadyState: number;
    videoWidth: number;
    videoHeight: number;
  }> | null>;
  start(): Promise<void>;
  requestViewport(viewport: {
    cssWidth: number;
    cssHeight: number;
    devicePixelRatio?: number;
    visibility?: "visible" | "hidden";
  }): Promise<void>;
  bindGestures(element?: HTMLElement): () => void;
  resumePlaybackFromUserGesture(): Promise<boolean>;
  stop(): Promise<void>;
}
