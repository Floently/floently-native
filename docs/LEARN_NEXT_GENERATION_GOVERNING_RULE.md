# KieliValmis Next-Generation Governing Rule

**Status:** GOVERNING ARCHITECTURE AND PRODUCT RULE  
**Effective:** 2026-10-02  
**Repository:** `Floently/floently-native`  
**Applies to:** next-generation KieliValmis Learn on iOS, Android, and future web surfaces  
**Legacy repository:** `Floently/floently-finnish` remains production/fallback until replacement gates pass

## 1. Final decision

KieliValmis is being rebuilt as a **clean parallel next-generation product**.

The new implementation is not a gradual conversion of the React Native/Expo app and is not required to preserve its source-code structure, navigation structure, screen composition, or visual layout.

The old app is:

- a production/fallback line;
- a functional reference;
- an API-behaviour reference;
- a rollback source.

The new app is:

- a separate native product implementation;
- free to use a new information architecture;
- free to combine, move, replace, or redesign old screens;
- required to preserve product-critical capabilities unless a change is explicitly approved.

**There is no source-code merge step between old and new KieliValmis.**

When the new native apps pass all replacement gates, they may take over the existing production application identities and be shipped as normal App Store / Play Store updates.

## 2. Isolation rule

All new mobile work lives under:

- `apps/ios/FloentlyLearn`
- `apps/android/FloentlyLearn`

Future independent web work belongs under:

- `apps/web/KieliValmisWeb`

Do not build next-generation Learn UI inside `Floently/floently-finnish`.

Do not copy native code back into the Expo app.

Do not modify the legacy app merely to make the native architecture easier.

The two implementations may use the same backend contracts while they coexist.

## 3. Governing engineering principle

> **Best tool for the component, minimum number of boundaries.**

KieliValmis is polyglot by architecture boundary, not by language count.

A new language/runtime is added only when it has a concrete responsibility and measurable product benefit.

## 4. Technology ownership

### iOS

Primary:

- Swift 6+
- SwiftUI

Use UIKit selectively for:

- advanced text/input behaviour;
- precision scrolling/gestures;
- specialized controls;
- platform APIs not adequately exposed by SwiftUI.

Use native Apple frameworks for:

- AVFoundation: recording/playback/audio session;
- Keychain: secrets/session material;
- SwiftData/SQLite as appropriate for bounded local state;
- BackgroundTasks where durable deferred work is justified;
- Core ML only when on-device inference has a measured benefit.

### Android

Primary:

- Kotlin
- Jetpack Compose

Use Android Views / Canvas / Paint where precision or performance requires them.

Use:

- Coroutines + Flow/StateFlow;
- Room for structured local data;
- DataStore for preferences;
- Android Keystore for secrets;
- WorkManager for durable deferred work;
- native audio/media APIs for speech capture/playback.

### Web

Primary:

- React + TypeScript.

Use browser-native APIs for routing, accessibility, storage, audio, recording, and offline behaviour.

Web Workers are preferred for expensive client work.

Rust/WASM is optional, not mandatory.

### Backend and learning intelligence

Python/FastAPI remains the default for:

- current KieliValmis/YKI domain runtime;
- evaluation;
- roleplay orchestration;
- speech/AI integration;
- writing intelligence;
- model-serving and ML-adjacent services.

PostgreSQL is the canonical durable learner-data store.

Redis may be used for ephemeral cache, rate limiting, coordination, or queues, but never as the only source of learner truth.

Object storage owns recordings/generated audio/large media artifacts where appropriate.

### Rust

Rust is **not introduced merely because READ uses Rust**.

Rust is justified for Learn only if a deterministic/high-performance shared kernel must run identically across iOS, Android, web, and/or backend, for example:

- offline review scheduling;
- deterministic mastery transitions;
- prerequisite graph traversal;
- session candidate ranking;
- shared low-level text or signal processing.

Until that need is proven, learner adaptation remains server-owned.

### Go

Go may be introduced for a measured high-concurrency service such as a realtime speech/event gateway when Python/Node is demonstrably the wrong operational fit.

It is not a default requirement.

## 5. Shared semantic contracts

The next-generation product will converge on versioned language-neutral contracts.

Target contract family:

- `LearningSessionPlanV1`
- `ActivityDefinitionV1`
- `LearningEventV1`
- `EvaluationResultV1`
- `LearnerSnapshotV1`
- `ProgressProjectionV1`
- `RecommendationV1`
- `SpeechTurnV1`

These contracts define meaning; platform UI code does not.

Platform types should be generated from OpenAPI/JSON Schema or another versioned schema source where practical rather than maintained independently by hand.

## 6. Durable state ownership

A screen must not own durable learning truth.

Long-lived app/session owners should include:

### LearningSession

Owns:

- session id and revision;
- pathway;
- activity queue;
- current activity/cursor;
- attempt state;
- bounded delivered content;
- pending learner events;
- resume state;
- completion/submission state.

### SpeechSession

Owns:

- recording lifecycle;
- microphone/audio route state;
- local recording reference;
- upload/stream state;
- transcript/evaluation state;
- AI/roleplay turn state;
- interruption/retry state.

### SyncSession / Outbox

Owns:

- pending `LearningEventV1` writes;
- idempotency keys;
- retry policy;
- connectivity recovery;
- acknowledgement state.

UI screens observe/render these owners. Mounting or unmounting a screen must not silently reset the learner session.

## 7. Learner truth rule

The new UI must never fabricate learner-specific:

