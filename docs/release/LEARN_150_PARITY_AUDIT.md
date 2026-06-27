# Learn 150% parity audit

## Decision

Floently Learn must not merely match the old app. The native Learn release target is **150% of the old Learn experience**.

That means:

1. Every important old Learn feature must be accounted for.
2. Anything not present in native must be either rebuilt before release or explicitly postponed with a reason.
3. The final native Learn should feel more polished, more stable, and more premium than the pre-native build.

## Audit sources

### Old Learn repo

Old/pre-native Learn source inspected from `galapoto/floently-finnish`:

- `apps/client/features/i18n/languages.ts`
- `apps/client/scripts/check-i18n-completeness.mjs`
- `apps/client/state/AppShell.tsx`
- `apps/client/state/HomeRoute.tsx`
- `apps/client/state/LearningRoute.tsx`
- `apps/client/state/SpeakingRoute.tsx`
- `apps/client/state/YkiPracticeRoute.tsx`
- `apps/client/state/ProfessionalRoute.tsx`
- `apps/client/state/ProgressRoute.tsx`
- `apps/client/state/SettingsRoute.tsx`
- `apps/client/state/BillingRoute.tsx`
- `apps/client/features/cards/screens/CardPracticeScreen.tsx`
- `apps/client/features/cards/components/CardBanksPanel.tsx`
- `apps/client/features/speaking/components/RoleplayScenarioHeader.tsx`
- `apps/client/features/speaking/components/SessionCompletion.tsx`
- `apps/client/features/yki-exam/screens/YkiExamScreen.tsx`
- `packages/ui/components/UtilityDrawer.tsx`
- `packages/ui/components/SmartHintPopup.tsx`
- `packages/ui/screens/HomeScreen.tsx`
- Backend card i18n/overlay runtime and overlay bank files.

### Native Learn repo

