# Agent A M32 final Cards parity closure

M32 listed blockers for Agent A:
- Verify Cards uses real backend/card-bank material.
- Verify flip/question/answer behavior matches old app.
- No fake/limited local deck where backend/generated card bank exists.

Patch:
- Removed the runtime service repository constructor fallback to `PreviewCardsRepository`.
- Removed the runtime service summary dependency on `PreviewCardsRepository`.
- Kept `PreviewCardsRepository` as a standalone preview/offline repository only; it is no longer part of the shipping service repository path.
- Preserved existing Cards flip/question/answer UI and runtime behavior from the old-source parity path.
- Added explicit backend empty-material messaging when the real backend returns no decks for the selected mode.

Verification:
- Android debug build required before commit.
- Before-push Android debug build required.
- Device QA still needs to confirm real backend card banks are visible on the phone.
