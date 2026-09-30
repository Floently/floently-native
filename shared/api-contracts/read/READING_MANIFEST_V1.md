# ReadingManifest v1

`ReadingManifest` is the language-neutral contract between Floently document ingestion, TTS orchestration, caches, and native playback sessions.

## Core invariant

The document is the public media item. Segments are hidden transport units.

No iOS/Android UI, lock-screen surface, system media session, resume API, or analytics event may expose a segment as though it were the document.

## Canonical text and offsets

Before indexing, text is normalized to LF line endings and trimmed at the outer document boundary. `textScalarLength`, `scalarStart`, and `scalarEnd` are counts of Unicode scalar values in that canonical normalized text. They are not UTF-8 byte offsets, Java/Kotlin UTF-16 code-unit offsets, or Swift grapheme-cluster indexes.

Every platform adapter is responsible for converting its native string indexes to/from these scalar offsets. This removes ambiguity across Swift, Kotlin, Rust, Python, Go, and TypeScript.

Segment text must correspond to the canonical source range represented by `scalarStart..<scalarEnd`; segmentation may skip inter-segment whitespace, but it may not silently rewrite the source coordinates.

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
