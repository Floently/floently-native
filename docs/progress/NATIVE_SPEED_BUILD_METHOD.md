# Native Speed Build Method

Status: ACTIVE

This document records the fastest proven method for completing Floently native builds without losing parity quality.

## Best method so far

The fastest and safest method is:

**Recover old source → map to native target → extract old-source Compose module → delegate from existing native runtime → verify debug/release.**

This is faster than rewriting from memory and safer than many small blind edits.

## Proven workflow

1. Search the old backup/source of truth.
2. Identify the original React Native screen/components.
3. Identify the current Kotlin/Compose screen.
4. Preserve the richer native runtime when it already exists.
5. Extract old-source visual structure into a dedicated Compose file.
6. Delegate the native screen/section to that module.
7. Patch actual inspected composables, not guessed names.
8. Build debug.
9. Build release.
10. Commit and push.
11. Update progress docs.

## What worked best

### 1. Source-driven parity
Use old app code as the specification, not inspiration.

### 2. Feature-pack batching
Work by feature packs, not tiny individual widgets.

Good packs:
- Home
- YKI Practice
- YKI Mock
- Cards
- Speaking / Roleplay
- Professional Finnish
- Progress
- Settings / Account
- Auth / Billing / Onboarding

### 3. Extract-and-delegate
When a native runtime already exists, do not rewrite it. Create a new old-source visual module and delegate to it.

Example:
- `OldSourceYkiPracticeLanding.kt`
- `OldSourceYkiMockLanding.kt`

### 4. Runtime preservation
If Kotlin already has stronger runtime logic than old React Native, keep the Kotlin runtime and only change visual shell/composables.

### 5. Inspect before patching
Before editing, inspect exact function names and shell wrappers. Avoid guessed names.

### 6. Build gates
Every feature pack must pass:
- `./scripts/nativectl build android-learn`
- `./scripts/nativectl build android-learn-release`

## Methods to avoid

- Blind search-and-replace patches.
- Rewriting a whole 1000+ line Kotlin screen when only the shell differs.
- Trying Android and iOS simultaneously before Android parity is frozen.
- UI improvements before old-app parity is verified.

## Android-to-iOS plan

Finish Android native first. Android becomes the reference implementation.

Then build iOS native with:
- Swift
- SwiftUI
- Same source-driven parity method
- Same feature-pack order
- Same backend/API/runtime contracts
- Android Compose screens as implementation blueprint

## Current rule

Until Android parity is frozen:

**No new product expansion. No UI improvement beyond old-source parity. Speed comes from feature-pack extraction and delegation, not shortcuts.**
