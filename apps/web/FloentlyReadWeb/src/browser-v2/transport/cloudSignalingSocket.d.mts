import type { BrowserV2SignalEnvelope, BrowserV2Signaling } from "./cloudWebRtcTransport.mjs";

export function cloudSignalSocketUrl(origin: string, sessionId: string): string;

/** Cookies/session permissions are validated at the trusted server edge. */
export class CloudSignalSocket implements BrowserV2Signaling {
  constructor(input: {
    origin: string;
    sessionId: string;
    socketTicket: string;
    resumeExpiresAtMs?: number | null;
    initialSequence?: number;
    WebSocketImpl?: typeof WebSocket;
    timeoutMs?: number;
  });
  readonly url: string;
  readonly sessionId: string;
  readonly timeoutMs: number;
  readonly WebSocketImpl: typeof WebSocket;
  connect(): Promise<void>;
  waitForReady(timeoutMs?: number): Promise<void>;
  send(envelope: BrowserV2SignalEnvelope): Promise<void>;
  sendWithSequence(envelope: BrowserV2SignalEnvelope): Promise<number>;
  exportResumeState(): Readonly<{
    ticket: string;
    expiresAtMs: number;
    nextSequence: number;
  }> | null;
  /** Best-effort synchronous stop for explicit terminal teardown only. */
  trySendSessionStopNow(): boolean;
  subscribe(listener: (message: BrowserV2SignalEnvelope) => void): () => void;
  close(): void;
}
