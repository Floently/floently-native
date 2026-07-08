# Agent B progress: Roleplay + Speaking

Branch: `m01/production-native-foundation`
Repo: `galapoto/floently-native`

## Completed in this pass

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
- `docs/progress/agent-b-i18n-requests.md`

## Build status

Not run in this connector-only environment. The required local command still needs to be run on the machine that has `/home/vitus/floently-native` mounted:

```bash
cd /home/vitus/floently-native
git pull --rebase origin m01/production-native-foundation
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

## QA focus for device pass

- Open Roleplay dashboard and confirm level tabs render.
- Start an unlocked scenario.
- Confirm conversation transcript bubbles still render and partner messages still expose TTS.
- Tap mic, grant permission, speak a short Finnish answer, confirm transcript appears.
- Confirm recorded response card appears.
- Send the response and confirm AI/backend flow continues.
- Confirm typed fallback still works if speech recognition is unavailable.
