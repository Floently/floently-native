# Native Android Smoke Test Checklist

Purpose: verify the old-source-mapped native Learn app on a real Android device before claiming visual or behavior parity.

Current baseline branch: `m01/production-native-foundation`

## Pre-test rules

- Use the latest pushed branch before installing.
- Test with the real account `vitus.idi@floently.com` unless another account is explicitly chosen.
- Do not claim exact complete parity from code review only.
- Mark each item as pass, fail, or needs patch.
- Capture screenshots when a visual mismatch appears.
- Keep Learn, Read, and Create Studio access/payment boundaries separate.

## Install preparation

1. Pull latest branch.
2. Build debug APK.
3. Install debug APK on Android device.
4. Launch app fresh.
5. Confirm no crash on startup.

Helper script:

```bash
./scripts/android-learn-smoke-install.sh
5. Confirm no crash on startup.

Helper script: `./scripts/android-learn-smoke-install.sh`

## Home / drawer

- [ ] App launches without crash.
- [ ] Login/session restore works.
- [ ] Learn opens as Learn, not Read or Create.
- [ ] Home old-app palette/header appears.
- [ ] Home Menu opens drawer.
- [ ] Drawer closes.
- [ ] Drawer navigates to Cards, Professional Finnish, YKI, Roleplay, Progress, Settings, Account.
- [ ] Drawer sign out works.
- [ ] Floating Menu appears on non-home screens.

## Cards

- [ ] Cards opens.
- [ ] Bank modal opens/closes.
- [ ] Practice starts.
- [ ] Native TTS works where expected.
- [ ] Hint popup works.
- [ ] Rating buttons work.
- [ ] No crash after several cards.

## Roleplay

- [ ] Roleplay opens.
- [ ] Scenario cards render.
- [ ] Conversation screen opens.
- [ ] Transcript bubbles render.
- [ ] Text input/send works.
- [ ] OpenAI-backed generation path is preserved.
- [ ] No obvious repeated beginner cue loop.

## YKI

- [ ] YKI opens.
- [ ] Level strip and module cards render.
- [ ] Session starts.
- [ ] Blank answer warning is Finnish.
- [ ] Feedback/completion appears.

## Professional Finnish

- [ ] Professional Finnish opens.
- [ ] Domain chips switch content.
- [ ] Session starts.
- [ ] Model phrase cards render.
- [ ] Blank response warning is Finnish.
- [ ] Completion appears.

## Progress

- [ ] Progress opens.
- [ ] Snapshot, route cards, sync card, and timeline render.
- [ ] No crash if backend is unavailable.

## Settings

- [ ] Settings opens.
- [ ] Language chips work.
- [ ] Support link opens externally.
- [ ] Privacy link opens externally.
- [ ] Terms link opens externally.
- [ ] Account deletion link opens externally.
- [ ] Learn/Read/Create separation is clear.

## Account

- [ ] Account opens.
- [ ] Email appears.
- [ ] Plan, billing, product access, devices, and deletion boundary cards render.
- [ ] Account deletion button opens externally.
- [ ] Learn/Read/Create separation is clear.

## Assets

- [ ] No accidental old logo import.
- [ ] No accidental UI sound import.
- [ ] Text fits on main test device.

## Release blockers found during smoke test

| Area | Issue | Severity | Fix milestone |
|---|---|---:|---|
|  |  |  |  |

## Smoke test result

- Tester:
- Device:
- Android version:
- Build installed:
- Date:
- Overall status: Not tested yet
