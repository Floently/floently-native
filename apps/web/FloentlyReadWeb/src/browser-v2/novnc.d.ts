declare module "@novnc/novnc" {
  export interface RFBOptions {
    shared?: boolean;
    credentials?: {
      username?: string;
      password?: string;
      target?: string;
    };
    wsProtocols?: string[];
  }

  export default class RFB {
    constructor(
      target: HTMLElement,
      urlOrChannel: string | WebSocket | RTCDataChannel,
      options?: RFBOptions,
    );

    scaleViewport: boolean;
    clipViewport: boolean;
    resizeSession: boolean;
    viewOnly: boolean;
    focusOnClick: boolean;

    addEventListener(
      type: string,
      listener: (event: Event) => void,
    ): void;
    removeEventListener(
      type: string,
      listener: (event: Event) => void,
    ): void;

    disconnect(): void;
    focus(options?: FocusOptions): void;
  }
}