Current native Learn source inspected from `galapoto/floently-native`:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/*`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/*`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/*`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/*`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress/*`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account/*`
- `apps/android/shared/src/main/java/com/floently/shared/design/*`

## High-level audit result

Native Learn now has the main product skeleton and polished first-release screens:

- Learn home
- YKI
- Professional Finnish
- Roleplay
- Cards
- Progress
- Account
- Product access boundary
- Build/release/bundle commands
- Release-candidate QA APK preparation

But native Learn is **not yet 150% of the old app**.

The biggest missing or partial areas are:

1. Full old i18n/translations system.
2. Language selector/settings and RTL handling.
3. Card i18n overlays and overlay language bank behavior.
4. Utility drawer / smart hint popup equivalents.
5. Old route-level details in Settings, Billing, Onboarding, and smaller UI states.
6. Old cards behavior beyond the simplified native review flow.
7. Old speaking/roleplay completion states and exact scenario metadata UI.
8. Backend-driven progress/payment/account verification.
9. Full real-device QA and screenshot polish.

## Detailed parity table

| Area | Old app evidence | Native status | Release decision | Augment-build requirement |
|---|---|---:|---|---|
| Learn home | Old home route and shared UI screen existed | Present and polished | Keep | Improve to 150% with better hierarchy, streak/next action, quick resume |
| App shell/navigation | Old AppShell existed | Present in native suite shell | Keep | Ensure Learn opens directly for Learn-first release and no Read/Create tabs appear inside Learn |
| YKI practice | Old YKI route/screen existed | Present and polished | Keep | Verify backend scoring, task variety, progress persistence, and real-device keyboard/layout |
| Professional Finnish | Old professional route existed | Present and polished | Keep | Add richer modules, feedback, workplace categories, and backend save/score verification |
| Roleplay | Old speaking route, scenario header, completion existed | Present and polished | Keep | Verify OpenAI dynamic responses, anti-repetition, completion summary, session history |
| Cards | Old card practice and card banks panel existed | Present but simplified | Build before release | Rebuild card banks panel behavior, overlay language support, scheduling, session summary |
| Progress | Old progress route existed | Present and polished | Keep | Verify backend data, streaks, timeline, saved state, empty/fallback states |
| Account | Old account/settings/billing split existed | Account present, billing not complete in Learn-native | Build before release or gate | Add account settings, privacy/deletion links, subscription/access view, device handling |
| Billing/subscription | Old BillingRoute and subscription screen existed | Payment boundary exists, Learn billing UX not fully parity | Build before release | Add Learn subscription screen or clear access management entry before Play release |
| Settings | Old SettingsRoute existed | No full native settings parity found | Build before release | Add language settings, app preferences, help/support/privacy, sign-out access |
| Onboarding/profession selection | Old onboarding screens existed | Not fully present in native Learn | Decide | For first release, either rebuild minimal onboarding or skip only if not required after login |
| i18n pages | Old i18n system existed with completeness checker | Missing in native | Build before release | Add native i18n foundation, language metadata, translated strings, and QA script |
| Enabled languages | Old enabled languages: fi, sv, en | Missing native language selection | Build before release | At minimum support FI/SV/EN UI selection if old release had it |
| Review languages | Old metadata included ru, et, uk, ar, so, fa, zh, sq, ku, vi, bn, tr, tl, th, ne, es, ur as in-progress/review | Missing native metadata | Build or document | Add metadata and hidden/review status so future expansion is preserved |
| RTL languages | Old metadata marked ar, fa, ur as RTL | Missing native RTL handling | Build or gate | Add direction metadata and avoid enabling RTL until layout is tested |
| Card i18n overlay runtime | Old backend runtime existed | Backend may exist separately; native UI not wired | Build before release if card overlays are part of Learn promise | Add overlay selection to native cards, fetch overlay options, show translated card side/helper text |
| Overlay bank/report files | Old overlay bank files existed | Not surfaced in native | Build | Add card overlay support plus backend contract verification |
| Smart hints | Old SmartHintPopup existed | Not present as native equivalent | Build for 150% | Add native smart hint/helper component for YKI, Professional, Roleplay, Cards |
| Utility drawer | Old UtilityDrawer existed | Not present as native equivalent | Build or replace | Add native help/practice tools drawer or better contextual helper sheet |
| Session completion | Old speaking completion existed | Partial native completion/summary exists | Improve | Add consistent completion summaries across Roleplay/YKI/Professional/Cards |
| Old visual polish | Old app had established brand/UI | Native screens improved but still need device visual QA | Continue | Polish after real-device screenshots: spacing, typography, cards, motion, empty/error states |
| Store/signing | Old app had release flow; native signing not configured | Missing local signing | Must do before Play | Configure keystore outside Git and build signed AAB |

## Critical build-before-release items

These should be done before claiming Learn is 150% release-ready:

### A. Native i18n foundation

Build a real native localization layer:

- `LearnLanguage` enum/model with old language codes.
- Language metadata: label, native label, flag, direction, enabled/review status.
- Translation key system for Learn UI strings.
- Initial native translated strings for at least FI/SV/EN.
- Hidden/review metadata for the previous review languages.
- Language selector in Settings.
- Persist selected language locally.
- Apply chosen language to Learn home, YKI, Professional Finnish, Roleplay, Cards, Progress, Account, Settings.
- RTL metadata preserved for ar/fa/ur, but keep disabled until tested.

### B. Native Settings screen

Build a proper Learn settings screen:

- Language selection.
- Practice preferences.
- Account access links.
- Support/contact.
- Privacy policy / terms / account deletion path.
- Sign out.

### C. Cards 150% upgrade

The old card system had more than a simple review screen. Native needs:

- Card banks panel equivalent.
- Card deck categories.
- Overlay language selector for cards.
- Overlay option/cache service contract.
- Translated helper/meaning overlay on cards.
- Better session summary.
- Scheduling/progress persistence verification.

### D. Smart hint/helper system

Build a native smart hint component:

- Reusable across YKI, Professional Finnish, Roleplay, Cards.
- Shows simple explanation, example, and learner-level guidance.
- Does not clutter the UI.
- Works with selected language when translation exists.

### E. Utility drawer or native equivalent

Rebuild or replace old utility drawer with a premium native helper sheet:

- Quick help.
- Language tools.
- Recent practice.
- Support/settings shortcut.
- Contextual tips.

### F. Session completion upgrades

Every practice area should have a strong completion state:

- YKI: summary, next recommended task, score/feedback if available.
- Professional Finnish: response summary, useful phrases, next scenario.
- Roleplay: conversation recap, repeated-cue/variety note, next scenario.
- Cards: ratings summary, next review suggestion.

### G. Backend verification

Native must be verified against real backend:

- Login/session persistence.
- Product access.
- YKI modules/tasks/scoring/progress.
- Professional Finnish modules/responses/progress.
- Roleplay scenario/session/message/OpenAI dynamic responses.
- Anti-repetition state.
- Cards decks/session/review/scheduling/progress.
- Progress dashboard.
- Account/device/access.
- Billing/access refresh if shown before release.

## Nice-to-have after release, but not blocker if hidden/gated

- Full native UI translations for all review languages.
- RTL UI enablement for ar/fa/ur.
- Audio roleplay and speech review.
- Phone-call practice.
- Advanced card media review.
- Deep personalization and onboarding if not needed for logged-in returning users.

## Release rule from this audit

The app can proceed to release only when:

1. Every old Learn feature is either present, better, or intentionally hidden/postponed.
2. No user-facing old capability silently disappears.
3. If a feature is postponed, the UI must not promise it.
4. The native app must look and feel more polished than the old app on a real device.
5. FI/SV/EN language support and card overlays must be explicitly decided before Play submission.

## Recommended build sequence

### M17-A Native i18n foundation

Add language models, translation keys, FI/SV/EN strings, language selector, persistence, and wire main Learn screens.

### M17-B Cards overlays and card banks parity

Add card banks panel equivalent, overlay language selector, backend overlay contract, and improved card session summary.

### M17-C Settings, support, privacy, billing/access surface

Add native settings and release-required policy/support/account-deletion access. Add Learn subscription/access page if needed.

### M17-D Smart hints and utility helper sheet

Add native contextual help and old utility drawer replacement.

### M17-E Session completion and progress upgrade

Unify completion screens and next-action recommendations across Learn.

### M17-F Backend/device parity QA

Run full real-device QA and backend verification with the latest APK.

### M17-G Signed release candidate

Configure signing outside Git, build signed AAB, verify store artifacts.

## Current conclusion

Native Learn is now a strong release-candidate foundation, but it is not yet 150% of the old app. The main blockers to 150% are i18n/language support, card overlays, settings/support/billing parity, smart hints/utility drawer, and full backend/device QA.
