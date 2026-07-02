# M20-D Rejected: Placeholder YKI Implementation

Updated: 2026-07-02

## Status

Rejected.

Commit `9743611` built successfully, but it does not meet the M20 pixel/function parity contract.

## User rejection summary

YKI Practice:

- Only the first page looked close to the original app.
- The passages were missing or different.
- The questions were different.
- The question sections were different.
- The question types were different.
- Text arrangement did not match the screenshots.
- The rest of the flow did not resemble the original screenshots.

YKI Mock Exam:

- Passages were missing.
- Passage formatting was wrong.
- Options looked wrong.
- Options could not be selected.
- Text/answers appeared to approve automatically.
- Flow progression did not work properly.
- The user could not progress far enough to inspect the rest.

## Root cause

M20-D represented screenshot names and broad states, but it used placeholder content and generic UI.

This is not acceptable.

## New YKI rebuild rule

Do not generate placeholder YKI questions, passages, answers, timers, or options.

Every YKI task must be built from the exact screenshot/source content.

YKI Practice must be locked first before YKI Mock Exam.

## Correct next target

M20-E must rebuild YKI Practice only from:

- IMG_0409
- IMG_0410
- IMG_0411
- IMG_0412
- IMG_0413
- IMG_0414
- IMG_0415
- IMG_0416
- IMG_0417
- IMG_0418
- IMG_0419
- IMG_0420
- IMG_0421
- IMG_0422
- IMG_0423
- IMG_0424
- IMG_0425
- IMG_0426
- IMG_0427
- IMG_0428
- IMG_0429
- IMG_0430
- IMG_0431

YKI Mock Exam IMG_0432-IMG_0479 must not be worked on until YKI Practice is accepted.
