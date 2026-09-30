# ReadingManifest v1

`ReadingManifest` is the language-neutral contract between Floently document ingestion, TTS orchestration, caches, and native playback sessions.

## Core invariant

The document is the public media item. Segments are hidden transport units.

No iOS/Android UI, lock-screen surface, system media session, resume API, or analytics event may expose a segment as though it were the document.

## Canonical text and offsets

Before indexing, text is normalized to LF line endings and trimmed at the outer document boundary. `textScalarLength`, `scalarStart`, and `scalarEnd` are counts of Unicode scalar values in that canonical normalized text. They are not UTF-8 byte offsets, Java/Kotlin UTF-16 code-unit offsets, or Swift grapheme-cluster indexes.

Every platform adapter is responsible for converting its native string indexes to/from these scalar offsets. This removes ambiguity across Swift, Kotlin, Rust, Python, Go, and TypeScript.

Segment text must correspond to the canonical source range represented by `scalarStart..<scalarEnd`; segmentation may skip inter-segment whitespace, but it may not silently rewrite the source coordinates.

## Progressive synthesis

Document indexing and audio generation are separate pipelines.

A segment may carry optional `synthesis` state:

- `pending` — indexed but not yet being generated;
- `generating` — synthesis work is active;
- `ready` — a playable `audio` asset must exist;
- `failed` — generation failed and may expose a stable error code/message.

The document does not change identity when a segment changes synthesis state. A native player may start from the **contiguous ready prefix** but must not skip an earlier pending segment merely because a later segment is already cached.

Optional `audio` metadata includes the asset URI, actual duration, content hash/cache key, voice/provider/model identity, and format. Optional `timings` map document-wide canonical scalar ranges to local audio milliseconds for highlighting and exact cursor reconstruction.

Actual audio durations may refine the logical timeline in later revisions without exposing physical segment identity to UI or system media surfaces.

A canonical fixture lives at:

- `shared/api-contracts/read/fixtures/reading-manifest-v1.sample.json`

Semantic invariants are checked by:

- `scripts/validate_read_manifest_contract.py`

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
