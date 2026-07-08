# M33 Device QA Hard Blockers

Status: ACTIVE

Android is not release-ready.

## Hard blockers

### Agent E — global shared blockers

1. Full 21-language i18n is not wired.
   - Language selector changes language, but most pages remain untranslated.
   - Old app already has translations for all pages and all words.
   - Do not retranslate manually; import/copy old i18n catalog.

2. Dark/light theme toggle does not work.
   - Settings theme state must update the whole app.

3. Settings is mostly non-functional.
   - Profile picture change not working.
   - External links not working.
   - Language not working globally.
   - Theme not working globally.

4. Backend connectivity must be audited.
   - Backend is hosted on Hetzner.
   - Confirm app points to the correct backend.
   - Confirm auth, billing/payment, cards, roleplay, YKI banks, progress, account/settings APIs.

### Agent A — Cards

- Cards do not display/connect to backend/card bank.
- Must use real designed/generated card bank.
- Flip/question/answer behavior must match old app.

### Agent B — Roleplay/Speaking

Roleplay is not old-app parity.

Required old flow:
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

### Agent D — YKI

- YKI Practice and Mock must be connected to correct YKI banks/material source.
- A1-A2, B1-B2, C1-C2 selection must work.
- No fake/local-only exam content.

## Release rule

Android cannot freeze until these are fixed and verified on device.
