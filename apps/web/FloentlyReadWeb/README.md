# Floently Read Web — next-generation polyglot app

This workspace is the **parallel next-generation** Floently Read web application. It does not replace or modify the current production web app in `Floently/flowreader`.

## Technology ownership

The web rebuild follows the repository-wide controlled-polyglot standard:

```
Public + signed-in UI
React 19 + TypeScript
        |
        +--> browser-native history / IndexedDB / Cache Storage / Media Session
        |
        v
app-owned ReadRuntimeProvider
        |
        +--> WebPlaybackSession
        +--> ReadDocumentSession
        |
        v
Web Worker
        |
        v
Rust Read Core compiled to WebAssembly
        |
        v
ReadingManifest v1
```

Responsibilities:

- **React/TypeScript**: landing/auth/product UI, routing, forms, accessibility, bounded reader rendering and application state.
- **Browser-native APIs**: history, IndexedDB document storage, physical media, Media Session and durable audio cache.
- **Web Worker**: CPU isolation and ownership of full ReadingManifest JSON.
- **Rust/WASM**: deterministic document segmentation, document-wide duration/seek mapping, canonical scalar mapping and prefetch planning.
- **Existing Floently/Learn auth API**: account/session authority. The new web build does not create a separate identity backend.
- **ReadingManifest v1**: language-neutral semantic contract shared with Swift, Kotlin and backend services.

The React reader deliberately receives only a bounded nearby document window. Hidden TTS segments are transport details, never the public document/media identity.

## Product routes

Public:

- `/` — READ landing page
- `/login`, `/signin`, `/auth/login` — shared account login
- `/signup`, `/register`, `/auth/signup` — shared account registration

Protected:

- `/app/library` — browser-local library for the migration build
- `/app/import` — text/Markdown import
- `/app/reader/:documentId` — worker-backed virtualized reader
- `/app/reader` — current document reader
- `/app/browser` — Browser V2 migration boundary
- `/app/preferences` — persistent theme, speed, voice, highlight and semantic-reader presentation controls
- `/app/account` — shared Floently account, Read entitlement and current-period quota
- `/app/subscription` — backend-authoritative regional Read pricing and checkout

Protected redirects accept only same-origin `/app/*` `returnTo` values.

## Authentication

The product shell reuses the existing Read/Learn auth contract:

