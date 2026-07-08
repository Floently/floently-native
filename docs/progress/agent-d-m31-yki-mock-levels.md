# Agent D M31 YKI Mock level selection

Date: 2026-07-08

Scope:
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiMockExamBank.kt
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiMockExamExactScreen.kt

M31 blocker addressed:
- YKI Mock Exam now exposes level selection for A1-A2, B1-B2, and C1-C2.
- The selected level resets exam state to avoid stale answers across levels.
- The selected level is visible in landing, summary, results, and export title.
- Mock tasks are requested through a level-aware bank function and carry authority/source metadata.
- The UI shows certified bank metadata instead of presenting local UI code as newly generated content.

No shared files were edited.
