# Speechify Architecture Study for Floently Read

**GitHub architecture edition**  
**Date:** 2026-09-30  
**Applies to:** the next-generation Floently native product line in `Floently/floently-native`  
**Does not mandate a rewrite of:** the current React Native/Expo production app.

## Executive thesis

Speechify should not be treated as "a prettier reader." The product quality visible in the supplied recordings is the result of multiple specialized systems behaving as one application: native UI, long-form document ingestion, a durable playback session, hidden synthesis/media queues, persistent state, background/lock-screen media integration, accessibility, cache/persistence, AI/TTS, and backend/platform services.

The individual techniques are established software-engineering tools. The integration is specialized. Floently should reproduce the architecture pattern rather than merely copy surface styling.

Public material confirms that Speechify itself is polyglot. Speechify publicly lists Swift, Kotlin, React, TypeScript, Node.js, Go, and Python. Apple documents the Apple app using SwiftUI, Swift 6 structured concurrency, SwiftData, Core ML, Metal, and App Intents, with Speechify's proprietary SIMBA TTS model integrated through Core ML. Speechify Android roles list Kotlin Coroutines, Flow, Dagger 2, Room, Custom Views, Canvas & Paint, Jetpack Compose, JUnit, and experience with long-running/background audio.

The engineering lesson is therefore:

> Use the best platform/runtime for the real subsystem, and connect those subsystems through stable contracts.

## 1. Evidence model

This study distinguishes four classes of statements.

### Confirmed
Explicitly documented by Speechify, Apple, Android, or other platform sources.

Examples:
- Swift/SwiftUI on Apple.
- Swift 6 structured concurrency.
- SwiftData.
- Core ML.
- Metal.
- App Intents.
- Kotlin/Compose/Room/Coroutines/Flow/Dagger on Android.
- React/TypeScript on web/frontend.
- Node.js/Go on backend.
- Python for AI.
- Speechify SIMBA as a proprietary TTS model.

### Observed
Visible in the supplied Speechify recordings.

Examples:
- persistent mini-player;
- stable bottom-player geometry;
- dedicated library, reader, voice, and import surfaces;
- state survives navigation;
- player feels like part of the app shell;
- one long document is presented as one media item rather than exposed as small synthesis clips;
- controlled information density and strong alignment.

### Inferred
A likely internal mechanism needed to explain the observed behavior but not publicly confirmed as Speechify proprietary implementation.

Examples:
- canonical document manifest;
- logical document clock;
- hidden segment queue;
- prefetch/cache policy;
- timing index;
- cross-screen playback state owner.

### Recommended for Floently
The concrete implementation proposed for Floently, even where Speechify's exact private implementation is unknown.

Examples:
- AVFoundation/Now Playing on Apple;
- Media3/MediaSessionService on Android;
- versioned ReadingManifest;
- Rust/C++ only for shared performance-critical engines when justified.

## 2. What the recordings prove about product architecture

### 2.1 The player is above the reader screen

The mini-player remains visually and behaviorally stable across different app surfaces. That means the correct product model is:

`ApplicationShell -> PlaybackSession -> individual screens`

not:

`ReaderScreen -> Paragraph -> AudioClip`.

A screen may disappear. The playback session must not.

### 2.2 Stable geometry is an architecture property

The consistent dimensions, spacing, margins, player location, transport layout, bottom navigation, and sheet behavior are not accidental styling. They imply shared components, design tokens, and screen contracts.

Floently must stop allowing each feature implementation to invent:
- player size;
- spacing;
- icon scale;
- control placement;
- sheet behavior;
- loading state;
- error state;
- navigation pattern.

### 2.3 Secondary complexity is hidden

Voice browsing, library management, import, settings, reader content, and playback are separated into coherent surfaces. Speechify does not expose every capability at once.

This reduces cognitive load and matches Apple's 2025 Design Award assessment that Speechify's design reduces cognitive load throughout the app.

## 3. Why long books can feel "loaded" in seconds

