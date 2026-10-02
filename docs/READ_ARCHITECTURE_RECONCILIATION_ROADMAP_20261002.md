# Floently Read Architecture Reconciliation Roadmap

**Recorded:** 2026-10-02  
**Repository:** `Floently/floently-native`  
**Current main at reconciliation:** `26689896d35ee1052fc52ddca23118da76e6147d`  
**Paper reviewed:** *Floently Read Native Architecture and Experience Design*, 1 October 2026  
**Paper native baseline:** `0051571c2b5c781aaef98fb88ecbe8d6cb399742`

This document converts the architecture paper plus a fresh repository reconciliation into the active implementation road for next-generation Read.

It does **not** authorize a production cutover, TestFlight/EAS/store release, or replacement of the existing supported React Native app. The current app remains the fallback until the native and polyglot replacements pass the completion gates below.

---

## 1. Executive direction

Read does **not** need a new architectural reset.

The current controlled-polyglot direction is correct:

- Swift/SwiftUI/UIKit own Apple UI and operating-system integration.
- AVFoundation owns Apple physical playback and media-session behavior unless measured evidence proves a lower-level engine is necessary.
- Kotlin/Compose/Views own Android UI and lifecycle integration.
- Media3/ExoPlayer/MediaSessionService own Android playback and background media behavior.
- Rust owns deterministic shared reading semantics: manifest validation, canonical scalar coordinates, document timing, source-position rules and shared contract logic.
- TypeScript/React owns web presentation and app coordination.
- Rust/WASM owns shared deterministic Read logic in the web workstream.
- Existing backend services remain authoritative for identity, projects, entitlement, synthesis and policy until a measured need justifies a new service boundary.
- Python/model workers are appropriate for model/evaluation work; they must not become the playback state authority.
- Additional languages or services require a real boundary, measurable benefit, an owner, an observable contract and a rollback/removal plan.

The product advantage should come from:

1. source-preserving reading;
2. fast and durable playback;
3. exact source-position ownership;
4. coherent native design;
5. reliable multilingual speech;
6. evidence-linked learning built on top of the same source anchors.

Do not optimize for the number of languages, frameworks, services, voices or AI buttons.

---

## 2. What Read is already getting right

### 2.1 Controlled polyglot boundaries

**Status: implemented direction; retain.**

The repository already follows the intended ownership split:

- iOS media state is app-owned and AVFoundation-backed.
- Android media state is service-owned and Media3-backed.
- Rust Read Core is bridged to native and compiled to WASM for web.
- React screens do not own the canonical document timeline.
- the physical audio segment is not exposed as the public book/document identity.

This is the correct foundation and should not be replaced by a cross-platform UI/media abstraction.

### 2.2 One logical document above hidden synthesis clips

**Status: implemented foundation; continue hardening.**

Read now treats the document as one logical media item while hidden TTS segments change underneath it.

This is the correct public contract for:

- document-wide seek;
- lock-screen/system timeline;
- persistent mini/full player;
- speed and voice continuity;
- progressive synthesis;
- later source-linked highlighting and notes.

### 2.3 Durable playback ownership above screens

**Status: implemented foundation.**

The player/session is long-lived relative to individual screens. This is materially better than paragraph-owned playback.

The target remains one public playback/session interface per active reading session.

### 2.4 Original-source preservation

**Status: strong direction; native and web both moving correctly.**

Live pages remain browser surfaces rather than being replaced by extracted text.

The merged web work now also separates semantic Read projects from original visual files:

- source-preserving PDF handling;
- background canonical extraction;
- original-file persistence;
- synced cloud projects;
- Rust-based cursor mapping;
- Google Drive ingress;
- race-safe progress persistence.

This “original source + semantic sidecar” model is a core Read invariant.

### 2.5 Web controlled-polyglot foundation

**Status: implemented and merged in multiple slices.**

The next-generation web app now has:

