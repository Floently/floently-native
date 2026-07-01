# M18-F1 Strict Web Parity Source Map

This trunk is not a redesign. It is a source-mapped native port of the original Floently web card practice UI.

## Native files installed

| Native file | Web source copied |
|---|---|
| `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt` | `apps/client/app/cards/index.tsx` -> `apps/client/features/cards/screens/CardPracticeScreen.tsx` -> `apps/client/features/cards/components/CardPracticeSession.tsx`, `CardModeTabs.tsx`, `CardBanksPanel.tsx` |
| `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/audio/NativeTextToSpeech.kt` | `CardPracticeSession.tsx` speaker action (`cardsListen`) mapped to Android native `TextToSpeech` |

## Copied visual details

- Back pill row from `CardPracticeScreen.tsx`
- Card mode tabs from `CardModeTabs.tsx`: `Sanasto`, `Lauseet`, `Kielioppi`
- Recall row from `CardPracticeSession.tsx`: `↺ Kertaus`, header, `Kertaus ↻`
- Progress line from `CardPracticeSession.tsx`
- Background glow circles and wave outlines from `CardPracticeSession.tsx`
- Card shell, outer card, inner card, top speaker, top skip button, center content, hint bubble, footer actions, dots row, bottom bank/restart controls from `CardPracticeSession.tsx`
- Bank panel sections from `CardBanksPanel.tsx`: `Vaikeat`, `Opitut`, `Opittavat`

## Known native mapping limits

The original web implementation uses `useCardPractice()` with follow-up typed answers and multiple-choice runtime cards. The native repository currently exposes `CardsPracticeSession`, `StudyCard`, skip, review ratings, and flag actions. This trunk keeps the native repository contract but ports the visible card practice shell and behavior as closely as possible without breaking the existing native backend/API flow.

A stricter follow-up answer port requires expanding native `StudyCard` models to match web `RuntimeCard.served_follow_up`.

## M18-F2 Original app behavior correction

- Screen: Cards practice
- Correction: Hint must open as a popup/modal, not render as a fixed inline card element.
- Native file: `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`
- Source priority: old Floently app behavior / uploaded IPA reference.

## M18-G1 latest old-source Home hint popup correction

- Source: `/home/vitus/backups/floently-finnish_2026-06-25_20-13-04.tar.gz`
- Old source files:
  - `packages/ui/screens/HomeScreen.tsx`
  - `packages/ui/components/SmartHintPopup.tsx`
  - `packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- Correction:
  - Removed fixed inline home helper card.
  - Added old-app bottom overlay hint popup with scrim, bottom-aligned card, badge, title/body, secondary and primary pill buttons.

## M18-G2 old-source palette and popup text sync

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/i18n/index.ts`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/HomeScreen.tsx`
- Native files:
  - `apps/android/shared/src/main/java/com/floently/shared/design/FloentlyDesign.kt`
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- Correction:
  - Learn palette now uses the old app dark tokens.
  - Home helper popup body now uses the old Finnish translation.
  - Hint badge is rendered uppercase to match old `textTransform: 'uppercase'`.

## M18-H1 old-source Home/Menu trunk

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/HomeScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/components/UtilityDrawer.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/components/SmartHintPopup.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- Correction:
  - Menu now opens an overlay utility drawer instead of navigating straight to Settings.
  - Home gets old-app-style ember glow background and readiness pillars.
  - Fixed account card is replaced with guide card that reopens hint popup or drawer.

## M18-H2 old-source Home component polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/HomeScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native files:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/WebParityComponents.kt`
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`
- Correction:
  - Home reusable cards now use old-app palette tokens instead of earlier hardcoded web-parity colors.
  - Home/Menu header no longer marks Menu as selected by default.
  - Remaining web-style wording was removed from the Learn home copy.

## M18-I1 old-source Cards banks modal

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/cards/components/CardBanksPanel.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/cards/components/CardModeTabs.tsx`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`
- Correction:
  - Review banks now opens as a modal overlay with scrim, matching the old app source pattern.
  - Bank sections now show up to 20 cards instead of only 5.
  - Mode pills now use old-app palette token behavior instead of hardcoded white transparency.

## M18-I2 old-source Cards hint/rating polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/components/SmartHintPopup.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/cards/components/CardPracticeSession.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`
- Correction:
  - Cards hint popup now uses old-app overlay/bottom-sheet pattern instead of a plain centered platform dialog.
  - Progress colors now use palette tokens.
  - Review labels are localized: Uudestaan, Vaikea, Hyvä, Helppo.

