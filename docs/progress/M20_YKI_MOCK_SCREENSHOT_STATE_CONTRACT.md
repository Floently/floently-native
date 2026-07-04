# M20 YKI Mock Exam Screenshot State Contract

Updated: 2026-07-04

YKI Mock Exam must not be tested until this contract passes build guards.

YKI Mock Exam follows the same logic as YKI Practice, but it has more sections and more timer/recording states.

## Required rule

Every screenshot is a required functional state, not decoration.

## Required states

| Screenshot | Required state |
|---|---|
| IMG_0432 | Mock overview, section cards visible |
| IMG_0433 | Mock overview continuation/scroll |
| IMG_0434 | Mock start action visible |
| IMG_0436 | Reading initial state, passage/options |
| IMG_0437 | Reading selected/continue state |
| IMG_0438 | Long reading passage state |
| IMG_0439 | Reading question/options state |
| IMG_0440 | Reading selected/continue state |
| IMG_0441 | Long reading passage/options state |
| IMG_0442 | Reading selected/continue state |
| IMG_0443 | Writing initial/editing state |
| IMG_0444 | Writing typed/draft state |
| IMG_0445 | Writing structured prompt state |
| IMG_0446 | Writing saved/continue-to-next-section state |
| IMG_0447 | Short preparation countdown state |
| IMG_0448 | Speaking preparation 00:29 state |
| IMG_0449 | Speaking preparation 00:27 state |
| IMG_0450 | Speaking preparation 00:17 state |
| IMG_0451 | Speaking preparation 00:03 state |
| IMG_0452 | Recording starts, 00:59 red/active state |
| IMG_0453 | Recording countdown 00:53 |
| IMG_0454 | Recording countdown 00:38 |
| IMG_0455 | Recording complete / next active state |
| IMG_0456 | Next preparation countdown 00:09 |
| IMG_0457 | Next preparation countdown 00:05 |
| IMG_0458 | Recording countdown 00:20 |
| IMG_0459 | Recording countdown 00:58 |
| IMG_0460 | Recording countdown 00:54 |
| IMG_0461 | Recording countdown 00:33 |
| IMG_0462 | Recording active red 00:26 |
| IMG_0463 | Recording active red 00:23 |
| IMG_0465 | Submitted / next-question state |
| IMG_0466 | Timer complete 00:00 state |
| IMG_0467 | Listening preparation 00:12 |
| IMG_0468 | Listening preparation 00:29 |
| IMG_0469 | Listening/timed answer 00:58 |
| IMG_0470 | Listening/timed answer 00:52 |
| IMG_0471 | Listening/timed answer 00:39 |
| IMG_0472 | Active/red timed state 00:30 |
| IMG_0473 | Timed answer/options 00:07 |
| IMG_0474 | Speaking preparation 00:30 |
| IMG_0475 | Speaking recording 00:58 |
| IMG_0476 | Speaking active/red 00:14 |
| IMG_0477 | Speaking submitted, Submit exam |
| IMG_0478 | Results overview |
| IMG_0479 | Results/share/export state |

## Required behavior

- Overview states must appear before the exam starts.
- Reading tasks must allow select/continue/check/next.
- Writing tasks must start empty, allow typing, save, then continue.
- Preparation timers must run automatically.
- Recording timers must auto-start recording when permission is available.
- Recording states must save/send before next question or submit exam.
- Results must show evaluation.
- PDF and Word-compatible export must be available.

## M20-O correction

Mock Exam must not show answer correctness during the exam.

Allowed during exam:

- selected option state,
- saved answer state,
- timer state,
- recording/submitted state.

Not allowed during exam:

- Correct feedback,
- Needs review feedback,
- green/red correction,
- highlighted correct answer.

Correction/evaluation belongs only on the final results screen.
