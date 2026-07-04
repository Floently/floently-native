# M20 Pixel Parity Contract

Updated: 2026-07-02

## Non-negotiable target

The native app must represent every original screenshot.

A section is not complete if any screenshot in that section is missing, approximated, skipped, or visually/functionally different.

The target includes:

- every screenshot
- every screen state
- every scroll continuation
- every line of text
- every alignment
- every spacing value
- every text size
- every text color
- every card color
- every button position
- every play button position
- every progress indicator
- every timer
- every transition in the logical flow
- every visible function represented in the old app

No broad visual sweeps are allowed anymore.

## Rejected approach

The M19 broad visual sweep is not enough. It created approximations and missed important flow and function details.

Do not repeat that approach.

## Required section-lock workflow

Each section must follow this process:

1. Select one section only.
2. List every screenshot belonging to that section.
3. Treat multi-screenshot captures as one continuous page in the given order.
4. Map each screenshot to:
   - route
   - screen state
   - user action before it
   - next action after it
   - old code/component source
   - native file to change
5. Rebuild the section.
6. Build debug and release.
7. Install on device.
8. Compare the native app against the screenshots.
9. User confirms the section is accepted.
10. Only then move to the next section.

## Definition of done for a section

A section is complete only when the user confirms:

- all screenshots in the section are represented
- all alignments match
- all spacing matches
- all text and colors match
- all relevant buttons and controls are in the same positions
- every visible function is represented
- the logical flow from screenshot to screenshot works

## YKI priority

YKI Practice and YKI Mock Exam are the clearest visual sources and must receive the strictest treatment.

YKI Practice source:

- IMG_0409-IMG_0431

YKI Mock Exam source:

- IMG_0432-IMG_0479

The YKI implementation must represent:

- every screen in the sequence
- every task state
- every timer state
- every audio/play button position
- every option button
- every writing/answer input state
- every progress dot or step indicator
- every result/review state
- the exact difference between YKI Practice and YKI Mock Exam

## Screenshot coverage table

| Section | Screenshot range | Status |
|---|---:|---|
| Home | IMG_0390-IMG_0392 | Not locked |
| Main drawer | IMG_0393 | Partially close, not final |
| Professional / My Profession entry | IMG_0394-IMG_0396 | Not locked |
| Cards | IMG_0397-IMG_0399 | Not locked |
| Professional scenario flow | IMG_0400-IMG_0408 | Not locked |
| YKI Practice | IMG_0409-IMG_0431 | Not locked |
| YKI Mock Exam | IMG_0432-IMG_0479 | Not locked |
| Settings | IMG_0480-IMG_0483 | Not locked |
| Subscription / billing | IMG_0484-IMG_0488 | Not locked |
| Language selector / drawer continuation | IMG_0489-IMG_0493 | Partially close, not final |
| Settings/profile continuation | IMG_0494 | Not locked |

## Immediate next section

The next locked section is YKI, because the user identified it as the clearest source and the current native implementation is farthest from the screenshots.

The next implementation must start with a YKI screenshot-by-screenshot map before changing layout code.

## M20-D rejection guard

The YKI implementation from commit `9743611` is not accepted.

A successful debug/release build is not enough.

The accepted standard is visual and functional parity against each screenshot. Placeholder data is not allowed.

## M20-G YKI Practice functional acceptance note

YKI Practice visual layout is close enough for a function pass, but it is not accepted until:

- listening audio works,
- speaking recording works,
- writing fields are editable without autocorrect or placeholder text,
- task content comes from the YKI bank layer,
- the user confirms Practice behavior.

## M20-I YKI Practice state rule

YKI Practice is not accepted until button states follow the screenshot sequence:

- choice tasks: select > check answer > feedback > next task
- writing tasks: type > save answer > next task
- speaking tasks: start roleplay > prepare countdown > start speaking > speaking countdown/recording > save answer > next task
- task material must come from the YKI bank layer

## M20-I screenshot-state rule

Each YKI screenshot must be interpreted as a functional state, not decoration.

The state contract is now recorded in:

- `docs/progress/M20_YKI_SCREENSHOT_STATE_PURPOSES.md`

YKI Practice is accepted only after the user confirms:

- bank-driven content,
- choice check/next behavior,
- writing save/next behavior,
- speaking preparation/recording/save/next behavior.
