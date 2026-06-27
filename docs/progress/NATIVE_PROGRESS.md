# Floently Native Progress

## Current milestone

M12 Store submission and manual QA pack.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android signing status, debug build, release build, and release bundle build succeeded after verifying store/signing readiness.

```bash
./scripts/nativectl signing-status
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
./scripts/nativectl build android-learn-bundle
```

Latest verified commit locally: `b5ba294`.

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
- Backend integration contracts are verified with native API route registry and suite selector backend contract dashboard.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Final readiness gap dashboard is verified with release gate counts and blocker order.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Prepare store submission text, screenshot requirements, data safety prompts, and real-device manual QA checklist before final release testing.

## Next step

Verify M12-A store submission and manual QA pack with a clean local pull and documentation check.

## Pending verification

- M12-A store submission and manual QA pack was pushed with Play Store listing draft, screenshot checklist, data safety preparation, release notes draft, and native manual QA checklist.
