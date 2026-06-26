# Floently Native Progress

## Current milestone

M06 Backend integration contracts.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after verifying the Payments and entitlement boundary.

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `c05df0a`.

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

## Active task

Fastest flow: define backend contracts, then wire real services, release build, and store readiness.

## Next step

Verify M06-A Backend integration contracts with a clean local pull/build, then continue directly into release build/signing preparation.

## Pending verification

- M06-A Backend integration contracts were pushed with backend contract models, backend repository boundary, native API route registry, service status counts, and suite selector backend contract dashboard.
