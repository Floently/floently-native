# M19-D/E YKI Visual Rebuild

Updated: 2026-07-01

## Screenshot source

- IMG_0409-IMG_0431: YKI Practice
- IMG_0432-IMG_0479: YKI Mock Exam

## Product naming correction

- "YKI test" means YKI Practice.
- "Full YKI exam" means YKI Mock Exam.

## M19-D/E correction

This batch separates the two concepts in native navigation and visual language:

- Added a distinct YKI Mock Exam destination.
- Drawer YKI Mock Exam now routes to its own destination instead of YKI Practice.
- YKI Practice and YKI Mock Exam now use different titles, subtitles, badges and action language.
- YKI feature screen now accepts `YkiScreenMode`.
- YKI session screen now accepts `YkiScreenMode`.
- YKI labels were converted toward screenshot language: Practice, Mock Exam, Tasks, Time, Skills, Start practice, Start mock section.
- Existing YKI repository/session behavior is preserved.

## Still pending

Exact section-by-section YKI mock exam timing/result layout may need another pass after the final full app check.

## M19-DER contract repair

Build feedback:

- Adding `YkiMockExam` made `LearnFeatureContract` non-exhaustive.

Correction:

- Added a dedicated `YkiMockExam` contract using the same access, parity, interaction, and release guard requirements as YKI Practice.
