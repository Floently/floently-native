# M37 Device Parity Acceptance Round

Status: ACTIVE

All M36 agents reported completion. Code now builds debug and release.

This round is not feature development first. It is clinical device verification against the screenshot lock and old app behavior.

## Verify on phone

1. Install latest debug APK.
2. Test all 20 languages.
3. Test RTL: Arabic, Persian, Urdu.
4. Test dark/light from drawer and settings.
5. Test public flow:
   - Floently landing
   - Learn landing
   - sign in
   - create account
   - placement/onboarding
   - Learn shell
   - account
   - billing/payment
6. Test Cards:
   - backend cards appear
   - flip works
   - overlays work
   - report flow works
   - selected language overlay applies
7. Test Roleplay:
   - AI starts automatically
   - mic start/stop sound
   - waveform
   - transcription
   - five turns
   - AI conclusion
   - PDF/Word export
8. Test YKI:
   - Practice
   - Mock A1-A2/B1-B2/C1-C2
   - YKI roleplay
   - banks/materials
9. Test Settings:
   - profile
   - language
   - theme
   - links
   - sign out
10. Compare every screen against docs/ui-lock screenshots.

## Agent redistribution after device QA

Only assign new work based on failed device checks:

- Agent A: any untranslated text / RTL / hardcoded language failure
- Agent B: roleplay/audio/mic/speech/transcript/export failure
- Agent C: dead routes / public/auth/payment flow failure
- Agent D: backend/material/fallback/payment API failure
- Agent E: theme/design/logo/screenshot visual mismatch/build/release failure

Android freezes only after device QA passes.
