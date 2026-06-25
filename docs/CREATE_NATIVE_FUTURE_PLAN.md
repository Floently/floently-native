# Floently Create Native Future Plan

The final Create architecture decision is recorded in:

- docs/CREATE_ABSOLUTE_BEST_STACK.md

Summary:

- iOS: Swift + SwiftUI, UIKit where needed, AVFoundation, Metal, StoreKit 2
- Android: Kotlin + Jetpack Compose, Android Views where needed, Media3/ExoPlayer/Transformer, CameraX if needed, WorkManager, Google Play Billing
- Shared engine later: C++ or Rust for cross-platform timeline/render/export logic

Floently Create must be native if the long-term target is CapCut-level or better quality.

Create should be built after Read and Learn are stable enough, but the architecture decision is final: do not build Create as another React Native/Expo patch.
