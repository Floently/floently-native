# Agent D M35 YKI and Progress parity patch

Date: 2026-07-09

Source:
- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- docs/progress/M34_DEVICE_QA_REOPENED_PARITY_BLOCKERS.md
- docs/progress/M33_DEVICE_QA_HARD_BLOCKERS.md
- docs/handover/m35/AGENT_D_M35_YKI_PROGRESS_BANKS.md

Scope:
- Agent D only.
- YKI and Progress only.
- No random redesign.
- No shared i18n/theme/navigation changes.

Patch:
- Added Agent-D-owned YKI roleplay material bank so the YKI roleplay route has level-banded YKI material to connect to.
- Added an M35 YKI parity contract that validates:
  - YKI Practice bank source/authority/material layer
  - YKI Practice reading/listening/writing/speaking coverage
  - YKI Mock A1-A2/B1-B2/C1-C2 coverage
  - YKI Mock full task count and level-specific bank source
  - YKI roleplay A1-A2/B1-B2/C1-C2 material coverage
- Routed YKI feature readiness through the M35 parity contract.
- Added a Progress fallback audit so fallback states cannot pretend completed units, streaks, or synced durable progress.

Known boundary:
- Full 21-language i18n remains Agent E ownership.
- YKI roleplay UI route/audio flow remains coordinated with Agent B, but Agent D now provides the YKI material bank and level coverage.
