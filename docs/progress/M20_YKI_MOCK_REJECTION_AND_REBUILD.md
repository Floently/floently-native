# M20 YKI Mock Exam rejection and rebuild rules

Updated: 2026-07-04

User rejected the first YKI Mock Exam implementation.

## Rejection reasons

- It did not look close enough to the original Full YKI Exam screenshots.
- It behaved too much like YKI Practice.
- It showed correct/wrong feedback during the exam.
- It did not properly include listening and speaking as real sections.
- It did not include enough exam tasks/passages.
- It did not logically recreate the full exam from:
  - screenshots,
  - existing YKI code,
  - project plan/progress docs,
  - YKI bank architecture.

## Correct Mock rule

YKI Mock Exam must mimic an actual exam:

- no correct/wrong answer feedback during the exam,
- no green/red answer correction during the exam,
- reading/listening answers are only selected and saved,
- evaluation happens only at the end,
- listening is a real section with audio from the bank layer,
- speaking is a real section with preparation and automatic recording,
- writing is a real section with empty editable text fields,
- all content comes from `YkiMockExamBank.kt`,
- results/export happen only after Submit exam.

## Required section shape

The native Mock Exam now follows this structure:

1. Overview / start
2. Reading section
   - 3 bank reading tasks
3. Writing section
   - 2 bank writing tasks
4. Listening section
   - listening preparation
   - 3 bank listening tasks with audio scripts
5. Speaking section
   - 3 bank speaking tasks
   - each with preparation and recording states
6. Submit exam
7. Results/evaluation/export
