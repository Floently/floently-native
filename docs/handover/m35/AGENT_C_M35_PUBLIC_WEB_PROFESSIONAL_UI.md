# Agent C M35 — Public Web Flow, Landing, Onboarding, Professional UI

## Mission

Represent the full Floently.com-to-Learn entry flow and Professional Finnish UI parity.

Build success is not enough. Screens must match old web/app UI, flows, animations, and translations through shared i18n.

## Source of truth

- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- docs/ui-lock/screenshots/*.PNG
- /home/vitus/m24-yki-backup-search/extracted/apps/client/app/index.tsx
- /home/vitus/m24-yki-backup-search/extracted/apps/client/web/LearnLandingPage.tsx
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/onboarding
- /home/vitus/m24-yki-backup-search/extracted/apps/client/state/PlacementRoute.tsx
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/placement
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/professional
- /home/vitus/m24-yki-backup-search/extracted/packages/ui/theme

## Ownership

You own:

- Professional Finnish UI files
- public/landing/onboarding/placement native screens if they already exist in your assigned area or new isolated files
- docs/progress/agent-c-*.md

Do not edit shared navigation/i18n/theme directly. Request shared changes in docs/progress/agent-c-requests.md.

## What changed from previous rounds

The target is no longer only Learn internal screens. The native app must represent the full old web-to-app journey:
- Floently.com landing
- Learn landing
- auth entry
- onboarding
- placement test
- route into Learn
- Professional path

## Required public/native equivalent

Represent:

- Floently.com landing
- product choice
- Learn landing
- marketing sections
- website animations in native equivalent
- CTA buttons
- pricing/access route entry
- legal/support links

## Required auth/placement/onboarding UI

Coordinate with Agent E for actual auth integration, but build/align UI for:

- login entry
- sign-up entry
- forgot/reset password entry
- onboarding welcome
- intent quiz
- plan selection
- practice frequency
- profession selection
- placement test home
- placement questions
- placement result
- recommended path into Learn

## Professional Finnish requirements

Match old app/web for:

- Professional landing
- Everyday Finnish vs My Profession branch
- profession selection
- workplace scenarios
- scenario practice
- recorded speaking
- incident practice
- professional cards
- professional roleplay
- interview practice
- phone/speech flow
- report writing
- A1-A2 / B1-B2 / C1-C2 support

## Animation requirements

Copy native equivalents of:
- marketing hero motion
- card hover/press feel
- drawer/page transition feel
- progress/section animation
- CTA animation

## Completion criteria

- Android debug build passes.
- Public/Learn landing flow is represented.
- Onboarding/placement screens are represented.
- Professional UI matches old app/web reference.
- All new strings are requested for i18n, not hardcoded permanently.
- Commit and push with `C:` prefix.
