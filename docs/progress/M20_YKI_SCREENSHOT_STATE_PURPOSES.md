# M20 YKI Screenshot State Purposes

Updated: 2026-07-02

This file defines what each YKI screenshot means in the native flow.

The screenshots are not decorative. Every image represents a required UI/function state.

## YKI Practice / YKI test

| Screenshot | Required purpose |
|---|---|
| IMG_0409 | Practice landing. Shows B1-B2 selected, YKI cards, and Start exam block. |
| IMG_0410 | Reading task initial state. Passage visible. No answer confirmed yet. User must choose/check. |
| IMG_0411 | Reading task checked-correct state. Correct option highlighted. Button changes to Next task. |
| IMG_0412 | Reading task checked-correct state for next task. Correct option highlighted. Button changes to Next task. |
| IMG_0413 | Long reading passage/scroll state. Passage length and answer region must be represented. |
| IMG_0414 | Reading checked-wrong state. Wrong option red, correct option green, explanation visible, Next task. |
| IMG_0415 | Reading checked-wrong state for another question. Wrong/correct/explanation/Next task. |
| IMG_0416 | Reading checked-wrong state with several options. Wrong/correct/explanation/Next task. |
| IMG_0417 | Listening task initial state. Audio button visible. No answer checked yet. |
| IMG_0418 | Listening checked-wrong state. Audio remains available. Wrong/correct/explanation/Next task. |
| IMG_0419 | Listening checked-correct state. Correct highlighted. Next task. |
| IMG_0420 | Listening checked-correct state. Correct highlighted. Next task. |
| IMG_0421 | Listening checked-wrong state. Wrong/correct/explanation/Next task. |
| IMG_0422 | Writing initial/editing state. Prompt from bank. Empty editable answer field. No placeholder. Autocorrect disabled. |
| IMG_0423 | Writing draft state after user types. Save answer button. |
| IMG_0424 | Writing draft/report state after user types. Save answer button. |
| IMG_0425 | Writing saved state. Save answer changes to Next task. |
| IMG_0426 | Writing email draft state after user types. Save answer button. |
| IMG_0427 | Writing email saved state. Save answer changes to Next task. |
| IMG_0428 | Speaking prompt state. Bank prompt visible. Start conversation roleplay / start speaking flow begins. |
| IMG_0429 | Speaking prompt state for occupational/healthcare roleplay. Bank prompt visible. |
| IMG_0430 | Speaking prompt state for food/opinion discussion. Bank prompt visible. |
| IMG_0431 | Speaking prompt state for phone-call roleplay. Bank prompt visible. |

## YKI Mock Exam / Full YKI exam

| Screenshot | Required purpose |
|---|---|
| IMG_0432 | Mock exam overview. Section cards visible. |
| IMG_0433 | Mock exam overview continuation/scroll. |
| IMG_0434 | Mock exam start action visible. |
| IMG_0436 | Reading task initial state. Passage and options. |
| IMG_0437 | Reading selected/continue state. |
| IMG_0438 | Long passage state. |
| IMG_0439 | Reading question/options state. |
| IMG_0440 | Reading selected/continue state. |
| IMG_0441 | Long reading passage/options state. |
| IMG_0442 | Reading selected/continue state. |
| IMG_0443 | Writing initial/editing state. |
| IMG_0444 | Writing typed state. |
| IMG_0445 | Writing structured/bullet prompt state. |
| IMG_0446 | Writing completed/continue-to-next-section state. |
| IMG_0447 | Timed preparation state. Timer visible around 00:07. |
| IMG_0448 | Speaking preparation state. Timer visible around 00:29. |
| IMG_0449 | Speaking preparation countdown state. Timer around 00:27. |
| IMG_0450 | Speaking preparation countdown state. Timer around 00:17. |
| IMG_0451 | Speaking preparation countdown state. Timer around 00:03. |
| IMG_0452 | Speaking/recording state. Timer around 00:59. Red/active recording state. |
| IMG_0453 | Speaking/recording countdown. Timer around 00:53. |
| IMG_0454 | Speaking/recording countdown. Timer around 00:38. |
| IMG_0455 | Speaking/recording complete or next-question active state. |
| IMG_0456 | Timed preparation state. Timer around 00:09. |
| IMG_0457 | Timed preparation state. Timer around 00:05. |
| IMG_0458 | Recording state. Timer around 00:20. |
| IMG_0459 | Recording state. Timer around 00:58. |
| IMG_0460 | Recording countdown. Timer around 00:54. |
| IMG_0461 | Recording countdown. Timer around 00:33. |
| IMG_0462 | Recording active/red state. Timer around 00:26. |
| IMG_0463 | Recording active/red state. Timer around 00:23. |
| IMG_0465 | Submitted/next-question state. |
| IMG_0466 | Timer complete state. Timer 00:00. |
| IMG_0467 | Preparation timer state. Timer around 00:12. |
| IMG_0468 | Preparation timer state. Timer around 00:29. |
| IMG_0469 | Listening/reading timer state. Timer around 00:58. |
| IMG_0470 | Listening/reading timer state. Timer around 00:52. |
| IMG_0471 | Listening/reading timer state. Timer around 00:39. |
| IMG_0472 | Active/red timer state. Timer around 00:30. |
| IMG_0473 | Timed answer/options state. Timer around 00:07. |
| IMG_0474 | Speaking timer state. Timer around 00:30. |
| IMG_0475 | Speaking timer state. Timer around 00:58. |
| IMG_0476 | Speaking active/red timer state. Timer around 00:14. |
| IMG_0477 | Submitted speaking state. Submit exam button. |
| IMG_0478 | Results overview. |
| IMG_0479 | Results overview with iOS share overlay in source; Android only needs results state underneath. |

## Rule

No screenshot may be treated as decoration.

Before a section is accepted, every screenshot must be represented as one of:

- initial state
- selected state
- checked-correct state
- checked-wrong state
- editing/draft state
- saved state
- preparation countdown state
- speaking/recording countdown state
- submitted state
- results state

## M20-K Practice completion state

The source screenshots end at speaking task states, but native Practice now adds the required product completion state:

- final evaluation screen
- PDF export
- Word-compatible export

This completion state is product-required feedback after shorter Practice sessions.

## M20-M Mock Exam implementation note

YKI Mock Exam screenshots are now represented through `YkiMockExamBank.kt` and `YkiMockExamExactScreen.kt`.

The timer screenshots are interpreted as live countdown states:

- preparation countdowns auto-advance,
- recording countdowns auto-start recording when microphone permission is available,
- submitted state moves to next question or Submit exam,
- results state includes export options.

## M20-MR2 Mock pretest guard

Mock testing is blocked until the build passes with:

- every Mock screenshot ID represented,
- delay/timer logic compiled,
- route set to the new bank-driven Mock screen,
- export actions present.
