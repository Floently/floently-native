# Floently Native Progress

## Current milestone

M18 web UI parity rebuild - M18-I2 old-source Cards hint/rating polish.

## Branch

`m01/production-native-foundation`

## Latest verified build

M18-C1 Learn dashboard web parity was verified locally with debug and release builds after the C1R sp-unit repair.

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn-release
git commit -m "Polish native Learn final parity surface"
git push
```

Latest verified commit locally: `59d1174`.

Latest QA artifact: `/home/vitus/floently-learn-rc-debug-0d2958b.apk`.

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
- M16-F cleaned user-facing fallback/audit wording in Roleplay, Cards, Account, YKI, and Professional Finnish; debug, release, and bundle builds passed.
- M16-G recorded the Learn 150% parity audit and augment-build backlog in `docs/release/LEARN_150_PARITY_AUDIT.md`.
- M17-A started native i18n restoration: old language metadata is present, FI/SV/EN copy is available, Settings exists, language persistence is added, and Learn home/shell are wired to language settings.
- M17-A follow-up corrected the release-contract compile error and changed Settings to show all 20 final web Learn language options.
- M17-A2 aligned native language availability with the final web build: all 20 languages are enabled and complete, debug build passed, and release build passed.
- M17-A3 imported final web CG5 translated copy into native Learn home/settings copy, removed English-only completion labels from the language selector, and passed debug/release builds.
- M17-A4 recorded `docs/release/LEARN_WEB_SOURCE_OF_TRUTH_PARITY_MAP.md`: completed web Learn is now the source of truth for navigation, home, settings, billing/access, YKI, Professional Finnish, Roleplay, Cards, Progress, smart hints, utility drawer, i18n, overlays, backend contracts, and release QA.
- M17-A4 propagated selected-language native copy into YKI, Professional Finnish, Roleplay, Cards, Progress, and Account entry/session screens; debug and release builds passed.
- M17-B1 added card-bank panel foundation, overlay language selector, overlay model/service contract, translated helper display layer, and stronger review summary foundation from the completed web Cards source.
- M17-B1 local verification passed: Android SDK 36 and Build Tools 34 were installed/licensed locally, then debug and release builds completed successfully.
- M17-B2 added native card state buckets, backend parsing for state/seen/correct/due fields, preview scheduling transitions, and visible bucket labels to align Cards with the completed web bank flow.
- M17-B2 repair fixed nullable dashboard bucket labels; debug and release builds passed locally after the repair.
- M17-B3 guarded Cards against completed web runtime card responses: first_card/next_card, session_id, runtime prompt/answer/hint fields, synthesized overlay fallback, and runtime bucket derivation.
- M17-B3 local verification passed: debug and release builds completed after runtime response and overlay guard patch.
- M17-B4 bridged native Cards ratings to the real completed web runtime answer route, added runtime deck/start route fallback, and labelled backend materialized hints as coach hints.
- M17-B4 local verification passed: debug and release builds completed after runtime answer route and coach hint parity patch.
- M17-B5 added Cards skip/next runtime route support, issue flag submission, runtime audio segment parsing, and visible native audio availability for card practice.
- M17-B5 local verification passed: debug and release builds completed after skip, flag, and audio segment parity patch.
- M17-B6 polished Cards practice UI with web-style hint reveal/hide, clearer audio transcript fallback, and reset-safe skip/review transitions.
- M17-B6 local verification passed: debug and release builds completed after Cards hint/audio/session polish.
- M17-C1 added native Settings legal/access surface for billing and plan guidance, support, privacy, terms, and account deletion links from the completed web Settings source.
- M17-C1 local verification passed: debug and release builds completed after native Settings legal/access surface patch.
- M17-C2 repair applied the actual Account models, preview repository data, and Account UI billing/access sections after the previous tracker-only commit.
- M17-C2 repair verification passed: debug and release builds completed after real Account billing/access code changes.
- M17-C3 wired native billing provider actions for subscription checkout, trial, portal, cancel trial or renewal, and reactivate using existing backend routes without adding Learn environment variables.
- M17-C3 local verification passed: debug and release builds completed after billing provider action wiring.
- M17-D1 added a reusable native smart helper surface with Home quick actions and Settings helper guidance as the first native replacement for the completed web UtilityDrawer/hints behavior.
- M17-D1 local verification passed: debug and release builds completed after native smart helper surface patch.
- M17-D2 added route-aware native helper cards across YKI, Professional Finnish, Roleplay, Progress, Account, and Cards, matching the completed web route hints without blocking the main flow.
- M17-D2 local verification passed: debug and release builds completed after route-aware native helper polish.
- M17-E1 removed final unfinished-sounding helper wording from the native Learn progress helper and preserved the route-aware helper surface without changing Learn/Read/Create product boundaries.
- M17-E1 local verification passed: debug and release builds completed after final Learn release polish.\n- M17-F live QA started on a real Android device: debug APK installed, app launched, and vitus.idi@floently.com login worked.\n- M17-F1 repaired the native Learn access route to use the native access endpoint before legacy fallback and removed internal release/backend dashboard panels from the signed-in user surface.\n- M17-F1 replaced raw user-facing access/checkout enum labels and hid unavailable Android checkout actions for products whose checkout is not enabled.\n- M17-F2 removed the temporary suite product selector from the Learn-first signed-in path and now opens the native Learn shell directly after auth/access verification.\n- M17-F3 added a Learn-only subscription-status fallback when native and legacy access endpoints are unavailable, preserving Read/Create separation while unblocking real Learn users.\n- M17-F4 cleaned the Learn home surface by removing temporary helper wording, removing duplicate Account from pathways, grouping Settings/Account at the bottom, and replacing visible review-count placeholders.\n- M18-B1 rebuilt shared native design tokens from the web-app visual baseline: navy shell, dark cards, thin borders, blue primary pills, teal accent, and web-style spacing. Existing native functionality and helper improvements were preserved.\n- M18-C1/C1R replaced the temporary generic Learn home with a web-app-inspired dashboard and repaired Compose sp unit usage so debug/release builds pass.\n- M18-C1R audited native TTS wiring before implementation; Android TextToSpeech wiring is still pending unless the audit output shows otherwise.
- Release build preparation is verified with production application ID, Floently app label, version bump, debug suffix, release build type, release build command, and release checklist.
- Learn UI and environment guardrails are verified: Learn is improvement-only, not a rebuild; functions are preserved; no new Learn environment variables unless explicitly approved.

## Active task

Build Learn to 150% of the completed web app before release. The completed web build is the source of truth for all Learn behavior, not only language.

## Next step

Reinstall the debug APK on the real Android device and verify the shared native shell now uses the web-app navy/dark-card visual baseline before rebuilding the Learn home dashboard.

## Pending verification

- Live backend QA: login/session restore, Learn access, YKI, Professional Finnish, Roleplay, Cards, Progress, Account, Settings/legal/support, and billing provider action boundaries.
- Real-device QA: app launch, auth, keyboard layouts, scroll behavior, roleplay input/audio behavior, cards review flow, YKI text inputs, Settings/Account visibility, native-only behavior, no broken navigation loops, and no Read/Create access leakage into Learn screens.
- Release signing and Play release readiness remain pending.

- M18-D1 wired real Android TextToSpeech into native Cards and Roleplay, replacing transcript-only audio placeholders with device speech actions while preserving the improved hint/helper system.
- Next workflow change: use big trunk-file bundles for web UI parity rebuilds instead of small patch edits, while still building debug/release before every commit.

- M18-E1/M18-E1R installed Big Trunk 01: full Learn dashboard replacement, reusable web-parity components, and the missing shared design trunk with secondary pill button support. Debug and release builds must pass before commit.

- M18-F1/M18-F1R installed strict Cards web parity trunk from the original web card practice sources, keeping native Android TTS and source-map tracking. Debug and release builds passed after the sp import repair.
\n- M18-F2 corrected Cards hint behavior to match the original app pattern: hint opens as a popup/modal instead of being fixed inline inside the card.\n
- M18-G1 selected `/home/vitus/backups/floently-finnish_2026-06-25_20-13-04.tar.gz` as the latest old-app UI source truth and corrected the Learn home helper from a fixed inline card into the old app bottom popup pattern.

- M18-G2 synced Learn native palette and Home hint popup text to the June 25 old app source tokens/translations.

- M18-H1 ported more of the old app Home/Menu behavior: ember background, readiness pillars, guide card, and a real utility drawer instead of routing Menu directly to Settings.

- M18-H2 polished Home shared components to use old-app palette tokens consistently and removed remaining web-style wording from the native Learn home.

- M18-I1 changed Cards review banks from an inline page panel to the old-app modal overlay pattern, and expanded bank rows toward the old source behavior.

- M18-I2 changed Cards hint popup to old-app overlay/bottom-sheet pattern, synced progress colors to old palette tokens, and localized review rating labels.
