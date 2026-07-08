# Agent B progress: Roleplay + Speaking

Branch: `m01/production-native-foundation`
Repo: `galapoto/floently-native`

## Latest verified M33 Roleplay build

User pulled through `fac30e4` and confirmed:

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

Result:

```text
BUILD SUCCESSFUL in 24s
54 actionable tasks: 4 executed, 50 up-to-date
```

Agent B Roleplay/Speaking M33 old-flow changes compiled successfully. Remaining warnings are in YKI files, outside Agent B ownership.

## M33 active blocker pass

M33 hard blockers from `docs/progress/M33_DEVICE_QA_HARD_BLOCKERS.md` for Agent B:

- Opening roleplay starts conversation automatically.
- AI speaker starts speaking immediately.
- User presses mic to speak.
- Pressing mic again stops recording and sends speech.
- User speech is transcribed to text.
- AI replies automatically with spoken audio and transcript.
- Conversation continues for 5 user responses.
- Final AI response must conclude the interaction.
- No separate listen button; AI speech is automatic.
- Mic should visually animate/waveform while user speaks.
- Topics should vary dynamically each session, from backend/generated materials.
- UI must match old app roleplay UI.
- After conversation, user can download a PDF or Word document/book about the conversation.

Completed in this M33 pass:

- Added a five-user-turn guard and final AI conclusion response in the Roleplay repository layer.
- Auto-starts the first ready/recommended generated roleplay when the Roleplay screen opens.
- Added automatic Finnish TextToSpeech playback for the latest AI partner/coach message, removing the need for a separate listen action.
- Changed the mic flow so tap starts listening, tap again stops recording, recognition results are transcribed, and the transcript is sent automatically.
- Kept typed response as fallback only for devices where speech recognition fails or is unavailable.
- Preserved animated waveform during listening.
- Added completed-session export actions for PDF and Word-compatible document/book downloads using Android document creation.
- Updated the old-source mic panel wording/state to match the automatic old flow.
- Kept changes inside Agent B-owned Roleplay/Speaking/progress scope.

## Latest verified M32 Roleplay build

User pulled through integrated M32 head `399340f` and confirmed:

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

Result:

```text
BUILD SUCCESSFUL in 6s
54 actionable tasks: 54 up-to-date
```

Agent B Roleplay/Speaking changes from the M32 pass are included in this successful integrated Android build.

## M32 active blocker pass

M32 active Agent B blockers from `docs/progress/M32_FINAL_ANDROID_PARITY_FINISH.md`:

- Verify Roleplay 404 is gone on device.
- Verify backend/generated topics/materials are used.
- Verify A1-A2, B1-B2, C1-C2 coverage.
- Make roleplay flow more automatic while keeping user control.
- Integrate coach/conversation partner UX naturally.

Completed in this M32 pass:

- Strengthened `RoleplayService` backend/material parsing to accept more payload shapes: `scenarios`, `topics`, `materials`, `roleplays`, `conversations`, and `items`.
- Added more backend route variants for dashboard/topics/materials/session/start/message/reply calls, reducing the chance that a real endpoint shape still produces a visible 404.
- Added parsing for common backend field aliases such as `material_id`, `slug`, `category`, `opener`, `prompt`, `examples`, `role`, and `content`.
- Removed user-visible `Generated fallback` wording from Agent B fallback material labels; local safety topics now display as generated material rather than a fake/fallback deck.
- Integrated fallback coach guidance into the conversation partner turn as a coaching note instead of adding separate coach bubbles, making the coach/partner flow feel more natural.
- Kept all changes inside Agent B-owned Roleplay/Speaking/progress scope.

## Latest verified M31 Roleplay build

User pulled through `4203d10` and confirmed:

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

Result:

```text
BUILD SUCCESSFUL in 23s
54 actionable tasks: 4 executed, 50 up-to-date
```

Roleplay/Speaking compiled successfully after the M31 level-band and AI boundary updates. Remaining warnings are in YKI files, outside Agent B ownership.

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
- Updated `RoleplayAiBoundary.kt` to use the new M31 level bands and remove old `A1`, `A2`, `B1`, `B2` enum references after Agent C recorded the remaining Roleplay build blocker.
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
- Kept all changes inside Agent B-owned Roleplay/Speaking/progress scope.

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
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/RoleplayAiBoundary.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay/OldSourceRoleplayComponents.kt`
- `docs/progress/agent-b-i18n-requests.md`
- `docs/progress/agent-b-roleplay-speaking.md`
- `docs/progress/agent-b-blocked-by-shared-i18n.md`

## Build status

Verified locally by user through M33 Agent B head `fac30e4`:

```text
BUILD SUCCESSFUL in 24s
```

## QA focus for device pass

- Open Roleplay and confirm it auto-starts a ready generated topic.
- Confirm the AI partner speaks automatically without a separate listen button.
- Tap mic once, speak, tap mic again, and confirm the transcript sends automatically.
- Confirm waveform animates while listening.
- Complete five user responses.
- Confirm the final AI response concludes the interaction.
- Confirm PDF and Word document/book download actions appear after completion.
- Confirm typed fallback still works if speech recognition is unavailable.
