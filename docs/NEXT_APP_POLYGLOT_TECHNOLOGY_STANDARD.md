# Floently Next-Generation Polyglot Technology Standard

**Status:** Architecture standard for the next-generation Floently apps  
**Effective:** 2026-09-30  
**Repository:** `Floently/floently-native`  
**Scope:** Future/native Floently Read, Learn, Create, and shared next-generation services.  
**Non-scope:** This document does **not** order a rewrite of the current production React Native/Expo app. The current app remains a supported legacy/release line until the next-generation products pass their release gates.

## 1. Decision

Floently will use a **controlled polyglot architecture**.

We will not force every subsystem into one language or framework. We will choose the strongest production technology for each real system boundary: native UI, media playback, document processing, shared high-performance engines, web, backend orchestration, low-latency services, AI/model work, persistence, and infrastructure.

This does **not** mean "use every language." Extra languages add FFI boundaries, build systems, debugging cost, security surface, duplicated models, and maintenance overhead. A language/framework is introduced only when it gives a material advantage that cannot be obtained cleanly inside the existing layer.

The governing rule is:

> **Best tool for the component, minimum number of boundaries.**

Platform-native responsibilities stay native. Shared high-performance logic may use a shared systems-language core. AI/model work uses the strongest ML ecosystem. Web stays web-native. Backend services may use different server languages when their workloads justify it.

## 2. Why this standard exists

The previous architecture repeatedly allowed product behavior to be shaped by framework limitations. For a world-class media/reading product, architecture must start from the user contract:

- a long book is one logical document immediately;
- playback is one durable session;
- hidden TTS/media segments must never leak into user-visible state;
- background/lock-screen/system controls are first-class;
- speed, voice, position, queue, and duration survive screen transitions;
- import, parsing, indexing, rendering, synthesis, buffering, and playback run concurrently where appropriate;
- UI geometry and interaction are governed by a design system rather than being re-invented per screen.

Speechify is useful evidence for this philosophy. Public Speechify material lists Swift, Kotlin, React, TypeScript, Node.js, Go, and Python across its stack. Apple separately documents Speechify using SwiftUI, Swift 6 structured concurrency, SwiftData, Core ML, Metal, and App Intents; its proprietary SIMBA TTS model is integrated with Apple devices through Core ML. Speechify's Android roles list Kotlin, Coroutines, Flow, Dagger 2, Room, Custom Views, Canvas & Paint, Jetpack Compose, JUnit, and experience with long-running/background audio.

The lesson is not to clone their exact source tree. The lesson is to avoid forcing unrelated technical problems through one abstraction.

## 3. Technology classification

Some names that appear in architecture discussions are not programming languages:

| Name | Category | Role |
|---|---|---|
| Swift | Programming language | Apple application and native platform code |
| Kotlin | Programming language | Android application and native platform code |
| TypeScript | Programming language | Web UI, browser extensions, Node.js services/tooling |
| Go | Programming language | Low-latency/high-concurrency backend services |
| Python | Programming language | ML/AI, model tooling, data pipelines, selected APIs |
| Rust | Programming language | Memory-safe high-performance shared engines/services |
| C++ | Programming language | Mature media/codec/render engines and cross-platform native cores |
| C | Programming language | Low-level libraries, ABI boundaries, codecs, OS/library interop |
| Objective-C / Objective-C++ | Programming languages | Apple interoperability/bridging when mature libraries require it |
| SwiftUI | UI framework | Apple declarative UI |
| UIKit | UI framework | Apple lower-level/custom UI where SwiftUI is insufficient |
| Jetpack Compose | UI framework | Android declarative UI |
| Android Views / Canvas / Paint | UI/render APIs | Precision Android controls, custom drawing, specialized surfaces |
| Kotlin Flow | Reactive stream API | Android state/event streams |
| Kotlin Coroutines | Concurrency model | Android async/concurrent work |
| Dagger 2 / Hilt | Dependency injection | Android dependency graph |
| Room | Persistence library | Android SQLite abstraction |
| AVFoundation | Apple media framework | Audio/video sessions, playback, processing |
| Media3 / ExoPlayer | Android media framework | Playback, buffering, media sessions, playlists |
| Metal | Apple GPU API / shading platform | GPU rendering/compute and advanced media work |
| Core ML | Apple ML framework | On-device model inference |
| SIMBA | Proprietary Speechify TTS model | Speech generation; not a language or framework we can adopt directly |
| Django | Python web framework | Admin-heavy/content-heavy backend services when its integrated model fits |
| FastAPI | Python web framework | Async Python APIs, especially model-serving/orchestration |
| React | Web UI library | Browser/web product surfaces |
| Node.js | JavaScript/TypeScript runtime | API/BFF/product orchestration and tooling |

