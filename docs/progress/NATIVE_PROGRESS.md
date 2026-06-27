# Floently Native Progress

## Current milestone

M16 Learn-first release completion and polish.

## Branch

`m01/production-native-foundation`

## Latest verified build

M16-E Learn release-candidate audit/build/QA artifact prep was pulled and verified with debug, release, and bundle builds.

```bash
git pull --ff-only
./scripts/nativectl signing-status
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
./scripts/nativectl build android-learn-bundle
```

Latest verified commit locally: `73ab08b`.

Latest QA artifact: `/home/vitus/floently-learn-rc-debug-73ab08b.apk`.

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
- M16-A replaced the Learn home engineering/status dashboard with a user-facing learning home.
- M16-B polished Roleplay and Cards with learner-facing copy, improved practice flow, conversation guidance, card reveal, card rating, and review summary.
- M16-C polished YKI and Professional Finnish with learner-facing copy, improved practice/session guidance, cleaner module cards, answer/response guidance, and completion summaries.
- M16-D polished Progress and Account with learner-facing progress, access, device, empty, and sign-out copy.
- M16-E prepared a fresh Learn release-candidate QA APK and exposed remaining wording-audit matches.
- M16-F started cleaning user-facing fallback/audit wording in Roleplay, Cards, Account, YKI, and Professional Finnish.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Verify M16-F wording cleanup, rerun builds, and prepare a new Learn QA APK if the audit is clean enough for real-device/backend QA.

## Next step

Run M16-F wording cleanup verification locally.

## Pending verification

- M16-F should rerun wording audit and builds after cleanup. Some non-user-facing internal model fields may still contain release-gate or preview enum/property names, but user-facing strings should be clear learner-facing copy.
