# Floently Native Progress

## Current milestone

M20 section-lock rebuild - M20-I YKI Practice bank/button/timer lock.

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

- M18-J1 ported the native Roleplay route toward the old source shell: speaking header, level chips, old-app scenario cards, transcript bubbles, and Finnish labels.

- M18-K1 ported the native YKI route toward the old source shell: exam header, level chips, old-app module cards, Finnish metadata labels, and status cards.

- M18-K2 ported the YKI session/task screen toward old source exam flow: progress card, task prompt card, Finnish labels, completion, save-result, and feedback cards.

- M18-L1 ported Professional Finnish route toward the old source shell: workplace header, domain chips, old-app module cards, progress metrics, status cards, and Finnish domain labels.

- M18-L2 ported the Professional Finnish session toward old source task flow: progress card, scenario prompt card, model phrase cards, Finnish response labels, and completion card.

- M18-M1 ported Progress toward old source shell: removed helper-card approximation, added snapshot, route progress, sync boundary, timeline cards, and Finnish progress labels.

- M18-N1 ported Settings and Account toward old source shell: removed helper-card approximations, added language chips, support/privacy cards, Learn access boundary, billing boundary, plan, device, and product-access cards with Finnish labels.

- M18-O1 added a big source parity audit using the old zip/tar source truth, without changing Kotlin app code.

- M18-O2 added a docs-only old source asset/file inventory for logos, sounds, screens, themes, navigation, i18n, and access/account/billing files.

- M18-O3 added a shared old-app-style Learn utility drawer and wired a floating Menu button into non-home signed-in screens, using the big source drawer audit without changing product access boundaries.

- M18-O4 made Settings support/privacy/terms/account-deletion links externally clickable and added an Account deletion boundary card while preserving Learn/Read/Create separation.

- M18-O5 added a native Android smoke-test checklist and debug APK install helper script for device testing; no Kotlin app code changed.

- M18-O6R removed the unused old SmartHelper Kotlin file after confirming no external references, then re-ran leftover guards and native builds before device testing.

- M19-A recorded the uploaded original-app screenshot source map, scroll-continuation rule, and YKI naming distinction: YKI test means YKI Practice, while Full YKI Exam means YKI Mock Exam.

- M19-B rebuilt the Learn drawer from screenshots IMG_0393 and IMG_0489-IMG_0493: large right panel, blue profile header, daily streak card, grouped rows, language card, theme/session panels, and shared Home/non-Home drawer usage.

- M19-B3 added the screenshot-mapped drawer language picker popover from IMG_0490 and bottom padding for Android gesture navigation.

- M19-B5 made the drawer language picker scrollable/selectable, wired it to persisted Learn language state, and made Theme/Session panels visible above Android gesture navigation.

- M19-C rebuilt high-impact Cards practice visuals from IMG_0397-IMG_0399: two-tab Vocabulary/Sentences layout, English screenshot labels, larger card, bigger word, and screenshot-style controls.

- M19-D/E separated YKI Practice from YKI Mock Exam in navigation and rebuilt high-impact YKI visuals/naming from IMG_0409-IMG_0479 while preserving existing YKI session behavior.

- M19-DER repaired the YKI Mock Exam feature contract so the new destination is included in release/access/parity guard mapping.

- M19-F/G/H updated Home, Professional Finnish, Settings, and Account visible structure/copy from screenshots IMG_0390-IMG_0394 and IMG_0480-IMG_0494 without an intermediate phone check.

- M19-I/J updated Roleplay and Progress copy/visual rhythm and recorded the final M19 sweep report before the single full-device check.

- M20-A started the section-lock rebuild after user rejection of broad M19 parity: Everyday Finnish is now a hub, drawer/Home no longer route Everyday directly to Cards, and Professional Finnish now represents Roleplay, Cards, Interview, Speech recording, and Report writing.

- M20-B recorded the non-negotiable pixel parity contract: every screenshot, alignment, spacing, text, color, button position, function state, and logical flow must be represented; no section can move forward until accepted.

- M20-C mapped every YKI Practice and YKI Mock Exam screenshot from IMG_0409-IMG_0479 into required native states before rebuilding YKI.

- M20-D replaced generic YKI module UI with deterministic screenshot-sequence YKI Practice and YKI Mock Exam screens for IMG_0409-IMG_0479.

- M20-DX records user rejection of the placeholder YKI implementation: M20-D used fake passages/questions and non-functional options; next rebuild must lock YKI Practice only from exact screenshot/source content before touching YKI Mock Exam.

- M20-E rebuilt YKI Practice only with screenshot-derived passages/questions/options and selectable answer behavior; YKI Mock Exam remains paused until Practice is accepted.

- M20-G corrected YKI Practice functions: listening audio uses Finnish TTS from the practice bank script, speaking has native recorder controls, writing fields are empty/editable with autocorrect disabled, and Practice task data is routed through a bank object.

- M20-I extracted YKI Practice content into a dedicated bank file and corrected Practice button/timer states: Writing Save answer now changes into Next task, and Speaking now has preparation countdown, speaking countdown/recording, save state, and next transition.