- React/TypeScript shell;
- Rust/WASM document core;
- worker-owned manifest state;
- document-wide PlaybackSession;
- virtualized reader;
- Browser V2 page-preserving reading;
- synced ingestion/library on current `main`.

The web workstream remains independently releasable and must not be counted as native parity.

### 2.6 CI discipline

**Status: useful but not release qualification.**

Repository CI already covers substantial contract and compile evidence:

- ReadingManifest contract;
- Rust core;
- WASM adapter;
- native Rust FFI;
- Browser contracts;
- web production build and tests;
- iOS simulator compile;
- Android debug compile.

A green CI run is necessary, not sufficient, for release.

---

## 3. Reconciliation fact: most native paper findings are still current on main

The paper reviewed native `main` at `0051571c...`.

Current `main` is `26689896...`, whose direct parent is `0051571c...`. The intervening merged change is the web ingestion/library PR #34.

Therefore the paper's reviewed native implementation is still materially the native implementation on current `main`.

Open native PRs may address pieces of the paper, but open code is not counted as integrated behavior.

---

## 4. Immediate native P0 reliability work

This is the first implementation gate. Do not expand broad native product scope until these items have clear evidence.

### P0-1. Fix iOS emitted reading-visual JavaScript

**Paper finding:** F7  
**Current main:** confirmed still present.

`ReadBrowserModel.swift` creates Swift strings named `activeValue` and `pulseValue`, but the emitted JavaScript contains literal JavaScript references:

`const active = (activeValue);`

`const pulse = (pulseValue);`

The JavaScript does not receive those Swift values and can fail before reaching the page.

**Required change**

- serialize/encode script arguments safely;
- observe JavaScript execution errors;
- test active, inactive, pulse, pause and navigation cleanup;
- do not replace or reflow the webpage.

**Collision note:** PR #21 also modifies `ReadBrowserModel.swift`. Reconcile rather than overwrite it.

### P0-2. Fence native browser extraction and source results

**Paper finding:** F8  
**Current main:** confirmed missing on iOS `readPage()`.

Every asynchronous extraction/synthesis completion must carry and validate:

- browserSessionId;
- navigationGeneration;
- source/document revision;
- request/session generation.

Cancellation is not enough. Late results must fail closed.

**Acceptance**

- begin slow extraction;
- navigate immediately;
- repeat with selection, close and reload;
- no obsolete page text may begin or resume speech.

**Existing precedent:** merged web Browser V2 already contains revision/generation fencing concepts that can inform the contract without copying the remote-browser transport.

### P0-3. Publish first playable audio before the selected batch finishes

**Paper finding:** F1  
**Current main:** confirmed.

Both native progressive coordinators select a horizon and synthesize/download the selected segments sequentially before returning the list.

The current passage can therefore be ready while a later segment still blocks first sound.

**Required change**

- prepare/publish the current segment first;
- load/play it as soon as verified if current play intent still requests playback;
- continue the forward horizon separately;
- a later prefetch failure must not discard the already playable current segment.

**Performance evidence target**

- measure cached and uncached tap-to-first-audio separately;
- preserve correctness before optimizing concurrency.

### P0-4. Qualify append-only physical queue refill

**Paper finding:** F2  
**Open work:** PR #40.

PR #40 has the correct architectural intention:

- iOS append behind the current AVQueuePlayer item;
- Android add Media3 items instead of ordinary queue replacement;
- retain replacement only for voice/asset/seek-window changes.

**Current repository status**

PR #40 is open. Its latest workflow failed Android compilation in `ReadDocumentTimelinePlayer.kt` with syntax/type errors. It is not integration-ready.

**Required change**

1. repair compile correctness;
2. rebase/reconcile against current `main`;
3. preserve source cursor and play intent;
4. qualify with real audio capture.

Append-only code does not itself prove acoustic continuity.

### P0-5. Replace file-count pruning with durable audio ownership

**Paper findings:** F3 and F4  
**Current main:** confirmed.

