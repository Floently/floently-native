# Agent C M35 web-to-native parity

## Scope

M35 expands Agent C beyond Professional Finnish into the public Floently.com-to-Learn journey.

Agent C ownership in this patch:

- public / Learn landing native equivalents
- onboarding / placement UI representation
- Professional branch UI representation
- native animation equivalents where safe
- Agent C request/progress docs

## Source references used

- `docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md`
- `docs/handover/m35/AGENT_C_M35_PUBLIC_WEB_PROFESSIONAL_UI.md`
- old `apps/client/app/index.tsx`
- old `apps/client/web/LearnLandingPage.tsx`
- old `apps/client/state/PlacementRoute.tsx`
- old `apps/client/features/professional/screens/WorkFinnishPathScreen.tsx`

## What was added

- `M35PublicFloentlyLandingNativeScreen`
- `M35LearnLandingNativeScreen`
- `M35OnboardingPlacementNativeScreen`
- `M35ProfessionalBranchNativeScreen`
- native Finnish correction demo animation equivalent
- native hero/pathway/outcome/platform sections
- native placement steps matching old welcome/goal/self/quiz/result route
- Professional Everyday Finnish vs My Profession branch representation

## Boundaries

Agent C did not wire these into shared navigation because the handover says not to edit shared navigation/i18n/theme directly.

Agent C did not edit:

- shared i18n catalog
- shared theme state
- auth shell
- billing/account/settings
- Cards
- Roleplay
- YKI
- Progress
- app shell

Shared integration and i18n wiring are requested in `docs/progress/agent-c-requests.md`.

## Build

Android debug build must pass before commit.
