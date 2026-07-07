# M28 Android Production Freeze

Status: ACTIVE

Goal: finish Android native to production-freeze so iOS SwiftUI can start from a stable reference implementation.

## Freeze requirements

Before iOS starts:

1. Android debug build passes.
2. Android release build passes.
3. No accidental placeholder route for a shipped feature.
4. Main Learn feature screens are present and reachable.
5. Old-source parity method is recorded.
6. Remaining known gaps are documented clearly.
7. Backend/runtime contracts are preserved.
8. Production-freeze commit is pushed.

## Feature packs already verified

- Home
- YKI Practice landing
- YKI Mock landing
- Cards runtime parity
- Roleplay visual component extraction

## Remaining focus

- Professional Finnish
- Progress
- Settings / Account
- Auth / access gate
- Billing/subscription if present in Android scope
- Final placeholder/TODO sweep
