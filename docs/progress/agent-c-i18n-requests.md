# Agent C i18n requests

## Previous shared build blocker

Resolved by Agent E in `b0e26f4`.

The earlier local Android build failed in `LearnCopy.kt` after Agent A Everyday i18n fields were added to the `LearnCopy` data class. Agent C did not patch shared i18n because `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n` is outside Agent C ownership.

## M31 active release blocker impact

M31 device QA says shipped pages must use centralized translations and the old 21-language catalog must be represented/wired instead of manual hardcoded strings.

Professional Finnish is a shipped page and still has owned-screen text that needs central i18n keys before release freeze.

## Professional Finnish keys needed

Agent C can consume these keys inside `professional/`, but Agent E/shared i18n owner should add them to the centralized copy/catalog layer:

- professional route/header eyebrow
- professional route/header title
- professional route/header body
- professional level path label
- professional workplace area label
- professional current path label
- professional notice/loading/empty-state strings
- professional roleplay/cards/interview/speech/report hub labels, titles, and bodies
- professional session progress labels: scenario, responses
- professional scenario labels: situation, task, model phrases
- professional response input label
- professional response hint
- professional save-and-continue button label
- professional completion title/body/next-step copy
- professional module metric labels: scenarios, level, time
- professional locked/open-practice labels
- professional domain display names for Healthcare, Office, Customer service, Job search, Safety, Small talk
- professional scenario type display names for Phrase practice, Dialogue practice, Email writing, Meeting response, Phone call
- professional level descriptions for A1, A2, B1, B2, C1, C2

## Scope note

Until these keys exist in shared i18n, Agent C should avoid editing `LearnCopy.kt` or shared translation catalogs directly. Agent C may keep Professional UI/function parity moving, but final M31 release freeze depends on Agent E/shared i18n wiring.
