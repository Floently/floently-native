# Floently Native Progress

## Current milestone

M08 Final readiness gap closure.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android debug and release builds succeeded after verifying final readiness gap dashboard.

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
```

Latest verified commit locally: `754a6bc`.

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
- Learn UI and environment guardrails are documented: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Fastest flow with compatibility guardrails: close real release blockers in order without rebuilding Learn or changing environment assumptions.

## Next step

Verify the Learn UI/environment guardrail docs with a clean local pull, then continue into payment provider and entitlement service wiring using the existing environment assumptions.

## Pending verification

- M08-B Learn UI and environment guardrails were pushed with a dedicated architecture note and release checklist guardrails.
