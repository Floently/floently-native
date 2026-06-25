# Android Architecture Baseline

## Target shape

The production Android app should move toward this structure:

- app shell
- shared design system
- shared API client
- auth feature
- subscription/access feature
- Learn domain feature
- Read domain feature
- Create shell/feature later
- platform storage/security layer
- build logic / Gradle conventions

## Build quality gates

Every macro build bundle must verify:

- Gradle configuration is deterministic
- Java and Kotlin targets match
- Android SDK path is explicit
- no generated archives are committed
- no old Expo app paths are modified
- debug APK build succeeds before deeper runtime testing

## Current B00-B03 state

B00-B03 are build-foundation stages.

They are not the final Learn implementation.

The current goal is to make the native toolchain compile cleanly, then replace smoke UI with maintainable production architecture.
