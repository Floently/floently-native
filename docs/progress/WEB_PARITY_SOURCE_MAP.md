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
