# M29 Screenshot Parity Execution

Status: ACTIVE

The installed Android app failed old-app parity QA. Android is no longer considered production-freeze candidate.

## Source of truth

1. `docs/ui-lock/screenshots/*.PNG`
2. `docs/ui-lock/contact_sheet_*.jpg`
3. Recovered old React Native source under `/home/vitus/m24-yki-backup-search/extracted`

## Immediate rule

No iOS work starts until Android matches the locked screenshots.

## First rebuild target

Everyday Finnish.

User QA finding:
- Old Everyday Finnish has two main sections/entry paths.
- From Home, Everyday Finnish should open into the correct Cards/Roleplay side structure.
- Current Android Everyday screen is a generic hub and is not visually or functionally matching.
- Level model should support A1–C2 where old app intended it, not stop at B2.

## Execution order

1. Visually classify screenshot pack.
2. Map Everyday Finnish screenshots to old source files.
3. Rebuild Everyday Finnish native UI from screenshot/source truth.
4. Install and test.
5. Repeat for Professional Finnish.
6. Repeat for Roleplay/Cards/YKI/Progress/Settings/Account.
