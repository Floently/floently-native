/**
 * Same-origin, cookie-authenticated Browser V2 signaling client.
 * The trusted edge MUST authenticate the user and authorize this session ID
 * before upgrading the socket; browser-controlled headers never assert ownerKey.
 * No bearer token, signaling credential, page URL, or CDP target goes into URL.
 */
const MAX_MESSAGE_BYTES = 16_384; // Outbound commands: E gateway hard cap.
const MAX_SERVER_ANSWER_BYTES = 65_536; // Inbound authenticated SDP answer only.
const COMMAND_TYPES = new Set([
  "client.offer", "client.ice", "client.restartIce", "client.viewport", "client.ping",
  "browser.open", "browser.back", "browser.forward", "browser.reload", "browser.stop",
  "input.pointer", "input.wheel", "input.touch", "input.key",
  "viewport.resize", "reader.extract", "reader.resolvePoint",
  "reader.highlightSentence", "reader.highlightWord", "reader.scrollToSentence",
  "reader.clearHighlights", "audio.play", "audio.pause", "audio.stop",
  "session.stop", "client.displayTicket",
]);

export function cloudSignalSocketUrl(origin, sessionId) {
  if (typeof sessionId !== "string" || !/^[A-Za-z0-9_.:-]{1,128}$/.test(sessionId))
    throw new Error("SESSION_ID_INVALID");
  const u = new URL(origin);
  const local = ["localhost", "127.0.0.1", "[::1]"].includes(u.hostname);
  if (u.username || u.password || u.search || u.hash || u.pathname !== "/" ||
      (u.protocol !== "https:" && !(u.protocol === "http:" && local)))
    throw new Error("SIGNAL_ORIGIN_NOT_TRUSTED");
  u.protocol = u.protocol === "https:" ? "wss:" : "ws:";
  u.pathname = "/api/browser-v2/cloud/sessions/" + encodeURIComponent(sessionId) + "/signal";
  return u.href;
}

export class CloudSignalSocket {
  #socket;
  #openPromise;
  #closed = false;
  #workerReady = false;
  #lastViewport = null;
  // Only the shared WS connection can allocate outbound sequences. The RTC
  // transport and B's BrowserBackend both produce messages for this socket.
  #nextSequence = 0;
  #subscribers = new Set();
  #timer = null;
  #resumeTicket = null;
  #resumeExpiresAt = 0;
  #reconnectTimer = null;
  #reconnectAttempt = 0;

  constructor({
    origin, sessionId, socketTicket,
    resumeExpiresAtMs = null, initialSequence = 0,
    WebSocketImpl = globalThis.WebSocket, timeoutMs = 8000,
  } = {}) {
    if (typeof socketTicket !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(socketTicket))
      throw new Error("SIGNAL_TICKET_REQUIRED");
    this.protocols = ["floently.v2", "auth." + socketTicket];
    if (typeof WebSocketImpl !== "function") throw new Error("WEBSOCKET_UNAVAILABLE");
    if (!Number.isInteger(timeoutMs) || timeoutMs < 100 || timeoutMs > 60000)
      throw new Error("SIGNAL_TIMEOUT_INVALID");
    if (!Number.isSafeInteger(initialSequence) || initialSequence < 0)
      throw new Error("SIGNAL_SEQUENCE_INVALID");
    if (resumeExpiresAtMs !== null &&
        (!Number.isFinite(resumeExpiresAtMs) ||
         resumeExpiresAtMs <= Date.now() || resumeExpiresAtMs > Date.now() + 900_000))
      throw new Error("SIGNAL_RESUME_INVALID");
    this.#nextSequence = initialSequence;
    if (resumeExpiresAtMs !== null) {
      this.#resumeTicket = socketTicket;
      this.#resumeExpiresAt = resumeExpiresAtMs;
    }
    this.url = cloudSignalSocketUrl(origin, sessionId);
    this.sessionId = sessionId;
    this.timeoutMs = timeoutMs;
    this.WebSocketImpl = WebSocketImpl;
  }

