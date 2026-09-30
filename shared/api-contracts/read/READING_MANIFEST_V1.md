# ReadingManifest v1

`ReadingManifest` is the language-neutral contract between Floently document ingestion, TTS orchestration, caches, and native playback sessions.

## Core invariant

The document is the public media item. Segments are hidden transport units.

No iOS/Android UI, lock-screen surface, system media session, resume API, or analytics event may expose a segment as though it were the document.

## Required mappings

Every implementation must support:

- document progress -> segment index + local offset;
- segment index + local offset -> document progress;
- document elapsed time -> segment + local media offset;
- segment actual duration updates -> corrected logical timeline;
- source character/word offset -> logical segment;
- logical segment -> source character/word range.

## Versioning

Schema version 1 is stored in:

- `shared/api-contracts/read/reading-manifest-v1.schema.json`

Breaking changes require a new schema version. Swift, Kotlin, Rust, Python, Go, and TypeScript implementations must not silently reinterpret an existing version.
