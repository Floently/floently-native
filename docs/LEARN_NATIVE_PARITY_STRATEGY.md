# Learn Native Parity Strategy

## Goal

Rebuild Floently Learn natively while preserving the existing product behavior.

The native Learn app must not become a reduced version of the current app.

## Fastest safe method

The fastest safe method is not file-by-file conversion.

The correct method is feature-bundle parity:

1. Use the old Expo Learn app as the reference implementation.
2. Inventory all Learn routes, screens, stores, API calls, and states.
3. Create a parity matrix.
4. Build native iOS and Android feature bundles from that matrix.
5. Test each bundle against the existing backend.
6. Keep old Expo app untouched as fallback.

## Why not file-by-file conversion

File-by-file conversion is unsafe because:

- React Native files do not map cleanly to SwiftUI/Compose files.
- State management is different.
- Navigation is different.
- Auth/session storage is different.
- Native file/audio/background behavior is different.
- It can preserve code shape while losing product behavior.

The target is feature parity, not source-code similarity.

## Learn parity bundles

Native Learn should be rebuilt in these bundles:

### Bundle L01: Shared design/navigation foundation

- product shell
- Learn visual system
- navigation structure
- loading/error/empty states

### Bundle L02: Auth/session/access

- login
- register
- Google auth strategy
- session storage
- entitlement/access state
- device-limit handling if active

### Bundle L03: Learn home and product paths

- YKI path
- Professional path
- Combined/bundle access
- internal all-access test handling

### Bundle L04: Card session UI

- start session
- fetch card
- answer card
- show feedback
- next card
- session summary

### Bundle L05: YKI practice/exam

- YKI practice flow
- YKI exam/runtime flow if supported
- result/review/certificate screens where available

### Bundle L06: Professional Finnish

- profession selection
- nurse/doctor/practical nurse paths
- professional card sessions

### Bundle L07: Speaking/roleplay

- speaking lab
- roleplay scenarios
- AI turn handling
- voice/audio where supported

### Bundle L08: Progress/settings/billing

- progress
- account/settings
- subscription/access screen
- billing state display

## Backend rule

YKI/card bank remains backend-owned.

Native Learn must consume backend APIs.

Native Learn must not:

- copy the full card bank into the app
- mutate canonical card files
- become a second source of truth
- bypass backend validation

## Release rule

Old Expo Learn remains fallback until native Learn passes parity testing and is better than the old app.

---

## No-loss parity gate

The native Learn rebuild must not lose any function, animation, transition, gesture, loading state, error state, empty state, accessibility behavior, or edge-case behavior from the old Expo/React Native Learn app.

The full no-loss rules are recorded in:

- docs/NO_LOSS_NATIVE_PARITY_RULE.md
- docs/LEARN_ANIMATION_AND_INTERACTION_PARITY_CHECKLIST.md

A Learn feature is not complete until its function and interaction parity are verified.
