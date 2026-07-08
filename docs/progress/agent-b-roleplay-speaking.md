# Agent B progress: Roleplay + Speaking

Branch: `m01/production-native-foundation`
Repo: `galapoto/floently-native`

## Latest update after clean Android baseline

User confirmed local branch was clean, equal to remote, and `./scripts/nativectl build android-learn` passed before this pass.

Completed in the latest Agent B pass:

- Added `coachingMode` to `RoleplayScenario` using the existing `RoleplayCoachingMode` enum.
- Parsed `coaching_mode` from the roleplay service boundary with safe level-based defaults.
- Applied coaching modes to fallback scenarios across beginner, natural, professional, and exam-style practice.
- Updated fallback coach cues/notes so turns reflect the active coaching mode.
- Displayed coaching mode metadata on old-source-style roleplay scenario cards.
- Kept all changes inside Agent B-owned Roleplay/progress scope.

## Previous update after shared i18n fix

Agent E fixed the shared Everyday i18n build blocker and pushed `b0e26f4`. Agent B then continued inside owned Roleplay/Speaking files.

Completed in the previous Agent B pass:

- Expanded fallback Roleplay scenario coverage from sparse one-card coverage to multiple practical scenarios across A1, A2, B1, and B2.
- Added target phrase lists for fallback scenarios so the scenario header/recorded response flow has real learner support content.
- Unlocked the B2 professional phone call because native speech capture now exists; no placeholder locked speaking path remains in the fallback dataset.
- Preserved `RoleplayRepository`, `RoleplayService`, and existing AI/backend handoff behavior.
- Kept all changes inside Agent B-owned Roleplay/progress scope.

## Earlier completed work

- Updated `RoleplayScreen.kt` to preserve the existing repository/service AI flow while adding a native Android speech-recognition path for speaking practice.
- Added real microphone permission handling through the existing Android `RECORD_AUDIO` permission and Compose permission launcher.
- Added Finnish speech recognition intent, partial transcript handling, final transcript review, and send-through to `RoleplayRepository.sendLearnerMessage`.
- Added old-source-style speaking components:
  - scenario header
  - mic ring
  - waveform indicator
  - recorded response review card
- Restored dashboard helper components after the screen extraction.
- Documented strings that need later i18n migration because shared i18n files are outside Agent B ownership.

## Files changed by Agent B

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayScreen.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplaySpeakingComponents.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayDashboardComponents.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayRepository.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayModels.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayService.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/OldSourceRoleplayComponents.kt`
- `docs/progress/agent-b-i18n-requests.md`
- `docs/progress/agent-b-roleplay-speaking.md`
- `docs/progress/agent-b-blocked-by-shared-i18n.md`

## Build status

Not run after the latest connector-side commits. The required local command still needs to be run on the machine that has `/home/vitus/floently-native` mounted:

```bash
cd /home/vitus/floently-native
git pull --rebase origin m01/production-native-foundation
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

## QA focus for device pass

- Open Roleplay dashboard and confirm level tabs render.
- Confirm A1/A2/B1/B2 each show multiple practical scenarios where expected.
- Confirm scenario cards show level/type and coaching mode chips.
- Start an unlocked scenario.
- Confirm conversation transcript bubbles still render and partner messages still expose TTS.
- Confirm coach note text changes appropriately by beginner, natural, professional, and exam-style mode.
- Tap mic, grant permission, speak a short Finnish answer, confirm transcript appears.
- Confirm recorded response card appears.
- Send the response and confirm AI/backend flow continues.
- Confirm typed fallback still works if speech recognition is unavailable.
