# Floently Native Mobile Rebuild Decision

Decision:
- Floently Read mobile will be rebuilt natively.
- iOS: Swift + SwiftUI.
- Android: Kotlin + Jetpack Compose.
- Floently Create mobile will also be native for long-term CapCut-level quality.
- Heavy Create media/timeline/rendering logic may later use a shared C++ or Rust core with native wrappers.

Hosting:
- No new hosting provider at the start.
- Existing Floently backend remains on the current server.
- Native apps will use clean API contracts under /api/v1/read and future /api/v1/create.
- Old React Native/Expo app remains legacy and must not be deleted yet.

Goal:
- Best-looking app.
- Fastest working import-to-play experience.
- Native file handling.
- Native audio/player behavior.
- No forever spinner.
- Clear upload/extraction/audio progress states.

---

## Added decision: Learn and Create native strategy

Floently Learn was originally built with React Native/Expo. That was practical and fast, but it is not the absolute best possible mobile stack if difficulty is ignored.

Final native direction:

- Learn mobile should eventually be native too.
- Read mobile is the first native rebuild priority.
- Create mobile will also be native after Learn/Read are stable.
- Create should target CapCut-level quality long-term, using Swift/Kotlin native UI and a serious native/media engine.

Safety rule:

- Do not delete or damage the old React Native/Expo Learn app.
- Keep old Learn as fallback until native Learn is better.
- Native Learn/Read/Create must live in /root/floently-native.
- Existing backend and YKI/card bank remain source of truth.
- Native apps consume YKI/card content through API contracts.

---

## Final Create architecture decision

Floently Create will use the absolute-best native architecture:

- iOS: Swift + SwiftUI, UIKit where needed, AVFoundation, Metal, StoreKit 2
- Android: Kotlin + Jetpack Compose, Android Views where needed, Media3/ExoPlayer/Transformer, CameraX if needed, WorkManager, Google Play Billing
- Shared engine later: C++ or Rust for cross-platform timeline/render/export logic

The full decision is recorded in:

- docs/CREATE_ABSOLUTE_BEST_STACK.md

Create must not be built as another React Native/Expo patch if the target is CapCut-level or better quality.
