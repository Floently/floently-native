# Agent A M33 Cards hard blocker closure

M33 active hard blockers for Agent A:
- Cards do not display/connect to backend/card bank.
- Cards must use real designed/generated card bank.
- Flip/question/answer behavior must match old app.

Patch:
- Kept work inside Agent A ownership.
- Tightened `ServiceCardsRepository.dashboard()` so the shipping Cards service path only treats backend/card-bank material as valid when real decks, banks, or bucketed cards are returned.
- Tightened `ServiceCardsRepository.startSession()` so an empty backend session is surfaced as an error instead of showing an empty/pretend practice session.
- Preserved the no-preview shipping service path from M32.
- Made the old-source recall buttons functional in native flip mode:
  - left recall returns to the question/front side;
  - right recall reveals the answer/back side.
- Changed the central flip action label from icon/check ambiguity to explicit question/answer flip labels without redesigning the screen.

Verification:
- Android debug build required before commit.
- Android debug build required again before push.
- Device QA still must confirm Hetzner backend connectivity and visible real card-bank material on phone.
