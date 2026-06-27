# Final local QA runbook

## Purpose

Use this runbook to test the native Floently Android app in a fixed order before public store submission. This is the handoff from build readiness to real QA.

The app is not ready for public store submission until this runbook and `docs/release/NATIVE_MANUAL_QA_CHECKLIST.md` have pass/fail notes.

## Before starting

Confirm you are on the correct repo and branch:

```bash
cd /root/floently-native
git status --short
git branch --show-current
git log --oneline -5
```

Expected branch:

```text
m01/production-native-foundation
```

Do not continue if there are uncommitted source changes unless they are intentional test-only local files.

## Phase 1: clean verification build

Run:

```bash
./scripts/nativectl signing-status
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
./scripts/nativectl build android-learn-bundle
```

Expected:

- Debug build succeeds.
- Release build succeeds.
- Bundle build succeeds.
- Signing may show `not_configured` until local signing values are added.
- `apps/android/signing.properties` remains untracked.

## Phase 2: install a debug build for device QA

Build output is ignored by Git. Use the latest debug APK produced by Gradle.

Recommended command from the server or build machine with ADB access:

```bash
find apps/android/FloentlyLearn/app/build/outputs/apk/debug -name '*.apk' -type f -print
```

Install the APK on a test device when ADB is available:

```bash
adb install -r apps/android/FloentlyLearn/app/build/outputs/apk/debug/app-debug.apk
```

If ADB is not available on the server, copy the debug APK to the machine/device used for testing.

## Phase 3: authentication QA

Record pass/fail notes for each item:

- Fresh install opens without crash.
- Signed-out screen appears.
- Login works with the existing backend environment.
- Wrong credentials show a clear error state.
- Session persists after app restart.
- Sign out returns to signed-out state.
- Access check failure shows a readable retry/gate state.

## Phase 4: suite selector QA

Record pass/fail notes:

- Floently suite selector appears after login.
- Learn, Read, and Create are shown as separate products.
- Product cards do not merge access or payment meaning.
- Opening Learn checks Learn access.
- Opening Read checks Read access.
- Opening Create checks Create access.
- Blocked access returns to suite safely.
- Prepare checkout does not crash.
- Back navigation returns to suite selector.

## Phase 5: Learn QA

Guardrail: Learn is improvement-only. Do not accept a result that looks like a rebuilt unrelated product or mixes Read/Create tabs into Learn.

Record pass/fail notes:

- Learn opens from suite selector.
- Learn home layout is stable and usable.
- YKI opens and returns.
- Professional Finnish opens and returns.
- Roleplay opens and starts a session.
- Roleplay sends learner message and receives partner/coach response or safe fallback.
- Roleplay does not repeat the same beginner cue in a broken loop.
- Cards opens and starts a session.
- Cards rating works and session advances.
- Progress opens and shows backend data or safe fallback.
- Account opens and sign-out works.
- No Read/Create tabs appear inside Learn.

## Phase 6: Read QA

Record pass/fail notes:

- Read opens from suite selector.
- Text input accepts pasted content.
- URL input is treated as URL source.
- Language detection runs or safe fallback appears.
- Read automatically defaults on.
- Generation creates a reader session or safe fallback.
- Reader shows current chunk, progress, and play/read state.
- Saved library displays.
- Back navigation returns to suite selector.

## Phase 7: Create Studio QA

Record pass/fail notes:

- Create Studio opens from suite selector.
- Hooks tool opens.
- Captions tool opens.
- Summarize tool opens.
- Repurpose pack tool opens.
- Brand brain tool opens.
- Tool selection updates input/draft state.
- Generate returns real backend output or safe fallback.
- Result card displays output.
- Project/history section displays.
- Back navigation returns to suite selector.

## Phase 8: payments and access QA

Record pass/fail notes:

- Billing dashboard loads real plans or safe fallback.
- Learn plan/access state stays separate.
- Read plan/access state stays separate.
- Create plan/access state stays separate.
- Checkout intent can be prepared.
- Real checkout URL works when provider is configured.
- Return/cancel flow updates app state correctly.
- Entitlement refresh works after completed checkout.
- No product receives accidental bundle access unless deliberately configured.

## Phase 9: offline/error QA

Record pass/fail notes:

- Disable network and launch app.
- Existing session state does not crash app.
- Backend errors show readable fallback/error states.
- Retry actions do not duplicate sessions.
- Navigation remains usable after failures.
- Re-enable network and verify recovery.

## Phase 10: signed release bundle check

Only run after local signing is configured outside Git.

```bash
./scripts/nativectl signing-status
./scripts/nativectl build android-learn-bundle
```

Expected:

- Signing status reports configured.
- Bundle build succeeds.
- Keystore and signing property file remain untracked.

## QA result note format

Use this format while testing:

```text
Date:
Device:
Build commit:
Tester:

Builds: PASS/FAIL
Auth: PASS/FAIL
Suite selector: PASS/FAIL
Learn: PASS/FAIL
Read: PASS/FAIL
Create Studio: PASS/FAIL
Payments/access: PASS/FAIL
Offline/error: PASS/FAIL
Signing: PASS/FAIL/NOT CONFIGURED
Store assets: PASS/FAIL

Blocking issues:
1.
2.
3.

Decision:
- Continue to store preparation
- Fix blockers first
```

## Stop conditions

Stop release preparation and fix before continuing if any of these happen:

- App crashes on launch.
- Login cannot complete with the intended backend.
- Learn access, Read access, and Create access are mixed together incorrectly.
- Read/Create tabs appear inside Learn screens.
- Checkout grants wrong product access.
- Release bundle cannot be built.
- Signing values or generated artifacts are tracked by Git.
- Privacy/data-safety answers cannot be completed honestly from current behavior.
