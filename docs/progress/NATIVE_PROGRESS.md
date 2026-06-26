# Floently Native Progress

## Current milestone

M05 Payments and entitlement wiring.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after verifying the Create Studio MVP vertical slice.

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `6c10e8a`.

## Completed

- Native repository created and pushed.
- Android Learn debug build verified.
- Auth, secure session storage, API client, product access gate, Learn app container, and signed-in shell are in place.
- YKI, Professional Finnish, Roleplay, Cards, Progress, and Account native foundations are in place.
- Learn readiness dashboard, production gates dashboard, Account boundary, and Progress sync boundary are verified.
- Floently suite shell is verified with Learn, Read, Create, product selector, separate access checks, and product-neutral sign-in.
- Native Read MVP vertical slice is verified with text/URL input, language detection boundary, generation boundary, Read automatically setting, reader panel, and saved library.
- Create Studio MVP vertical slice is verified with direct function cards, input screen, generation boundary, result panel, projects panel, export-ready state, and suite shell repository wiring.

## Active task

Fastest flow: wire payments and entitlement boundaries, then backend services, release build, and store readiness.

## Next step

Verify M05-A Payments and entitlement boundary with a clean local pull/build, then continue directly into backend integration contracts.

## Pending verification

- M05-A Payments and entitlement boundary was pushed with billing models, billing repository boundary, separate Learn/Read/Create plan snapshots, checkout intent boundary, suite product plan display, and checkout preparation UI.
