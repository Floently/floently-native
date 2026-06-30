# Big Source Parity Audit

Updated: 2026-07-01T00:10:36

Old source truth directory: `/home/vitus/floently-old-latest-source-truth-m18x6/files`

Purpose: use the large old source zip/tar truth as an audit source before making more native changes. This file does not claim exact visual parity; it records old-source-mapped parity and remaining gaps.

## Summary

| Area | Old source present | Native present | Status |
|---|---:|---:|---|
| Home / launch shell | 6/6 | 3/3 | old-source-mapped, needs visual/device audit |
| Cards / vocabulary practice | 5/5 | 1/2 | old-source-mapped, needs visual/device audit |
| Roleplay / Speaking Lab | 4/4 | 1/1 | old-source-mapped, needs visual/device audit |
| YKI route and session | 2/4 | 3/3 | old-source-mapped, needs visual/device audit |
| Professional Finnish | 1/1 | 2/2 | old-source-mapped, needs visual/device audit |
| Progress | 2/2 | 3/3 | old-source-mapped, needs visual/device audit |
| Settings / Account | 3/3 | 4/4 | old-source-mapped, needs visual/device audit |
| Assets / sounds / logos | 3/5 | 1/1 | old-source-mapped, needs visual/device audit |

## Detailed checklist

### Home / launch shell

Old source references:
- [FOUND] `packages/ui/screens/HomeScreen.tsx`
- [FOUND] `packages/ui/components/AppScaffold.tsx`
- [FOUND] `packages/ui/components/PageHeader.tsx`
- [FOUND] `packages/ui/components/UtilityDrawer.tsx`
- [FOUND] `packages/ui/components/SmartHintPopup.tsx`
- [FOUND] `packages/ui/theme/floentlyPalette.ts`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/WebParityComponents.kt`
- [FOUND] `apps/android/shared/src/main/java/com/floently/shared/design/FloentlyDesign.kt`

Already ported or mapped:
- Old palette tokens
- Home hint popup
- Home menu/drawer shell
- Header and quick path cards
- Readiness pillars

Remaining before confident testing/release:
- Full screenshot verification against old app
- Exact old animation timings for ember/smoke background
- Reusable drawer across every Learn screen
- Old assets/sounds audit

### Cards / vocabulary practice

Old source references:
- [FOUND] `apps/client/features/cards/components/CardPracticeSession.tsx`
- [FOUND] `apps/client/features/cards/components/CardBanksPanel.tsx`
- [FOUND] `apps/client/features/cards/components/CardModeTabs.tsx`
- [FOUND] `apps/client/features/cards/hooks/useCardPractice.ts`
- [FOUND] `apps/client/features/cards/screens/CardPracticeScreen.tsx`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`
- [CHECK] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/tts/NativeTextToSpeech.kt`

Already ported or mapped:
- Native TTS preserved
- Banks modal overlay
- Hint popup bottom-sheet pattern
- Localized review labels
- Old palette progress colors

Remaining before confident testing/release:
- Old inline hint-vs-popup behavior decision needs visual test
- Exact old card spacing/animation audit
- Real spaced repetition scheduling parity
- Deck data parity against production backend

### Roleplay / Speaking Lab

Old source references:
- [FOUND] `packages/ui/screens/SpeakingLabScreen.tsx`
- [FOUND] `apps/client/features/speaking/screens/RoleplayConversationScreen.tsx`
- [FOUND] `apps/client/features/speaking/components/RoleplayTranscriptList.tsx`
- [FOUND] `apps/client/features/speaking/components/RoleplayMicButton.tsx`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayScreen.kt`

Already ported or mapped:
- Speaking header
- Level chips
- Scenario cards
- Transcript bubbles
- Finnish labels
- OpenAI-backed repository path preserved

Remaining before confident testing/release:
- Microphone/recording interaction parity
- Exact old conversation controls
- End-to-end API generation test
- Anti-repetition real-session verification

### YKI route and session

Old source references:
- [FOUND] `packages/ui/screens/YkiPracticeScreen.tsx`
- [CHECK] `apps/client/features/yki-practice/screens/YkiPracticeHomeScreen.tsx`
- [CHECK] `apps/client/features/yki-practice/screens/YkiPracticeSessionScreen.tsx`
- [FOUND] `apps/client/features/yki-exam`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiFeatureScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiSessionScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiRepository.kt`

Already ported or mapped:
- YKI exam header
- Level chips
- Module cards
- Session progress card
- Prompt/answer/feedback cards
- Finnish task/skill labels

Remaining before confident testing/release:
- Real scoring/service persistence
- Official exam content parity audit
- Listening/speaking task media flow
- Visual comparison on device

### Professional Finnish

Old source references:
- [FOUND] `packages/ui/screens/ProfessionalFinnishScreen.tsx`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishRepository.kt`

Already ported or mapped:
- Workplace header
- Domain chips
- Module cards
- Session progress
- Scenario prompt cards
- Model phrase cards
- Finnish labels

Remaining before confident testing/release:
- Backend module/content parity
- Audio/phone-call practice gate
- Real feedback persistence
- Device visual test

### Progress

Old source references:
- [FOUND] `packages/ui/screens/ProgressScreen.tsx`
- [FOUND] `packages/ui/theme/floentlyPalette.ts`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress/LearnProgressScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress/LearnProgressRepository.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress/LearnProgressModels.kt`

Already ported or mapped:
- Progress header
- Snapshot card
- Route progress cards
- Sync boundary card
- Timeline card
- Finnish labels

Remaining before confident testing/release:
- Durable server progress sync
- Offline queue and merge behavior
- Actual activity history from backend
- Device visual test

### Settings / Account

Old source references:
- [FOUND] `apps/client/state/SettingsRoute.tsx`
- [FOUND] `packages/ui/components/UtilityDrawer.tsx`
- [FOUND] `packages/ui/theme/floentlyPalette.ts`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/settings/LearnSettingsScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account/AccountScreen.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account/AccountRepository.kt`
- [FOUND] `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account/AccountModels.kt`

Already ported or mapped:
- Settings header
- Language chips
- Support/privacy cards
- Account header
- Plan card
- Billing boundary card
- Device cards
- Learn/Read/Create access separation copy

Remaining before confident testing/release:
- Clickable external links
- Real billing provider wiring
- Real device revoke/manage actions
- Account deletion request action

### Assets / sounds / logos

Old source references:
- [FOUND] `components/public/logo.png`
- [FOUND] `assets/components/public/logo.png`
- [FOUND] `assets/components/public/sounds/ui`
- [CHECK] `features/read/mobile/assets/floently_read.png`
- [CHECK] `features/create/mobile/assets/floently_create_word_logo_no_shadow.png`

Native references:
- [FOUND] `apps/android/FloentlyLearn/app/src/main/res`

Already ported or mapped:
- Core Learn theme colors
- App identity preserved

Remaining before confident testing/release:
- Full asset inventory from old IPA/source bundle
- Native resource import decision
- UI sound policy for Android
- Logo density verification

## Next safe work order

1. M18-O2: source/asset inventory from old zip/tar and IPA, docs only.
2. M18-O3: reusable old-app drawer across signed-in screens, Kotlin patch.
3. M18-O4: clickable settings/account external links and account deletion boundary.
4. M18-O5: visual smoke test checklist and debug APK install script.
5. M19: device testing and screenshot comparison.

## Notes

- Do not copy TSX/React Native files directly into Kotlin Compose.
- Use old source for layout, copy, behavior, theme, and asset truth.
- Keep Learn/Read/Create access and payment separation intact.
- Keep Android native implementation real native, not WebView.

