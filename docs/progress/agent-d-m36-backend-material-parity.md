# Agent D M36 backend/material parity

Date: 2026-07-09

Source:
- docs/progress/M36_FASTEST_PATH_CROSS_CUTTING_PARITY.md
- docs/handover/m36/AGENT_D_BACKEND_MATERIAL_PARITY.md

Scope:
- Agent D M36 material/backend parity only.
- No UI redesign.
- No theme/i18n/navigation ownership changes.

Patch:
- Cards backend sessions now prefer direct backend cards instead of merging backend cards with local fallback cards.
- Cards local fallback bank/overlay/review metadata is explicitly marked as offline fallback, not backend-verified production material.
- Roleplay fallback material is explicitly marked as offline generated-material fallback, not backend material.
- Account/billing preview copy is explicitly marked as API/boundary status, not verified production billing/account API completion.
- Progress service fallback messages now state backend boundary status and durable backend verification requirements.
- Existing YKI M35 material parity guards remain in place for Practice, Mock, and YKI roleplay materials.

M36 result:
- No fallback/demo/local content should be presented as real production backend content from the patched surfaces.
- Remaining device/screenshot freeze gates still belong to the final M36 QA process.
