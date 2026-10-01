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
- `/app/preferences` — live playback speed/voice controls
- `/app/account` — shared Floently account/session

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

## Document and playback lifetime

`ReadRuntimeProvider` is mounted around the protected product shell, not around individual pages.

That means:

- internal navigation does not recreate PlaybackSession;
- playback can continue while moving Reader → Library → Import/Preferences;
- the active worker manifest stays alive while it is the current document;
- switching documents drops the previous manifest only after the replacement is ready;
- leaving the protected app tears down the worker/player cleanly.

The physical audio element never becomes the public document clock. Physical asset time is mapped into the logical document timeline.

## Browser-local library

The first product-shell slice stores imported text/Markdown in IndexedDB. This is intentionally an isolation layer while the production cloud library/project API is migrated.

PDF, EPUB, Office documents, web capture and cloud synchronization are **not** being faked through this store. They should arrive as dedicated ingestion/persistence adapters behind the same document/session interfaces.

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

## TTS and audio cache

The provider adapter currently reuses the existing Read Render contract:

- default base: `https://flowreader-api.onrender.com`
- `POST /api/tts/prerender`
- `GET /api/voices/unified`
- optional authenticated bearer header

The audio cache uses Cache Storage + IndexedDB metadata and remains bounded. Media Session publishes one logical document item even while hidden synthesis assets change underneath it.

## Runtime variables

See `.env.example`.

Key variables:

- `VITE_AUTH_API_URL` — explicit shared auth API base for non-production hosts
- `VITE_API_URL` — general fallback API base
- `VITE_READ_API_BASE_URL` — TTS/voice API base

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

## Qualification still required before cutover

A green CI build does **not** authorize replacing the current production READ web app. Remaining qualification includes:

- live authenticated login/register/Google flows against deployed auth;
- live authenticated TTS and voice catalog;
- Safari/Chrome/Firefox cache and Media Session testing;
- long-document transition-gap measurement;
- PDF/EPUB/document ingestion;
- cloud library/project synchronization;
- full Browser V2 page-preserving reader migration;
- account/billing/subscription feature parity;
- accessibility/i18n;
- Playwright route/auth/import/library/reader flows;
- long-session browser/device testing;
- migration/cutover checklist with rollback.
