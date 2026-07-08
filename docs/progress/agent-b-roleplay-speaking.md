# Agent B progress: Roleplay + Speaking

Branch: `m01/production-native-foundation`
Repo: `galapoto/floently-native`

## M31 active blocker pass

M31 device QA recorded active Roleplay/Speaking blockers:

- Roleplay showed `request failed with status 404`.
- Roleplay needed level bands `A1-A2`, `B1-B2`, and `C1-C2`.
- Topic list was too limited/hardcoded.
- Roleplay should use backend/generated topics where available.
- Flow should start more automatically while still giving user control.
- Coach and conversation partner should feel integrated.

Completed in this M31 pass:

- Changed roleplay level model from separate `A1`, `A2`, `B1`, `B2` entries to the required level bands:
  - `A1-A2`
  - `B1-B2`
  - `C1-C2`
- Added backend route fallbacks for dashboard/topics/session/message calls so native Roleplay tries several likely backend route shapes before falling back.
- Stopped surfacing backend 404 as the visible Roleplay dashboard state; if backend is unavailable, the repository falls back without showing `request failed with status 404` to the user.
- Replaced the tiny old fallback list with broader generated fallback topics across all three M31 level bands.
- Added recommended topic metadata and a `Start recommended ... roleplay` button so users can start faster while still keeping manual topic control.
- Updated dashboard cards and session header to display level-band labels instead of raw enum names.
- Kept all changes inside Agent B-owned Roleplay/Speaking/progress scope.

## Latest verified state before M31 pass

User pulled through `ef1e50d` and confirmed:

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

Result:

```text
BUILD SUCCESSFUL in 12s
54 actionable tasks: 4 executed, 50 up-to-date
```

No Roleplay/Speaking warnings remained after the locale cleanup.

## Latest update after local build verification

User pulled through `4f30f9d` and confirmed `./scripts/nativectl build android-learn` passed. The only warning was a deprecated Java `Locale(String, String)` constructor in Agent B-owned `RoleplayScreen.kt`.

Completed after that verification:

- Replaced deprecated `Locale("fi", "FI")` usage with `Locale.forLanguageTag("fi-FI")` in the Finnish speech-recognition intent.
- Kept the same Finnish speech-recognition behavior while removing the warning source.
- Kept all changes inside Agent B-owned Roleplay/progress scope.

## Previous update after clean Android baseline

User confirmed local branch was clean, equal to remote, and `./scripts/nativectl build android-learn` passed before this pass.

Completed in the previous Agent B pass:

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

Verified locally by user through `ef1e50d` before the M31 pass:

```text
BUILD SUCCESSFUL in 12s
```

The M31 pass still needs local verification.

## QA focus for device pass

- Open Roleplay and confirm no visible `request failed with status 404` message appears.
- Confirm level bands are `A1-A2`, `B1-B2`, and `C1-C2`.
- Confirm recommended start button appears for a ready topic.
- Confirm cards show level band, type, mode, and material source.
- Start an unlocked scenario from the recommended button and from a card.
- Confirm conversation transcript bubbles still render and partner messages still expose TTS.
- Confirm coach note text changes appropriately by beginner, natural, professional, and exam-style mode.
- Tap mic, grant permission, speak a short Finnish answer, confirm transcript appears.
- Confirm recorded response card appears.
- Send the response and confirm backend or fallback flow continues.
- Confirm typed fallback still works if speech recognition is unavailable.
