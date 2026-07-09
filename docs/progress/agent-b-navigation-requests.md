# Agent B navigation requests

## M35 YKI Roleplay route

Agent B added a Roleplay-owned YKI entry composable:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayScreen.kt`
- `YkiRoleplayScreen(repository, copy, onBack)`
- `RoleplayRouteMode.Yki`

Request for Agent E / Agent D integration:

- Add a reachable YKI Roleplay route from the YKI area and/or Learn navigation shell.
- Route should call `YkiRoleplayScreen(...)` rather than the generic Everyday Roleplay entry.
- The route label should clearly read as YKI roleplay, not Everyday or Professional roleplay.
- Keep YKI roleplay grouped with YKI Practice / YKI Mock materials.
- If YKI-specific backend/material endpoint wiring is added by Agent D/E, pass it through the existing `RoleplayRepository` boundary or add a scoped repository variant; do not duplicate roleplay UI.

Reason:

M35 requires that YKI roleplay route exists and works, while Agent B ownership is limited to the Roleplay/Speaking package and progress docs.