Native caches still prune to a fixed 32 files.

Missing:

- byte budget;
- active-file leases;
- complete local rendition index;
- atomic metadata ownership;
- offline-first lookup;
- orphan cleanup;
- pinned-download semantics;
- cold-start offline bundle verification.

**Required architecture**

Introduce a durable `AudioAssetRepository` that:

- resolves a complete synthesis identity locally before network work;
- verifies bytes/checksum;
- commits atomically;
- leases playing/queued assets;
- prunes by byte/storage policy;
- separates automatic cache from user-pinned download;
- survives process restart.

**Acceptance**

- two-hour session;
- force eviction while listening;
- backward seek after cache pressure;
- cold start in airplane mode for a downloaded item;
- clearing cache cannot delete an active leased asset.

### P0-6. Cross-language source-coordinate correctness

**Paper risk:** scalar/string index mismatch.

The canonical contract uses Unicode scalar positions. Swift grapheme indices, Kotlin UTF-16 offsets, JavaScript string offsets and Rust byte offsets must never be treated as interchangeable.

**Acceptance corpus**

- emoji;
- combining characters;
- Finnish;
- mixed scripts;
- CJK;
- RTL;
- ligatures/soft hyphens where source adapters can produce them.

Round-trip tests are required across Rust, Swift, Kotlin and JavaScript boundaries.

---

## 5. P1 data-contract work that unlocks the rest of the product

### P1-1. Source-anchor-first resume

**Paper finding:** F5  
**Current main:** confirmed.

Current native resume records still persist document/revision + logical time + rate, but no durable source anchor/rendition identity.

**Target resume record**

- accountId;
- documentId;
- sourceRevisionId;
- blockId/locator;
- canonical scalar position;
- quote/context fallback;
- renditionId;
- local audio offset;
- selected voice;
- playback rate;
- update sequence/time.

Source anchor is primary. Logical/audio time is an accelerator only when the rendition still matches.

### P1-2. Separate source revision, audio rendition and user state

Do not compress these into one mutable document object.

Minimum model:

**Document**
- documentId;
- owner/workspace;
- title;
- source type;
- original asset identity;
- permissions.

**SourceRevision**
- sourceRevisionId;
- source hash;
- parser version;
- canonical text hash;
- reading-order version.

**Block/Anchor**
- block ID/type;
- scalar range;
- language/section;
- page geometry, EPUB locator or DOM quote/context.

**AudioRendition**
- renditionId;
- voice;
- provider;
- model/version;
- pronunciation version;
- synthesis settings;
- asset identities.

**TimingMap**
- renditionId;
- scalar ranges;
- audio times;
- granularity;
- confidence;
- provenance.

**User state**
- resume anchor;
- voice/rate;
- annotations;
- study evidence;
- sync operation IDs.

### P1-3. Complete rendition identity

**Paper finding:** F6  
**Current main:** confirmed weak fallback identity.

Separate:

1. canonical text hash;
2. synthesis-request hash;
3. audio-byte checksum;
4. timing-map version/identity.

Synthesis identity must include relevant provider/model/pronunciation/settings fields.

A provider/model change must not silently reuse incompatible audio or timing.

### P1-4. Capability-based highlighting

**Paper finding:** F9.

Current visual emphasis can identify a region, but region emphasis is not precise sentence/word synchronization.

Only expose:

- word tracking when validated word/phoneme timing maps back to canonical source;
- sentence/phrase highlighting when sentence/phrase marks are validated;
- approximate region/progress emphasis when only duration estimates exist.

Never fabricate precise karaoke highlighting from text length.

---

## 6. Complete native product surface

Native Read is currently a substantial browser/playback foundation, not yet the complete product described by the paper.

Build explicit modules for:

### Product shell/routing

- account context;
- durable playback session;
- deep links by document + source anchor;
- one session across navigation.

### Add to Read

One chooser that routes to:

- document;
- paste;
- live website;
- scan;
- connected provider where available.

