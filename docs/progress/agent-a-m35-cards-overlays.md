# Agent A M35 Cards overlays parity

M35 source docs:
- `docs/handover/m35/AGENT_A_M35_CARDS_OVERLAYS.md`
- `docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md`
- `docs/progress/M34_DEVICE_QA_REOPENED_PARITY_BLOCKERS.md`
- `docs/progress/M33_DEVICE_QA_HARD_BLOCKERS.md`

Agent A owned scope:
- Cards
- card banks
- card overlays
- card report/problem overlay
- language-specific overlays
- card flip/recall/check/skip/review flow
- card backend/material source parity

Patch:
- Added visible card overlay actions for translation, explanation, grammar, example sentence, and difficult-card review.
- Added a report/problem overlay with the required M35 report reasons.
- Wired the report/problem overlay to the existing `repository.flagCard(...)` backend path.
- Kept card bank/backend source enforcement from M33.
- Kept old flip/recall/show-answer/show-question behavior from M33.
- Overlay language uses the selected app language as the first visible overlay language and shows the selected language label inside overlay/report surfaces.

Shared requests:
- Full localized strings for these overlay/report labels belong to Agent E i18n work.
- Agent A did not edit shared i18n/theme/navigation.

Verification:
- Android debug build required before commit.
- Android debug build required before push.
- Device QA must confirm overlays open/close, report flow sends, selected language affects visible overlay content, and backend material remains primary.
