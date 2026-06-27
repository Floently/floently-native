# Learn premium polish audit

## Decision

The first public release should focus on Floently Learn only. Read and Create Studio remain available foundations, but they are not blockers for the Learn-first release.

## Current audit result

The native Learn foundation exists and builds, but the Learn home still contained internal release-readiness language. That is not acceptable for a polished first release because users should see learning value, not engineering status.

## M16-A first polish patch

The Learn home was changed from an internal build/status dashboard into a user-facing Learn home:

- Clear Learn headline.
- Human learning subtitle.
- Welcome card using the signed-in user email prefix.
- Recommended next action into Roleplay.
- Clean Learn area cards for YKI, Professional Finnish, Roleplay, Cards, Progress, and Account.
- User-facing descriptions instead of native foundation/gate language.
- Account/sign-out section remains available.
- Back-to-suite remains available only when the suite shell provides it.

## Remaining Learn polish gaps

These are still required before release:

### YKI

- Replace any internal foundation wording with learner-facing copy.
- Improve task/session layout for small mobile screens.
- Polish answer, feedback, loading, and empty states.

### Professional Finnish

- Replace any internal foundation wording.
- Polish scenario cards, phrase cards, response input, and result states.

### Roleplay

- Replace internal service-boundary wording.
- Polish level selection and scenario cards.
- Improve chat/session layout so it feels like a real conversation tool.
- Verify dynamic OpenAI-backed responses and anti-repetition against backend.

### Cards

- Polish card session interaction.
- Make review/rating states feel complete enough for release.
- Verify progress/scheduling behaviour.

### Progress

- Polish progress dashboard, empty states, and fallback states.
- Verify backend progress data.

### Account

- Polish profile/access/sign-out layout.
- Keep account clear and simple for first release.

## Release rule

Do not ship Learn while screens contain engineering phrases such as native foundation, service boundary, parity gate, or production gate in user-facing UI.

## Next patch target

M16-B should polish Roleplay and Cards because these are core Learn value screens and likely screenshot candidates.