## 4. Default next-generation stack by component

### 4.1 Apple app shell and UI

**Primary:** Swift 6+, SwiftUI  
**Use UIKit where it is demonstrably stronger:** advanced text layout, custom gestures, precise scrolling, complex collection/layout behavior, specialized media surfaces, or system APIs not exposed adequately through SwiftUI.

Use:
- Swift structured concurrency (`async/await`, actors, task groups);
- SwiftData where it fits simple native persistence;
- SQLite/Core Data or a dedicated persistence layer when scale/query/control exceeds SwiftData's fit;
- Keychain for secrets/session material;
- FileManager/native file coordination for local document/audio caches;
- App Intents for system actions when useful;
- Dynamic Type, VoiceOver, Reduce Motion, content size/accessibility APIs as release requirements.

**Rule:** Apple media/system behavior is not delegated to a JavaScript bridge unless there is a proven reason.

### 4.2 iOS/iPadOS playback and media session

**Primary:** Swift + AVFoundation + MediaPlayer/Now Playing APIs.

Candidate engine path:
1. Start with an AVFoundation queue-based implementation when it meets gap/seek/rate requirements.
2. Escalate to AVAudioEngine/AVAudioPlayerNode when sample-accurate scheduling, processing, or transition requirements cannot be met by queue items.
3. Publish one **document-wide** logical duration/elapsed cursor to system media controls.
4. Route remote play/pause/seek/skip commands into the logical document timeline, not the physical segment timeline.

Use:
- AVAudioSession with spoken/media-appropriate category/mode;
- AVQueuePlayer or AVAudioEngine depending measured requirements;
- MPNowPlayingInfoCenter / current Apple Now Playing APIs;
- MPRemoteCommandCenter;
- background audio capability;
- interruption and route-change handling.

### 4.3 Android app shell and UI

**Primary:** Kotlin + Jetpack Compose.

Use:
- Kotlin Coroutines for structured async work;
- Kotlin Flow/StateFlow for observable application/media state;
- Hilt as the default Dagger-family integration unless direct Dagger 2 is justified;
- Room for structured local data;
- DataStore for preferences;
- Android Keystore for secrets;
- WorkManager for durable deferred/background work.

Use Android Views, Canvas, Paint, or custom drawing when Compose cannot provide the required precision/performance for a specific surface.

### 4.4 Android playback and media session

**Primary:** Kotlin + Android Media3.

Use:
- ExoPlayer;
- MediaSession;
- MediaSessionService for background playback;
- MediaLibraryService if the system/library integration benefits from it;
- Audio focus/interruption APIs;
- system notification/headset/Bluetooth command integration.

The Player and MediaSession must be owned outside individual activities/screens. The document is the logical media item; hidden synthesis segments are queue implementation details.

### 4.5 Shared high-performance engine

A shared engine is permitted only where duplicated Swift/Kotlin implementations would create correctness drift or where measured CPU/memory/latency requirements justify it.

**Default preference for new shared engine code: Rust.**
Reasons:
- memory safety;
- good concurrency/tooling;
- strong FFI story through a C-compatible boundary;
- suitable for parsers, indexing, timing maps, document graphs, sync algorithms, caching primitives, and selected media/data pipelines.

**C++ is preferred instead when:**
- integrating mature C/C++ media/codec libraries;
- using ecosystems where C++ is the established implementation language;
- GPU/render/media engine dependencies already require C++;
- the engineering benefit of the mature library is greater than the memory-safety advantage of a new Rust implementation.

**C is used for:**
- stable ABI boundaries;
- small low-level interop layers;
- mature C libraries/codecs;
- not for broad application business logic.

For an iOS C++ core, Objective-C++ may be used as a narrow bridge. Android connects native engines through the NDK/JNI. The bridge boundary must remain small and versioned.

### 4.6 Document ingestion and canonical document engine

Target output is a language-neutral/versioned `ReadingManifest` / canonical document graph.

Responsibilities:
- PDF text extraction and page coordinate mapping;
- EPUB spine/XHTML parsing;
- DOCX/TXT parsing;
- HTML/readability extraction;
- OCR for scans;
- chapter/paragraph/sentence/token indexing;
- stable source offsets;
- whole-document word/token counts;
- estimated duration;
- mapping between document cursor and source coordinates.