## M18-J1 old-source Roleplay shell polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/SpeakingLabScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/speaking/screens/RoleplayConversationScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/speaking/components/RoleplayTranscriptList.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/speaking/components/RoleplayMicButton.tsx`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayScreen.kt`
- Correction:
  - Removed the earlier helper-card approximation from Roleplay.
  - Added old-app-style speaking header, level chips, scenario cards, status cards, session metrics, transcript bubbles, and Finnish action labels.

## M18-K1 old-source YKI route polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/YkiPracticeScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/yki-practice/screens/YkiPracticeHomeScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiFeatureScreen.kt`
- Correction:
  - Removed earlier helper-card approximation.
  - Added old-app-style YKI header, level chips, module cards, metrics, Finnish status text, and localized skill labels.

## M18-K2 old-source YKI session polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/yki-practice/screens/YkiPracticeSessionScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/features/yki-exam`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiSessionScreen.kt`
- Correction:
  - Replaced generic task/session cards with old-app-style exam progress, prompt, answer, completion, save-result, and feedback cards.
  - Localized YKI task and skill labels.

## M18-L1 old-source Professional Finnish route polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/ProfessionalFinnishScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishScreen.kt`
- Correction:
  - Removed earlier helper-card approximation from Professional Finnish route.
  - Added old-app-style workplace header, domain chips, module cards, progress metrics, status cards, and Finnish domain labels.

## M18-L2 old-source Professional Finnish session polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/ProfessionalFinnishScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishScreen.kt`
- Correction:
  - Replaced generic Professional Finnish session cards with old-app-style progress, scenario, phrase, response, and completion cards.
  - Localized scenario type labels and input/status text.

## M18-M1 old-source Progress route polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/screens/ProgressScreen.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native file:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress/LearnProgressScreen.kt`
- Correction:
  - Removed earlier helper-card approximation from Progress.
  - Added old-app-style progress header, snapshot card, summary cards, sync boundary card, timeline card, and Finnish progress labels.

## M18-N1 old-source Settings + Account polish

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/state/SettingsRoute.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/components/UtilityDrawer.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/theme/floentlyPalette.ts`
- Native files:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/settings/LearnSettingsScreen.kt`
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account/AccountScreen.kt`
- Correction:
  - Removed earlier helper-card approximations from Settings and Account.
  - Added old-app-style settings/account headers, language chips, support/privacy cards, Learn access boundary, billing boundary, product-access boundary, and device cards.
  - Finnish copy added while preserving Learn/Read/Create access separation.

## M18-O1 big source parity audit

- Source:
  - Large old source zip/tar extraction under `/home/vitus/floently-old-latest-source-truth-m18x6/files` or fallback old-source candidates.
- Native repo:
  - `docs/progress/BIG_SOURCE_PARITY_AUDIT.md`
- Correction:
  - Added a tracked audit that maps old source files to native Kotlin screens and records what is already ported, what is still missing, and what must be checked before visual/device testing.
  - No Kotlin app code changed in this milestone.

## M18-O2 old-source asset/file inventory

- Source:
  - Large old source zip/tar extraction under `/home/vitus/floently-old-latest-source-truth-m18x6/files`.
- Native repo:
  - `docs/progress/OLD_SOURCE_ASSET_INVENTORY.md`
- Correction:
  - Added docs-only inventory of old source assets, sounds, logos, screen/component source files, themes, navigation/drawer files, i18n files, and access/account/billing/auth files.
  - No Kotlin app code changed in this milestone.

## M18-O3 reusable old-app drawer

- Source:
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/packages/ui/components/UtilityDrawer.tsx`
  - `/home/vitus/floently-old-latest-source-truth-m18x6/files/apps/client/config/navigation/AppShell_sidebar_sections.ts`
- Native files:
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnUtilityDrawer.kt`
  - `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnSignedInShell.kt`
- Correction:
  - Added a reusable old-app-style utility drawer for signed-in Learn screens.
  - Added a floating Menu button on non-home screens so users can reach Home, Cards, Professional Finnish, YKI, Roleplay, Progress, Account, Settings, and sign out without returning manually.
  - Preserved Learn/Read/Create access separation and did not import old web code directly.