- `POST /api/v1/auth/login/password`
- `POST /api/v1/auth/register/password`
- `GET /api/v1/auth/session`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/google/config`
- `POST /api/v1/auth/login/google`

Session bootstrap is cookie-first, with bearer fallback for compatibility. The TTS adapter resolves the currently authenticated bearer token lazily at request time.

## Read entitlement, quota and billing

The web app keeps identity and payment authority outside the browser:

- shared Floently/Learn auth remains the session authority;
- Read entitlement is normalized from backend session policy rather than inferred from UI state;
- `GET /api/v1/read/usage` supplies current-period usage and limits;
- `GET /api/v1/billing/read-pricing` supplies regional Reader/Creator pricing;
- `POST /api/v1/billing/read-checkout` creates the authenticated checkout session;
- checkout URLs must be valid HTTPS URLs returned by the backend;
- the optional configured customer-portal URL is also accepted only when it is valid HTTPS.

The frontend never grants itself a paid plan and never constructs a Stripe Checkout
session URL. An optional existing-subscriber portal can be configured with
`VITE_STRIPE_READ_BILLING_PORTAL_URL`.

## Document and playback lifetime

`ReadRuntimeProvider` is mounted around the protected product shell, not around individual pages.

That means:

- internal navigation does not recreate PlaybackSession;
- playback can continue while moving Reader → Library → Import/Preferences;
- the active worker manifest stays alive while it is the current document;
- switching documents drops the previous manifest only after the replacement is ready;
- leaving the protected app tears down the worker/player cleanly.

The physical audio element never becomes the public document clock. Physical asset time is mapped into the logical document timeline.

## Synced project library and source-preserving ingestion

The next-generation web app now separates **semantic Read projects** from
**original visual files** instead of forcing every source through one storage
or rendering model.

Cloud project contract:

- `GET /api/v1/projects`
- `GET /api/v1/projects/:id`
- `POST /api/v1/projects/from-text`
- `POST /api/v1/projects/upload`
- `PUT /api/v1/projects/:id/progress`
- `DELETE /api/v1/projects/:id`

Requests reuse the current Floently bearer token and browser-default same-origin
cookie behavior. Cross-origin project APIs are not forced into credentialed CORS
mode.

Supported ingestion paths in this slice:

- **PDF** — original blob is persisted in IndexedDB and opened immediately as
  the browser-rendered PDF pages. Cloud extraction runs behind that visible
  source and links its project id back to the original local record.
- **DOCX / EPUB / HTML / Markdown / TXT** — canonical upload/extraction creates
  a synced project and opens the Rust/WASM Reader. If canonical extraction
  cannot finish, the original file is still preserved as a local fallback.
- **Pasted text** — saved directly as a synced cloud project.
- **Website** — never copied into a document project; it is handed to the live
  Browser V2 route and opened only inside remote Chromium.
- **Google Drive** — Picker selection/download produces the same browser
  `File` abstraction, then follows the identical rules above.

Large files use the production fast-open policy: at 1.5 MB and above,
the **foreground wait** has an 1800 ms first-open budget so a cold extractor
cannot prevent the original source from appearing. The canonical upload/
extraction promise is not cancelled by that budget; it continues behind the
visible original and links its project when it completes. The original-document
surface can retry the reading layer, and a retry joins the same in-flight task
when one still exists.

File fingerprint reuse samples the first/last 64 KiB plus stable metadata and
uses SHA-256 when available. The original visual-file store computes a full
hash in the background but always merges that result into the latest record so
a concurrently attached cloud `projectId` cannot be overwritten. Background
dedupe never deletes the id of an already-open original; Library collapses
duplicate identities for presentation, while explicit removal deletes the full
local duplicate identity set. PDF Blob identity is kept stable while semantic
metadata is polled so the visible PDF does not reload every polling interval.

Synced reading progress stores canonical character/scalar position, logical
segment position, percentage, playback speed and voice. Resume maps the saved
character offset back through the Rust scalar→logical-time function instead of
guessing from paragraphs.

Google Drive deployment also requires:

- `VITE_GOOGLE_DRIVE_API_KEY`
- `VITE_GOOGLE_DRIVE_APP_ID`

The OAuth client id remains owned by the shared Floently auth backend and the
same Google Identity Services loader is shared between login and Drive.

## Browser-local migration library

The first product-shell slice stored text/Markdown in IndexedDB before the
synced project API was ported. That store remains readable only as a migration
compatibility layer so documents imported during early development are not
silently lost.

New text/file imports use the synced project API described above. Original
visual files use the dedicated original-document IndexedDB store, and websites
use Browser V2.

## Reader virtualization

The virtualized reader:

- asks the worker for a bounded nearby segment window;
- never mounts the entire multi-hour manifest/text in React;
- follows the active logical playback region by default;
- lets the person browse earlier/later without interrupting playback;
- offers “Follow playback” to rejoin;
- maps clicks on visible text to the logical document timeline;
- exposes region-level active state only.

Word-level highlighting is intentionally deferred until backend timing payloads are verified and can be mapped to canonical scalar offsets without guessing.

## Persistent reading preferences

Read preferences are app-owned defaults rather than paragraph/TTS-asset state. They
survive page reloads and document replacement in the current browser profile.

Current persistent controls:

- Read chrome theme: light, dark or live system preference;
- playback speed from 0.5× through 3×;
- preferred voice plus normalized language affinity;
- visual highlight mode: none, sentence, or capability-gated word;
- semantic-reader typeface, text size and line spacing.

Theme, reading mode and selected voice reuse the proven production storage keys so
an eventual cutover does not silently discard those preferences. New presentation
settings use versioned Read-specific keys. Document/project resume state may restore
its own saved speed and voice without rewriting these global defaults.

The preference layer never restyles source-authoritative surfaces: original PDF pages
remain browser-rendered and Browser V2 keeps the live remote webpage visible. Word
highlighting remains disabled until the TTS timing contract provides verified
canonical scalar mappings; a stored word preference safely falls back to sentence
highlighting rather than fabricating timestamps.

Reader text regions are pointer- and keyboard-operable. Pointer selection maps the
chosen vertical point into the region timeline, while Enter/Space starts from the
focused region boundary.

## Browser V2 migration

The next-generation Browser route reuses the hardened authenticated remote-browser transport from the current production Browser V2 while moving reading semantics onto the polyglot Read runtime.

Key invariants:

- the rendered remote Chromium page remains the primary visible surface;
- pressing **Read page** never swaps the page for extracted text;
- DOM extraction is private semantic input to the Rust/WASM document model;
- one app-owned remote-browser client survives internal `/app/*` navigation;
- the WebRTC/noVNC display stays mounted with that client so Browser → Library → Browser does not allocate a second Chromium session;
- one app-owned `WebPlaybackSession` owns TTS, speed, voice, seek, cache and Media Session;
- selected speed remains app/session-owned across browser page/document replacement; same-language selected voice is preserved as well;
- automatic noVNC display retries are bounded; a person can explicitly retry display or replace the secure browser session afterward;
- page sentence anchors are mapped to canonical Unicode scalar ranges;
- playback's canonical scalar cursor maps back to remote page highlighting;
- Follow controls scrolling only; highlighting can remain active while Follow is off;
- Read From Here revalidates the current private page before seeking;
- page revision/navigation invalidates browser-owned speech immediately;
- stale browser events cannot stop a different library document that later owns playback.

The Browser V2 transport remains one owner-authenticated channel. Display tickets stay out of URLs/query strings, remote viewport revisions are validated, and mobile noVNC touch is sent through the existing owner input channel.

Runtime variable:

- `VITE_BROWSER_V2_ORIGIN` — authenticated Browser V2 HTTPS/WSS ingress. Required when the deployed Read host cannot proxy the Browser V2 WebSocket/session path.

## TTS and audio cache

The provider adapter currently reuses the existing Read Render contract:

- default base: `https://flowreader-api.onrender.com`
- `POST /api/tts/prerender`
- `GET /api/voices/unified`
- optional authenticated bearer header

The audio cache uses Cache Storage + IndexedDB metadata and remains bounded by
bytes rather than a tiny file-count assumption. The default policy begins
pruning above 96 MiB and evicts least-recently-used, inactive assets toward a
72 MiB target. Active object-URL leases protect assets currently handed to the
player; releasing the last lease schedules another prune pass. Legacy metadata
without a byte size is hydrated from Cache Storage and treated conservatively
if it cannot be measured. A broad 256-entry metadata guard remains only as a
secondary cardinality bound. Cache APIs are still an optimization: direct media
playback remains the fallback when browser storage is unavailable.

Media Session publishes one logical document item even while hidden synthesis
assets change underneath it.

## Runtime variables

See `.env.example`.

Key variables:

- `VITE_AUTH_API_URL` — explicit shared auth API base for non-production hosts
- `VITE_API_URL` — general fallback API base
- `VITE_READ_API_BASE_URL` — TTS/voice API base
- `VITE_STRIPE_READ_BILLING_PORTAL_URL` — optional HTTPS customer billing portal

Known production host routing mirrors the existing web app: `floently.com` can use same-origin auth proxying while `read.floently.com`, `learn.floently.com` and `create.floently.com` resolve auth to `https://learn-api.floently.com`.

## Local build

From repository root:

```bash
bash scripts/build_read_web_wasm.sh
```

The script:

1. ensures the `wasm32-unknown-unknown` Rust target;
2. builds `shared/read-core-wasm` using `wasm-pack`;
3. writes generated bindings under `src/generated/read-core-wasm`;
4. installs web dependencies;
5. builds the production Vite bundle.

Generated WASM bindings are build artifacts and are not committed.

## Web visual system

Floently-owned Read chrome uses one CSS token vocabulary for product geometry:
spacing, shell gutters/content width, card/control radii, subtle borders, card
surfaces/elevation, muted text, accent state and motion duration. The token layer
has dark and light values while retaining the reduced-motion behavior described
below.

Landing, sign-in, sign-up, Library, Import, Preferences, Account and
Subscription now share the same geometry vocabulary rather than defining
separate card systems. The auth form is a deliberate card surface, while the
landing reader preview and principle cards reuse the same radius/spacing system
without losing their editorial roles. Responsive qualification checks public
and protected routes at desktop and phone widths for horizontal overflow.

These tokens apply only to Floently UI. Original PDF iframe contents and the
Browser V2 remote webpage remain source-authoritative and are not restyled by
the Read visual system.

## Deterministic browser qualification

The web workspace includes Playwright journeys that exercise the built application
against intercepted backend contracts. They cover landing/login/signup geometry,
protected auth routing, safe `returnTo` handling, core signed-in navigation,
account/quota/subscription presentation, source-preserving PDF fail-open behavior,
accessibility/localization persistence, and desktop/phone layout containment.

After the production bundle has been built:

```bash
cd apps/web/FloentlyReadWeb
npx playwright install chromium
npm run test:e2e
```

CI downloads the already-built web artifact into a separate `read-web-e2e` job,
installs Chromium, and runs the same journeys. No production credentials, live
checkout, or live TTS service is used by these deterministic tests.

## Accessibility and interface localization

The protected Read shell has an explicit keyboard/screen-reader foundation:

- a keyboard-visible skip link targets the protected app's main landmark;
- route changes restore focus to that main landmark instead of leaving keyboard
  focus on controls from the previous page;
- focus-visible styling remains strong in both Read light and dark themes;
- non-essential animation and transitions respect `prefers-reduced-motion`;
- playback failures are announced as meaningful errors rather than silent visual
  state.

Interface language is app-owned and independent of document language and voice
language. English is the fallback locale and Finnish is the first additional
interface locale. The selected locale persists in versioned browser storage,
updates `<html lang>`, synchronizes across tabs, and uses locale-aware plural
rules. Original PDFs and Browser V2 remain source-authoritative surfaces; locale
and accessibility chrome do not replace or restyle their source contents.

Physical VoiceOver/NVDA and cross-browser assistive-technology qualification is
still required before production cutover.

## Qualification still required before cutover

A green CI build does **not** authorize replacing the current production READ web app. Remaining qualification includes:

- live authenticated login/register/Google flows against deployed auth;
- live authenticated TTS and voice catalog;
- Safari/Chrome/Firefox cache and Media Session testing;
- long-document transition-gap measurement with real synthesized audio;
- live PDF/EPUB/document ingestion and cloud project synchronization against deployed services;
- live Browser V2 remote-session/page-preservation/reconnect qualification across supported browsers;
- live account/quota/regional-pricing/checkout and billing-portal qualification;
- physical VoiceOver/NVDA and multilingual interface review;
- long-session browser/device testing, including background/system-media behavior;
- migration/cutover checklist with rollback and production host routing validation.
