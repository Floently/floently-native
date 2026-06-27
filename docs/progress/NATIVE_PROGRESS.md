# Floently Native Progress

## Current milestone

M11 Store and signing readiness.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android debug and release builds succeeded after verifying Learn service wiring.

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
```

Latest verified commit locally: `46d29a1`.

## Completed

- Native repository created and pushed.
- Android Learn debug build verified.
- Auth, secure session storage, API client, product access gate, Learn app container, and signed-in shell are in place.
- YKI, Professional Finnish, Roleplay, Cards, Progress, and Account native foundations are in place.
- Learn readiness dashboard, production gates dashboard, Account boundary, and Progress sync boundary are verified.
- Floently suite shell is verified with Learn, Read, Create, product selector, separate access checks, and product-neutral sign-in.
- Native Read MVP vertical slice is verified with text/URL input, language detection boundary, generation boundary, Read automatically setting, reader panel, and saved library.
- Create Studio MVP vertical slice is verified with direct function cards, input screen, generation boundary, result panel, projects panel, export-ready state, and suite shell repository wiring.
- Payments and entitlement service wiring is verified using existing backend routes and fallback-safe UI without new Learn environment variables.
- Read/Create backend service wiring is verified with service routes, repository fallbacks, app container injection, and suite shell repository injection.
- Learn roleplay, cards, and progress service wiring is verified while preserving the existing Learn UI/function structure.
- Backend integration contracts are verified with native API route registry and suite selector backend contract dashboard.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Final readiness gap dashboard is verified with release gate counts and blocker order.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Prepare store/signing readiness without committing private signing values or generated artifacts.

## Next step

Verify M11-A store/signing readiness with a clean local pull, signing status, debug build, release build, and release bundle build.

## Pending verification

- M11-A store/signing readiness was pushed with ignored signing properties, optional release signing configuration, signing status command, Android release bundle command, and signing documentation.