  subscribe(listener) {
    if (typeof listener !== "function") throw new Error("SIGNAL_LISTENER_REQUIRED");
    if (this.#closed) throw new Error("SIGNAL_SOCKET_CLOSED");
    this.#subscribers.add(listener);
    // The E worker can become ready before React mounts its <video>.
    // Late RTC consumers still need the authenticated applied viewport.
    if (this.#lastViewport) listener(this.#lastViewport);
    return () => this.#subscribers.delete(listener);
  }

  #scheduleReconnect() {
    if (this.#closed || !this.#resumeTicket ||
        Date.now() >= this.#resumeExpiresAt || this.#reconnectTimer) return;
    const delay=Math.min(4000,250*(2**Math.min(4,this.#reconnectAttempt++)));
    this.#reconnectTimer=setTimeout(() => {
      this.#reconnectTimer=null;
      void this.connect().then(() => {
        this.#reconnectAttempt=0;
      }).catch(() => this.#scheduleReconnect());
    },delay);
  }

  async connect() {
    if (this.#closed) throw new Error("SIGNAL_SOCKET_CLOSED");
    if (this.#resumeTicket && Date.now() >= this.#resumeExpiresAt)
      throw new Error("SIGNAL_RESUME_EXPIRED");
    if (this.#socket?.readyState === 1) return;
    if (this.#openPromise) return this.#openPromise;
    const socket = new this.WebSocketImpl(this.url, this.protocols);
    this.#socket = socket;
    this.#openPromise = new Promise((resolve, reject) => {
      let done = false;
      const finish = (error) => {
        if (done) return;
        done = true;
        clearTimeout(this.#timer);
        this.#timer = null;
        if (error) reject(new Error(error));
        else resolve();
      };
      this.#timer = setTimeout(() => finish("CLOUD_SIGNALING_TIMEOUT"), this.timeoutMs);
      socket.addEventListener("open", () => {
        this.#reconnectAttempt=0;
        finish();
      });
      socket.addEventListener("error", () => finish("CLOUD_SIGNALING_FAILED"));
      socket.addEventListener("close", () => {
        if (this.#socket === socket) this.#socket = null;
        this.#workerReady = false;
        this.#lastViewport = null;
        finish("CLOUD_SIGNALING_CLOSED");
        this.#scheduleReconnect();
      });
      socket.addEventListener("message", event => {
        if (this.#closed || typeof event.data !== "string" ||
            event.data.length > MAX_SERVER_ANSWER_BYTES) return;
        let message;
        try { message = JSON.parse(event.data); } catch { return; }
        // Keep 16KiB for all other server events. The real multi-m-line
        // SDP answer can be larger without increasing client command limits.
        if (event.data.length > MAX_MESSAGE_BYTES &&
            message?.type !== "server.answer") return;
        if (!message || message.version !== "0.1" ||
            message.sessionId !== this.sessionId || !Number.isSafeInteger(message.sequence) ||
            message.sequence < 1 || typeof message.type !== "string" ||
            !message.type.startsWith("server.") ||
            !message.payload || typeof message.payload !== "object" ||
            Array.isArray(message.payload)) return;
        if (message.type === "server.event" &&
            message.payload?.event === "session.resumeTicket") {
          const ticket=message.payload?.ticket;
          const expires=message.payload?.expiresInSeconds;
          if (typeof ticket === "string" && /^[A-Za-z0-9_-]{43}$/.test(ticket) &&
              Number.isInteger(expires) && expires >= 30 && expires <= 900) {
            this.#resumeTicket=ticket;
            this.#resumeExpiresAt=Date.now()+expires*1000;
            this.protocols=["floently.v2","auth."+ticket];
          }
          return;
        }
        if (message.type === "server.viewportApplied") this.#lastViewport = message;
        if (message.type === "server.state" &&
            ["NEGOTIATING", "CONNECTED", "DISPLAY_WAITING"].includes(message.payload?.state))
          this.#workerReady = true;
        for (const listener of [...this.#subscribers]) listener(message);
      });
    }).finally(() => { this.#openPromise = null; });
    return this.#openPromise;
  }

  async waitForReady(timeoutMs = 20000) {
    if (this.#closed) throw new Error("SIGNAL_SOCKET_CLOSED");
    if (this.#workerReady) return;
    if (!Number.isInteger(timeoutMs) || timeoutMs < 100 || timeoutMs > 60000)
      throw new Error("SIGNAL_TIMEOUT_INVALID");
    let off = null, timer = null, settled = false;
    const ready = new Promise((resolve, reject) => {
      const complete = (error) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        off?.();
        error ? reject(new Error(error)) : resolve();
      };
      off = this.subscribe(message => {
        if (message.type === "server.state" &&
            ["NEGOTIATING", "CONNECTED", "DISPLAY_WAITING"].includes(message.payload?.state))
          complete();
        else if (message.type === "server.error")
          complete("ENGINE_START_FAILED");
      });
      timer = setTimeout(() => complete("ENGINE_START_TIMEOUT"), timeoutMs);
      void this.connect().catch(() => complete("CLOUD_SIGNALING_FAILED"));
    });
    return ready;
  }

  async send(envelope) {
    await this.sendWithSequence(envelope);
  }

  async sendWithSequence(envelope) {
    if (this.#closed) throw new Error("SIGNAL_SOCKET_CLOSED");
    if (!envelope || envelope.version !== "0.1" ||
        envelope.sessionId !== this.sessionId ||
        !Number.isSafeInteger(envelope.sequence) || envelope.sequence < 1 ||
        typeof envelope.type !== "string" || !COMMAND_TYPES.has(envelope.type) ||
        !envelope.payload || typeof envelope.payload !== "object" ||
        Array.isArray(envelope.payload)) throw new Error("SIGNAL_PROTOCOL_ERROR");
    // Validate caller size before network access, but assign the canonical
    // sequence only AFTER connect resolves: concurrent producers may await it
    // in a different order. E rejects replay or any non-monotonic sequence.
    if (JSON.stringify(envelope).length > MAX_MESSAGE_BYTES)
      throw new Error("SIGNAL_PROTOCOL_ERROR");
    await this.connect();
    if (this.#closed || this.#socket?.readyState !== 1) throw new Error("CLOUD_SIGNALING_CLOSED");
    const sequence=++this.#nextSequence;
    const wire = JSON.stringify({ ...envelope, sequence });
    if (wire.length > MAX_MESSAGE_BYTES) throw new Error("SIGNAL_PROTOCOL_ERROR");
    this.#socket.send(wire);
    // Return the canonical on-wire sequence so callers can correlate a
    // server-side acknowledgement with the exact command that was accepted.
    return sequence;
  }

  /**
   * Page teardown cannot await the normal reconnect/send path. Queue exactly
   * one owner-authenticated stop frame on the ALREADY OPEN socket so a full
   * Read-page reload does not strand Chromium in the single-owner slot.
   * Never reconnect and never persist a resume capability here.
   */
  trySendSessionStopNow() {
    if (this.#closed || this.#socket?.readyState !== 1) return false;
    const sequence=++this.#nextSequence;
    const wire=JSON.stringify({
      version:"0.1",sessionId:this.sessionId,sequence,
      type:"session.stop",payload:{},
    });
    if (wire.length > MAX_MESSAGE_BYTES) return false;
    try { this.#socket.send(wire); return true; }
    catch { return false; }
  }

  exportResumeState() {
    if (this.#closed || !this.#resumeTicket ||
        Date.now() >= this.#resumeExpiresAt) return null;
    return Object.freeze({
      ticket:this.#resumeTicket,
      expiresAtMs:this.#resumeExpiresAt,
      nextSequence:this.#nextSequence,
    });
  }

  close() {
    if (this.#closed) return;
    this.#closed = true;
    this.#workerReady = false;
    this.#lastViewport = null;
    clearTimeout(this.#timer);
    clearTimeout(this.#reconnectTimer);
    this.#timer = null;
    this.#reconnectTimer = null;
    this.#resumeTicket = null;
    this.#resumeExpiresAt = 0;
    this.#subscribers.clear();
    try { this.#socket?.close(); } catch {}
    this.#socket = null;
  }
}
