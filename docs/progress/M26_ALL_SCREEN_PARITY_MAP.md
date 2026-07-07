# M26-A All Screen Parity Map

Status: ACTIVE

Goal: accelerate parity by working screen-by-screen using extracted old-source modules, not tiny edits.

## Method

For every screen:

1. Find old source file.
2. Find native Kotlin target.
3. Extract reusable old-source Compose module.
4. Delegate native screen to module where safe.
5. Preserve existing richer native runtime.
6. Build debug and release after each multi-screen batch.

## Priority order

1. YKI Mock Summary / Results / Runtime
2. Speaking / Roleplay screens
3. Cards practice screens
4. YKI Planner / Confidence / Phrase Bank
5. Professional / workplace screens
6. Settings / Account / Billing / Legal
7. Final visual sweep
