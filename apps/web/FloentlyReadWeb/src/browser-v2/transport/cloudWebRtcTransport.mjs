/**
 * Floently Browser V2 web media + gesture transport.
 * Platform-neutral E/B integration seam; NEVER exposes CDP to a browser client.
 *
 * The authenticated edge/Agent E supplies an owner-scoped signaling transport:
 *   { send(envelope): Promise<void>|void, subscribe(onEnvelope): unsubscribe }
 * No signaling URL or identity is taken from a page URL/query parameter.
 * This is not a BrowserBackend: navigation, tabs and semantic Reader remain
 * separate authenticated operations, supplied by E and mounted by B.
 */
export const BROWSER_V2_WIRE_VERSION = "0.1";

const MAX_SIGNAL_BYTES = 64 * 1024;
const SAFE_ERRORS = new Set(["SIGNAL_AUTH_FAILED", "SIGNAL_PROTOCOL_ERROR", "ICE_FAILED",
  "TURN_FAILED", "MEDIA_NEGOTIATION_FAILED", "MEDIA_STALLED", "INPUT_CHANNEL_FAILED",
  "VIEWPORT_STALE", "WORKER_GONE", "RECONNECT_TIMEOUT"]);
const clamp = (value, lo, hi) => Math.max(lo, Math.min(hi, value));
// Fixed WebRTC enum values only; never surface candidate/SDP/selected pair/IP.
const PEER_ICE_STATES = new Set(["new","checking","connected","completed",
  "disconnected","failed","closed"]);
const PEER_CONNECTION_STATES = new Set(["new","connecting","connected",
  "disconnected","failed","closed"]);
const safeEnum = (value, allowed) => allowed.has(value) ? value : "unknown";
const SAFE_DTLS = new Set(["new","connecting","connected","closed","failed"]);
const SAFE_ICE_PAIR = new Set(["frozen","waiting","in-progress","failed","succeeded"]);
const SAFE_TRACK = new Set(["live","ended","muted","not-received"]);
const SAFE_CANDIDATE_TYPE = new Set(["host","srflx","prflx","relay"]);
const nonnegative = value => Number.isFinite(value) && value >= 0 ?
  Math.min(Math.floor(value), 1_000_000_000) : 0;

function validSessionId(id) {
  return typeof id === "string" && /^[a-zA-Z0-9_.:-]{1,128}$/.test(id);
}

function validViewport(value) {
  if (!value || typeof value !== "object") return false;
  const fields = ["revision", "cssWidth", "cssHeight", "mediaWidth", "mediaHeight"];
  return fields.every(key => Number.isSafeInteger(value[key]) && value[key] > 0) &&
    value.cssWidth >= 320 && value.cssHeight >= 360 &&
    value.mediaWidth <= 2560 && value.mediaHeight <= 1600 &&
    Number.isFinite(value.effectiveDpr) && value.effectiveDpr >= 1 && value.effectiveDpr <= 2;
}

export function browserVideoPoint(video, viewport, clientX, clientY) {
  if (!validViewport(viewport)) return null;
  const bounds = video.getBoundingClientRect();
  if (!(bounds.width > 0 && bounds.height > 0)) return null;
  const frameWidth = video.videoWidth || viewport.mediaWidth;
  const frameHeight = video.videoHeight || viewport.mediaHeight;
  if (!(frameWidth > 0 && frameHeight > 0) ||
      Math.abs(frameWidth - viewport.mediaWidth) > 1 ||
      Math.abs(frameHeight - viewport.mediaHeight) > 1) return null;
  const scale = Math.min(bounds.width / frameWidth, bounds.height / frameHeight);
  const width = frameWidth * scale, height = frameHeight * scale;
  const left = bounds.left + (bounds.width - width) / 2;
  const top = bounds.top + (bounds.height - height) / 2;
  if (clientX < left || clientY < top || clientX >= left + width || clientY >= top + height) return null;
  return { x: clamp(Math.floor((clientX - left) / width * viewport.cssWidth), 0, viewport.cssWidth - 1),
    y: clamp(Math.floor((clientY - top) / height * viewport.cssHeight), 0, viewport.cssHeight - 1) };
}

