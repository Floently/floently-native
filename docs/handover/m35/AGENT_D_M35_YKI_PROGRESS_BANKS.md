# Agent D M35 — YKI Practice, Mock, Roleplay, Banks, Progress

## Mission

Make YKI and Progress 100% old-app/backend/material parity.

Build success is not enough. YKI is complete only when Practice, Mock, Roleplay, banks, recording, evaluation, results, and progress match old app expectations.

## Source of truth

- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- docs/ui-lock/screenshots/*.PNG
- /home/vitus/m24-yki-backup-search/extracted/apps/client/app/yki-exam
- /home/vitus/m24-yki-backup-search/extracted/apps/client/app/yki-practice
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/yki-exam
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/yki-practice
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/exam
- /home/vitus/m24-yki-backup-search/extracted/packages/core/api/ykiExam.ts
- /home/vitus/m24-yki-backup-search/extracted/packages/core/api/ykiPractice.ts

## Ownership

You own only:

- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/progress
- docs/progress/agent-d-*.md

Do not edit shared i18n/theme/navigation directly. Request keys/routes in docs/progress/agent-d-requests.md.

## What changed from previous rounds

Device QA raised that:
- YKI roleplay appears missing
- Practice/Mock must use correct banks/material sources
- A1-A2, B1-B2, C1-C2 must work
- recording/evaluation/result flows need parity
- Progress must represent real saved activity, not fake completion

## YKI Practice requirements

- reading
- writing
- listening
- speaking
- task bank/material wiring
- instructions
- answer input
- recording where required
- evaluation
- result/export
- all CEFR bands where intended

## YKI Mock requirements

- A1-A2 / B1-B2 / C1-C2 selection
- exam intro
- reading section
- writing section
- listening section
- speaking section
- timers
- recording flow
- submission
- processing
- results overview
- detailed evaluation
- certificate/export
- material/bank source wiring

## YKI roleplay

Coordinate with Agent B. Ensure YKI roleplay materials/banks exist and route can be connected.

## Progress requirements

- YKI readiness
- workplace communication progress
- vocabulary/cards progress
- streaks
- saved activity events
- empty states
- fallback states must not pretend completion
- durable backend progress where available

## Completion criteria

- Android debug build passes.
- YKI Practice works on device.
- YKI Mock works on device.
- Level selection works.
- Banks/material source is represented.
- Progress does not fake completion.
- Commit and push with `D:` prefix.
