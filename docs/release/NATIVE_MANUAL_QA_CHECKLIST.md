# Native manual QA checklist

## Scope

Run this checklist on a real Android device or emulator before store submission. Do not treat Gradle build success as store readiness by itself.

## Build checks

- `./scripts/nativectl signing-status`
- `./scripts/nativectl build android-learn`
- `./scripts/nativectl build android-learn-release`
- `./scripts/nativectl build android-learn-bundle`

Expected before signing is configured: signing status can show `not_configured`, but debug, release, and bundle builds must still succeed.

## Authentication

- App launches cleanly.
- Signed-out screen appears when no session exists.
- Login works with the existing backend environment.
- Session persists after app restart.
- Sign out clears session and returns to sign-in.
- Access failure shows a clear retry/gate state.

## Suite selector

- Product selector shows Learn, Read, and Create separately.
- Opening each product triggers its own access check.
- Blocked products show access-needed UI without breaking the app.
- Checkout boundary can be prepared without crashing.
- Back navigation returns to the suite selector.

## Learn

Guardrail: Learn must look like an improved Learn experience, not a rebuilt unrelated UI.

- Learn home opens from the suite selector.
- YKI practice opens and returns correctly.
- Professional Finnish opens and returns correctly.
- Roleplay opens, starts a session, sends a learner message, and receives partner/coach response or safe fallback.
- Cards opens, starts a session, records a rating, and shows summary behavior or safe fallback.
- Progress opens and displays real backend data or safe fallback.
- Account opens and sign-out still works.
- No Read/Create tabs are mixed into Learn screens.

## Read

- Read opens from the suite selector.
- Text input accepts pasted text.
- URL input is treated as URL source.
- Language detection runs or uses safe fallback.
- Read automatically defaults on.
- Generation creates a reader session or safe fallback.
- Saved library displays without crashing.
- Back navigation returns to the suite selector.

## Create Studio

- Create Studio opens from the suite selector.
- Direct tools are visible: hooks, captions, summarize, repurpose, brand brain.
- Tool selection updates the draft state.
- Generate calls backend or safe fallback.
- Result card displays output.
- Project/history section displays without crashing.
- Back navigation returns to the suite selector.

## Payments and entitlement

- Billing dashboard loads real plans or safe fallback.
- Learn, Read, and Create plan states remain separate.
- Checkout intent handles real checkout URL or safe pending state.
- Product access remains separate unless a bundle is intentionally introduced later.

## Offline/error states

- App does not crash when backend is unreachable.
- Error/fallback messaging is understandable.
- Retrying access does not duplicate sessions or break navigation.

## Store blockers

Do not submit until these are resolved:

- Signing configured outside Git.
- Privacy policy URL available.
- Data safety answers completed honestly.
- Screenshots captured from a real device.
- Payment provider checkout verified.
- Learn roleplay dynamic generation and anti-repetition verified against backend.
- Read upload/generation verified against backend.
- Create generation/export verified against backend.
