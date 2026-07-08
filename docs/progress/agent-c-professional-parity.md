# Agent C Professional Finnish parity update

Branch: `m01/production-native-foundation`

Commits created through GitHub connector because the local repo was not mounted in this execution environment:

- `C: add Professional Finnish CEFR model`
- `C: rebuild Professional Finnish workplace scenarios`
- `C: rebuild Professional Finnish parity screen`
- `C: add Professional Finnish level filtering`

## Scope touched

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishModels.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishRepository.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/professional/ProfessionalFinnishScreen.kt`

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

## Build status

Not run here. The required Android SDK/local repo was not available in this execution environment.

Agent E fixed the shared Everyday i18n blocker on origin in `b0e26f4`, and Agent C continued from that pushed branch state.

Required local verification:

```bash
cd /home/vitus/floently-native
git fetch origin
git pull --rebase origin m01/production-native-foundation
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
git status --short
```

## Notes

No shared i18n, global navigation, Roleplay, Cards, Everyday, YKI, Progress, Settings, Account, or shared design files were edited in the Agent C continuation commit.