- M20-I records every YKI screenshot as a required state, extracts Practice content into a bank file, adds a Practice screenshot state map, and fixes writing/speaking button state transitions.

- M20-K corrected YKI Practice speaking flow so preparation automatically starts recording, adds sent state, and adds final evaluation export as PDF and Word-compatible document.

- M20-M added a bank-driven YKI Mock Exam screen with overview, reading, writing, timed preparation, timed recording, submitted/results states, and PDF/Word evaluation exports.

- M20-MR2 repairs the Mock Exam build, adds a strict Mock screenshot state contract, and guards all IMG_0432-IMG_0479 Mock states before testing.

- M20-O rebuilt YKI Mock Exam behavior to mimic a real exam: no correct/wrong feedback during the exam, listening section with bank audio tasks, full reading/writing/listening/speaking structure, and final-only evaluation.

- M20-R locked the full YKI Mock parity rebuild contract from user screenshots: landing levels, exam summary, 17-task structure, section-specific visual states, speaking timing model, and deeper final evaluation.

- M20-S rebuilt YKI Mock to the full 17-task old-app-style flow: dark landing with all level bands, exam summary, 5 reading, 4 listening, 4 writing, 4 speaking tasks, speaking 10/30/60 timing with 30s minimum, final-only deep evaluation, PDF and Word export.

- M20-SR repaired the M20-S full parity rebuild compile issue and corrected the 17-task guard to count bank tasks, not the data class declaration.

- M20-SR2 repaired Mock compile blockers by namespacing the Mock speaking enum and restoring MockOption textValue usage without shadowing the shared text color.

- M20-SR3 repaired the remaining Mock compile blocker by renaming the Mock global text color to mockText and making textValue Text calls explicit.

- M21-R started Home visual parity audit after source-mapped Home component migration and cleanup passed debug/release verification.
\n- M23-A aligned Home full-screen order and guide/readiness labels to old source: hero, pathways, readiness, stats, guide.\n
- M24-E started YKI Practice landing old-source parity audit after M24-D passed debug/release at commit 0c4592b.

- M24-F tuned YKI Practice old-source landing spacing, card heights, info cards, and CTA label toward recovered React Native source.

- M25-A wired YKI Mock landing to dedicated old-source Compose module while preserving existing mock runtime.

- M26-A started all-screen parity acceleration map after Home, YKI Practice landing, and YKI Mock landing verified debug/release.

- M26-C moving into remaining parity implementation packs: Cards, Roleplay/Speaking, Professional, Progress, Settings, Auth/Billing.

- M26-D applied real Cards composable parity tuning to mode tabs, runtime card shell, footer, dots, and spacing.

- M26-E recorded the proven native speed build method: old-source recovery, feature-pack batching, extract-and-delegate, runtime preservation, and debug/release gates before Android freeze and iOS port.

- M27-A extracted Roleplay old-source visual components and delegated dashboard/session runtime to them while preserving Kotlin repository and AI flow.

- M27-B fixed Roleplay extraction integration by renaming legacy local components after extracted OldSource components became the active runtime call targets.

- M28-A started Android production-freeze gap audit before iOS SwiftUI work begins.
\n- M28-B removed reachable Everyday Finnish placeholder actions by routing Interview to Roleplay and Speech recording to YKI speaking/runtime flow for production-freeze readiness.\n
- M28-C fixed Everyday Speech recording route to use the real YkiPractice destination instead of a non-existent Yki enum.

- M28-D started placeholder reachability gate after Everyday route fix passed debug/release.

- M29-B started screenshot-based parity execution after device QA showed Android is not yet old-app parity; iOS remains blocked.

- M29-C rebuilt Everyday Finnish as old-app style two-entry Vocabulary & Roleplay screen with A1-C2 level rail; removed generic four-card hub.

- M29-D started global i18n implementation by wiring Everyday Finnish to LearnCopy and making A1-A2/B1-B2/C1-C2 selectable.

- E: added multi-agent Android parity handover and copy-paste prompts.

- M31 device QA found release blockers: Cards backend/card-bank behavior, Roleplay 404/backend/topics/A1-C2 flow, YKI Mock A1-C2 levels, global theme toggle, and full 21-language i18n catalog wiring.

- M32 started final Android parity finish after M31 debug/release integration gate passed; remaining blockers are Cards backend behavior, Roleplay backend/topics/flow, YKI level/material parity, Professional polish, theme, and full i18n.

- M33 device QA hard blockers recorded: full i18n, theme/settings, backend connectivity, Cards backend, old Roleplay mic/audio/transcription flow, and YKI bank wiring.

- M34 reopened Android parity after device QA: full language/theme still failing, roleplay UI/function not old-app parity, YKI roleplay missing, cards overlays need language/backend parity.

- M35 started full web-to-native parity round: Floently.com landing, Learn landing, auth, placement, full i18n, theme, cards overlays, roleplay, YKI, settings, billing, animations, and device parity must be included before Android freeze.