The chooser captures intent; it does not parse documents.

### Import job coordinator

Durable phases:

- received;
- validated;
- original available;
- preview ready;
- partially indexed;
- fully indexed;
- audio available.

Support cancellation, retry and first-usable-content before whole-job completion.

### Source adapters

- PDF: preserve original geometry and map semantic anchors to page coordinates;
- EPUB: evaluate Readium before inventing a new publication stack;
- DOCX/structured text: preserve headings, lists, tables, links/images and reading order;
- scans: preserve page images and expose OCR as a semantic layer, not as a replacement page.

### Native Library

- repository-backed;
- search;
- sorting/filtering;
- folders/collections where adopted;
- recent activity;
- progress;
- true offline/download state;
- stable/paged item IDs;
- account isolation.

### Search/navigation

Search results resolve to source anchors. Opening a search result should not mutate the active playback queue until the person chooses to read.

### Annotation/selection

Source-anchored:

- bookmark;
- highlight;
- note;
- Listen;
- Explain;
- Translate;
- Create study card.

Failed anchor migration must become an explicit orphan/review state, never a silently misplaced note.

### Downloads

A “Downloaded” state is valid only when the required offline bundle is actually present.

Automatic cache and user-pinned downloads are separate concepts.

---

## 7. Design-system correction

### Current repository finding

iOS and Android still encode different Read visual systems:

- iOS: dark violet/cyan direction;
- Android: amber accent and white cards;
- both still use 28-unit card radius in shared implementations;
- published target uses 24-unit cards;
- shared design values are duplicated rather than generated from one versioned token authority.

### Required direction

Create one language-neutral, versioned token source and generate/adapt platform values.

Shared semantic system should cover:

- canvas/surface/raised colors;
- primary/secondary/tertiary text;
- action/brand colors with verified contrast;
- spacing scale;
- margins;
- field/button/card/sheet radii;
- touch target/control heights;
- typography roles;
- motion durations;
- light/dark/increased-contrast variants.

Native rendering remains native.

### Priority component catalogue

Implement and visually qualify:

- ContinueCard;
- DocumentRow;
- SourceActionRow;
- AddSourceSheet;
- ImportProgress surface;
- PlaybackDock;
- full Player;
- Library filters/search;
- Reader chrome;
- Settings/account rows.

Cards are for meaningful grouped/resumable content. Repeated library records should normally remain quiet rows, not nested rounded rectangles.

### Accessibility is part of the component contract

Every relevant component needs:

- loading;
- ready;
- pressed;
- focused;
- selected;
- disabled;
- failed;
- offline;
- large-text;
- screen-reader semantics.

Test VoiceOver/TalkBack, large text, reduced motion, contrast, keyboard/switch navigation and localization fixtures.

---

## 8. Reader composition rule

The source must dominate the reading screen.

For a PDF or live website:

- retain the source’s original presentation;
- add reversible semantic highlight/overlay only;
- keep the source interactive where appropriate;
- put native Read controls around/above/below the source;
- do not apply app reflow typography to an external website;
- Follow is user-controlled;
- user scrolling suspends automatic follow;
- provide an explicit action to resume following.

Mini and full player bind to the same durable session. Opening/closing player surfaces cannot reset voice, rate or cursor.

---

## 9. Learning and AI: intentionally after source correctness

Do not rush broad AI features ahead of the source-anchor/revision model.

Initial Read learning scope should remain small and dependable:

- source-linked explanation;
- vocabulary in context;
- editable evidence-linked study cards;
- optional review queue;
- source return from every factual output.

Every generated artifact stores:

- source revision;
- source anchor(s);
- model/version;
- generation/evaluation metadata as needed.

Unsupported claims are withheld or clearly labeled. Model-generated citations are not trusted without source validation.

Do not count listening minutes, page progress or streaks as mastery.

Learning release evidence should include:

