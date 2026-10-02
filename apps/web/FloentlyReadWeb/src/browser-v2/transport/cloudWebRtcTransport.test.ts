import { describe, expect, it } from "vitest";
import { browserVideoPoint } from "./cloudWebRtcTransport.mjs";

const viewport = {
  revision: 7,
  cssWidth: 1_200,
  cssHeight: 800,
  mediaWidth: 2_400,
  mediaHeight: 1_600,
  effectiveDpr: 2,
};

function video({
  videoWidth = 2_400,
  videoHeight = 1_600,
  left = 0,
  top = 0,
  width = 1_000,
  height = 800,
}: Partial<{
  videoWidth: number;
  videoHeight: number;
  left: number;
  top: number;
  width: number;
  height: number;
}> = {}): HTMLVideoElement {
  return {
    videoWidth,
    videoHeight,
    getBoundingClientRect: () => ({
      x: left,
      y: top,
      left,
      top,
      width,
      height,
      right: left + width,
      bottom: top + height,
      toJSON: () => ({}),
    }),
  } as unknown as HTMLVideoElement;
}

describe("Browser V2 video coordinate safety", () => {
  it("maps points through the contained video frame into canonical CSS coordinates", () => {
    expect(
      browserVideoPoint(video(), viewport, 500, 400),
    ).toEqual({ x: 600, y: 400 });
  });

  it("fails closed when decoded frame dimensions do not match the applied viewport", () => {
    expect(
      browserVideoPoint(
        video({ videoWidth: 1_920, videoHeight: 1_080 }),
        viewport,
        500,
        400,
      ),
    ).toBeNull();
  });

  it("rejects pointer input in letterbox space outside the rendered frame", () => {
    // The 3:2 video is vertically centered inside this 5:4 box.
    expect(browserVideoPoint(video(), viewport, 500, 20)).toBeNull();
    expect(browserVideoPoint(video(), viewport, 500, 790)).toBeNull();
  });

  it("rejects invalid or stale viewport proof before mapping input", () => {
    expect(
      browserVideoPoint(
        video(),
        { ...viewport, revision: 0 },
        500,
        400,
      ),
    ).toBeNull();

    expect(
      browserVideoPoint(
        video(),
        { ...viewport, effectiveDpr: 3 },
        500,
        400,
      ),
    ).toBeNull();
  });

  it("rejects mapping when the displayed video has no measurable bounds", () => {
    expect(
      browserVideoPoint(video({ width: 0 }), viewport, 0, 0),
    ).toBeNull();
  });
});
