# Floently Native Progress

## Current milestone

M08 Final readiness gap closure.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android debug and release builds succeeded after verifying release build preparation.

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
```

Latest verified commit locally: `e49608b`.

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
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.

## Active task

Fastest flow: close real release blockers in order: payment provider, backend service wiring, signing, manual QA, and store assets.

## Next step

Verify M08-A final readiness gap dashboard with a clean local pull, debug build, and release build.

## Pending verification

- M08-A final readiness gap dashboard was pushed with release readiness models, release readiness repository, verified/wiring/blocker counts, store-ready status, top release gates, and suite selector release readiness card.