- human answer validity;
- multilingual review;
- citation/grounding validity;
- revision handling;
- prompt-injection safety;
- delayed-recall/task-success evaluation.

---

## 10. Billing and entitlement

Backend/account-level authority remains the source of truth.

The web account/billing PR #42 correctly moves toward:

- backend Read policy;
- typed quota;
- regional pricing;
- authenticated checkout creation;
- no frontend-created entitlement.

**Current status:** PR #42 passed its original full CI but is now based on the pre-#34 main and is non-mergeable. Rebase/reconcile before counting it as integrated.

Native launch later requires StoreKit/Play Billing adapters feeding verified purchase state into authoritative account-level reconciliation, including:

- pending;
- active;
- expired;
- refunded;
- grace period;
- restore;
- account-linking races.

A stale local boolean must never grant paid synthesis.

---

## 11. Open PR reconciliation

### PR #21 - native browser auth/recovery

- direction: local platform browser ownership is correct;
- CI: last recorded run green;
- status: draft and currently non-mergeable;
- overlaps `ReadBrowserModel.swift` and related browser files.

Action: rebase/reconcile before implementing overlapping F7/F8 changes. Do not overwrite it blindly.

### PR #40 - append-only progressive refill

- direction: correct;
- status: open;
- latest CI: Android compile failure;
- failure includes syntax/type errors in `ReadDocumentTimelinePlayer.kt`.

Action: repair/rebase and then qualify acoustically.

### PR #42 - web account/billing

- direction: correct;
- original exact-head CI: green;
- status: draft, stale against current main/non-mergeable after #34.

Action: rebase/reconcile, preserve backend-owned entitlement semantics, rerun exact-head CI.

### PR #44 - web preferences/reader presentation

- direction: correct on persistent defaults and capability-gated highlighting;
- status: draft;
- based on current main;
- latest CI: web/WASM build failure;
- TypeScript parse errors currently reported in `BrowserWorkspace.tsx`.

Action: repair syntax/build, rerun CI, then complete accessibility/visual QA.

### PR #5 - old TestFlight release branch

This old release PR predates the current reconstruction and conflicts with the standing instruction not to produce another iOS release build without explicit agreement.

Action: mark stale/blocked or close with an explanatory handoff. Do not trigger a TestFlight/store build.

---

## 12. Documentation conflict to resolve

The parity registry still states that authenticated arbitrary-site desktop reading should remain extension-first and warns against proxying arbitrary signed-in sessions through the web app.

Merged PR #31/current web README now define the polyglot `/app/browser` around Browser V2 remote Chromium.

This is an architectural decision conflict.

Do not silently let both documents remain authoritative.

Required decision record:

- intended supported use cases for extension reading;
- intended supported use cases for Browser V2;
- privacy/security boundary;
- authentication capability and limitations;
- operational cost/availability;
- fallback behavior;
- whether one is migration-only, complementary or replacement.

Until reconciled, implementation must not assume that one paragraph in an older document automatically cancels merged behavior or vice versa.

---

## 13. Quality targets to turn into evidence gates

These are engineering targets, not claims about current performance.

- visible control acknowledgement: <=100 ms p95;
- cached first audio: <=0.8 s p95;
- uncached first audio: <=3 s p50, <=6 s p95 on declared network profile;
- cached seek: <=0.5 s p95;
- controlled artificial boundary gap: aim <80 ms p95;
- rate/voice resets: zero across 1,000 automated transitions;
- stale source results committed: zero in race suite;
- sentence highlight onset error: <250 ms p95 when valid marks exist;
- long-session reliability: two-hour device run with interruptions/outages;
- memory: no continuing growth after configured queue/cache limits;
- core scrolling/controls: meet representative device frame budget.

Report distributions and failures, not averages alone.

---

## 14. Acceptance journeys before Read can replace the current product

Record device, OS/WebView, commit, fixture, network profile, result and any trace/recording.