A seven-hour or twenty-hour book does not need seven or twenty hours of synthesized waveform before the app understands it.

The immediate work is:

1. acquire the file/content;
2. identify document type;
3. extract structure/text;
4. build a canonical document representation;
5. count words/tokens and determine chapter/section boundaries;
6. compute an estimated document duration;
7. render the first visible content;
8. synthesize the first playable region;
9. continue synthesis/indexing/cache work concurrently.

A long text document can therefore have a whole-document duration almost immediately while audio is generated progressively.

This is the critical distinction:

> **Document understanding and document playback are separate pipelines.**

## 4. Canonical document architecture

All supported input types should converge on one versioned model.

### Inputs

- PDF with embedded text
- scanned PDF/images
- EPUB
- DOCX
- TXT/Markdown
- pasted text
- web article/page
- later: email, cloud document, camera scan

### Canonical graph

```
Document
  -> Revision
  -> Chapter / Section
  -> Paragraph
  -> Sentence
  -> Token / Word
  -> Source location
  -> Timing / playback mapping
```

The graph must preserve:
- source offsets;
- page/DOM/XHTML coordinates where useful;
- chapter order;
- semantic boundaries;
- word/token count;
- language;
- revision identity;
- hash/cache identity.

The TTS layer should consume canonical text units. It should not consume whatever paragraph happens to be mounted in the UI.

## 5. Whole-document duration

Initial duration is an estimate:

`estimated seconds = calibrated linguistic workload / voice baseline / playback rate`

A simple first estimate can use words per minute, but production quality should refine by:
- language;
- punctuation density;
- sentence length;
- selected voice;
- synthesis provider/model;
- known actual durations from completed segments;
- playback speed.

Estimated and actual duration must be different fields.

When real segment durations become available, the manifest can refine the model without changing the user's logical document identity.

## 6. TTS architecture

Speechify has a proprietary speech advantage in SIMBA. Floently cannot simply "use SIMBA" because it is not a public language/library; it is Speechify's proprietary TTS model.

Floently can reproduce the system boundary.

### TTS Orchestrator input

- document revision;
- segment id;
- content hash;
- language;
- voice id;
- style;
- synthesis parameters.

### TTS Orchestrator output

- audio asset URI/local cache key;
- actual duration;
- word/sentence timing marks;
- generation status;
- retry/failure classification;
- provider/model metadata.

The player must not care whether the audio came from:
- a cloud vendor;
- Floently's own Python model service;
- a local Core ML model;
- an Android on-device model runtime;
- a future proprietary model.

That is how Read can improve models later without rewriting the player.

## 7. The player is the central system

The user's book is one logical media item.

Internally, speech may be divided into hundreds or thousands of hidden segments. Those segments are transport units only.

### PlaybackSession owns

- document id/revision;
- voice;
- speed;
- logical duration;
- logical elapsed time;
- play/pause/buffering state;
- current document cursor;
- segment queue;
- cache/prefetch state;
- interruption/audio-route state;
- resume point;
- lock-screen/system media metadata.

### Reader UI owns

- visible document layout;
- theme;
- highlight drawing;
- selection/annotation interaction;
- mapping logical document cursor to on-screen coordinates.

### Screens do not own

- speed;
- voice;
- total duration;
- queue;
- resume;
- background playback;
- system media state.

This directly addresses the previous Floently problem where a speed could behave as though it belonged to the current paragraph.

## 8. Logical document clock

Suppose hidden segments have actual media lengths:

```
segment 0: 18.4s
segment 1: 22.7s
segment 2: 17.1s
...
```

The manifest stores logical ranges:

```
segment 0 -> document 00:00.000 - 00:18.400
segment 1 -> document 00:18.400 - 00:41.100
segment 2 -> document 00:41.100 - 00:58.200
...
```

The UI/system sees only:

```
Document elapsed: 02:47:18
Document duration: 07:14:32
```

A seek to 05:13:10 performs an inverse lookup:
1. identify the hidden segment containing the logical target;
2. calculate local segment offset;
3. prepare/seek that media source;
4. keep the public state on the document clock.

