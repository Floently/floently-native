/**
 * Browser V2 session allocation against E's actual authenticated gateway.
 * Allocation does NOT prove a running Chromium renderer or WebRTC video.
 */
import { cloudSignalSocketUrl } from "./cloudSignalingSocket.mjs";

const SESSION_LIMIT_RETRY_DELAYS_MS=Object.freeze([80,160,320,640,1200]);
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));

export async function createCloudBrowserSession({
  origin,
  viewport,
  authorization,
  resumeSessionId,
  fetchImpl = globalThis.fetch,
  // Render's free Read API can cold-start after idle. Keep the pending
  // authenticated allocation alive; never treat an upstream wake as logout.
  timeoutMs = 90_000,
} = {}) {
  const verifierUrl = cloudSignalSocketUrl(origin, "preflight");
  const trustedOrigin = new URL(verifierUrl).origin.replace(/^wss:/, "https:").replace(/^ws:/, "http:");
  if (!viewport || !Number.isSafeInteger(viewport.cssWidth) ||
      !Number.isSafeInteger(viewport.cssHeight) ||
      viewport.cssWidth < 320 || viewport.cssWidth > 2560 ||
      viewport.cssHeight < 360 || viewport.cssHeight > 1600 ||
      !Number.isFinite(viewport.devicePixelRatio ?? 1) ||
      (viewport.devicePixelRatio ?? 1) < 1 ||
      (viewport.devicePixelRatio ?? 1) > 2)
    throw new Error("VIEWPORT_INVALID");
  if (viewport.cssWidth * (viewport.devicePixelRatio ?? 1) > 2560 ||
      viewport.cssHeight * (viewport.devicePixelRatio ?? 1) > 1600)
    throw new Error("VIEWPORT_INVALID");
  if (authorization !== undefined &&
      (typeof authorization !== "string" || !/^Bearer [A-Za-z0-9._~+/-]{8,8192}={0,2}$/.test(authorization)))
    throw new Error("AUTHORIZATION_INVALID");
  if (resumeSessionId !== undefined &&
      (typeof resumeSessionId !== "string" ||
       !/^[A-Za-z0-9_.:-]{1,128}$/.test(resumeSessionId)))
    throw new Error("RESUME_SESSION_INVALID");
  if (!Number.isInteger(timeoutMs) || timeoutMs < 100 || timeoutMs > 120000)
    throw new Error("SESSION_TIMEOUT_INVALID");

  const requestUrl=trustedOrigin + "/api/browser-v2/session";
  const requestInit=()=>({
    method: "POST",
    // Browser V2 is served by browser-v2.obumai.com while Read lives at
    // read.floently.com. Sending credentials:"include" cross-site requires
    // Access-Control-Allow-Credentials and leaks unrelated ObumAI cookies.
    // Read supplies a bearer key explicitly instead. Preserve Read cookies
    // only for legacy same-origin deployments.
    credentials: trustedOrigin === globalThis.location?.origin ? "include" : "omit",
    redirect: "error",
    cache: "no-store",
    headers: { "content-type": "application/json",
      ...(authorization ? { authorization } : {}) },
    body: JSON.stringify({ viewport: {
      cssWidth: viewport.cssWidth, cssHeight: viewport.cssHeight,
      devicePixelRatio: viewport.devicePixelRatio ?? 1,
    }, ...(resumeSessionId ? { resumeSessionId } : {}) }),
    signal: AbortSignal.timeout(timeoutMs),
  });
  let response;
  for(let attempt=0;;attempt++){
    try {
      response=await fetchImpl(requestUrl,requestInit());
    } catch {
      throw new Error("CLOUD_ALLOCATE_FAILED");
    }
    if(![409,429].includes(response.status)||
       attempt>=SESSION_LIMIT_RETRY_DELAYS_MS.length)break;
    // A SPA route change can send the old authenticated session.stop and
    // immediately mount Browser V2 again. Give that already-authorized stop
    // a tiny bounded window to free the single-owner slot. Never stop, steal,
    // resume or inspect the occupied session from this fresh allocator.
    await sleep(SESSION_LIMIT_RETRY_DELAYS_MS[attempt]);
  }
  if (response.status === 503)
    throw new Error("AUTH_UPSTREAM_UNAVAILABLE");
  if (response.status === 401 || response.status === 403)
    throw new Error("AUTH_REQUIRED");
  if (response.status !== 201 ||
      !(response.headers.get("content-type") || "").includes("application/json"))
    throw new Error("CLOUD_ALLOCATE_FAILED");
  let result;
  try { result = await response.json(); }
  catch { throw new Error("CLOUD_ALLOCATE_FAILED"); }
  const resumed=result?.status==="RESUMED_NOT_MEDIA_READY";
  if (result?.version !== "0.1" ||
      typeof result.socketTicket !== "string" ||
      !/^[A-Za-z0-9_-]{43}$/.test(result.socketTicket) ||
      !result.sessionId || typeof result.sessionId !== "string" ||
      result.signalPath !==
        "/api/browser-v2/cloud/sessions/" + encodeURIComponent(result.sessionId) + "/signal" ||
      !["ALLOCATED_NOT_MEDIA_READY","RESUMED_NOT_MEDIA_READY"].includes(result.status) ||
      (resumed&&(!Number.isSafeInteger(result.resumeSequence)||
                 result.resumeSequence<0))) {
    throw new Error("CLOUD_ALLOCATE_FAILED");
  }
  const iceServers = result.iceServers ?? [];
  if (!Array.isArray(iceServers) || iceServers.length>3 ||
      iceServers.some(s => typeof s.urls!=="string" ||
        !/^(stun|stuns|turn|turns):[A-Za-z0-9.-]+:[0-9]{2,5}(\?transport=(udp|tcp))?$/.test(s.urls) ||
        (s.urls.startsWith("turn") &&
          (typeof s.username!=="string" || s.username.length>160 ||
           typeof s.credential!=="string" || s.credential.length>160))))
    throw new Error("ICE_CONFIG_INVALID");
  // The remote-display grant MUST be for the same authenticated owner session.
  // Never accept an arbitrary VNC address, prebuilt WebSocket URL or ticket in a query.
  const vnc = result.displayTransport === "vnc";
  const hasDisplayGrant=result.displayPath!==undefined||
    result.displayTicket!==undefined||result.displayExpiresInSeconds!==undefined;
  if ((result.displayTransport !== undefined &&
       !["vnc", "webrtc"].includes(result.displayTransport)) ||
      (!vnc && (hasDisplayGrant || result.displayOrigin !== undefined)) ||
      (vnc && (
        !/^[A-Za-z0-9_.:-]{1,128}$/.test(result.sessionId) ||
        (result.displayOrigin !== undefined && (()=>{
          try{
            const u=new URL(result.displayOrigin);
            return u.protocol!=="https:"||u.username||u.password||
              u.search||u.hash||u.pathname!=="/";
          }catch{return true;}
        })()) ||
        (!resumed&&!hasDisplayGrant) ||
        (hasDisplayGrant&&(
          result.displayPath !==
            "/api/browser-v2/cloud/sessions/" +
            encodeURIComponent(result.sessionId) + "/display" ||
          typeof result.displayTicket !== "string" ||
          !/^[A-Za-z0-9_-]{43}$/.test(result.displayTicket) ||
          !Number.isInteger(result.displayExpiresInSeconds) ||
          result.displayExpiresInSeconds < 1 ||
          result.displayExpiresInSeconds > 90
        ))
      ))) throw new Error("DISPLAY_GRANT_INVALID");
  // Revalidate the returned opaque ID before constructing any socket URL.
  const signalUrl = cloudSignalSocketUrl(trustedOrigin, result.sessionId);
  return Object.freeze({ sessionId: result.sessionId, signalUrl,
    socketTicket: result.socketTicket, iceServers,
    initialViewport: result.viewport,
    resumed,
    resumeSequence: resumed ? result.resumeSequence : 0,
    ...(vnc ? {
      displayTransport: "vnc",
      ...(result.displayOrigin ? { displayOrigin: new URL(result.displayOrigin).origin } : {}),
      ...(hasDisplayGrant ? {
        displayPath: result.displayPath,
        displayTicket: result.displayTicket,
        displayExpiresInSeconds: result.displayExpiresInSeconds,
      } : {}),
    } : {}),
    decodedMediaReady: false });
}
