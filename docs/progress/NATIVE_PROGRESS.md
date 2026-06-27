# Floently Native Progress

## Current milestone

M16 Learn-first release completion and polish.

## Branch

`m01/production-native-foundation`

## Latest verified build

M15-A prepared the debug QA APK and QA result file for device testing.

```bash
git pull --ff-only
./scripts/nativectl signing-status
./scripts/nativectl build android-learn
cp -f apps/android/FloentlyLearn/app/build/outputs/apk/debug/*.apk /home/vitus/
```

Latest verified commit locally: `6d15f07`.

Latest QA artifact: `/home/vitus/floently-qa-debug-6d15f07.apk`.

## Completed

- Native repository created and pushed.
- Android Learn debug build verified.
- Auth, secure session storage, API client, product access gate, Learn app container, and signed-in shell are in place.
- YKI, Professional Finnish, Roleplay, Cards, Progress, and Account native foundations are in place.
- Learn readiness dashboard, production gates dashboard, Account boundary, and Progress sync boundary are verified.
- Floently suite shell is verified with Learn, Read, Create, product selector, separate access checks, and product-neutral sign-in.
- Native Read MVP vertical slice is available but no longer release-blocking for the first release.
- Create Studio MVP vertical slice is available but no longer release-blocking for the first release.
- Payments and entitlement service wiring is verified using existing backend routes and fallback-safe UI without new Learn environment variables.
- Read/Create backend service wiring is available for later release-quality work.
- Learn roleplay, cards, and progress service wiring is verified while preserving the existing Learn UI/function structure.
- Store/signing readiness is verified with ignored signing properties, optional signing config, signing status command, release build, and release bundle build.
- Store submission and manual QA pack is verified with listing draft, screenshot checklist, data-safety prompts, release notes draft, and real-device checklist.
- Final release blocker dashboard cleanup is verified with only true blockers remaining: live backend QA, payment provider checkout, local release signing, privacy/data-safety/screenshots, and manual real-device QA.
- Final local QA runbook is verified with fixed-order build, install, auth, suite, Learn, Read, Create, payment/access, offline/error, signing, and result-recording instructions.
- Learn-first release decision is recorded: complete and polish Learn first, release Learn, then continue Read and Create to release quality.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Complete and polish Floently Learn to match or improve the previous pre-native Learn experience, then release Learn first. Read and Create remain later release-quality tracks after Learn release.

## Next step

Run M16-A Learn-first release scope verification, inspect current Learn files, then patch Learn UI/function gaps without rebuilding it from scratch.

## Pending verification

- M16-A should lock Learn as the first release target, keep Read/Create non-blocking for first release, and start the Learn premium polish/completion pass.
