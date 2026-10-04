import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type KeyboardEvent as ReactKeyboardEvent,
  type PointerEvent as ReactPointerEvent,
} from "react";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { useBrowserReadingSnapshot } from "./useBrowserReadingSnapshot";
import {
  BrowserV2VncDisplay,
  preloadBrowserV2VncRfb,
  type BrowserV2VncStage,
} from "./BrowserV2VncDisplay";
import { BrowserV2VncAttachGate } from "./BrowserV2VncAttachGate";
import { browserVideoPoint } from "./transport/cloudWebRtcTransport.mjs";
import type {
  BrowserSnapshot,
  TabSnapshot,
  ViewportPoint,
} from "./transport/browserContracts";
import type { ReadVoice } from "../tts/readTtsProvider";
import {
  normalizeReadLanguage,
  setReadSpeedPreference,
  setReadVoicePreference,
} from "../preferences/readPreferencesStore";

function normalizeTarget(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return null;

  if (/\s/.test(trimmed) || (!trimmed.includes(".") && !trimmed.includes("://"))) {
    return `https://www.google.com/search?q=${encodeURIComponent(trimmed)}`;
  }

  try {
    const parsed = new URL(
      trimmed.includes("://") ? trimmed : `https://${trimmed}`,
    );
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      return null;
    }
    return parsed.href;
  } catch {
    return null;
  }
}

function activeTab(snapshot: BrowserSnapshot | null): TabSnapshot | null {
  if (!snapshot?.activeTabId) return null;
  return snapshot.tabs.find((tab) => tab.id === snapshot.activeTabId) ?? null;
}

function friendlyBrowserError(reason: unknown): string {
  const message = reason instanceof Error ? reason.message : String(reason);

  switch (message) {
    case "CLOUD_RECONNECTING":
      return "The secure browser is reconnecting.";
    case "NAVIGATION_FAILED":
      return "The page could not be opened.";
    case "SESSION_GONE":
      return "The secure browser session ended.";
    case "DISPLAY_UNAVAILABLE":
    case "DISPLAY_GRANT_INVALID":
      return "The remote page display is unavailable.";
    case "CLOUD_SIGNALING_FAILED":
      return "The secure browser connection could not be established.";
    default:
      return "The secure browser could not complete that action.";
  }
}

function vncPointForClient(
  target: HTMLDivElement | null,
  viewport: {
    renderWidth: number;
    renderHeight: number;
    revision: number;
  } | null,
  clientX: number,
  clientY: number,
): ViewportPoint | null {
  const canvas = target?.querySelector("canvas");
  if (!canvas || !viewport) return null;

  const bounds = canvas.getBoundingClientRect();
  if (
    bounds.width <= 0
    || bounds.height <= 0
    || clientX < bounds.left
    || clientX >= bounds.right
    || clientY < bounds.top
    || clientY >= bounds.bottom
  ) {
    return null;
  }

  return {
    x: Math.min(
      viewport.renderWidth - 1,
      Math.max(
        0,
        Math.floor((clientX - bounds.left) / bounds.width * viewport.renderWidth),
      ),
    ),
    y: Math.min(
      viewport.renderHeight - 1,
      Math.max(
        0,
        Math.floor((clientY - bounds.top) / bounds.height * viewport.renderHeight),
      ),
    ),
    viewportRevision: viewport.revision,
  };
}

