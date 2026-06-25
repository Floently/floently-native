# Floently Learn Native Rebuild Safety Plan

## Current Learn stack

The current Learn mobile app was built with:

- React Native
- Expo
- Expo Router
- TypeScript
- EAS Build

This stack was practical and allowed faster cross-platform development.

## Absolute best mobile stack decision

If difficulty, time, and maintenance are ignored, the absolute best mobile stack for Floently Learn is also native:

- iOS: Swift + SwiftUI
- Android: Kotlin + Jetpack Compose

Native is the highest-quality path for final mobile polish, platform feel, animations, accessibility, offline behavior, background work, and long-term performance.

## Does this mean current Learn is useless?

No.

The current Learn app remains valuable as:

- Working fallback
- Existing released product path
- Reference implementation
- API behavior reference
- UI/content structure reference
- Emergency rollback option

The native Learn rebuild must not damage the current Learn app.

## Safe rebuild rule

The native Learn rebuild must be isolated.

Do not build native Learn inside the old React Native app folder.

Use:

- /root/floently-native/apps/ios/FloentlyLearn
- /root/floently-native/apps/android/FloentlyLearn

The current app remains in:

- /root/floently-finnish

## Backup and rollback strategy

Before replacing Learn in production:

1. Keep the old Expo/React Native Learn app intact.
2. Keep the old Git branch and release artifacts.
3. Keep the old backend routes working.
4. Build native Learn using separate development bundle IDs first.
5. Test native Learn with the same backend APIs.
6. Only replace the production store app when native Learn is clearly better.
7. If native Learn is delayed, continue using the existing app with zero disruption.

Suggested temporary bundle IDs during development:

- iOS native Learn test: com.vitusidi.floently.learn.native.dev
- Android native Learn test: com.vitusidi.floently.learn.native.dev

Final production replacement can later use the existing production bundle/package only when ready.

## YKI and card bank safety

The YKI engine and card bank must remain backend/server-owned.

Native Learn must consume cards through APIs.

Native Learn must not:

- move the card bank into the mobile app
- rewrite canonical card files
- mutate YKI/card-bank source files directly
- bypass backend validation
- create a second source of truth

Safe architecture:

- Backend/card bank remains source of truth.
- Native iOS/Android apps call backend APIs.
- Card schemas are documented in shared API contracts.
- Apps cache only user/session/card delivery data locally.

## Why this is safe

Swift/Kotlin clients can use the same YKI/card bank safely because the content lives on the backend.

The native app is only the presentation/client layer.

As long as the backend API contract is stable, the old Expo app and new native apps can coexist during migration.
