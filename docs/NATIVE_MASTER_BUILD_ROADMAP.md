# Floently Native Master Build Roadmap

## Final mobile direction

All serious Floently mobile products will be built natively.

- iOS: Swift + SwiftUI
- Android: Kotlin + Jetpack Compose

This applies to:

- Floently Learn
- Floently Read
- Floently Create

The old React Native/Expo app remains the fallback until the native apps are better and ready.

## Build sequence

### Phase 1: Sanitation and containment

Status: started.

Rules:

- Do not delete the old Expo app.
- Do not break existing backend routes.
- Do not move or mutate the YKI/card bank.
- Do not mix native work into `/root/floently-finnish`.
- Keep native work under `/root/floently-native`.

### Phase 2: Native shared foundation

Build shared structure for:

- API contracts
- Auth/session model
- Subscription/access model
- Design tokens
- Product navigation
- Error states
- Offline/cache conventions
- Backend endpoint map

### UI design freeze gate — required before screen implementation

UI is designed before it is coded.

For Iloadi/Floently native product work, implementation of a new or materially changed screen is blocked until the corresponding design contract is defined and frozen under `docs/design/`.

The design contract must define, as applicable:

- screen/navigation structure;
- exact layout measurements;
- typography tokens;
- color tokens;
- component mapping;
- button/control dimensions and placement;
- spacing and alignment;
- loading, empty, disabled, success, error and offline states;
- safe-area/adaptive behaviour;
- accessibility behaviour;
- motion behaviour.

UI implementation must use the shared component/token system instead of locally invented styles.

Source of truth:

- `docs/design/README.md`
- `docs/design/ILOADI_UI_CONSTITUTION.md`
- `docs/design/ILOADI_DESIGN_TOKENS.md`
- `docs/design/ILOADI_COMPONENT_SPEC.md`
- `docs/design/ILOADI_SCREEN_BLUEPRINTS.md`
- `docs/design/ILOADI_DESIGN_CHANGE_GATE.md`

### Phase 3: Native Learn

Goal:

Rebuild Learn natively without damaging the old Learn app.

Learn must use the existing backend and YKI/card bank through APIs.

Native Learn must include:

- Login/account shell
- YKI path
- Professional path
- Combined/bundle access handling
- Card session UI
- Speaking/roleplay shell if APIs are ready
- Progress state
- Subscription state
- Offline-safe caching for delivered cards only

Native Learn must not:

- copy the full card bank into the app
- mutate canonical card files
- bypass backend gates
- become a second source of truth

### Phase 4: Native Read

Goal:

Build a premium native document-to-audio app.

Native Read must include:

- Product-specific Read login
- Import
- Library
- Reader
- Player
- Upload progress
- First-pages-first state
- First-audio-first state
- Retry/cancel/failure states
- Native audio playback
- Native cache
- Subscription/access shell

Read must not show a forever spinner.

### Phase 5: Initial Native Create shell

Goal:

Include only the initial Create foundation while Learn and Read are being released.

Initial Create includes:

- Create landing
- Create login/account shell
- Create project dashboard placeholder
- Coming-soon or waitlist state
- Future API contract notes

Full Create editor is not built yet.

### Phase 6: Release Learn and Read

Release order:

1. Internal/native test builds
2. Device testing
3. Backend API validation
4. Store/test release
5. Production readiness review
6. Release Learn and Read
7. Keep old Expo fallback until native release is stable

### Phase 7: Full Create build

After Learn and Read are stable, focus fully on Create.

Create final stack:

iOS:

- Swift + SwiftUI
- UIKit where needed
- AVFoundation
- Metal
- StoreKit 2
- BackgroundTasks
- FileManager/native cache

Android:

- Kotlin
- Jetpack Compose
- Android Views where needed
- Media3 / ExoPlayer / Transformer
- CameraX if needed
- WorkManager
- Google Play Billing
- Room/DataStore/native cache

Shared engine later:

- C++ or Rust for cross-platform timeline/render/export logic

## Build/package workflow

When building with external AI or human systems:

1. Collect only the necessary files.
2. Package files into clear zip bundles.
3. Build or modify in isolated workspace.
4. Return a modified-files zip.
5. Apply only to `/root/floently-native`, never directly into legacy Expo app.
6. Run checks.
7. Build native artifacts.
8. Only promote after testing.

## Rollback rule

Until native apps are stable, the old Expo app remains the rollback option.

No production package, route, or backend contract should be replaced without a rollback path.

---

## Bundle-only build rule

All native rebuild work must be delivered through bundles, not loose code patches.

Each bundle must include:

- manifest
- files
- apply script
- rollback notes
- verification notes
- known gaps

The full rule is recorded in:

- docs/BUNDLE_ONLY_NATIVE_BUILD_RULE.md
- docs/LEARN_NATIVE_PARITY_STRATEGY.md

---

## No-loss parity gate

Native Learn, Read, and Create must not silently lose existing function or interaction quality.

For Learn especially, every existing function, animation, transition, gesture, loading state, error state, empty state, accessibility behavior, and edge case must be inventoried and either preserved, improved, or explicitly documented as a deliberate approved change.

See:

- docs/NO_LOSS_NATIVE_PARITY_RULE.md
- docs/LEARN_ANIMATION_AND_INTERACTION_PARITY_CHECKLIST.md


---

## Active Read reconciliation roadmap

The architecture paper/repository reconciliation recorded on 2026-10-02 is the active detailed road for the next-generation Read work:

- `docs/READ_ARCHITECTURE_RECONCILIATION_ROADMAP_20261002.md`

For Read, this detailed roadmap refines Phase 4 into evidence gates: establish truth -> reliable browser/playback -> complete reading product -> coherent visual system -> evidence-linked learning -> controlled launch. Where an older Read note conflicts with the reconciliation roadmap, record and resolve the decision explicitly rather than silently choosing one document.

---

## Governing next-generation technology standard

From 2026-09-30 onward, technology selection for the next-generation native app line is governed by:

- `docs/NEXT_APP_POLYGLOT_TECHNOLOGY_STANDARD.md`
- `docs/SPEECHIFY_ARCHITECTURE_STUDY_20260930.md`

This changes the interpretation of "native" from "one language per app" to a controlled polyglot architecture:

- Swift/SwiftUI and Apple-native frameworks own Apple UI/system/media responsibilities.
- Kotlin/Jetpack Compose and Android-native frameworks own Android UI/system/media responsibilities.
- Rust is the preferred language for new shared high-performance systems code when a shared engine is justified.
- C++ is preferred where mature media/codec/render ecosystems or dependencies make it the stronger choice.
- C is limited to low-level/ABI/library interop.
- TypeScript/React owns web/browser UI.
- TypeScript/Node.js, Go, Python/FastAPI or Django may own backend components according to workload.
- Python owns ML/model/data work by default; platform ML runtimes such as Core ML are used for on-device inference when appropriate.

The rule is **best tool for the component, minimum number of boundaries**. Adding languages without a concrete architecture boundary and measurable benefit is prohibited.

This standard applies to the next-generation `floently-native` line. It does not order a rewrite of the current production React Native/Expo app.
