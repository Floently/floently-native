# Floently Read Web — next-generation foundation

This is the **next-generation** web surface for Floently Read. It does not replace or modify the current production web reader.

## Architecture

```
React / TypeScript
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

- React/TypeScript: browser UI and presentation state.
- Web Worker: CPU isolation and ownership of full manifest JSON.
- Rust/WASM: deterministic document segmentation, duration estimation, logical-time mapping, progress mapping, and prefetch planning.
- ReadingManifest v1: shared semantic contract across web, Swift, Kotlin, Python, and backend services.

The React tree deliberately receives a lightweight manifest summary rather than the entire document text/manifest.

## Local build

From repository root:

```bash
bash scripts/build_read_web_wasm.sh
```

The script:

1. adds the `wasm32-unknown-unknown` Rust target if needed;
2. builds `shared/read-core-wasm` with `wasm-pack`;
3. writes generated bindings into `src/generated/read-core-wasm`;
4. installs web dependencies;
5. creates the production Vite bundle.

Generated WASM bindings are build artifacts and are not committed.

## Current functional slice

- paste text;
- import TXT/Markdown;
- build a canonical whole-document manifest off the UI thread;
- display whole-document words/duration/segment count;
- map public document progress into hidden segment/local position;
- compute the next 120 seconds of prefetch indexes.

Browser playback, Media Session, IndexedDB/cache, document virtualization, and real TTS asset integration are subsequent slices.


## Document-wide browser playback

The next-generation web runtime now owns playback above React screens:

```
ReadRuntimeProvider
  -> WebPlaybackSession
      -> Rust/WASM worker for logical document mapping
      -> ReadTtsProvider for hidden synthesis assets
      -> ReadAudioCache for bounded browser caching
      -> BrowserAudioEngine for physical media
      -> Media Session API for browser/OS controls
  -> React screens observe/control the session
```

`WebPlaybackSession` owns:

- public document elapsed time and total duration;
- play/pause/buffering/ended/error state;
- playback speed;
- voice identity;
- exact document seek requests;
- hidden segment transitions;
- 120-second time-horizon prefetch planning;
- resume persistence;
- Media Session metadata/actions;
- telemetry hooks.

The physical media element never becomes the public document clock. Physical
asset time is mapped proportionally into the hidden segment's logical document
range until actual-duration manifest refinement is implemented.

### Current TTS adapter

The first provider adapter reuses the existing Floently Read Render contract:

- default base: `https://flowreader-api.onrender.com`;
- `POST /api/tts/prerender`;
- `GET /api/voices/unified`;
- supports optional bearer-token injection;
- consumes audio URL, server cache key/hit, voice/provider/model metadata, and
  raw timing metadata when supplied.

The adapter intentionally does not guess the unit of the legacy ambiguous
`duration` property. Explicit `durationMs`/`duration_seconds` variants are
accepted; otherwise the browser media element supplies physical asset duration.

Word timing remains raw until the backend timing payload is verified and can be
mapped safely to ReadingManifest canonical scalar offsets.

### Browser audio cache

The initial cache layer:

- keys assets by server cache key or SHA-256 synthesis identity;
- uses Cache Storage when the audio response is CORS-readable;
- records cache metadata in IndexedDB;
- keeps a bounded maximum of 32 durable entries;
- uses object URLs for cached blobs;
- falls back to the remote audio URL if browser caching is unavailable.

### Still not qualified

A successful CI build does **not** prove Speechify-class playback quality.
Before browser playback is considered complete we still require:

- live authenticated Render TTS integration testing;
- CORS/cache behavior testing in Safari/Chrome/Firefox;
- real long-document transition-gap measurement;
- Media Session behavior on supported desktop/mobile browsers;
- interruption/device-route behavior where browsers expose it;
- actual timing normalization for word highlighting;
- Playwright and physical-device long-session tests.
