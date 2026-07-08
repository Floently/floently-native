# M32 Final Android Parity Finish

Status: ACTIVE

M31 integration gate passed:

- Debug build passed
- Release build passed
- Latest integrated M31 head: `7811344`
- Remaining work is parity polish, backend/material wiring, i18n/theme, and device QA.

## Active release blockers

### Agent A — Cards

- Verify Cards uses real backend/card-bank material.
- Verify flip/question/answer behavior matches old app.
- No fake/limited local deck where backend/generated card bank exists.

### Agent B — Roleplay/Speaking

- Verify Roleplay 404 is gone on device.
- Verify backend/generated topics/materials are used.
- Verify A1-A2, B1-B2, C1-C2 coverage.
- Make roleplay flow more automatic while keeping user control.
- Integrate coach/conversation partner UX naturally.

### Agent C — Professional Finnish

- Verify Professional Finnish UI/function against screenshot lock.
- Verify A1-C2 filtering and work scenarios.
- Remove any remaining user-visible “placeholder” wording.

### Agent D — YKI

- Verify YKI Mock A1-A2, B1-B2, C1-C2 selection on device.
- Verify mock content comes from correct bank/material layer.
- Verify YKI Practice + Mock do not use placeholder/fake content.

### Agent E — Shared release blockers

- Fix dark/light theme switching globally.
- Import/represent old app 21-language translation catalog.
- Remove hardcoded strings from shipped pages.
- Verify no reachable placeholder routes.
- Run final debug/release/device QA gate.

## Android freeze rule

Android is frozen only after:

1. All blockers above are closed.
2. Debug build passes.
3. Release build passes.
4. Phone QA passes.
5. Screenshot lock comparison is accepted.
6. Branch is clean and pushed.

No iOS work starts before this.