1. **Import and start** - long source appears promptly; original pages open early; first audio does not wait for all synthesis.
2. **Read in place** - live page with forms/images/table stays interactive and visually intact.
3. **Settings continuity** - voice and 1.5x survive paragraphs, routes, background and lock screen.
4. **Navigation race** - extraction + immediate navigation never commits obsolete source.
5. **Voice and seek race** - newest intent always wins.
6. **Progressive exhaustion** - Buffering is shown and playback resumes; prefix exhaustion is not document end.
7. **Offline cold start** - downloaded source resumes after process termination with network disabled.
8. **Eviction/low storage** - active audio survives pressure; unfinished downloads recover.
9. **System audio** - calls, audio focus, headphones, Bluetooth, lock screen and remote seek; user pause wins.
10. **Account/token changes** - expiry/logout/account switch leaks no source, stale output or entitlement.
11. **Revision/learning** - source update/OCR correction preserves mappable notes and marks stale derived artifacts.
12. **Accessible core tasks** - import, play, rate, voice, note and source return work with VoiceOver/TalkBack and large text.
13. **Upgrade/rollback** - supported schema migration preserves user data.

Device coverage must include lower-tier supported hardware, current phones and tablet/wide-window cases.

---

## 15. Delivery gates

### Gate A - Establish truth

**Status: active / substantially complete.**

- pin baseline;
- reconcile paper with repository;
- preserve source rendering invariant;
- record architecture authorities;
- maintain parity ledger and open-PR status.

Remaining:
- resolve Browser V2 vs extension-first documentation conflict;
- keep this roadmap current with exact commit/PR evidence.

### Gate B - Reliable browser reading and playback

**Status: current implementation focus.**

Required before moving on:

- fix iOS emitted visual script;
- native navigation/source generation fencing;
- first-segment startup;
- append-only queue compile + acoustic qualification;
- cache leases/byte ownership;
- rate/voice persistence across transitions;
- physical-device interruption/browser tests.

### Gate C - Complete reading product

Build:

- native Library;
- Add/Import;
- durable import jobs;
- PDF/EPUB adapters;
- source anchors;
- anchor-first resume;
- annotations/search;
- verified offline bundles;
- migrations/account isolation.

### Gate D - Coherent visual system

Build:

- one versioned token source;
- generated/adapted Swift/Kotlin tokens;
- card/row/player/sheet catalogue;
- adaptive layouts;
- complete states;
- paired device review;
- accessibility and performance evidence.

### Gate E - Learning depth

Only after reliable source anchors:

- source-linked explanation;
- editable cards;
- review scheduler;
- grounded AI;
- evaluation corpus;
- delayed-recall/task evidence.

### Gate F - Controlled launch

- entitlement reconciliation;
- privacy controls;
- diagnostics/support;
- migration/rollback;
- staged cohort;
- signed-release evidence only after explicit build authorization.

---

## 16. Concrete implementation packages

Keep each package independently reviewable. Do not mix a renderer rewrite, provider migration and visual redesign into one PR.

### Package 1 - Script and source fencing

Starting area:
- iOS `ReadBrowserModel.swift`;
- browser state/recovery;
- Android browser generation contract.

Result:
- structured script arguments;
- observed script errors;
- generation-fenced callbacks;
- stale-page fixtures.

### Package 2 - Incremental audio readiness

Starting area:
- native `ReadProgressiveAudioCoordinator`;
- iOS `ReadDocumentPlaybackLoader`;
- Android `ReadPlaybackService`.

Result:
- current segment published first;
- forward horizon begins independently;
- later prefetch failure cannot suppress ready audio;
- explicit cancellation/play intent.

### Package 3 - Stable physical queues

Starting area:
- PR #40;
- `ReadPlaybackSession.swift`;
- `ReadDocumentTimelinePlayer.kt`.

Result:
- append-only ordinary refill;
- explicit replacement for voice/seek/asset changes;
- recorded acoustic boundary evidence.

### Package 4 - Durable audio repository

