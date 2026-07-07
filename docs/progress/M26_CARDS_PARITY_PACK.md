# M26-C Cards Parity Pack

Status: ACTIVE

Old source recovered:

- `apps/client/features/cards/screens/CardPracticeScreen.tsx`
- `apps/client/features/cards/components/CardPracticeSession.tsx`
- `apps/client/features/cards/components/CardBanksPanel.tsx`
- `apps/client/features/cards/components/CardModeTabs.tsx`

Native target:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`

## Finding

Native Cards already contains the full runtime shape:
- mode tabs
- practice session
- adaptive card copy
- card shell
- hint/show-answer flow
- ratings
- review banks
- summary
- repository/service/fallback

## Current implementation strategy

Do not rewrite Cards from scratch. The native implementation is already richer and structurally aligned with the recovered old source. Continue with focused visual parity tuning and preserve runtime behavior.
