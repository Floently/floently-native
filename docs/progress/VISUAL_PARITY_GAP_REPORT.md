# Visual Parity Gap Report

Updated: 2026-07-01

## Real-device smoke result

The first Android device smoke test confirmed that the native app is not visually close enough to the original app.

Observed gaps:

- Side drawer looks very different from the original app.
- Cards are closer than other areas, but still have many visual differences.
- YKI design looks very different from the original app.
- Overall, many screens need significant design work before claiming original-app parity.

## Correct project status

Current native app status:

- Native Android foundation: working
- Real device install: working
- Old source mapped: partial
- Visual parity with original app: not achieved yet
- Exact original-app design: requires screenshot-driven rebuild

Do not describe the app as exact, complete, or visually matched until screenshot comparison confirms it.

## Required source of truth for M19

M19 visual parity must use:

1. Original app screenshots
2. Current native app screenshots from the same or similar device
3. Screen-by-screen comparison
4. Kotlin Compose patch
5. Rebuild/install
6. Repeat comparison

## YKI naming correction

- "YKI test" means YKI Practice / YKI-harjoittelu.
- "Full YKI exam" means YKI Mock Exam / YKI-koesimulaatio.

## Screenshot continuation rule

If multiple screenshots are from the same page, they must be combined as one continuous scrollable page in order.
