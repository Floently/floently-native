# Floently Read web production cutover and rollback runbook

Status: release-engineering procedure for the next-generation polyglot Read web
app.

This document is **not authorization to cut over production**. The current
production Read web app remains authoritative until the live qualification
gates below have passed and an explicit production-cutover decision is made.

## 1. Scope

This runbook covers only the web application and its production host/routing.

It does not authorize or require:

- an iOS build, TestFlight upload or App Store release;
- an Android release;
- retirement of the Read browser extension;
- migration of Learn or Create;
- changes that bypass backend entitlement/payment authority.

The new web app continues to use the approved polyglot boundaries:

- React + TypeScript for Read web UI/application orchestration;
- Rust/WASM for deterministic document/read-timeline semantics;
- existing authenticated APIs for identity, projects, billing policy and TTS;
- Browser V2 for the isolated authenticated remote-browser surface.

## 2. Non-negotiable rollback principle

The production switch must be a routing/deployment change, not a destructive
replacement.

Until post-cutover acceptance is complete:

1. keep the last known-good legacy/current production deployment intact;
2. keep its configuration and routing target recoverable;
3. do not delete the previous deployment, storage or extension compatibility
   contracts;
4. do not perform one-way data/schema migration solely for the web cutover;
5. keep backend APIs backward-compatible with the previous web client for the
   defined rollback window.

A rollback must be possible without rebuilding the previous application.

## 3. Candidate pin

Before qualification is signed off, record:

- candidate Git commit SHA;
- candidate CI workflow run IDs;
- preview deployment URL and deployment identifier;
- build-time public endpoint/origin configuration;
- Browser V2 service environment;
- auth/API/TTS environment;
- qualification date;
- browser/device versions tested;
- named person approving the production-routing change.

Never cut over from an unpinned floating branch.

## 4. Required pre-cutover evidence

All of the following are required before a production-routing change.

### Repository gates

- Rust Read Core tests green;
- Rust/WASM adapter tests green;
- ReadingManifest contract validation green;
- web TypeScript/Vite production build green;
- web unit tests green;
- Chromium Playwright journeys green;
- deterministic Firefox/WebKit Playwright journeys green;
- browser contract tests green;
- no unapproved iOS build marker present or required.

### Live preview gates

On the isolated HTTPS preview origin:

- password sign-in/sign-up/session restoration verified;
- Google/provider sign-in verified when enabled;
- auth CORS/cookie behavior verified for the preview origin;
- real voice catalog loads;
- real TTS starts, pauses, resumes, seeks and changes voice/speed;
- long-document sequential handoffs have captured diagnostics;
- human listening confirms no unacceptable repeat/skip/gap at hidden boundaries;
- Cache Storage/IndexedDB behavior verified under realistic quota pressure;
- background/foreground and Media Session behavior verified where supported;
- TXT/Markdown/DOCX/EPUB/PDF ingestion verified against live services;
- original PDF remains the visible source surface;
- project progress sync/resume verified across refresh/login/device as applicable;
- Browser V2 remote page stays visible while Read operates;
- Browser V2 navigation/reconnect/stale-revision behavior verified;
- regional pricing/account/quota/checkout/billing portal verified;
- keyboard/focus/reduced-motion checks passed;
- VoiceOver/NVDA/TalkBack smoke checks completed for the supported matrix.

Use `docs/READ_WEB_PREVIEW_QUALIFICATION.md` as the evidence matrix.

## 5. Pre-cutover configuration review

Record the production values or deployment references for:

- auth API origin;
- general API origin;
- Read TTS/voice API origin;
- Browser V2 HTTPS/WSS origin;
- Google Picker public configuration when enabled;
- Stripe customer portal public URL when enabled.

Confirm:

- every browser-exposed value is intentionally public;
- no server secret/token is present in Vite variables;
- backend origin allowlists include the final production Read origin;
- CORS is not widened to `*` merely for migration;
- Browser V2 owner/session tickets remain short-lived transport credentials;
- production entitlement and checkout remain backend-authoritative.

## 6. Current-production preservation checkpoint

Before routing changes:

1. record the current production deployment identifier/commit if available;
2. record the current DNS/host routing target;
3. capture the current environment-variable/configuration set through the
   hosting provider's secure configuration mechanism;
4. confirm the previous deployment can still be selected or promoted;
5. confirm backend compatibility with both old and new clients;
6. record the exact rollback action and who has permission to execute it.

