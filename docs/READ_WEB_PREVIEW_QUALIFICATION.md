# Next-generation Read web preview qualification contract

Status: repository-side deployment contract for the parallel polyglot Read web app.

This document does **not** authorize production cutover. The existing
`Floently/flowreader` production web app remains untouched until the new Read
web app has completed live qualification and an explicit cutover decision is
made.

## Isolation rules

The preview must satisfy all of these conditions before it is treated as a
qualification environment:

1. It has its own HTTPS origin and is not served from `floently.com`,
   `www.floently.com`, `read.floently.com`, `learn.floently.com`, or
   `create.floently.com`.
2. Production traffic is never redirected to the preview.
3. Every SPA route, including direct loads of `/app/*`, falls back to the
   built `index.html`.
4. Only public build-time `VITE_*` configuration is embedded in the client.
   API keys that are intentionally browser-public, such as Google Picker
   configuration, remain subject to provider origin restrictions. Server
   secrets, bearer tokens, Stripe secrets and Browser V2 display/socket tickets
   must never be build variables.
5. Identity, Read entitlement, usage limits, pricing and checkout remain
   backend-authoritative. Preview code must not grant itself a paid plan or
   construct payment-session URLs.
6. Browser V2 continues to use its separately authenticated HTTPS/WSS ingress;
   the preview host must not expose or proxy a public CDP/VNC control surface.
7. Deleting the preview deployment must be sufficient to remove it. No
   production DNS record, app-store configuration or native release lane is
   part of this preview.

## Required build-time configuration

The preview deployment must set:

- `READ_PREVIEW_ORIGIN` — the preview's own HTTPS origin. This is deployment
  validation metadata and is **not** exposed to the Vite client.
- `VITE_API_URL` — the backend base used for projects, usage and billing API
  calls that cannot be served by the static preview origin.
- `VITE_AUTH_API_URL` — shared Floently/Learn authentication API base.
- `VITE_READ_API_BASE_URL` — Read TTS and voice-catalog API base.
- `VITE_BROWSER_V2_ORIGIN` — Browser V2 authenticated HTTPS/WSS origin.

Optional public configuration:

- `VITE_GOOGLE_DRIVE_API_KEY`
- `VITE_GOOGLE_DRIVE_APP_ID`
- `VITE_STRIPE_READ_BILLING_PORTAL_URL`

Run before building/deploying:

```bash
node scripts/validate_read_web_preview_config.mjs
```

The validator rejects missing required values, non-HTTPS URLs, URL credentials,
query/fragment-bearing API origins where they are unsafe, a Browser V2 value
that is not a pure origin, and any production Floently hostname used as the
preview origin. It prints only origins/configured booleans, never API keys or
tokens.

## Backend allowlist gate

A successful frontend build is not enough. Before live qualification:

- the auth service must explicitly accept the preview origin for CORS and any
  cookie/credential behavior it requires;
- project/usage/billing APIs must explicitly accept the preview origin;
- the TTS/voice service must explicitly accept the preview origin;
- Google OAuth/Picker configuration, when exercised, must allow the preview
  origin;
- Browser V2 must accept the authenticated owner connection from the preview
  origin under the existing session-ticket contract.

Do not weaken a production CORS policy to `*` merely to make preview testing
pass.

## Qualification matrix

Record browser version, operating-system version, device, preview commit SHA,
preview URL, backend environment and evidence for every row.

| Surface | Chrome desktop | Firefox desktop | Safari desktop | Chrome Android | Safari iPhone/iPad |
| --- | --- | --- | --- | --- | --- |
| Landing, sign-in, sign-up, protected redirect | Required | Required | Required | Required | Required |
| Password login/logout/session restoration | Required | Required | Required | Required | Required |
| Google login, when enabled | Required | Required | Required | Required | Required |
| Library/project sync and progress resume | Required | Required | Required | Required | Required |
| TXT/Markdown/DOCX/EPUB ingestion | Required | Required | Required | Required | Required |
| Source-preserving PDF open/fail-open | Required | Required | Required | Required | Required |
| TTS start, pause, seek, speed and voice persistence | Required | Required | Required | Required | Required |
| Sequential media-start handoff diagnostics | Required | Required | Required | Required | Required |
| Media Session / lock-screen or system controls where supported | Required | Observe support | Required | Required | Required |
| Background/foreground resume without stale playback | Required | Required | Required | Required | Required |
| Cache hit after reload and quota-pressure fallback | Required | Required | Required | Required | Required |
| Browser V2 page remains visible while reading | Required | Required | Required | Required | Required |
| Browser V2 reconnect and stale-revision rejection | Required | Required | Required | Required | Required |
| Read From Here / highlight / Follow semantics | Required | Required | Required | Required | Required |
| Account usage, regional pricing and checkout handoff | Required | Required | Required | Required | Required |
| Keyboard/focus accessibility | Required | Required | Required | N/A | N/A |
| Screen-reader smoke test | NVDA/Chrome | NVDA/Firefox | VoiceOver/Safari | TalkBack/Chrome | VoiceOver/Safari |

Use the current stable browser release available on the qualification day. The
evidence record must include the exact tested version; "latest" by itself is not
acceptable evidence.

## Playback acceptance evidence

Enable the hidden qualification panel with `?readDiagnostics=1`. Capture its
sanitized JSON at the end of each long-reading run.

For at least one long source per browser/device:

- confirm the document remains one logical media item;
- exercise natural segment transitions at multiple playback speeds;
- exercise a long seek and voice replacement;
- background and restore the app where the platform permits;
- record sequential `segment_handoff` media-start latency;
- separately listen for and record perceptible acoustic gaps, repeats or skips.

The telemetry value is browser **media-start latency**, not acoustic silence.
Acoustic continuity requires human/device evidence.

## Browser V2 acceptance evidence

For each supported browser/device:

1. start the secure browser and load a real HTTPS page;
2. start Read and confirm the rendered remote page stays visible;
3. turn Follow off and confirm highlight can continue without forced scroll;
4. use Read From Here, then navigate/reload and confirm old anchors fail closed;
5. leave the Browser route and return; confirm one app-owned session is reused;
6. force a display reconnect and confirm obsolete grants/frames do not attach;
7. on mobile, exercise touch and focused text input with viewport-revision
   validation.

## Exit criteria

Issue #73 can close only when:

- an isolated HTTPS preview is actually provisioned;
- direct-load SPA routing works;
- the fail-closed configuration validator passes in that deployment;
- required backend origin allowlists are configured intentionally;
- the matrix has named evidence for the agreed supported browser/device set;
- no production routing or legacy web deployment was changed.

Passing deterministic GitHub CI alone does not close this issue.
