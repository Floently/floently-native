# Agent A M31 Cards backend/card-bank fix

Active M31 blocker:
- Cards must use the real designed card bank/backend/generated material source.
- Limited fake/local preview cards must not silently ship when backend/generated material exists.

Agent A patch:
- Changed `ServiceCardsRepository` so runtime Cards no longer falls back to `PreviewCardsRepository` after backend/dashboard/session/review/skip failure.
- Dashboard backend failure now returns an empty real-bank state with an explicit backend error instead of preview/local decks.
- Session start/review/skip backend failures now surface explicit errors instead of substituting local preview behavior.
- Flag submission failure returns false instead of pretending the backend accepted the flag.
- Existing `PreviewCardsRepository` remains available only as a standalone preview/offline repository, not as the shipping service fallback path.

Owned files changed:
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsRepository.kt
- docs/progress/agent-a-m31-cards-backend.md