Starting area:
- `ReadNativeAudioPipeline.swift/.kt`;
- new AudioAssetRepository abstraction.

Result:
- offline-first lookup;
- complete rendition identity;
- checksums;
- leases;
- byte quota;
- crash cleanup;
- pinned bundle support.

### Package 5 - Source anchors and mapping

Starting area:
- ReadingManifest contract;
- Rust core;
- native FFI/WASM;
- format source-map adapters.

Result:
- versioned anchor model;
- source revision vs rendition separation;
- Unicode fixtures;
- timing capability metadata.

### Package 6 - Resume, persistence and sync

Starting area:
- resume stores;
- document repository;
- local DB;
- SyncOutbox.

Result:
- anchor-first resume;
- account isolation;
- migrations;
- idempotent sync/conflict behavior.

### Package 7 - Feature separation

Starting area:
- native shells;
- Android large integration/service files;
- browser ViewModels/controllers.

Result:
- thin screens;
- clear owners;
- typed repository/service interfaces;
- lifecycle tests.

### Package 8 - Native product surfaces

Build:
- Library;
- Import;
- SourceReader;
- Downloads;
- Settings/account.

Result:
- end-to-end source journeys;
- empty/loading/partial/error/offline states.

### Package 9 - Design implementation

Starting area:
- current shared Swift/Kotlin design files;
- `docs/design` authority.

Result:
- one token source;
- native component catalogues;
- repaired contrast;
- adaptive/player layouts;
- paired accessibility review.

### Package 10 - Evidence-linked learning

Build only after anchors are reliable.

Result:
- editable grounded outputs;
- review events;
- source return;
- evaluation corpus.

---

## 17. Implementation ordering rule

Parallel work is allowed only when dependencies are respected.

Can progress together after ownership contracts are stable:

- Gate B playback/browser reliability;
- token-source/design-system preparation;
- web PR reconciliation that does not modify the same files.

Must wait for source anchors:

- durable annotation semantics;
- precise highlighting;
- evidence-linked learning;
- revision-safe study artifacts.

Must wait for durable audio repository:

- credible offline promise;
- user-pinned downloaded status;
- offline launch qualification.

Must wait for explicit authorization:

- TestFlight;
- EAS;
- Play/App Store;
- production release build/cutover.

---

## 18. Handoff requirements for every continuation

Every AI/human continuation must record:

- repository;
- branch;
- exact head commit;
- PR/issue;
- files/components changed;
- behavior newly supported;
- automated evidence;
- physical-device evidence, if any;
- known gaps;
- next concrete task.

Use the following status words accurately:

- **Implemented** - code exists on the named branch/commit.
- **Automatically checked** - named CI/tests passed at the exact head.
- **Device qualified** - required physical-device journeys passed.
- **Released** - production/store release is complete.

An open PR is not shipped behavior. A compile pass is not device qualification.

---

## 19. First implementation work launched from this roadmap

The first non-conflicting implementation slice should address **Package 2 / F1 first-audio readiness**, because:

- it is P0 for fast-start behavior;
- it does not require the unresolved source-anchor redesign;
- it avoids the files actively modified by PR #21 and PR #40;
- it gives direct user-visible improvement;
- it is measurable.

Initial implementation goal:

1. initial document/seek/voice window publishes the current segment as soon as it is ready;
2. playback can start from that verified first segment;
3. the existing forward-refill scheduler continues independently after publication;
4. later refill failure does not turn an already playing document into initial-load failure;
5. current session ownership and whole-document public timeline remain unchanged.

Do **not** force an immediate physical queue refresh on current main merely to begin prefetch sooner: ordinary refill still rebuilds the hidden queue until Package 3 / PR #40 is repaired and qualified. Once append-only queue growth is integrated, the forward horizon can be filled more aggressively without trading startup speed for an immediate queue reset.

After that slice is green, continue Package 1 source fencing and Package 3 queue qualification with careful PR reconciliation.
