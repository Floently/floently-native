# Agent A M35 — Cards, Card Banks, Overlays

## Mission

Make Cards 100% old-app/backend parity.

Build success is not enough. Cards are complete only when device QA confirms that card behavior, backend/material source, overlays, translations, and UI match the old app.

## Source of truth

- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- docs/ui-lock/screenshots/*.PNG
- docs/ui-lock/contact_sheet_*.jpg
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/cards
- /home/vitus/m24-yki-backup-search/extracted/packages/core/api/cards.ts
- /home/vitus/m24-yki-backup-search/extracted/packages/core/schemas/cards.ts

## Ownership

You own only:

- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards
- docs/progress/agent-a-*.md

Do not edit i18n/shared shell/theme/navigation directly. If you need strings, write requests to docs/progress/agent-a-i18n-requests.md.

## What changed from previous rounds

Device QA showed Cards still need deeper parity:
- card bank/backend must be real
- overlays must exist and work
- overlays must translate with selected language
- card report/problem overlay must exist
- flip/question/answer behavior must match old app

## Required implementation

Cards must support:

- card bank overview
- deck selection
- vocabulary mode
- sentence mode
- question side
- answer/meaning side
- options
- hint
- check answer
- skip
- recall/review
- difficult-card review
- progress dots
- speaker/audio button
- end session
- report card problem

Overlays must support:

- translation overlay
- explanation overlay
- grammar overlay
- example sentence overlay
- hint overlay
- report/problem overlay
- difficult-card overlay
- language-specific overlay

Problem report overlay must include:

- wrong answer
- options do not match question
- duplicate options
- bad Finnish
- not a real Finnish idiom
- bad grammar explanation
- bad example sentence
- audio problem
- translation/language problem
- other problem

## Backend/material rule

Use backend/card-bank material first.

Local fallback may exist only as offline fallback and must be visibly guarded as fallback, not fake production data.

## Completion criteria

- Android debug build passes.
- Cards open on device.
- Card content displays.
- Flip/check/skip/review works.
- Overlays open and close.
- Overlay language follows selected app language.
- Report card problem flow works.
- No fake-only card bank is used as primary source.
- Commit and push with `A:` prefix.
