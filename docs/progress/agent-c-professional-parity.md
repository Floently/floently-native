# Agent C Professional Finnish parity update

Branch: `m01/production-native-foundation`

Commits created through GitHub connector because the local repo was not mounted in this execution environment:

- `C: add Professional Finnish CEFR model`
- `C: rebuild Professional Finnish workplace scenarios`
- `C: rebuild Professional Finnish parity screen`
- `C: add Professional Finnish level filtering`
- `C: update Professional i18n requests for M31`

## Scope touched

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishModels.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishRepository.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishScreen.kt`
- `docs/progress/agent-c-i18n-requests.md`
- `docs/progress/agent-c-professional-parity.md`

## What changed

- Added explicit `ProfessionalFinnishLevel` model with A1, A2, B1, B2, C1, and C2.
- Replaced the short placeholder module list with workplace/profession modules across the A1-C2 path.
- Added healthcare, doctor, practical nurse/family update, office, report writing, customer phone, interview, safety, and workplace small-talk scenario coverage.
- Removed placeholder hub copy such as “will be connected after visual acceptance”.
- Replaced the hardcoded page identity `Doctor` with `Professional Finnish` and a broader workplace Finnish header.
- Added visible level path chips for A1-C2.
- Upgraded the A1-C2 chips from passive labels to active filters, including an All-levels view.
- Added a current-path summary card for workplace area, selected level, module count, and scenario count.
- Connected Interview, Speech/Phone, and Report hub actions to real module filters inside the Professional screen without changing shared navigation.
- Kept Roleplay and Cards links routed through the existing destination callback only.

## M31 active blocker review

M31 device QA does not list a Professional-specific backend/function blocker. The active M31 item that affects Agent C is the global i18n release rule: shipped pages must use centralized translations and the old 21-language catalog must be represented/wired.

Agent C documented the Professional-specific keys needed in `docs/progress/agent-c-i18n-requests.md` and did not edit shared `LearnCopy.kt`, shared translation catalogs, navigation, or app shell.

## Build status

Not run here. The required Android SDK/local repo was not available in this execution environment.

Latest local build reported by Vitus before this continuation passed after Agent E fixed shared Everyday i18n:

```text
BUILD SUCCESSFUL in 13s
```

Required local verification after pulling the M31 continuation:

```bash
cd /home/vitus/floently-native
git fetch origin
git pull --rebase origin m01/production-native-foundation
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
git status --short
```

## Notes

No shared i18n, global navigation, Roleplay, Cards, Everyday, YKI, Progress, Settings, Account, or shared design files were edited in the Agent C M31 continuation.
