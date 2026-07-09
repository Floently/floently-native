# Agent B M35 — Roleplay, Speaking, Mic Flow, YKI Roleplay

## Mission

Make Roleplay/Speaking 100% old-app UI and function parity.

Build success is not enough. Roleplay is complete only when it behaves like the old app on device.

## Source of truth

- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- attached roleplay/microphone screenshots from device QA
- docs/ui-lock/screenshots/*.PNG
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/speaking
- /home/vitus/m24-yki-backup-search/extracted/apps/client/state/SpeakingRoute.tsx
- /home/vitus/m24-yki-backup-search/extracted/packages/core/api/roleplay.ts
- /home/vitus/m24-yki-backup-search/extracted/packages/core/api/speakingLab.ts

## Ownership

You own only:

- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/roleplay
- docs/progress/agent-b-*.md

Do not edit shared i18n/theme/navigation directly. If needed, request keys/routes in docs/progress/agent-b-i18n-requests.md or agent-b-navigation-requests.md.

## What changed from previous rounds

Device QA showed Roleplay is still not old-app parity:
- UI does not match old roleplay/mic screen
- old mic interaction is missing
- listen button should not be central
- AI should start automatically
- mic tap start/end should control user response
- transcription must show
- YKI roleplay appears missing
- topics must vary dynamically
- flow should be exactly 5 user turns, then conclusive AI ending

## Required old-app flow

1. User opens Roleplay.
2. AI speaker starts immediately.
3. AI text transcript appears.
4. User taps mic to start speaking.
5. Start mic sound feedback plays.
6. Mic ring/waveform animates while user speaks.
7. User taps mic again to stop.
8. Stop mic sound feedback plays.
9. Speech is transcribed and sent.
10. AI replies automatically.
11. AI reply speaks automatically and transcript appears.
12. Repeat until exactly 5 user responses.
13. Final AI response concludes the interaction.
14. User can download PDF or Word conversation document.

## UI requirements

- Match old mic screen screenshots.
- Speaker/AI label must be clear.
- No confusing split between coach and conversation partner.
- No separate main "listen" button in the normal flow.
- Mic is the primary action.
- Waveform/ring must show speaking/recording state.
- Transcript list must show AI and user turns.
- PDF/Word export must remain after completion.

## Topic/material rule

- Topics must not be limited to a small static list.
- Use backend/generated topics/materials first.
- Offline fallback may exist, but must not block dynamic backend topics.
- A1-A2, B1-B2, C1-C2 must be available.

## YKI roleplay

Add/fix YKI roleplay route if missing.
YKI roleplay must not be confused with Everyday or Professional roleplay.

## Completion criteria

- Android debug build passes.
- Roleplay opens on device.
- AI starts automatically.
- Mic start/stop works.
- Sound feedback works.
- Transcript appears.
- AI replies/speaks automatically.
- 5-turn completion works.
- Final response is conclusive.
- PDF/Word export works.
- YKI roleplay route exists.
- Commit and push with `B:` prefix.
