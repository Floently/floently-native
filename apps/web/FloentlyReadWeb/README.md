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
