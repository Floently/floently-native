# Agent C i18n request

## Build blocker observed

Local Android build failed in `LearnCopy.kt` after Agent A Everyday i18n fields were added to the `LearnCopy` data class.

Error pattern:

- `No value passed for parameter 'everydayTitle'`
- `No value passed for parameter 'everydaySubtitle'`
- `No value passed for parameter 'everydayHeroEyebrow'`
- `No value passed for parameter 'everydayHeroTitle'`
- `No value passed for parameter 'everydayHeroBody'`
- `No value passed for parameter 'everydayLevelEyebrow'`
- `No value passed for parameter 'everydayChoosePractice'`
- `No value passed for parameter 'everydayCardsBody'`
- `No value passed for parameter 'everydayRoleplayBody'`
- `No value passed for parameter 'everydayOpenTemplate'`

## Scope note

Agent C did not patch shared i18n because `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n` is outside Agent C ownership.

## Requested owner action

Agent A or Agent E should add the Everyday fields to every `LearnCopy(...)` constructor after Finnish, or make the new Everyday fields safe defaults in the `LearnCopy` data class.

## Agent C status

Professional Finnish files are patched and documented, but final local Android build cannot pass until this shared i18n blocker is fixed.
