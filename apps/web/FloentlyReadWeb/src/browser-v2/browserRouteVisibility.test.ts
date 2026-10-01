import { describe, expect, it } from "vitest";
import { CloudWebRtcTransport } from "./transport/cloudWebRtcTransport.mjs";

class FakeVideo {
  listeners = new Map<string, Set<(event?: unknown) => void>>();
  videoWidth = 1280;
  videoHeight = 720;
  readyState = 0;
  srcObject: unknown = null;
  style = { touchAction: "auto" };
  firstFrame: (() => void) | null = null;

  addEventListener(name: string, listener: (event?: unknown) => void) {
    if (!this.listeners.has(name)) {
      this.listeners.set(name, new Set());
    }
    this.listeners.get(name)?.add(listener);
  }

  removeEventListener(name: string, listener: (event?: unknown) => void) {
    this.listeners.get(name)?.delete(listener);
  }

  fire(name: string, event: unknown = {}) {
    for (const listener of this.listeners.get(name) ?? []) {
      listener(event);
    }
  }

  getBoundingClientRect() {
    return { left: 0, top: 0, width: 390, height: 300 };
  }

  play() {
    return Promise.resolve();
  }

  requestVideoFrameCallback(callback: () => void) {
    this.firstFrame = callback;
    return 1;
  }

  cancelVideoFrameCallback() {
    this.firstFrame = null;
  }

  loadFrame() {
    this.readyState = 2;
    this.fire("loadeddata");
  }
}

function fixture() {
  const video = new FakeVideo();
  let receiver:
    | ((message: {
        version: "0.1";
        sessionId: string;
        sequence: number;
        type: string;
        payload: Record<string, unknown>;
      }) => void)
    | null = null;

  const sent: Array<{
    type: string;
    payload: Record<string, unknown>;
  }> = [];

  const peer = {
    connectionState: "connecting",
    iceConnectionState: "new",
    addTransceiver() {
      return {};
    },
    createDataChannel() {
      return {
        readyState: "open",
        send() {},
        close() {},
      };
    },
    async createOffer() {
      return { type: "offer", sdp: "fixture-offer" };
    },
    async setLocalDescription() {},
    async setRemoteDescription() {},
    async addIceCandidate() {},
    close() {
      this.connectionState = "closed";
    },
  };

  const transport = new CloudWebRtcTransport({
    sessionId: "owned-123",
    signaling: {
      send(message) {
        sent.push(message);
      },
      subscribe(listener) {
        receiver = listener;
        return () => {
          receiver = null;
        };
      },
    },
    video: video as unknown as HTMLVideoElement,
    rtcFactory: () => peer as unknown as RTCPeerConnection,
  });

  const server = (
    type: string,
    payload: Record<string, unknown>,
    sequence: number,
  ) => {
    receiver?.({
      version: "0.1",
      sessionId: "owned-123",
      sequence,
      type,
      payload,
    });
  };

  const connect = () => {
    peer.connectionState = "connected";
    (
      peer as unknown as {
        onconnectionstatechange: () => void;
        ontrack: (event: unknown) => void;
      }
    ).onconnectionstatechange();

    (
      peer as unknown as {
        ontrack: (event: unknown) => void;
      }
    ).ontrack({
      track: {
        kind: "video",
        muted: false,
        readyState: "live",
      },
      streams: [{}],
    });
  };

  return { video, peer, sent, transport, server, connect };
}

describe("Browser V2 route visibility", () => {
  it("requires a fresh visible frame after the browser route returns", async () => {
    const f = fixture();

    f.connect();
    f.video.loadFrame();
    f.server(
      "server.viewportApplied",
      {
        revision: 1,
        cssWidth: 1280,
        cssHeight: 720,
        mediaWidth: 1280,
        mediaHeight: 720,
        effectiveDpr: 1,
      },
      1,
    );

    expect(f.transport.decodedFrameReady).toBe(true);

    await f.transport.requestViewport({
      cssWidth: 1280,
      cssHeight: 720,
      devicePixelRatio: 1,
      visibility: "hidden",
    });
    expect(f.transport.decodedFrameReady).toBe(false);

    // A frame callback while the Browser route is hidden cannot re-authorize
    // page input/read-from-here.
    f.video.firstFrame?.();
    expect(f.transport.decodedFrameReady).toBe(false);

    await f.transport.requestViewport({
      cssWidth: 1280,
      cssHeight: 720,
      devicePixelRatio: 1,
      visibility: "visible",
    });
    expect(f.transport.decodedFrameReady).toBe(false);

    f.video.firstFrame?.();
    expect(f.transport.decodedFrameReady).toBe(true);

    expect(
      f.sent.filter((message) => message.type === "client.viewport")
        .map((message) => message.payload.visibility),
    ).toEqual(["hidden", "visible"]);

    await f.transport.stop();
  });

  it("rejects invalid visibility before signaling it", async () => {
    const f = fixture();

    await expect(
      f.transport.requestViewport({
        cssWidth: 1280,
        cssHeight: 720,
        devicePixelRatio: 1,
        visibility: "background" as "visible",
      }),
    ).rejects.toThrow("VIEWPORT_INVALID");

    expect(
      f.sent.some((message) => message.type === "client.viewport"),
    ).toBe(false);

    await f.transport.stop();
  });
});
