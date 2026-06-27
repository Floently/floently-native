# Floently Native Progress

## Current milestone

M16 Learn-first release completion and polish.

## Branch

`m01/production-native-foundation`

## Latest verified build

M16-D Progress and Account polish was pulled and verified with debug and release builds.

```bash
git pull --ff-only
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
sed -n '1,280p' docs/release/LEARN_PREMIUM_POLISH_AUDIT.md
sed -n '1,170p' docs/progress/NATIVE_PROGRESS.md
```

Latest verified commit locally: `d7ab6ec`.

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
- M16-A replaced the Learn home engineering/status dashboard with a user-facing learning home.
- M16-B polished Roleplay and Cards with learner-facing copy, improved practice flow, conversation guidance, card reveal, card rating, and review summary.
- M16-C polished YKI and Professional Finnish with learner-facing copy, improved practice/session guidance, cleaner module cards, answer/response guidance, and completion summaries.
- M16-D polished Progress and Account with learner-facing progress, access, device, empty, and sign-out copy.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Run the Learn release-candidate wording audit, build debug/release/bundle, prepare a fresh QA APK under `/home/vitus/`, then start real-device/backend QA.

## Next step

Run M16-E Learn release-candidate audit and QA artifact preparation locally.

## Pending verification

- M16-E should check for remaining user-facing engineering wording, run debug/release/bundle builds, copy the fresh QA APK and QA result template to `/home/vitus/`, and print the exact artifact paths for real-device testing.
