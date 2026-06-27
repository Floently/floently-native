# Learn premium polish audit

## Decision

The first public release should focus on Floently Learn only. Read and Create Studio remain available foundations, but they are not blockers for the Learn-first release.

## Current audit result

The native Learn foundation exists and builds, but some Learn screens still contained internal release-readiness language. That is not acceptable for a polished first release because users should see learning value, not engineering status.

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

## M16-B Roleplay and Cards polish patch

Roleplay and Cards were updated because they are core Learn value screens and likely screenshot candidates.

Roleplay changes:

- Replaced internal service-boundary wording with learner-facing conversation copy.
- Polished level selection language.
- Improved scenario cards with beginner-friendly helper text.
- Reworked conversation screen copy to feel like a real practice session.
- Added clearer reply guidance and coach note labeling.
- Kept dynamic repository/service behavior intact.

Cards changes:

- Replaced internal foundation wording with learner-facing review copy.
- Polished deck selection language.
- Added a review session screen after starting a deck.
- Added answer reveal behavior.
- Added rating flow for Again, Hard, Good, and Easy.
- Added review summary after session completion.
- Kept repository/service behavior intact.

## M16-C YKI and Professional Finnish polish patch

YKI and Professional Finnish were updated because they define the original Learn value and are important for the Learn-first release.

YKI changes:

- Replaced internal guarded/parity wording with learner-facing exam practice copy.
- Polished level selection and module cards.
- Improved task session copy, progress text, answer guidance, and completion summary.
- Removed release-gate wording from user-facing YKI feedback cards.
- Kept repository, evaluator, and progress-store behavior intact.

Professional Finnish changes:

- Replaced internal guarded/parity wording with learner-facing workplace practice copy.
- Polished domain selection and module cards.
- Improved scenario session copy, model phrase presentation, response guidance, and completion summary.
- Removed release-gate wording from user-facing scenario cards.
- Kept repository behavior intact.

## Remaining Learn polish gaps

These are still required before release:

### YKI

- Verify backend scoring, progress, and real-device task layout.

### Professional Finnish

- Verify backend feedback, progress, and real-device keyboard behaviour.

### Roleplay

- Verify dynamic OpenAI-backed responses and anti-repetition against backend.
- Check real-device chat layout and keyboard behaviour.

### Cards

- Verify progress/scheduling behaviour against backend.
- Check real-device card layout and rating ergonomics.

### Progress

- Polish progress dashboard, empty states, and fallback states.
- Verify backend progress data.

### Account

- Polish profile/access/sign-out layout.
- Keep account clear and simple for first release.

## Release rule

Do not ship Learn while screens contain engineering phrases such as native foundation, service boundary, parity gate, or production gate in user-facing UI.

## Next patch target

M16-D should polish Progress and Account, then run a search for remaining internal engineering wording in Learn user-facing screens.