This is the foundation for:
- exact resume;
- long-duration scrubber;
- lock-screen timeline;
- headset/system seeking;
- cross-device progress;
- synchronized highlighting.

## 9. Seamless playback and buffering

Preloading the next audio URL is not enough.

Gapless perceived playback depends on:
- next bytes being available;
- decoding being ready;
- transition scheduling;
- consistent sample/audio format;
- queue handoff;
- logical clock continuity;
- rate continuity;
- cache behavior.

The correct prefetch policy is time-based, not only "next N chunks."

For example:
- keep 90-180 seconds of prepared audio ahead under normal conditions;
- increase when synthesis/network latency rises;
- reduce under memory/battery pressure;
- account for playback speed;
- prewarm decoder/queue state where platform APIs allow.

## 10. iOS architecture for Read

Recommended next-generation stack:

- Swift 6+
- SwiftUI for primary application UI
- UIKit for specialized/precision UI where necessary
- AVFoundation
- Swift structured concurrency
- native file/cache layer
- SwiftData/SQLite/Core Data according to persistence requirements
- Keychain
- App Intents where useful
- Core ML/Metal only for workloads where they provide measurable value

### Media layer

Start with a native AVFoundation queue implementation if it can meet transition, rate, seek, and interruption requirements.

Escalate to AVAudioEngine/AVAudioPlayerNode if precise scheduling or processing cannot be achieved adequately with queue items.

Use system Now Playing/MediaPlayer integration so lock-screen and Control Center show:
- document title;
- artwork;
- one logical elapsed time;
- one logical duration;
- play/pause;
- seek/skip behavior mapped back to the ReadingManifest.

The physical clip duration must never become the public document duration.

## 11. Android architecture for Read

Recommended next-generation stack:

- Kotlin
- Jetpack Compose
- Android Views/Canvas/Paint for specialized drawing/controls
- Coroutines
- Flow/StateFlow
- Hilt/Dagger family DI
- Room
- DataStore
- WorkManager
- Media3/ExoPlayer
- MediaSession
- MediaSessionService

Android's media service should own the player independently of activities/screens so playback continues when the UI is backgrounded or destroyed.

Again, the document is the logical media item; physical synthesis segments are queue entries.

## 12. Web and browser

Speechify also operates web/browser surfaces. Public Speechify material lists React and TypeScript among frontend technologies.

Floently web surfaces should use web-native technology:
- TypeScript;
- React;
- browser APIs;
- Web Workers for CPU-bound tasks;
- Service Workers/offline cache when useful;
- Playwright for end-to-end testing.

The native app should not be constrained by browser limitations merely to share code.

Browser Reader is a legitimate WebView/browser feature because rendering a live website is itself the product feature. That does not mean the main Reader/player should be a WebView shell.

## 13. Backend architecture

Speechify publicly lists Node.js and Go on backend and Python for AI. This is a useful pattern.

For Floently:

### Go
Use for concurrency-heavy, low-latency services:
- streaming/gateway;
- queue workers;
- high-volume orchestration;
- services where resource predictability matters.

### TypeScript/Node.js
Use for:
- product APIs/BFF;
- account/subscription orchestration;
- web-facing services;
- rapid business/product iteration.

### Python
Use for:
- TTS/model training and evaluation;
- ML inference orchestration;
- data processing;
- OCR/AI pipelines where Python libraries dominate.

### Django
Use only where Django's integrated admin, relational models, permissions, migrations, and content-management strengths are a good match. Django is a Python framework, not a separate language.

### Rust
Use for:
- performance/security-sensitive services;
- shared engines;
- parsers/indexers;
- measured hot paths.

Do not split a backend into extra runtimes without a real operational/product benefit.

## 14. C, C++, and Rust

A multi-language system becomes worse, not better, if every library is written in a different language without strong boundaries.

