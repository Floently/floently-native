# Floently Create Absolute-Best Native Stack

## Final decision

Floently Create must be built with the absolute best long-term native architecture, not the easiest architecture.

The goal is a product that can eventually compete with or exceed CapCut-level quality in user experience, timeline precision, media handling, rendering, export reliability, and platform polish.

## iOS stack

Use:

- Swift
- SwiftUI for primary UI
- UIKit where needed for advanced gestures, timeline precision, custom controls, scroll/drag interactions, and lower-level interface control
- AVFoundation for audio, video, media composition, preview, trimming, synchronization, and export foundations
- Metal for high-performance rendering, effects, GPU processing, transitions, and future export pipeline acceleration
- StoreKit 2 for subscriptions and in-app purchases
- BackgroundTasks where needed for long-running export or preparation jobs
- FileManager and native caching for local project/media storage

## Android stack

Use:

- Kotlin
- Jetpack Compose for primary UI
- Android Views where needed for advanced timeline/gesture precision or specialized media surfaces
- Media3 / ExoPlayer / Transformer for playback, editing, transformation, and export foundations
- CameraX if capture/recording is needed
- WorkManager for long-running background jobs such as export, media preparation, upload, or sync
- Google Play Billing for subscriptions and in-app purchases
- Room/DataStore and native file cache for local project/media storage

## Shared engine later

If Create grows into a full timeline editor with complex rendering/export needs, add a shared engine later:

- C++ or Rust for cross-platform timeline, render, export, effect graph, and media-processing logic
- Native Swift wrapper for iOS
- Native Kotlin/JNI wrapper for Android

The shared engine should only be added when the Create product reaches the point where duplicated Swift/Kotlin media logic would slow progress or reduce consistency.

## Why this stack

Create is not a simple form app.

Create needs:

- Timeline editing
- Drag/drop precision
- Low-latency preview
- Audio/video sync
- Template rendering
- Background export
- Local media cache
- Camera/media capture later
- Smooth gestures
- Advanced text/audio/video composition
- Future GPU effects
- Reliable subscription/payment handling

For this product goal, React Native/Expo is not the best final foundation.

The final Create app should be native from the beginning.

## Build order

Do not build full Create before Read and Learn are stable.

Correct order:

1. Finish native Read foundation.
2. Stabilize shared native design system.
3. Stabilize shared native auth/account/subscription shell.
4. Release or stabilize Read.
5. Keep Learn stable and decide whether to rebuild Learn natively after Read.
6. Start Create native shell.
7. Build Create project dashboard.
8. Build Create timeline foundation.
9. Build media import/preview.
10. Build export jobs.
11. Add advanced editing/rendering.
12. Add shared C++/Rust engine only when needed.

## Hosting

No hosting change at the start.

Create should use the existing Floently server and add clean API contracts later under:

- /api/v1/create/projects
- /api/v1/create/assets
- /api/v1/create/timeline
- /api/v1/create/export-jobs
- /api/v1/create/templates
- /api/v1/create/billing

Create must remain separate from Read and Learn access unless a deliberate bundle is created.

## Safety rule

Do not mix Create native code into the old React Native/Expo mobile app.

Native Create belongs under:

- /root/floently-native/apps/ios/FloentlyCreate
- /root/floently-native/apps/android/FloentlyCreate

The old app remains legacy/fallback until native products replace it safely.
