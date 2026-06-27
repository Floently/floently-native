# Floently Native Progress

## Current milestone

M10 Backend service wiring for Read/Create/Learn.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android debug and release builds succeeded after verifying Read/Create backend service wiring.

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
```

Latest verified commit locally: `e077c54`.

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
- Backend integration contracts are verified with native API route registry and suite selector backend contract dashboard.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Final readiness gap dashboard is verified with release gate counts and blocker order.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Wire Learn roleplay, cards, and progress services into the native app using existing backend/environment assumptions first. Keep Learn UI improvement-only and avoid new Learn environment variables.

## Next step

Verify M10-B Learn service wiring with a clean local pull, debug build, and release build.

## Pending verification

- M10-B Learn service wiring was pushed with roleplay, cards, and progress service repository injection into the app container, suite shell, and Learn shell while preserving the existing Learn UI/function structure.
