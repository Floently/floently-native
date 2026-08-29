# Iloadi Mobile Design System

This directory is the design source of truth for Iloadi Mobile.

## Permanent rule

**No user-interface implementation is allowed before the corresponding design contract is defined and frozen here.**

A feature can be functionally specified before design, but its UI must not be coded until:

1. the screen inventory is defined;
2. layout measurements are defined;
3. component usage is defined;
4. all primary states are defined;
5. typography, color and spacing tokens are assigned;
6. accessibility requirements are defined;
7. the design is approved/frozen for implementation.

The design system is product-wide. Learn, Read and Create must not independently invent fonts, button shapes, spacing, colors, cards, navigation patterns or interaction rules.

## Documents

- `ILOADI_UI_CONSTITUTION.md` — brand/UI principles and non-negotiable design rules.
- `ILOADI_DESIGN_TOKENS.md` — exact font, color, spacing, radius, elevation and motion values.
- `ILOADI_COMPONENT_SPEC.md` — exact geometry and states for shared components.
- `ILOADI_SCREEN_BLUEPRINTS.md` — screen-by-screen structure and measurements for known product features.
- `ILOADI_DESIGN_CHANGE_GATE.md` — GitHub/PR rule for changing UI after the design is frozen.

## Design-system version

Current baseline: **Iloadi UI 1.0.0 — design specification phase**.

No implementation should claim conformance until the complete 1.0.0 design specification has been reviewed and the affected screens have passed device visual QA.