- progress;
- readiness;
- due counts;
- mastery;
- confidence;
- streaks;
- recommendations;
- completed missions;
- exam results.

The data path is:

```text
learner action
    -> LearningEventV1
    -> durable learner event store
    -> projections/mastery
    -> progress/recommendations
    -> UI
```

If authoritative learner data is unavailable, show a true empty/loading/offline/unavailable state.

## 8. Offline rule

The native apps may cache bounded delivered activities and collect learner events offline.

Preferred flow:

```text
server LearningSessionPlan
        -> bounded device cache
        -> learner works
        -> local event outbox
        -> sync/retry
        -> server durable store
        -> authoritative projections
```

The complete canonical card bank must not be shipped into the mobile app.

Offline completion does not authorize the client to invent permanent mastery/readiness values.

## 9. YKI authority

YKI remains server-authoritative for:

- exam/session definition;
- task selection;
- server timing/deadlines where applicable;
- attempt history;
- writing/speaking evidence;
- evaluation/result generation;
- audit records.

Native clients are responsible for excellent interaction, local draft safety, recording, interruption recovery, submission state, and resumability.

## 10. Speech and roleplay rule

Native platforms own microphone/audio lifecycle.

Python/ML services own speech intelligence by default.

STT transcript similarity and pronunciation quality are not the same concept. A future pronunciation system must distinguish:

- what the learner said;
- how the speech sounded;
- phonetic/acoustic evidence;
- calibrated pedagogical feedback.

Roleplay sessions must be bound to the authenticated learner. Shared `preview` ownership is not an acceptable long-term runtime contract.

## 11. Progress architecture

Progress is an evidence product, not a decorative dashboard.

The shared learning loop is:

```text
Diagnose -> Learn -> Retrieve -> Produce -> Correct -> Schedule -> Review
```

The same evidence foundation should power:

- vocabulary mastery;
- grammar development;
- listening;
- speaking;
- reading;
- writing;
- Everyday Finnish;
- Professional Finnish;
- YKI;
- Revision Vault;
- Phrase Bank;
- Confidence;
- Planner;
- recommendations;
- Continue/next-action decisions.

Separate features must not invent competing definitions of learner progress.

## 12. Clean redesign rule

The old KieliValmis UI is a **capability reference, not a visual template**.

For every legacy capability, the native rebuild classifies it as one of:

1. preserve;
2. improve;
3. combine with another capability;
4. move to a better location;
5. replace with a better interaction;
6. deliberately retire with written approval.

Visual parity is not a goal.

Source-code parity is not a goal.

The new product should feel intentionally native, calm, highly aligned, spacious, and visually sophisticated.

## 13. Card and surface rule

KieliValmis uses a controlled family of surfaces rather than one generic card.

Approved semantic families:

- Hero / Continue card
- Pathway card
- Skill card
- Progress card
- Activity card
- Recommendation card
- Mission / scenario card
- Review card
- Achievement card
- Compact row/list surface

Cards are used only for real grouping, selection, preview, state, or an elevated task.

**Excessive cardification is prohibited.**

Large open canvas, lists, dividers, typography, and whitespace remain first-class layout tools.

Detailed geometry is governed by:

- `docs/design/ILOADI_DESIGN_TOKENS.md`
- `docs/design/ILOADI_COMPONENT_SPEC.md`
- `docs/design/KIELIVALMIS_CARD_AND_LAYOUT_SYSTEM_V1.md`

## 14. Design-before-code rule

Every materially new screen follows:

```text
behaviour
-> screen inventory
-> blueprint
-> component mapping
-> states
-> accessibility
-> design freeze
-> implementation
-> device QA
```

No developer/agent may invent arbitrary spacing, radii, typography, card styles, or navigation styles inside a screen.

## 15. Production replacement gates

The new native app may replace the old app only when the retained product scope passes:

- functional capability coverage;
- deliberate-change review;
- visual/UI quality;
- interaction/motion quality;
- accessibility;
- authentication/session migration;
- subscription/entitlement correctness;
- durable learner-data continuity;
- offline/retry behaviour;
- YKI/card-bank authority checks;
- speech/roleplay checks;
- performance/memory/battery checks;
- iOS and Android real-device testing;
- deep-link/app-identity migration;
- store-signing/release readiness;
- rollback readiness.

Until then, the old Expo app remains intact.

## 16. Build/cutover safety

Do not spend production/TestFlight/Play release builds merely to prove source changes.

Source, static validation, unit tests, and local/CI compilation should be used first.

A store/device build is produced only when the agreed qualification bundle is ready.

## 17. Immediate implementation order

1. Freeze this governing rule.
2. Freeze KieliValmis card/layout geometry.
3. Build shared semantic contract definitions.
4. Build durable learner-event/data foundation.
5. Build native long-lived session owners.
6. Build the new Home shell and card system.
7. Port Everyday learning using existing backend/card APIs.
8. Build evidence-based Progress and recommendations.
9. Port YKI over the existing server engine.
10. Port Professional Finnish.
11. Build advanced speech/roleplay.
12. Add Rust/Go only when a measured boundary justifies them.
13. Qualify native apps.
14. Cut over production identities without source-merging with Expo.

## 18. Conflict resolution

If an older native-rebuild document implies that the new app must visually reproduce the Expo app, this rule takes precedence.

"No-loss parity" means no silent loss of required capability. It does **not** mean preserving old screen geometry, navigation, card design, or code structure.

The native app may be completely different while still satisfying capability coverage.
