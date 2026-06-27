# Floently Native Progress

## Current milestone

M13 Final release blocker dashboard cleanup.

## Branch

`m01/production-native-foundation`

## Latest verified build

Store submission and manual QA pack was pulled and documentation was verified.

```bash
git pull --ff-only
sed -n '1,220p' docs/release/PLAY_STORE_SUBMISSION_PACK.md
sed -n '1,260p' docs/release/NATIVE_MANUAL_QA_CHECKLIST.md
sed -n '1,120p' docs/progress/NATIVE_PROGRESS.md
```

Latest verified commit locally: `cb336dd`.

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
- Store/signing readiness is verified with ignored signing properties, optional signing config, signing status command, release build, and release bundle build.
- Store submission and manual QA pack is verified with listing draft, screenshot checklist, data-safety prompts, release notes draft, and real-device checklist.
- Backend integration contracts are verified with native API route registry and suite selector backend contract dashboard.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Clean up the in-app release readiness dashboard and docs so only true final blockers remain.

## Next step

Verify M13-A final release blocker dashboard cleanup with a clean local pull, debug build, release build, bundle build, and final blocker document check.

## Pending verification

- M13-A final release blocker dashboard cleanup was pushed with verified service wiring gates and true remaining blockers: live backend QA, payment provider checkout, local release signing, privacy/data-safety/screenshots, and manual real-device QA.
