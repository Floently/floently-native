# Legacy Containment and Sanitation Plan

Created UTC: 20260625T174029Z

## Decision

The old React Native/Expo app remains untouched as the fallback and current production safety line.

The new native rebuild must live in:

- /root/floently-native/apps/ios
- /root/floently-native/apps/android
- /root/floently-native/shared/api-contracts
- /root/floently-native/docs

## What sanitation means

Sanitation does not mean deleting the old app.

Sanitation means:

1. Inventory the old server/app state.
2. Freeze the old app as legacy/fallback.
3. Stop mixing new native work into the old Expo app.
4. Keep backend, auth, billing, YKI engine, card bank, and production routes alive.
5. Add new native API contracts separately.
6. Build native clients in the isolated native workspace.
7. Replace production only when native apps are tested and better.

## What must not be deleted now

Do not delete:

- /root/floently-finnish
- backend services
- YKI/card bank
- payment config
- auth/session config
- nginx routes
- database/data
- old React Native app
- existing Android/iOS release artifacts
- card-bank archives or canonical bank

## Backend source of truth

The backend remains the source of truth for:

- users
- subscriptions
- YKI engine
- card bank
- learning sessions
- Read document processing APIs
- future Create APIs

Native apps must consume backend APIs.

## Native rebuild rule

Do not build native Learn/Read/Create inside the old Expo app folder.

Use /root/floently-native only.

## Rollback rule

Until native apps are fully ready, the old Expo app remains the rollback option.

No production route or store package should be replaced without a tested rollback path.
