/**
 * Only a change in media readiness may change BrowserBackend connectivity.
 * WebRTC emits many informational messages AFTER first frame (ICE enum,
 * viewport, answer, statistics, negotiation) which MUST NOT pause Reader,
 * stop TTS, or tell the owner their secure browser is reconnecting.
 */
export const CLOUD_MEDIA_LOST = Object.freeze([
  "CLOUD_RECONNECTING",
  "ICE_FAILED",
  "INPUT_WS_FALLBACK_FAILED",
  "MEDIA_WAITING_FOR_FRAME",
  "MEDIA_STALLED",
  "REMOTE_DESCRIPTION_FAILED",
]);

const lost = new Set(CLOUD_MEDIA_LOST);

/** @returns {true | false | null} null = informational status; no lifecycle mutation. */
export function cloudMediaConnectivity(status) {
  if (status === "MEDIA_CONNECTED") return true;
  if (typeof status === "string" && lost.has(status)) return false;
  return null;
}