/** Signal payloads only; raw media and private CDP target IDs never leave worker. */
export class CloudWebRtcTransport {
  #peer;
  #signal;
  #video;
  #offSignal = null;
  #channels = new Map();
  #sequence = { signal: 0, "browser.control": 0, "browser.motion": 0 };
  #remoteSequence = 0;
  #viewport = null;
  #videoReady = false;
  // After a dropped peer, video.readyState and dimensions may still describe
  // the LAST frozen decoded frame. Require a NEW rVFC callback on recovery.
  #freshFrameRequired = false;
  #frameGeneration = 0;
  #announcedReady = false;
  #suppressClickUntil = 0;
  #stopped = false;
  #frameCallbackId = null;
  #listeners = [];
  #gestureOffs = [];
  #lastTouch = null;
  #pendingIce = [];
  #remoteDescriptionReady = false;
  #remoteAnswer = "not-received";
  #remoteTrack = "not-received";
  #inputFallback = null;
  #fallbackActive = false;
  #fallbackFailed = false;
  #lastFallbackMoveAt = 0;

  constructor({ sessionId, signaling, video, iceServers = [], relayOnly = false,
    rtcFactory = (servers, requireRelay) => new RTCPeerConnection({
      iceServers:servers,iceTransportPolicy:requireRelay?"relay":"all",
    }),
    onStatus = () => {}, onDecodedFrame = () => {},
    onInputFallback = null } = {}) {
    if (!validSessionId(sessionId)) throw new Error("SESSION_ID_INVALID");
    if (!signaling || typeof signaling.send !== "function" ||
        typeof signaling.subscribe !== "function") throw new Error("SIGNALING_TRANSPORT_REQUIRED");
    if (!video || typeof video.addEventListener !== "function" ||
        typeof video.getBoundingClientRect !== "function") throw new Error("VIDEO_ELEMENT_REQUIRED");
    if (typeof rtcFactory !== "function") throw new Error("RTC_FACTORY_REQUIRED");
    this.sessionId = sessionId;
    this.#signal = signaling;
    this.#video = video;
    if (!Array.isArray(iceServers) || iceServers.length>3 ||
        typeof relayOnly!=="boolean" ||
        (relayOnly && (iceServers.length!==1 ||
          typeof iceServers[0]?.urls!=="string" ||
          !/^turn:[A-Za-z0-9.-]+:[0-9]{2,5}\?transport=tcp$/.test(iceServers[0].urls))))
      throw new Error("ICE_CONFIG_INVALID");
    this.#peer = rtcFactory(iceServers,relayOnly);
    if (onInputFallback !== null && typeof onInputFallback !== "function")
      throw new Error("INPUT_FALLBACK_INVALID");
    this.onStatus = onStatus;
    this.onDecodedFrame = onDecodedFrame;
    this.#inputFallback = onInputFallback;
    this.#peer.ontrack = event => {
      if (this.#stopped || event.track?.kind !== "video") return;
      this.#videoReady = false;
      this.#announcedReady = false;
      this.#frameGeneration++;
      this.#remoteTrack = event.track.muted ? "muted" : event.track.readyState === "live" ?
        "live" : "ended";
      event.track.onmute = () => {
        if (this.#stopped) return;
        this.#remoteTrack = "muted";
        this.#invalidateDecodedFrame();
        this.onStatus("CLOUD_RECONNECTING");
      };
      event.track.onunmute = () => {
        if (!this.#stopped) this.#remoteTrack = "live";
        // Unmute does NOT prove a newly decoded frame.
      };
      event.track.onended = () => {
        if (this.#stopped) return;
        this.#remoteTrack = "ended";
        this.#invalidateDecodedFrame();
        this.onStatus("MEDIA_STALLED");
      };
      for (const off of this.#listeners.splice(0)) off();
      if (this.#frameCallbackId !== null && this.#video.cancelVideoFrameCallback)
        this.#video.cancelVideoFrameCallback(this.#frameCallbackId);
      this.#frameCallbackId = null;
      const stream = event.streams?.[0] ||
        (typeof MediaStream !== "undefined" ? new MediaStream([event.track]) : null);
      if (!stream) { this.onStatus("MEDIA_NEGOTIATION_FAILED"); return; }
      this.#video.srcObject = stream;
      this.#video.playsInline = true;
      this.#video.autoplay = true;
      this.#watchFirstFrame();
      // Browser autoplay may require a real user gesture; never mark ready on track alone.
      try {
        const playing = this.#video.play();
        if (playing && typeof playing.catch === "function")
          void playing.catch(() => { if (!this.#stopped) this.onStatus("MEDIA_AUTOPLAY_BLOCKED"); });
      } catch { this.onStatus("MEDIA_AUTOPLAY_BLOCKED"); }
    };
    this.#peer.onicecandidate = event => {
      if (this.#stopped || !event.candidate) return;
      void this.#sendSignal("client.ice", { candidate: event.candidate.toJSON ?
        event.candidate.toJSON() : event.candidate }).catch(() => this.onStatus("ICE_FAILED"));
    };
    this.#peer.oniceconnectionstatechange = () => {
      if (this.#stopped) return;
      const state = safeEnum(this.#peer.iceConnectionState, PEER_ICE_STATES);
      this.onStatus("ICE_STATE_" + state.toUpperCase());
      if (state === "failed" || state === "disconnected" || state === "closed") {
        this.#invalidateDecodedFrame();
        this.onStatus(state === "failed" ? "ICE_FAILED" : "CLOUD_RECONNECTING");
      } else if (state === "checking" && this.#videoReady) {
        // An ICE restart can transition through checking without emitting
        // disconnected, so old decoded-frame proof must be revoked here too.
        this.#invalidateDecodedFrame();
        this.onStatus("CLOUD_RECONNECTING");
      } else if (state === "connected" || state === "completed") {
        this.#notifyVideoState();
      }
    };
    this.#peer.onconnectionstatechange = () => {
      if (this.#stopped) return;
      const state = this.#peer.connectionState;
      if (state === "failed" || state === "disconnected" || state === "closed") {
        this.#invalidateDecodedFrame();
        this.onStatus(state === "failed" ? "ICE_FAILED" : "CLOUD_RECONNECTING");
      } else if (state === "connecting" && this.#videoReady) {
        this.#invalidateDecodedFrame();
        this.onStatus("CLOUD_RECONNECTING");
      } else if (state === "connected") this.#notifyVideoState();
    };
    this.#channels.set("browser.control", this.#peer.createDataChannel("browser.control",
      { ordered: true }));
    this.#channels.set("browser.motion", this.#peer.createDataChannel("browser.motion",
      { ordered: false, maxRetransmits: 0 }));
    for (const channel of this.#channels.values()) {
      channel.onclose = () => {
        if (this.#stopped) return;
        // A lost SCTP datachannel does not imply lost decoded video. The
        // authenticated owner WebSocket can carry bounded input commands
        // over the EXISTING private E gateway without touching CDP publicly.
        if (this.#inputFallback) {
          this.#fallbackActive = true;
          this.onStatus("INPUT_AUTHENTICATED_FALLBACK");
        } else this.onStatus("INPUT_CHANNEL_FAILED");
      };
    }
    this.#offSignal = signaling.subscribe(raw => { void this.#receive(raw).catch(() => {
      if (!this.#stopped) this.onStatus("SIGNAL_PROTOCOL_ERROR");
    }); });
  }

  #invalidateDecodedFrame() {
    this.#lastTouch = null;
    this.#videoReady = false;
    this.#freshFrameRequired = true;
    this.#announcedReady = false;
    // This invalidates media/input/read authorization even when Chromium's
    // last frame remains visually painted in the HTMLVideoElement.
  }
  get decodedFrameReady() {
    return this.#videoReady && !this.#freshFrameRequired && !this.#stopped &&
      this.#peer.connectionState === "connected" && validViewport(this.#viewport) &&
      Math.abs(this.#video.videoWidth - this.#viewport.mediaWidth) <= 1 &&
      Math.abs(this.#video.videoHeight - this.#viewport.mediaHeight) <= 1;
  }

  #notifyVideoState() {
    const ready = this.decodedFrameReady;
    if (ready !== this.#announcedReady) {
      this.#announcedReady = ready;
      this.onStatus(ready ? "MEDIA_CONNECTED" : "MEDIA_WAITING_FOR_FRAME");
    }
  }
  get appliedViewport() { return this.#viewport ? { ...this.#viewport } : null; }
  get safePeerState() {
    return Object.freeze({
      remoteAnswer: this.#remoteAnswer,
      iceConnectionState: safeEnum(this.#peer.iceConnectionState, PEER_ICE_STATES),
      connectionState: safeEnum(this.#peer.connectionState, PEER_CONNECTION_STATES),
    });
  }

  // A cumulative framesDecoded statistic cannot prove the owner can CLICK.
  // This is bounded, enum/boolean-only live state with no CDP, ICE, SDP,
  // browsing content or credentials.
  get safeInputState() {
    const control = this.#channels.get("browser.control");
    const motion = this.#channels.get("browser.motion");
    const viewportMatches = validViewport(this.#viewport) &&
      Math.abs(this.#video.videoWidth - this.#viewport.mediaWidth) <= 1 &&
      Math.abs(this.#video.videoHeight - this.#viewport.mediaHeight) <= 1;
    return Object.freeze({
      browserControlOpen: control?.readyState === "open",
      browserMotionOpen: motion?.readyState === "open",
      peerConnected: this.#peer.connectionState === "connected",
      videoElementReady: this.#video.readyState >= 2,
      videoProof: this.#videoReady,
      freshFrameRequired: this.#freshFrameRequired,
      viewportMatches,
      authenticatedFallback: this.#fallbackActive && !this.#fallbackFailed,
      fallbackFailed: this.#fallbackFailed,
      inputReady: this.decodedFrameReady && (
        (control?.readyState === "open" &&
         motion?.readyState === "open" && !this.#fallbackActive) ||
        (this.#fallbackActive && !this.#fallbackFailed &&
         typeof this.#inputFallback === "function")
      ),
    });
  }

  /**
   * One bounded, privacy-safe snapshot for diagnosing ICE-connected but
   * never-decoded sessions. No SDP, ICE addresses/ports, candidate IDs,
   * cryptographic material, browsing URLs, or individual packet contents.
   */
  async safeMediaStats() {
    if (this.#stopped || typeof this.#peer.getStats !== "function") return null;
    try {
      const reports = await this.#peer.getStats();
      let transport = null, pair = null, inbound = null;
      const values = [...reports.values()];
      for (const value of values) {
        if (value.type === "transport" && !transport) transport = value;
        if (value.type === "candidate-pair" &&
            value.state === "succeeded" && value.nominated) pair = value;
        if (value.type === "inbound-rtp" &&
            (value.kind === "video" || value.mediaType === "video")) inbound = value;
      }
      if (transport?.selectedCandidatePairId) {
        const chosen = reports.get(transport.selectedCandidatePairId);
        if (chosen?.type === "candidate-pair") pair = chosen;
      }
      const localCandidate = pair?.localCandidateId && reports.get(pair.localCandidateId);
      const remoteCandidate = pair?.remoteCandidateId && reports.get(pair.remoteCandidateId);
      const candidateType = candidate => candidate?.type === "local-candidate" ||
        candidate?.type === "remote-candidate" ?
        safeEnum(candidate.candidateType, SAFE_CANDIDATE_TYPE) : "unknown";
      return Object.freeze({
        remoteAnswer: this.#remoteAnswer,
        ice: safeEnum(this.#peer.iceConnectionState, PEER_ICE_STATES),
        connection: safeEnum(this.#peer.connectionState, PEER_CONNECTION_STATES),
        dtls: safeEnum(transport?.dtlsState, SAFE_DTLS),
        pair: safeEnum(pair?.state, SAFE_ICE_PAIR),
        localType: candidateType(localCandidate),
        remoteType: candidateType(remoteCandidate),
        rxPackets: nonnegative(inbound?.packetsReceived),
        rxBytes: nonnegative(inbound?.bytesReceived),
        rxLost: nonnegative(inbound?.packetsLost),
        rxPli: nonnegative(inbound?.pliCount),
        rxNack: nonnegative(inbound?.nackCount),
        framesReceived: nonnegative(inbound?.framesReceived),
        framesDecoded: nonnegative(inbound?.framesDecoded),
        keyFramesDecoded: nonnegative(inbound?.keyFramesDecoded),
        jitterFrames: nonnegative(inbound?.jitterBufferEmittedCount),
        codec: inbound?.codecId && reports.get(inbound.codecId)?.mimeType === "video/VP8"
          ? "VP8" : "unknown",
        track: safeEnum(this.#remoteTrack, SAFE_TRACK),
        videoReadyState: nonnegative(this.#video.readyState),
        videoWidth: nonnegative(this.#video.videoWidth),
        videoHeight: nonnegative(this.#video.videoHeight),
      });
    } catch { return null; }
  }

  #sendSignal(type, payload) {
    if (this.#stopped) throw new Error("SESSION_GONE");
    return Promise.resolve(this.#signal.send({
      version: BROWSER_V2_WIRE_VERSION, sessionId: this.sessionId,
      sequence: ++this.#sequence.signal, type, payload,
    }));
  }

  async start() {
    if (this.#stopped) throw new Error("SESSION_GONE");
    // E creates sandboxed Chromium and the GStreamer sender asynchronously.
    // Do not flood its four-message pre-ready buffer with ICE candidates.
    if (typeof this.#signal.waitForReady === "function")
      await this.#signal.waitForReady();
    if (this.#stopped) throw new Error("SESSION_GONE");
    // A data-channel-only offer has no m=video: GStreamer cannot add a
    // video track in its ANSWER. Advertise a real recvonly transceiver
    // before generating SDP so E can send the captured Chromium VP8 frames.
    this.#peer.addTransceiver("video", { direction: "recvonly" });
    const offer = await this.#peer.createOffer();
    await this.#peer.setLocalDescription(offer);
    await this.#sendSignal("client.offer", {
      sdp: this.#peer.localDescription?.sdp || offer.sdp,
      type: "offer",
    });
    this.onStatus("MEDIA_NEGOTIATING");
  }

  async #receive(raw) {
    if (this.#stopped) return;
    if (typeof raw === "string" && raw.length > MAX_SIGNAL_BYTES) throw new Error("SIGNAL_PROTOCOL_ERROR");
    const msg = typeof raw === "string" ? JSON.parse(raw) : raw;
    if (!msg || msg.version !== BROWSER_V2_WIRE_VERSION ||
        msg.sessionId !== this.sessionId || !Number.isSafeInteger(msg.sequence) ||
        msg.sequence <= this.#remoteSequence || typeof msg.type !== "string" ||
        !msg.payload || typeof msg.payload !== "object") return;
    this.#remoteSequence = msg.sequence;
    if (msg.type === "server.answer") {
      if (this.#remoteDescriptionReady || msg.payload.type !== "answer" ||
          typeof msg.payload.sdp !== "string" || msg.payload.sdp.length > MAX_SIGNAL_BYTES)
        throw new Error("SIGNAL_PROTOCOL_ERROR");
      this.#remoteAnswer = "applying";
      try {
        await this.#peer.setRemoteDescription({ type: "answer", sdp: msg.payload.sdp });
      } catch {
        this.#remoteAnswer = "failed";
        this.onStatus("REMOTE_DESCRIPTION_FAILED");
        return;
      }
      this.#remoteDescriptionReady = true;
      this.#remoteAnswer = "applied";
      this.onStatus("REMOTE_DESCRIPTION_APPLIED");
      for (const candidate of this.#pendingIce.splice(0)) await this.#peer.addIceCandidate(candidate);
    } else if (msg.type === "server.ice") {
      const candidate = msg.payload.candidate;
      if (candidate && typeof candidate.candidate === "string") {
        if (this.#remoteDescriptionReady) await this.#peer.addIceCandidate(candidate);
        else if (this.#pendingIce.length < 64) this.#pendingIce.push(candidate);
      }
    } else if (msg.type === "server.viewportApplied") {
      if (!validViewport(msg.payload)) throw new Error("VIEWPORT_STALE");
      if (!this.#viewport || msg.payload.revision > this.#viewport.revision) {
        this.#viewport = { ...msg.payload };
        this.#notifyVideoState();
      }
    } else if (msg.type === "server.error") {
      const safeCode = msg.payload.code;
      this.onStatus(SAFE_ERRORS.has(safeCode) ? safeCode : "SIGNAL_PROTOCOL_ERROR");
    } else if (msg.type === "server.state") {
      if (typeof msg.payload.state === "string" &&
          ["NEW","ALLOCATING","NEGOTIATING","CONNECTED","RECONNECTING","DEGRADED","FAILED","STOPPING","STOPPED"].includes(msg.payload.state))
        this.onStatus(msg.payload.state);
    }
  }

  #watchFirstFrame() {
    const generation = this.#frameGeneration;
    const check = (freshFrame = false) => {
      if (this.#stopped || generation !== this.#frameGeneration ||
          this.#video.readyState < 2 || !this.#video.videoWidth ||
          !this.#video.videoHeight) return;
      if (this.#freshFrameRequired) {
        // loadeddata/readyState can be stale after a transient ICE outage.
        // Only a NEW decoded callback WHILE connected can restore readiness.
        if (!freshFrame || this.#peer.connectionState !== "connected") return;
        this.#freshFrameRequired = false;
      }
      if (!this.#videoReady) {
        this.#videoReady = true;
        this.onDecodedFrame({ width: this.#video.videoWidth, height: this.#video.videoHeight });
      }
      this.#notifyVideoState();
    };
    const onLoaded = () => check(false);
    this.#video.addEventListener("loadeddata", onLoaded);
    this.#listeners.push(() => this.#video.removeEventListener("loadeddata", onLoaded));
    if (typeof this.#video.requestVideoFrameCallback === "function") {
      const callback = () => {
        if (generation !== this.#frameGeneration || this.#stopped) return;
        this.#frameCallbackId = null;
        check(true);
        if (!this.#stopped && generation === this.#frameGeneration)
          this.#frameCallbackId = this.#video.requestVideoFrameCallback(callback);
      };
      this.#frameCallbackId = this.#video.requestVideoFrameCallback(callback);
    }
    check(false);
  }

  async requestViewport({ cssWidth, cssHeight, devicePixelRatio = 1, visibility = "visible" } = {}) {
    if (![cssWidth, cssHeight].every(v => Number.isSafeInteger(v) && v > 0) ||
        !Number.isFinite(devicePixelRatio) || devicePixelRatio < 1 || devicePixelRatio > 2)
      throw new Error("VIEWPORT_INVALID");
    await this.#sendSignal("client.viewport", { cssWidth, cssHeight, devicePixelRatio, visibility });
  }

  #input(channel, type, payload) {
    if (!this.decodedFrameReady || !this.#viewport || this.#stopped) return false;
    const wire = this.#channels.get(channel);
    const revision = this.#viewport.revision;
    // REAL V2 routes all owner input over its already-authenticated WSS
    // command lane. Datachannel and WebSocket sequence counters are
    // INDEPENDENT, while older E workers share an InputSequencer for both.
    // Mixing the lanes causes valid clicks to be dropped after WS keys or
    // a datachannel outage. Do not send input via both paths in one session.
    if (!this.#inputFallback && !this.#fallbackActive &&
        wire?.readyState === "open") {
      try {
        wire.send(JSON.stringify({ version: BROWSER_V2_WIRE_VERSION,
          sequence: ++this.#sequence[channel], viewportRevision: revision, type, payload }));
        return true;
      } catch {
        // A synchronous send failure must never strand an otherwise live
        // media session. Do not re-use the datachannel once WS fallback starts:
        // each transport has an independent anti-replay sequence space.
        this.#fallbackActive = true;
      }
    }
    if (typeof this.#inputFallback !== "function" || this.#fallbackFailed) return false;
    this.#fallbackActive = true;
    // The canonical owner gateway allows <=120 commands/sec. Never send an
    // unrestricted mousemove flood; clicks and touch endings are not dropped.
    const at=performance.now();
    if (type === "pointerMove") return true; // Hover is not needed to click.
    if ((type === "touchMove" || type === "wheel") &&
        at-this.#lastFallbackMoveAt<45) return true;
    if (type === "touchMove" || type === "wheel")
      this.#lastFallbackMoveAt=at;
    try {
      void Promise.resolve(this.#inputFallback(type,payload,revision)).catch(()=>{
        this.#fallbackFailed=true;
        this.onStatus("INPUT_WS_FALLBACK_FAILED");
      });
      return true;
    } catch {
      this.#fallbackFailed=true;
      this.onStatus("INPUT_WS_FALLBACK_FAILED");
      return false;
    }
  }

  /** One-finger touch is forwarded as real remote touch, not a CSS page scroll. */
  bindGestures(element = this.#video) {
    if (!element || typeof element.addEventListener !== "function")
      throw new Error("INPUT_SURFACE_REQUIRED");
    this.#unbindGestures();
    if (element.style) {
      const previous = element.style.touchAction;
      element.style.touchAction = "none";
      this.#gestureOffs.push(() => { element.style.touchAction = previous; });
    }
    const listen = (type, handler, options) => {
      element.addEventListener(type, handler, options);
      this.#gestureOffs.push(() => element.removeEventListener(type, handler, options));
    };
    const point = event => browserVideoPoint(this.#video, this.#viewport, event.clientX, event.clientY);
    const down = event => {
      if (event.pointerType !== "touch" || this.#lastTouch) return;
      const p = point(event);
      if (!p) return;
      if (!this.#input("browser.control", "touchStart", { ...p, touchId: 0 })) return;
      this.#lastTouch = p;
      try { element.setPointerCapture?.(event.pointerId); } catch {}
      event.preventDefault();
    };
    const move = event => {
      if (event.pointerType !== "touch" || !this.#lastTouch) return;
      const p = point(event) || this.#lastTouch;
      if (this.#input("browser.motion", "touchMove", { ...p, touchId: 0 })) this.#lastTouch = p;
      event.preventDefault();
    };
    const up = event => {
      if (event.pointerType !== "touch" || !this.#lastTouch) return;
      const p = point(event) || this.#lastTouch;
      this.#input("browser.control", "touchEnd", { ...p, touchId: 0 });
      this.#lastTouch = null;
      this.#suppressClickUntil = performance.now() + 650;
      try { element.releasePointerCapture?.(event.pointerId); } catch {}
      event.preventDefault();
    };
    const wheel = event => {
      if (!point(event)) return;
      if (this.#input("browser.motion", "wheel", { deltaX: event.deltaX, deltaY: event.deltaY }))
        event.preventDefault();
    };
    const click = event => {
      if (event.pointerType === "touch" || performance.now() < this.#suppressClickUntil) return;
      const p = point(event);
      if (p && this.#input("browser.control", "click", { ...p, button: event.button === 2 ? "right" : "left" }))
        event.preventDefault();
    };
    listen("pointerdown", down); listen("pointermove", move);
    listen("pointerup", up); listen("pointercancel", up);
    listen("wheel", wheel, { passive: false }); listen("click", click);
    return () => this.#unbindGestures();
  }

  #unbindGestures() {
    this.#lastTouch = null;
    for (const off of this.#gestureOffs.splice(0)) off();
  }

  async resumePlaybackFromUserGesture() {
    if (this.#stopped) return false;
    try { await this.#video.play(); return true; } catch {
      this.onStatus("MEDIA_AUTOPLAY_BLOCKED"); return false;
    }
  }

  async stop() {
    if (this.#stopped) return;
    this.#stopped = true;
    this.#videoReady = false;
    this.#freshFrameRequired = true;
    this.#frameGeneration++;
    this.#unbindGestures();
    for (const off of this.#listeners.splice(0)) off();
    if (this.#frameCallbackId !== null && this.#video.cancelVideoFrameCallback)
      this.#video.cancelVideoFrameCallback(this.#frameCallbackId);
    this.#offSignal?.();
    this.#offSignal = null;
    for (const channel of this.#channels.values()) { try { channel.close(); } catch {} }
    try { this.#peer.close(); } catch {}
    this.#video.srcObject = null;
  }
}