Implementation choices:
- Swift/Kotlin platform parsers when platform APIs are best for the format;
- Rust for a shared canonical parser/index engine where consistent behavior across iOS/Android is valuable;
- C/C++ libraries only when their mature parser/rendering ecosystem gives a clear advantage;
- Python server-side processing only for workloads that are inappropriate or too expensive on-device.

The UI must never be the canonical parser.

### 4.7 TTS and speech intelligence

**Server/model ecosystem:** Python first for model training, evaluation, experimentation, alignment, and speech research.

Use:
- PyTorch or the strongest supported model framework for training/research;
- ONNX/Core ML or other target-specific formats for deployment when appropriate;
- Core ML on Apple for on-device inference;
- Android-compatible on-device runtimes when model size/latency/battery justify local inference;
- GPU acceleration only where measured benefit warrants it.

**TTS Orchestrator contract** must be provider-independent:
- document revision + segment id;
- text hash;
- voice/language/style;
- synthesis parameters;
- audio URI/local cache key;
- actual duration;
- sentence/word timing marks;
- generation status and retry class.

Speechify's SIMBA is proprietary. Floently should not pretend it can "use SIMBA." We can adopt the architecture pattern: isolate the model behind a stable speech-generation contract so providers/models can change without rewriting the player.

### 4.8 Backend/API services

No single server language is mandatory.

**Go** — default for:
- high-concurrency low-latency services;
- streaming/gateway services;
- queue workers with large concurrent workloads;
- services where predictable resource use matters.

**TypeScript/Node.js** — default for:
- product/BFF APIs;
- web-facing orchestration;
- rapid iteration on user/business workflows;
- shared TypeScript schemas/tooling with web clients where useful.

**Python/FastAPI** — default for:
- model-serving/orchestration;
- ML-adjacent APIs;
- data/analysis pipelines;
- services whose dependencies live primarily in Python.

**Python/Django** — use when:
- a strong admin/content-management workflow is central;
- relational CRUD, permissions, admin UI, migrations, and integrated server features are more valuable than ultra-low overhead.

**Rust backend services** — use only for measured hot paths/security-critical/high-throughput services where Go/Node/Python are demonstrably insufficient or a Rust library is strategically important.

Do not split a service into a new language without an ADR explaining the operational benefit.

### 4.9 Web app and browser extension

**Primary:** TypeScript + React.

Use:
- a modern React application framework/build system based on product needs;
- Web Workers for CPU-bound browser tasks;
- Service Workers for offline/cache where appropriate;
- browser-native media APIs only for browser surfaces;
- Playwright for end-to-end browser tests.

The web reader is a separate presentation/runtime. It must consume the same logical document/playback contracts where possible but must not constrain native iOS/Android architecture.

### 4.10 Database, cache, and storage

Server defaults:
- PostgreSQL for canonical relational product data;
- Redis or equivalent for ephemeral cache/coordination/rate-limiting where justified;
- object storage for documents/audio/render assets;
- queue/event infrastructure for background synthesis/import pipelines.

Device defaults:
- Apple native database/SQLite layer;
- Room/SQLite on Android;
- OS secure storage for credentials;
- content-addressed file cache for synthesized audio and document artifacts.

### 4.11 Create/media/render engine

For next-generation Create:
- iOS UI: SwiftUI/UIKit;
- iOS media: AVFoundation;
- iOS GPU: Metal;
- Android UI: Compose/Views;
- Android media: Media3/Transformer;
- Android GPU: platform-native GPU/render APIs as requirements dictate;
- shared timeline/render/effect graph: Rust or C++ when duplication becomes harmful.

Do not introduce a shared native render engine before the product has a real timeline/effect workload requiring it.

### 4.12 Observability and quality

Use platform-native crash/performance telemetry plus OpenTelemetry-compatible server telemetry where practical.

Release quality requires:
- structured logs with correlation ids;
- synthesis/import/playback latency metrics;
- cache hit rate;
- queue underrun/gap metrics;
- playback interruption/route-change metrics;
- real-device long-session tests;
- battery/memory/network tests;
- crash-free session targets;
- accessibility checks.

Testing defaults:
- iOS: Swift Testing/XCTest/XCUITest as appropriate;
- Android: JUnit + Compose UI/Espresso/instrumentation;
- web: Playwright;
- backend: language-native unit/integration tests;
- cross-product contract tests for the versioned manifest/media-session APIs.

## 5. Floently Read target architecture

