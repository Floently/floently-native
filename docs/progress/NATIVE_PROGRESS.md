# Floently Native Progress

## Current milestone

M09 Payment provider and entitlement service wiring.

## Branch

`m01/production-native-foundation`

## Latest verified build

Learn UI/environment guardrail docs were pulled and verified.

```bash
git pull --ff-only
```

Latest verified commit locally: `7a7b652`.

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
- Final readiness gap dashboard is verified with release gate counts and blocker order.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Wire payment and entitlement services using existing backend/environment assumptions first. Do not introduce new Learn environment variables.

## Next step

Verify M09-A payment and entitlement service wiring with a clean local pull, debug build, and release build.

## Pending verification

- M09-A payment and entitlement service wiring was pushed with service billing repository injection into the suite, existing `/api/v1/billing/dashboard` and `/api/v1/billing/checkout` route usage, checkout URL recognition, and fallback-safe UI when existing backend routes are not ready.
