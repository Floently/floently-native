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
