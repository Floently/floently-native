# Agent D M33 YKI bank/material wiring

Date: 2026-07-08

Source:
- docs/progress/M33_DEVICE_QA_HARD_BLOCKERS.md

Agent D hard blockers:
- YKI Practice and Mock must be connected to correct YKI banks/material source.
- A1-A2, B1-B2, C1-C2 selection must work.

Patch:
- Added a YKI material wiring guard at the YKI feature entry point.
- Added explicit Practice bank source/authority/material-layer metadata.
- Mock bank wiring remains level-aware through YkiMockLevelBand and YkiMockExamBank.tasks(levelBand).
- The wiring guard validates:
  - source id exists
  - authority exists
  - material layer exists
  - A1-A2/B1-B2/C1-C2 level coverage exists
  - bank task count is non-zero

No redesign:
- No UI redesign was made.
- No shared i18n/navigation/app shell files were edited.
