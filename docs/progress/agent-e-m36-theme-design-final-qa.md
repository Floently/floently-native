# Agent E M36 theme/design/final QA

Date: 2026-07-09

Source:
- docs/progress/M36_FASTEST_PATH_CROSS_CUTTING_PARITY.md
- docs/handover/m36/AGENT_E_THEME_DESIGN_FINAL_QA.md

Scope:
- Agent E M36 theme/design system/final QA only.

Patch:
- Replaced the plain MainActivity MaterialTheme root with the global FloentlyTheme provider.
- Added a real shared FloentlyTheme provider.
- Added system/light/dark theme mode model.
- Added dark and light palettes for Learn, Read, and Create.
- Centralized base design tokens for screen padding, card radius, card spacing, chip radius, border width, and card padding.
- Updated FloentlyScreen, FloentlyCard, primary buttons, and secondary buttons to read the global theme mode.
- Added a shared FloentlyLogoMark for consistent Floently logo use.
- Preserved the existing old-app dark Learn palette and added light equivalents for parity QA.

M36 screenshot parity matrix:
- Public landing: dark/light provider ready; device screenshot still required.
- Auth/sign-in/create account: dark/light provider ready; device screenshot still required.
- Placement/onboarding: dark/light provider ready; device screenshot still required.
- Learn shell/home: dark/light provider ready; device screenshot still required.
- Cards/overlays: dark/light provider ready; device screenshot still required.
- Roleplay/YKI roleplay: dark/light provider ready; device screenshot still required.
- YKI Practice/Mock: dark/light provider ready; device screenshot still required.
- Progress: dark/light provider ready; device screenshot still required.
- Settings/account/billing: dark/light provider ready; device screenshot still required.

Known remaining gate:
- Device install QA and human screenshot acceptance are still required before Android freeze.
