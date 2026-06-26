# Floently Native Progress

## Current milestone

M07 Release build and signing preparation.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after verifying Backend integration contracts.

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `a432592`.

## Completed

- Native repository created and pushed.
- Android Learn debug build verified.
- Auth, secure session storage, API client, product access gate, Learn app container, and signed-in shell are in place.
- YKI, Professional Finnish, Roleplay, Cards, Progress, and Account native foundations are in place.
- Learn readiness dashboard, production gates dashboard, Account boundary, and Progress sync boundary are verified.
- Floently suite shell is verified with Learn, Read, Create, product selector, separate access checks, and product-neutral sign-in.
- Native Read MVP vertical slice is verified with text/URL input, language detection boundary, generation boundary, Read automatically setting, reader panel, and saved library.
- Create Studio MVP vertical slice is verified with direct function cards, input screen, generation boundary, result panel, projects panel, export-ready state, and suite shell repository wiring.
- Payments and entitlement boundary is verified with separate Learn, Read, and Create plan snapshots and checkout intent boundary.
- Backend integration contracts are verified with native API route registry and suite selector backend contract dashboard.

## Active task

Fastest flow: prepare release build, signing documentation, backend wiring, QA, and store readiness.

## Next step

Verify M07-A release build preparation with a clean local pull, debug build, and release build.

## Pending verification

- M07-A release preparation was pushed with production application ID, Floently app label, version bump, release build type, native release build command, and Android release checklist.