The next-generation Read stack is:

### iOS
Swift + SwiftUI  
UIKit where needed  
AVFoundation  
Now Playing/MediaPlayer system integration  
Swift structured concurrency  
native persistence/cache  
Core ML/Metal only for workloads that actually benefit from them  
optional Rust/C++ shared document/media engine behind a narrow FFI boundary

### Android
Kotlin + Jetpack Compose  
Views/Canvas where needed  
Coroutines + Flow  
Hilt/Dagger family DI  
Room/DataStore  
Media3/ExoPlayer + MediaSessionService  
WorkManager  
optional Rust/C++ shared engine behind JNI/NDK

### Shared/backend
Versioned language-neutral ReadingManifest contract  
Python AI/TTS/model layer  
Go for concurrency-heavy services  
TypeScript/Node for product/web orchestration  
PostgreSQL + object storage + queue/cache infrastructure  
Rust/C++ only for measured/shared systems-engine problems

## 6. State-ownership rule

Architecture is invalid if UI components own durable media state.

`PlaybackSession` owns:
- document id/revision;
- logical elapsed time and duration;
- playback state;
- speed;
- voice;
- queue/buffer state;
- current logical cursor;
- interruption state;
- media-route state;
- resume point;
- lock-screen publication.

Reader UI owns:
- presentation theme;
- visible page/flow;
- selection/annotation interaction;
- mapping logical cursor to highlight coordinates.

Library UI owns:
- browsing/filtering/sorting/presentation.

Import UI owns:
- user input and progress presentation.

None of these screens may reset PlaybackSession simply because they mount/unmount.

## 7. Technology-adoption gate

A new language/framework/library requires an Architecture Decision Record if it:
- creates a new runtime;
- creates an FFI boundary;
- adds a new package manager/build system;
- becomes a production service language;
- owns persisted data;
- owns a release-critical media path;
- adds a major cloud dependency.

The ADR must answer:
1. What exact limitation are we solving?
2. Why can the current layer not solve it cleanly?
3. What measurable product benefit results?
4. What are the build/test/debug/deployment costs?
5. What is the rollback/removal path?
6. Who owns the boundary and contract?
7. How will it be tested on real devices or production-like workloads?

## 8. What we explicitly reject

- "One language everywhere" as an architectural goal.
- "Use every possible language" as an architectural goal.
- JavaScript/React Native owning core iOS/Android media-session behavior in the next-generation app.
- One physical TTS segment being exposed as the user's book/document.
- Screen/paragraph lifecycle owning speed, voice, duration, or resume state.
- Introducing C/C++/Rust merely for prestige without measured need.
- Writing ML/model pipelines in mobile UI code.
- Treating a proprietary model name (for example SIMBA) as a language or transferable library.
- Choosing a framework only because an AI agent already knows it.
- Declaring media quality complete from CI without physical-device qualification.

## 9. Relationship to the current app

This standard governs **the next-generation native app line** in `Floently/floently-native`.

The current React Native/Expo application in `Floently/floently-finnish` is not automatically rewritten by this decision. It may continue receiving stabilization fixes and shipping releases while the native line is built and qualified.

Migration happens feature-by-feature only after the next-generation implementation is demonstrably better and passes:
- functional parity;
- visual/interaction quality;
- accessibility;
- long-form media/background tests;
- performance/memory/battery tests;
- release and rollback readiness.

## 10. Source evidence

- Apple Developer, "How Speechify is evolving into a hands-free AI assistant" (2026): SwiftUI; 1,000+ voices/60 languages; proprietary SIMBA model; Core ML; Metal; SwiftData; Swift 6 structured concurrency; App Intents.
- Apple Design Awards 2025: Speechify won Inclusivity; Apple highlighted approachable UI, VoiceOver/Dynamic Type, and reduction of cognitive load.
- Speechify careers: public stack lists Swift, Kotlin, React, TypeScript, Node.js, Go, and Python.
- Speechify Android roles: Kotlin, Coroutines, Flow, Dagger 2, Room, Custom Views/Canvas/Paint, Jetpack Compose, JUnit, and background-audio experience.
- Android Developers: MediaSessionService is the platform model for maintaining Player/MediaSession for background playback.

## 11. Final engineering principle

> Floently should be polyglot **by architecture boundary**, not polyglot **by enthusiasm**.

The objective is not the largest technology list. The objective is a product in which each critical subsystem is implemented using the technology that provides the best combination of platform integration, performance, correctness, maintainability, accessibility, testability, and long-term control.
