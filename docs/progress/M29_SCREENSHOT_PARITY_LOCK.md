# M29 Screenshot Parity Lock

Status: ACTIVE

Date: 2026-07-07

## Why this exists

Manual QA on the installed Android debug app showed that the app is **not production-freeze ready**. Several screens compile and route correctly, but they do not yet match the old app UI/UX and function structure.

Android production-freeze is paused until this screenshot lock is satisfied.

## New source of truth

The attached old-app UI pack contains **103 screenshots** and is now the primary parity source of truth.

Stored repo assets:

- `docs/ui-lock/screenshots/`
- `docs/ui-lock/screenshot_manifest.csv`
- `docs/ui-lock/contact_sheet_*.jpg`

## Non-negotiable parity rules

1. Do not start iOS until Android passes this parity lock.
2. Do not call Android production-freeze while major screens only compile but visually differ from the old app.
3. Use screenshots first, old React Native source second, current Kotlin implementation third.
4. Every shipped route must match old app structure, not just have a working placeholder/native approximation.
5. No user-facing route may stop at B2 if the intended old-app flow covers A1-C2.
6. Do not route around missing old-app flows just to pass build gates.
7. If a screen has two old-app sections, the native screen must expose the same section structure.

## Immediate QA findings from installed Android app

### Everyday Finnish

Current Android behavior is not in parity.

Expected from old app/user QA:

- Everyday Finnish has two main sections.
- From Home, tapping Everyday Finnish should open into the old app's card/roleplay-oriented structure.
- Native should not show a generic invented hub that differs from old app.

Required action:

- Rebuild Everyday Finnish from screenshot lock and old source.
- Preserve correct routing to Cards and Roleplay, but match the old visible structure first.

### Professional Finnish

Current Android behavior is not in parity.

Required action:

- Rebuild Professional Finnish from screenshot lock and old source.
- Ensure workplace/professional flows match the old app structure and UI.

### Level coverage

Current Android screens often stop at B2.

Expected:

- Finnish level coverage should support A1-C2 where the old app intended A1-C2.

Required action:

- Audit every level selector, level strip, progress model, repository, and UI copy.
- Replace A1-B2-only assumptions where they are not intended.

## Production-freeze gates

Android cannot be production-frozen until:

1. Screenshot manifest is committed.
2. Everyday Finnish is rebuilt against screenshot lock.
3. Professional Finnish is rebuilt against screenshot lock.
4. A1-C2 level coverage is audited and patched.
5. Navigation reaches no placeholder or invented parity-bypass screen.
6. Debug build passes.
7. Release build passes.
8. Manual device QA confirms old-app parity for the locked flows.

## Fastest method to use

Continue using the proven method recorded in `NATIVE_SPEED_BUILD_METHOD.md`, but with this correction:

**Screenshot lock first -> old source second -> native runtime third.**

The fastest safe workflow is:

1. Group screenshots by feature.
2. Map screenshots to old React Native screen/component files.
3. Map those to current Kotlin screens.
4. Extract old-source Compose modules only when they match screenshot structure.
5. Delegate existing native runtime only after the visible UI structure is correct.
6. Build debug/release after each feature pack.
7. Install and manually compare on device.

## Next implementation order

1. Screenshot grouping and map.
2. Everyday Finnish rebuild.
3. Professional Finnish rebuild.
4. A1-C2 level coverage patch.
5. Progress/Settings/Account parity sweep.
6. Full debug/release build.
7. Device QA.
8. Android production-freeze tag.
9. Start iOS SwiftUI.