Do not place secret values in this repository or issue comments.

## 7. Cutover strategy

Prefer the least destructive routing mechanism supported by the chosen host.

Order of preference:

1. deployment alias/promotion that can be atomically reversed;
2. weighted/canary routing from a small share to 100%;
3. DNS record change with a deliberately reduced TTL prepared in advance.

Avoid a code commit whose only purpose is to hard-replace the legacy site when
the hosting layer can switch deployment targets instead.

### Canary, when supported

Suggested progression:

- internal/owner-only validation;
- small canary share;
- broader share after error/latency/auth checks;
- full production only after the smoke matrix stays clean.

The exact percentages/durations are an operational decision at cutover time;
they are not hard-coded by this document.

## 8. Immediate post-routing smoke test

Immediately after the production origin resolves to the candidate:

1. open a fresh private/incognito session;
2. verify landing page and static assets;
3. sign in with password;
4. verify an existing authenticated session restoration in a normal session;
5. open Library and a real synced project;
6. start real TTS;
7. pause/resume and seek;
8. change speed and confirm it persists across a hidden segment transition;
9. change voice and confirm playback remains on the same logical document;
10. open/import a document;
11. open an original PDF and confirm original pages remain visible;
12. start Browser V2 and load a real page;
13. start Read on the remote page and confirm the page is not replaced by text;
14. test Read From Here and Follow;
15. visit Account/Plan and verify live quota/pricing data;
16. verify checkout handoff without completing an unnecessary purchase;
17. verify logout and protected-route redirect;
18. inspect client/network errors and backend error rates.

Repeat a reduced smoke set in at least Chrome and Safari/Firefox according to
the supported launch matrix.

## 9. Hard rollback triggers

Rollback immediately if any of these occurs and cannot be resolved by a
configuration-only correction without increasing user risk:

- widespread sign-in/session failure;
- production CORS/cookie failure;
- entitlement bypass or incorrect paid/free access;
- checkout/pricing authority mismatch;
- TTS cannot start for otherwise eligible users;
- playback repeatedly skips/repeats/resets speed at segment boundaries;
- a document/source surface is replaced or corrupted unexpectedly;
- Browser V2 exposes/attaches the wrong session/frame or cannot fence stale
  navigation safely;
- data/progress writes are assigned to the wrong project/document/user;
- severe accessibility/navigation regression blocking core use;
- error rate, latency or crash behavior materially exceeds the previous
  production client;
- rollback observability is lost.

Do not keep a broken new deployment live merely to avoid switching back.

## 10. Rollback procedure

Use the hosting provider's reversible deployment/routing primitive.

1. freeze further production deployment changes;
2. restore the recorded previous production target;
3. verify DNS/alias propagation as applicable;
4. open a fresh session and run:
   - landing;
   - login/session restoration;
   - Library;
   - TTS start/pause;
   - account/logout;
5. verify backend health and error rates normalize;
6. record the incident, candidate SHA and exact failing journey;
7. keep the failed candidate available for forensic comparison when safe;
8. fix on a new candidate branch/preview; never patch production blindly.

A web rollback must not trigger an iOS build or native release.

## 11. Post-cutover acceptance window

Keep the previous production deployment available throughout the agreed
acceptance window.

During the window, monitor:

- auth success/error rates;
- TTS request/start failures;
- client errors;
- project/progress write failures;
- Browser V2 session/display failures;
- checkout/billing handoff failures;
- support reports about repeats/skips/gaps;
- browser-specific regressions.

Only after the acceptance window is explicitly closed may legacy hosting
resources be considered for retirement.

## 12. Legacy/extension compatibility review

The Chrome/desktop extension remains a protected compatibility mode.

Before retiring any legacy web surface:

- verify extension auth bridge compatibility;
- verify existing `flowReader.auth.*` storage/session assumptions;
- verify fallback links into the full Read app;
- verify backend APIs required by the extension remain supported;
- make an explicit retirement decision rather than assuming Browser V2 replaced
  the extension.

Browser V2 and the extension solve different authenticated-session ownership
problems and may coexist.

## 13. Completion record

When cutover is eventually approved and executed, record:

- old production target;
- new production target;
- candidate commit SHA;
- routing-change timestamp;
- operator/approver;
- smoke-test result;
- browser/device evidence references;
- whether any canary stage was used;
- rollback deadline/acceptance-window end;
- final decision: accepted or rolled back.

Until this record exists, production cutover is not considered completed.
