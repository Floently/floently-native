export interface RequestedCloudViewport {
  cssWidth: number;
  cssHeight: number;
  devicePixelRatio?: number;
}

export interface AllocatedCloudBrowserSession {
  readonly sessionId: string;
  readonly socketTicket: string;
  readonly iceServers: RTCIceServer[];
  readonly resumed: boolean;
  readonly resumeSequence: number;
  readonly signalUrl: string;
  readonly initialViewport: {
    revision: number;
    cssWidth: number;
    cssHeight: number;
    devicePixelRatio: number;
    renderWidth: number;
    renderHeight: number;
  };
  /** Only present when the trusted E gateway allocated this SAME owner session in VNC mode. */
  readonly displayTransport?: "vnc";
  /** Optional separate public HTTPS origin for the display WebSocket (iOS reliability). */
  readonly displayOrigin?: string;
  readonly displayPath?: string;
  /** One-use display-only grant; never put it into URL/query/storage/logs. */
  readonly displayTicket?: string;
  readonly displayExpiresInSeconds?: number;
  readonly decodedMediaReady: false;
}

export function createCloudBrowserSession(input: {
  origin: string;
  viewport: RequestedCloudViewport;
  authorization?: string;
  resumeSessionId?: string;
  fetchImpl?: typeof fetch;
  timeoutMs?: number;
}): Promise<AllocatedCloudBrowserSession>;