### Rust: preferred for new shared systems code
Good candidates:
- canonical document engine;
- token/index mapping;
- shared timing/seek algorithms;
- content hashing/cache primitives;
- selected parsing;
- synchronization;
- other CPU/memory-sensitive cross-platform logic.

### C++
Use when:
- mature media/codec/render libraries already exist in C++;
- a rendering/media ecosystem strongly favors C++;
- performance-critical integration makes it the practical choice.

### C
Use mainly for:
- stable ABI boundaries;
- mature C libraries;
- codecs;
- system/library interop;
- very small low-level modules.

Avoid putting broad business logic in C.

## 15. UI/UX architecture

World-class UI does not come from a "UI language" alone.

Required layers:

### Design source of truth
Figma or equivalent design tooling for:
- layouts;
- component states;
- measurements;
- typography;
- spacing;
- motion;
- dark/light variants;
- accessibility states.

### Code design system
Native shared tokens/components:
- spacing;
- type scale;
- corner radii;
- player heights;
- navigation dimensions;
- safe-area rules;
- icon sizes;
- motion curves;
- haptic semantics;
- loading/empty/error patterns.

### Screen freeze gate
A material screen is not coded until its design contract specifies:
- exact structure;
- dimensions;
- interactions;
- loading/empty/error/disabled/offline states;
- keyboard behavior;
- safe area;
- accessibility;
- motion;
- system integrations.

### Persistent media shell
Mini-player geometry is owned centrally and reused everywhere.

No screen may independently reproduce the player.

## 16. Accessibility

Speechify's 2025 Apple Design Award specifically highlighted approachable UI, VoiceOver, Dynamic Type, and reduced cognitive load.

Floently therefore treats accessibility as architecture:
- Dynamic Type/content scaling;
- VoiceOver/TalkBack;
- sufficient hit targets;
- high contrast;
- Reduce Motion;
- accessible media controls;
- semantic reading order;
- keyboard/switch access where applicable;
- no essential information conveyed by color alone.

## 17. Current Floently Read gap

The current production/release work in `Floently/floently-finnish` has improved substantially but still illustrates the architectural transition.

The current Read work has:
- React Native/Expo;
- Expo Audio;
- a logical `ReadingPlaybackManifest`;
- improved whole-document estimated duration;
- document-wide logical progress;
- prefetch lookahead.

However, its physical player still replaces bounded audio sources. Recent device evidence showed iOS Now Playing exposing a short current-clip timeline instead of the entire reading.

The manifest work is necessary but not sufficient.

The remaining architectural step is a native media-session/queue engine that publishes the same document-wide logical timeline to:
- UI;
- background playback;
- lock screen;
- Control Center;
- headset/Bluetooth commands;
- seek/resume.

The next-generation native repo should solve this at the platform media layer rather than continuing to patch presentation around physical chunks.

## 18. Next-generation Read implementation sequence

### Phase 0 — freeze contracts
- versioned ReadingManifest;
- PlaybackSession state machine;
- document clock;
- segment/timing contract;
- design tokens;
- player geometry;
- state ownership.

### Phase 1 — ingestion/index
- PDF/EPUB/DOCX/TXT/web;
- source offsets;
- language detection;
- whole-document duration estimate;
- durable library object immediately.

### Phase 2 — native media core
- Swift AVFoundation media session;
- Kotlin Media3 service;
- system controls;
- document-wide time;
- exact seek/resume;
- interruptions/audio routes.

### Phase 3 — synthesis queue/cache
- provider-independent TTS orchestrator;
- timing marks;
- content-addressed cache;
- adaptive time-horizon prefetch;
- retry/backpressure.

### Phase 4 — reader synchronization
- sentence/word highlight;
- visual cursor mapping;
- scalable rendering for huge documents;
- no paragraph-level playback ownership.

### Phase 5 — world-class shell/UI
- persistent mini-player;
- expanded player;
- library;
- import;
- voice browser;
- settings;
- stable native navigation;
- exact design-system measurements.

### Phase 6 — offline/background
- local document/audio cache;
- robust restore;
- lock-screen/headset/Bluetooth;
- interruption tests;
- process/background survival.