export function BrowserWorkspace({
  active,
  userId,
}: {
  active: boolean;
  userId: string;
}) {
  const runtime = useReadRuntime();
  const cloud = runtime?.browser ?? null;
  const reading = useBrowserReadingSnapshot(runtime?.browserReading);
  const playback = useWebPlaybackSnapshot(runtime?.playback);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const surfaceRef = useRef<HTMLDivElement | null>(null);
  const vncTargetRef = useRef<HTMLDivElement | null>(null);
  const vncDisplayRef = useRef<BrowserV2VncDisplay | null>(null);
  const vncAutoRetryRef = useRef(0);
  const vncAttachGateRef = useRef<BrowserV2VncAttachGate | null>(null);
  if (!vncAttachGateRef.current) {
    vncAttachGateRef.current = new BrowserV2VncAttachGate(active);
  }
  const vncAttachGate = vncAttachGateRef.current;
  const mobileKeyboardRef = useRef<HTMLTextAreaElement | null>(null);
  const gestureUnbindRef = useRef<(() => void) | null>(null);
  const startedRef = useRef(false);
  const attachingVideoRef = useRef(false);
  const pointerStartRef = useRef<{
    pointerId: number;
    x: number;
    y: number;
  } | null>(null);
  const touchPointRef = useRef<ViewportPoint | null>(null);
  const handledImportTargetRef = useRef<string | null>(null);

  const [snapshot, setSnapshot] = useState<BrowserSnapshot | null>(
    () => cloud?.getBrowserSnapshot() ?? null,
  );
  const [address, setAddress] = useState("");
  const [mediaReady, setMediaReady] = useState(
    () => cloud?.isMediaReady() ?? false,
  );
  const [started, setStarted] = useState(
    () => Boolean(cloud?.getBrowserSnapshot()),
  );
  const [starting, setStarting] = useState(false);
  const [retryAttempt, setRetryAttempt] = useState(0);
  const [vncStage, setVncStage] = useState<BrowserV2VncStage | null>(null);
  const [remoteTextInputFocused, setRemoteTextInputFocused] = useState(false);
  const [lastPoint, setLastPoint] = useState<ViewportPoint | null>(null);
  const [voices, setVoices] = useState<ReadVoice[]>([]);
  const [voicesLoading, setVoicesLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const requestedImportTarget =
    typeof window !== "undefined"
      ? new URLSearchParams(window.location.search).get("url")
      : null;
  const requestedAutostart =
    typeof window !== "undefined"
      && new URLSearchParams(window.location.search).get("autostart") === "1";

  const tab = useMemo(() => activeTab(snapshot), [snapshot]);
  const ownsBrowserPlayback = Boolean(
    runtime
    && reading.status === "ready"
    && reading.documentId !== null
    && playback.documentId === reading.documentId
    && playback.revisionId === reading.revisionId,
  );
  const isPlaying =
    ownsBrowserPlayback
    && ["preparing", "buffering", "playing"].includes(playback.status);

  useLayoutEffect(() => {
    vncAttachGate.setActive(active);
  }, [active, vncAttachGate]);

  useEffect(() => {
    if (!runtime || !active || voices.length > 0 || voicesLoading) return;

    const listVoices = runtime.tts.listVoices?.bind(runtime.tts);
    if (!listVoices) return;

    let cancelled = false;
    setVoicesLoading(true);

    void listVoices()
      .then((catalog) => {
        if (!cancelled) {
          setVoices(catalog.voices);
        }
      })
      .catch(() => undefined)
      .finally(() => {
        if (!cancelled) {
          setVoicesLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [active, runtime, voices.length, voicesLoading]);

  useEffect(() => {
    if (!cloud) return;

    const update = () => {
      setSnapshot(cloud.getBrowserSnapshot());
      setMediaReady(cloud.isMediaReady());
    };

    const unsubscribeBrowser = cloud.subscribe(() => update());
    const unsubscribeMedia = cloud.subscribeMedia(() => update());

    update();

    const onPageHide = (event: PageTransitionEvent) => {
      if (!event.persisted) {
        cloud.stopForPageHide();
      }
    };
    window.addEventListener("pagehide", onPageHide);

    return () => {
      window.removeEventListener("pagehide", onPageHide);
      unsubscribeBrowser();
      unsubscribeMedia();
    };
  }, [cloud]);

  useEffect(() => {
    if (tab?.displayUrl) {
      setAddress(tab.displayUrl);
    }
  }, [tab?.displayUrl]);

  const attachDisplay = useCallback(async () => {
    if (!cloud || !surfaceRef.current) return;

    if (cloud.getDisplayTransport() === "vnc") {
      if (!cloud.isDisplayWorkerReady() || !vncTargetRef.current) return;

      if (!vncDisplayRef.current) {
        const nativeInput = !window.matchMedia("(pointer: coarse)").matches;
        vncDisplayRef.current = new BrowserV2VncDisplay(
          vncTargetRef.current,
          (stage) => {
            setVncStage(stage);
            if (
              stage === "DISPLAY_DISCONNECTED"
              || stage === "DISPLAY_FRAME_TIMEOUT"
              || stage === "DISPLAY_AUTH_FAILED"
              || stage === "DISPLAY_UNAVAILABLE"
            ) {
              cloud.setVncFrameReady(false);
              setMediaReady(false);
            }
          },
          () => {
            vncAutoRetryRef.current = 0;
            cloud.setVncFrameReady(true);
            setMediaReady(cloud.isMediaReady());
          },
          preloadBrowserV2VncRfb,
          nativeInput,
        );
      }

      const display = vncDisplayRef.current;
      if (!display.frameReady && vncStage !== "DISPLAY_CONNECTING") {
        const attachToken = vncAttachGate.begin();
        if (!attachToken) return;

        if (vncAutoRetryRef.current >= 3) {
          vncAttachGate.finish(attachToken);
          return;
        }
        vncAutoRetryRef.current += 1;

        try {
          let grant = cloud.takeVncDisplayGrant();
          if (!grant) {
            await cloud.refreshVncDisplayGrant();

            if (!vncAttachGate.isCurrent(attachToken)) {
              // The one-use ticket arrived for a route generation that is no
              // longer visible. Consume it from client state without ever
              // handing it to noVNC; a visible route will request a fresh one.
              cloud.takeVncDisplayGrant();
              return;
            }

            grant = cloud.takeVncDisplayGrant();
          }

          if (!grant) {
            throw new Error("DISPLAY_UNAVAILABLE");
          }

          if (!vncAttachGate.isCurrent(attachToken)) {
            return;
          }

          await display.connect(grant);
        } catch (reason) {
          if (!vncAttachGate.isCurrent(attachToken)) {
            return;
          }

          setVncStage("DISPLAY_UNAVAILABLE");
          setError(friendlyBrowserError(reason));
        } finally {
          const stale = !vncAttachGate.isCurrent(attachToken);
          vncAttachGate.finish(attachToken);

          if (stale && vncAttachGate.isActive) {
            queueMicrotask(() => {
              void attachDisplay();
            });
          }
        }
      }
      return;
    }

    if (
      attachingVideoRef.current
      || cloud.getTransport()
      || !videoRef.current
    ) {
      return;
    }

    attachingVideoRef.current = true;
    try {
      await cloud.attachVideo(
        videoRef.current,
        () => setMediaReady(cloud.isMediaReady()),
        (status) => {
          setMediaReady(cloud.isMediaReady());
          if (status === "MEDIA_CONNECTED") {
            setError(null);
          } else if (
            status === "MEDIA_NEGOTIATION_FAILED"
            || status === "ICE_FAILED"
            || status === "SIGNAL_PROTOCOL_ERROR"
          ) {
            setError("The secure browser display connection was interrupted.");
          }
        },
      );

      if (cloud.getTransport() && surfaceRef.current) {
        gestureUnbindRef.current?.();
        gestureUnbindRef.current =
          cloud.getTransport()?.bindGestures(surfaceRef.current) ?? null;
      }
    } catch (reason) {
      setError(friendlyBrowserError(reason));
    } finally {
      attachingVideoRef.current = false;
    }
  }, [cloud, vncAttachGate, vncStage]);

  useEffect(() => {
    if (!cloud || !active || startedRef.current || started) return;

    startedRef.current = true;
    setStarting(true);
    setError(null);

    const viewport = {
      width: Math.max(320, window.innerWidth - 280),
      height: Math.max(360, window.innerHeight - 190),
      deviceScaleFactor: window.devicePixelRatio || 1,
    };

    void cloud
      .start({
        profileId: `read-${userId}`,
        privateMode: false,
        viewport,
      })
      .then((next) => {
        setSnapshot(next);
        setStarted(true);
        return attachDisplay();
      })
      .catch((reason) => {
        startedRef.current = false;
        setError(friendlyBrowserError(reason));
      })
      .finally(() => setStarting(false));
  }, [active, attachDisplay, cloud, retryAttempt, started, userId]);

  useEffect(() => {
    if (!started || !cloud || !active) return;

    void attachDisplay();
  }, [
    active,
    attachDisplay,
    cloud,
    mediaReady,
    snapshot?.connectivity,
    snapshot?.lifecycle,
    started,
  ]);

  useEffect(() => {
    if (
      !cloud
      || !started
      || active
      || cloud.getDisplayTransport() !== "vnc"
    ) {
      return;
    }

    // Keep the authenticated Chromium/profile alive, but stop hidden
    // framebuffer traffic. Returning to Browser requests a fresh one-use
    // display ticket and reconnects this same app-owned session.
    vncAutoRetryRef.current = 0;
    cloud.takeVncDisplayGrant();
    vncDisplayRef.current?.disconnect();
    cloud.setVncFrameReady(false);
    setMediaReady(false);
    setRemoteTextInputFocused(false);
  }, [active, cloud, started]);

  useEffect(() => {
    if (!active) {
      handledImportTargetRef.current = null;
    }
  }, [active]);

  useEffect(() => {
    if (
      !active
      || !mediaReady
      || !tab
      || !requestedAutostart
      || !requestedImportTarget
      || handledImportTargetRef.current === requestedImportTarget
    ) {
      return;
    }

    const target = normalizeTarget(requestedImportTarget);
    if (!target) return;

    if (tab.url === target) {
      handledImportTargetRef.current = requestedImportTarget;
      return;
    }

    handledImportTargetRef.current = requestedImportTarget;
    void navigate(target);
  }, [
    active,
    mediaReady,
    requestedAutostart,
    requestedImportTarget,
    tab?.id,
    tab?.url,
  ]);

  useEffect(() => {
    if (!cloud || !started || cloud.getDisplayTransport() === "vnc") {
      return;
    }

    const transport = cloud.getTransport();
    const viewport = transport?.appliedViewport;
    if (!transport || !viewport) return;

    if (!active) {
      setMediaReady(false);
    }

    void transport
      .requestViewport({
        cssWidth: viewport.cssWidth,
        cssHeight: viewport.cssHeight,
        devicePixelRatio: viewport.effectiveDpr,
        visibility: active ? "visible" : "hidden",
      })
      .then(() => {
        setMediaReady(cloud.isMediaReady());
      })
      .catch(() => {
        if (active) {
          setError("Secure browser viewport update failed.");
        }
      });
  }, [active, cloud, started, snapshot?.connectivity]);

  useEffect(() => {
    return () => {
      gestureUnbindRef.current?.();
      gestureUnbindRef.current = null;
      vncDisplayRef.current?.close();
      vncDisplayRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!cloud || !started || !surfaceRef.current) return;

    const observer = new ResizeObserver((entries) => {
      const box = entries[0]?.contentRect;
      if (!box || box.width < 2 || box.height < 2) return;

      void cloud.updateViewport({
        width: Math.max(320, box.width),
        height: Math.max(360, box.height),
        deviceScaleFactor: window.devicePixelRatio || 1,
      }).catch(() => undefined);
    });

    observer.observe(surfaceRef.current);
    return () => observer.disconnect();
  }, [cloud, started]);

  async function retrySecureBrowser(): Promise<void> {
    if (!cloud) return;

    setError(null);
    setMediaReady(false);
    setStarted(false);
    setStarting(false);
    setSnapshot(null);
    setLastPoint(null);
    setRemoteTextInputFocused(false);
    runtime?.browserReading.invalidate(null);

    gestureUnbindRef.current?.();
    gestureUnbindRef.current = null;
    vncDisplayRef.current?.close();
    vncDisplayRef.current = null;
    vncAutoRetryRef.current = 0;
    setVncStage(null);

    try {
      await cloud.stop();
      cloud.prepareRestart();
      startedRef.current = false;
      attachingVideoRef.current = false;
      setRetryAttempt((value) => value + 1);
    } catch (reason) {
      setError(friendlyBrowserError(reason));
    }
  }

  async function navigate(target: string): Promise<void> {
    if (!cloud || !tab || !mediaReady) return;

    setError(null);
    runtime?.browserReading.invalidate(null);
    setLastPoint(null);
    setRemoteTextInputFocused(false);

    try {
      await cloud.open(target, tab.id);
    } catch (reason) {
      setError(friendlyBrowserError(reason));
    }
  }

  function submitAddress(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    const target = normalizeTarget(address);

    if (!target) {
      setError("Enter a valid web address or search.");
      return;
    }

    void navigate(target);
  }

  async function browserCommand(
    command: "back" | "forward" | "reload" | "stop",
  ): Promise<void> {
    if (!cloud || !tab || !mediaReady) return;

    setError(null);
    runtime?.browserReading.invalidate(null);
    setLastPoint(null);
    setRemoteTextInputFocused(false);

    try {
      if (command === "back") await cloud.back(tab.id);
      if (command === "forward") await cloud.forward(tab.id);
      if (command === "reload") await cloud.reload(tab.id);
      if (command === "stop") await cloud.stopLoading(tab.id);
    } catch (reason) {
      setError(friendlyBrowserError(reason));
    }
  }

  async function toggleReadPage(): Promise<void> {
    if (!runtime || !tab || !mediaReady) return;

    setError(null);

    try {
      if (ownsBrowserPlayback) {
        await runtime.playback.togglePlayPause();
        return;
      }

      await runtime.browserReading.startReading(tab.id);
      await runtime.playback.play();
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "The page could not be prepared for reading.",
      );
    }
  }

  async function readFromHere(): Promise<void> {
    if (!runtime || !lastPoint) return;

    setError(null);

    try {
      const startedFromPoint =
        await runtime.browserReading.readFromHere(lastPoint);
      if (!startedFromPoint) {
        setError(
          "That reading point is no longer valid on the current page.",
        );
      }
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "The selected reading position could not be started.",
      );
    }
  }

  function mapPoint(
    event: ReactPointerEvent<HTMLDivElement>,
  ): ViewportPoint | null {
    if (!cloud || !mediaReady) return null;

    if (cloud.getDisplayTransport() === "vnc") {
      return vncPointForClient(
        vncTargetRef.current,
        cloud.getVncViewport(),
        event.clientX,
        event.clientY,
      );
    }

    const video = videoRef.current;
    const viewport = cloud.getTransport()?.appliedViewport;
    if (!video || !viewport) return null;

    const point = browserVideoPoint(
      video,
      viewport,
      event.clientX,
      event.clientY,
    );

    return point
      ? { ...point, viewportRevision: viewport.revision }
      : null;
  }

  function pointerDown(event: ReactPointerEvent<HTMLDivElement>): void {
    if (!mediaReady) return;

    pointerStartRef.current = {
      pointerId: event.pointerId,
      x: event.clientX,
      y: event.clientY,
    };

    if (
      event.pointerType === "touch"
      && cloud?.getDisplayTransport() === "vnc"
    ) {
      const point = mapPoint(event);
      touchPointRef.current = point;

      if (point) {
        void cloud
          .sendVncTouch("start", point.x, point.y, 0)
          .catch(() => undefined);
      }
    }
  }

  function pointerMove(event: ReactPointerEvent<HTMLDivElement>): void {
    const start = pointerStartRef.current;
    if (
      start
      && start.pointerId === event.pointerId
      && Math.hypot(event.clientX - start.x, event.clientY - start.y) > 12
    ) {
      pointerStartRef.current = null;
    }

    if (
      event.pointerType === "touch"
      && cloud?.getDisplayTransport() === "vnc"
    ) {
      const point = mapPoint(event);
      if (point) {
        touchPointRef.current = point;
        void cloud
          .sendVncTouch("move", point.x, point.y, 0)
          .catch(() => undefined);
      }
    }
  }

  function pointerUp(event: ReactPointerEvent<HTMLDivElement>): void {
    const start = pointerStartRef.current;
    pointerStartRef.current = null;

    const point = mapPoint(event);
    if (
      start
      && start.pointerId === event.pointerId
      && Math.hypot(event.clientX - start.x, event.clientY - start.y) <= 12
      && point
    ) {
      setLastPoint(point);
    }

    if (
      event.pointerType === "touch"
      && cloud?.getDisplayTransport() === "vnc"
    ) {
      const touchPoint = point ?? touchPointRef.current;
      touchPointRef.current = null;

      if (touchPoint) {
        void cloud
          .sendVncTouch("end", touchPoint.x, touchPoint.y, 0)
          .then((ack) => {
            const focused = ack?.textInputFocused === true;
            setRemoteTextInputFocused(focused);

            if (focused) {
              const field = mobileKeyboardRef.current;
              if (field) {
                field.value = "";
                field.focus({ preventScroll: true });
              }
            }
          })
          .catch(() => {
            setRemoteTextInputFocused(false);
          });
      }
    }
  }

  function openMobileKeyboard(): void {
    const field = mobileKeyboardRef.current;
    if (!field) return;
    field.value = "";
    field.focus({ preventScroll: true });
  }

  function mobileKeyboardInput(event: FormEvent<HTMLTextAreaElement>): void {
    if (!cloud) return;

    const nativeInput = event.nativeEvent as InputEvent;
    if (nativeInput.isComposing) return;

    const text = event.currentTarget.value;
    event.currentTarget.value = "";

    if (text) {
      void cloud.sendText(text).catch(() => {
        setError("Phone keyboard input could not reach the focused webpage field.");
      });
    }
  }

  function mobileKeyboardKeyDown(
    event: ReactKeyboardEvent<HTMLTextAreaElement>,
  ): void {
    if (!cloud) return;

    const special = new Set([
      "Backspace",
      "Enter",
      "Tab",
      "Escape",
      "ArrowLeft",
      "ArrowRight",
      "ArrowUp",
      "ArrowDown",
      "Delete",
      "Home",
      "End",
      "PageUp",
      "PageDown",
    ]);

    if (!special.has(event.key)) return;

    event.preventDefault();
    void cloud
      .sendKey("down", event.key)
      .then(() => cloud.sendKey("up", event.key))
      .catch(() => undefined);
  }

  const vncActive =
    started && cloud?.getDisplayTransport() === "vnc";
  const coarsePointer =
    typeof window !== "undefined"
    && window.matchMedia("(pointer: coarse)").matches;
  const nativeDesktopVncMode = Boolean(vncActive && !coarsePointer);

  const lifecycleLabel = starting
    ? "Starting secure browser…"
    : mediaReady
      ? "Connected"
      : started
        ? "Connecting display…"
        : "Not started";

  return (
    <section
      className={active ? "browser-workspace active" : "browser-workspace"}
      aria-hidden={!active}
    >
      <header className="browser-command-bar">
        <div className="browser-nav-buttons">
          <button
            type="button"
            aria-label="Back"
            disabled={!mediaReady || !tab?.canGoBack}
            onClick={() => void browserCommand("back")}
          >
            ←
          </button>
          <button
            type="button"
            aria-label="Forward"
            disabled={!mediaReady || !tab?.canGoForward}
            onClick={() => void browserCommand("forward")}
          >
            →
          </button>
          <button
            type="button"
            aria-label={tab?.loading ? "Stop loading" : "Reload"}
            disabled={!mediaReady || !tab}
            onClick={() =>
              void browserCommand(tab?.loading ? "stop" : "reload")
            }
          >
            {tab?.loading ? "×" : "↻"}
          </button>
        </div>

        <form className="browser-address-form" onSubmit={submitAddress}>
          <span
            className="browser-security-dot"
            data-security={tab?.securityState ?? "unknown"}
            aria-hidden="true"
          />
          <input
            aria-label="Address or search"
            value={address}
            onChange={(event) => setAddress(event.target.value)}
            placeholder="Search or enter address"
            disabled={!mediaReady}
          />
        </form>

        <button
          type="button"
          className={isPlaying ? "browser-read-button active" : "browser-read-button"}
          disabled={!mediaReady || !tab}
          onClick={() => void toggleReadPage()}
        >
          {reading.status === "extracting"
            ? "Preparing…"
            : isPlaying
              ? "Pause"
              : ownsBrowserPlayback
                ? "Resume"
                : "Read page"}
        </button>
      </header>

      <div className="browser-reader-strip">
        <span className="browser-connection-state" role="status" aria-live="polite" aria-atomic="true">
          <i data-ready={mediaReady} />
          {lifecycleLabel}
        </span>

        {reading.status === "ready" ? (
          <>
            <button
              type="button"
              disabled={!ownsBrowserPlayback}
              onClick={() => void runtime?.browserReading.moveBySentence(-1)}
            >
              Previous
            </button>
            <button
              type="button"
              disabled={!ownsBrowserPlayback}
              onClick={() => void runtime?.browserReading.moveBySentence(1)}
            >
              Next
            </button>
            <button
              type="button"
              aria-pressed={reading.follow}
              onClick={() => runtime?.browserReading.toggleFollow()}
            >
              {reading.follow ? "Follow on" : "Follow"}
            </button>
            <button
              type="button"
              disabled={!lastPoint || !ownsBrowserPlayback}
              onClick={() => void readFromHere()}
            >
              Read from here
            </button>

            <label className="browser-reader-select">
              <span>Speed</span>
              <select
                value={playback.speed}
                disabled={!ownsBrowserPlayback}
                onChange={(event) => {
                  const speed = Number(event.target.value);
                  setReadSpeedPreference(speed);
                  runtime?.playback.setSpeed(speed);
                }}
              >
                {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3].map(
                  (speed) => (
                    <option key={speed} value={speed}>
                      {speed}×
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="browser-reader-select browser-reader-voice">
              <span>Voice</span>
              <select
                value={playback.voiceId}
                disabled={
                  !ownsBrowserPlayback
                  || voicesLoading
                  || voices.length === 0
                }
                onChange={(event) => {
                  const voiceId = event.target.value;
                  const voice = voices.find(
                    (candidate) => candidate.id === voiceId,
                  );
                  const language = normalizeReadLanguage(
                    voice?.language || voice?.locale,
                  );
                  setReadVoicePreference(voiceId, language);
                  void runtime?.playback.setVoice(
                    voiceId,
                    { language },
                  );
                }}
              >
                {voices.length === 0 ? (
                  <option value={playback.voiceId}>
                    {voicesLoading ? "Loading…" : "Current voice"}
                  </option>
                ) : (
                  <>
                    {!voices.some((voice) => voice.id === playback.voiceId) ? (
                      <option value={playback.voiceId}>Current voice</option>
                    ) : null}
                    {voices.map((voice) => (
                      <option key={voice.id} value={voice.id}>
                        {voice.name}
                        {voice.locale ? ` · ${voice.locale}` : ""}
                      </option>
                    ))}
                  </>
                )}
              </select>
            </label>

            <span className="browser-reading-title">
              {reading.title ?? "Current page"}
            </span>
          </>
        ) : (
          <span className="browser-reading-hint">
            The web page stays visible when Read is active.
          </span>
        )}
      </div>

      {error || reading.error ? (
        <div className="browser-error" role="alert">
          <span>{error ?? reading.error}</span>
          <span className="browser-error-actions">
            {started
            && !mediaReady
            && cloud?.getDisplayTransport() === "vnc" ? (
              <button
                type="button"
                onClick={() => {
                  setError(null);
                  vncAutoRetryRef.current = 0;
                  setVncStage(null);
                  void attachDisplay();
                }}
              >
                Retry display
              </button>
            ) : null}
            {!mediaReady ? (
              <button
                type="button"
                onClick={() => void retrySecureBrowser()}
              >
                Retry secure browser
              </button>
            ) : null}
          </span>
        </div>
      ) : null}

      <div
        ref={surfaceRef}
        className="browser-page-surface"
        tabIndex={mediaReady ? 0 : -1}
        onPointerDownCapture={pointerDown}
        onPointerMoveCapture={pointerMove}
        onPointerUpCapture={pointerUp}
        onPointerCancelCapture={() => {
          pointerStartRef.current = null;
          touchPointRef.current = null;
        }}
        onKeyDownCapture={(event) => {
          if (!cloud || !mediaReady || nativeDesktopVncMode) return;

          const ownChrome =
            event.target instanceof Element
            && Boolean(
              event.target.closest(
                ".browser-mobile-keyboard-button, .browser-mobile-keyboard-proxy",
              ),
            );
          if (ownChrome) return;
          if (!vncActive && event.key === "Tab") return;
          if (event.key.length > 32) return;

          event.preventDefault();
          void cloud.sendKey("down", event.key).catch(() => undefined);
        }}
        onKeyUpCapture={(event) => {
          if (!cloud || !mediaReady || nativeDesktopVncMode) return;

          const ownChrome =
            event.target instanceof Element
            && Boolean(
              event.target.closest(
                ".browser-mobile-keyboard-button, .browser-mobile-keyboard-proxy",
              ),
            );
          if (ownChrome) return;
          if (!vncActive && event.key === "Tab") return;
          if (event.key.length > 32) return;

          event.preventDefault();
          void cloud.sendKey("up", event.key).catch(() => undefined);
        }}
      >
        <video
          ref={videoRef}
          className={
            cloud?.getDisplayTransport() === "vnc"
              ? "browser-remote-video hidden"
              : "browser-remote-video"
          }
          autoPlay
          playsInline
          muted
        />
        <div
          ref={vncTargetRef}
          className={
            cloud?.getDisplayTransport() === "vnc"
              ? "browser-vnc-target"
              : "browser-vnc-target hidden"
          }
        />

        {!mediaReady ? (
          <div className="browser-display-gate">
            <span className="browser-display-spinner" aria-hidden="true" />
            <strong>{lifecycleLabel}</strong>
            <p>
              Floently is establishing the authenticated remote page display.
            </p>
            {vncStage ? <small>{vncStage}</small> : null}
          </div>
        ) : null}

        {vncActive && mediaReady && coarsePointer ? (
          <>
            <button
              type="button"
              className="browser-mobile-keyboard-button"
              data-ready={remoteTextInputFocused}
              aria-label={
                remoteTextInputFocused
                  ? "Type in focused remote webpage field"
                  : "Open phone keyboard for remote webpage"
              }
              onPointerDown={(event) => event.stopPropagation()}
              onClick={(event) => {
                event.stopPropagation();
                openMobileKeyboard();
              }}
            >
              ⌨ Type
            </button>
            <textarea
              ref={mobileKeyboardRef}
              className="browser-mobile-keyboard-proxy"
              aria-label="Remote webpage keyboard input"
              autoCapitalize="sentences"
              autoCorrect="off"
              spellCheck={false}
              inputMode="text"
              enterKeyHint="go"
              onInput={mobileKeyboardInput}
              onKeyDown={mobileKeyboardKeyDown}
              onBlur={(event) => {
                event.currentTarget.value = "";
              }}
            />
          </>
        ) : null}

        {lastPoint ? (
          <div className="browser-read-point-badge">
            Reading point selected
          </div>
        ) : null}
      </div>
    </section>
  );
}