### Phase 7 — AI/on-device optimization
- Core ML/Android on-device inference where useful;
- proprietary or self-hosted model experimentation;
- intelligent document cleanup;
- optional AI assistant features.

## 19. Performance requirements

"Speechify-like" must be measurable.

Initial targets for engineering qualification:

- import acknowledgement: effectively immediate after file selection;
- library object creation: < 500 ms after basic validation for ordinary local files;
- first visible text: target < 1-2 s for ordinary text-based files;
- whole-document duration estimate: target < 2 s after canonical text is available;
- first audio: target < 2-4 s on typical network/cache conditions;
- transport command feedback: < 100 ms UI response;
- physical audio start after ready state: minimized and measured;
- hidden segment handoff: no perceptible repeated/skipped word and no obvious silence gap;
- seek: visible feedback immediately, playback resume target within a few seconds depending cache/network;
- background playback: long-session survival on real devices;
- speed: persists across segments/screens/background;
- resume: exact logical document position, not only segment boundary;
- memory: bounded for multi-hour books;
- no UI requirement to render all content at once just to know the whole document.

Targets must be refined from device telemetry, but the product cannot be accepted using only subjective "seems smooth" language.

## 20. Definition of done

Read is not complete because:
- CI passes;
- the next clip is preloaded;
- the UI shows a multi-hour estimate;
- a player looks polished in one screenshot.

A Speechify-class architecture is complete only when a long real document can:

1. import quickly;
2. become one durable library object;
3. expose whole-document duration;
4. begin reading quickly;
5. continue through many hidden segments without user-visible chunk semantics;
6. retain voice/speed;
7. seek across hours;
8. resume exactly;
9. survive background/lock screen;
10. respond correctly to system/headset controls;
11. keep document-wide Now Playing time;
12. synchronize reader highlight;
13. handle interruptions;
14. work under changing network/cache conditions;
15. remain visually stable across navigation;
16. pass accessibility requirements;
17. pass real-device long-session tests.

## 21. Governing technology rule

The next-generation app is intentionally polyglot, but disciplined.

Use:
- Swift where Apple-native behavior is strongest;
- Kotlin where Android-native behavior is strongest;
- Rust/C++ where a shared performance-critical engine is justified;
- C for narrow low-level/ABI integration;
- TypeScript/React for web;
- Node.js/TypeScript and Go for server responsibilities that suit them;
- Python for AI/model/data workloads;
- Django/FastAPI only where their Python service model is a good fit;
- native GPU/ML/media frameworks where their platform advantage is real.

Do **not** use a technology just because it appears in Speechify's hiring list.

The standard is:

> **Best tool for each component; minimum number of boundaries; one coherent product contract.**

## 22. References

- Apple Developer (2026), *How Speechify is evolving into a hands-free AI assistant*.
- Apple Design Awards (2025), Speechify — Inclusivity winner.
- Speechify public careers listings describing Swift, Kotlin, React, TypeScript, Node.js, Go, Python.
- Speechify Android engineering listings describing Kotlin Coroutines, Flow, Dagger 2, Room, Custom Views/Canvas/Paint, Jetpack Compose, JUnit, and long-running/background audio.
- Android Developers, Media3 background playback / MediaSessionService guidance.
- Current Floently Read Build 48 handoff/defect ledger in `Floently/floently-finnish`.
- Companion governing standard: `docs/NEXT_APP_POLYGLOT_TECHNOLOGY_STANDARD.md`.

## Final conclusion

The main lesson from Speechify is not a single secret framework.

Its advantage comes from:
- platform-native ownership where operating-system integration matters;
- specialized AI/model tooling;
- a persistent media architecture;
- fast canonical document understanding;
- hidden internal segmentation;
- disciplined cache/concurrency;
- a coherent design system;
- extensive platform-specific engineering;
- multiple languages used at the boundaries where they are genuinely better.

Floently's next-generation app should follow the same engineering philosophy without becoming technology-chaotic.
