# Iloadi UI Constitution

Status: **Design authority**

Applies to: **Iloadi Home, Learn, Read, Create, account, subscription and shared system UI on iOS and Android.**

## 1. Product character

Iloadi should feel like **quiet intelligence**: precise, calm, premium, focused and unmistakably deliberate.

The interface must not look like a generic template, dashboard kit or a collection of unrelated cards. Visual distinction comes from typography, proportion, rhythm, surface treatment and motion rather than decoration for its own sake.

The intended emotional qualities are:

- calm rather than noisy;
- premium rather than flashy;
- intelligent rather than technical-looking;
- confident rather than dense;
- focused rather than dashboard-like;
- distinctive rather than trendy-for-trend's-sake.

## 2. One visual language

Iloadi has one shared visual language across products.

The following are prohibited outside the design-system layer:

- arbitrary hex colors;
- arbitrary spacing values;
- arbitrary corner radii;
- arbitrary font families;
- arbitrary button heights;
- one-off shadows or glows;
- locally invented card styles;
- locally invented navigation styles.

Learn, Read and Create may have product-specific content layouts, but they inherit the same design grammar.

## 3. Typography rule

The product UI font is **Onest**.

Reasons for the choice:

- it is a humanistic/geometric sans designed for screen interfaces and long-form reading;
- it remains distinguishable at small sizes;
- it supports a broad variable weight range;
- it is available under the SIL Open Font License 1.1;
- it gives the product a recognisable typographic voice without becoming decorative.

All Iloadi chrome, navigation, controls, settings, labels, onboarding, Learn UI, Read UI and Create UI use Onest.

Read content also defaults to Onest in Iloadi UI 1.0. A future user-selectable reading-font accessibility preference may be added, but it must not alter the brand/UI font.

No screen may fall back to a generic platform font unless the custom font fails to load, in which case the fallback is a technical contingency rather than a design choice.

## 4. Signature color rule

Iloadi's signature hue is **Iloadi Violet**, supported by a restrained near-black/plum neutral system.

The signature color is not sprayed across the interface. It identifies:

- selected navigation;
- primary action;
- active progress;
- meaningful focus;
- key product identity moments.

Large areas should remain dark and calm. Glow is reserved for active/important states and should never become a permanent haze over the whole interface.

## 5. Geometry rule

The visual system uses a 4-point base grid and an 8-point primary rhythm.

Every screen must align to the same horizontal container logic. Components must align to shared baselines and never appear visually scattered.

Phone content margins:

- compact phones: 20 pt/dp;
- standard phones: 20 pt/dp;
- large phones: 24 pt/dp;
- tablets: 32 pt/dp with constrained content widths.

No screen author may choose a new margin because it "looks better" locally.

## 6. Touch-target rule

Interactive controls must meet or exceed:

- Android: 48 dp touch target;
- iOS: 44 pt touch target.

Iloadi's standard interactive height is 52 pt/dp, deliberately above both platform minimums.

Small visual icons may be 20–24 pt/dp but must remain inside a 48 pt/dp interactive container.

## 7. Navigation architecture

Iloadi uses one global product navigation model:

- Home
- Learn
- Read
- Create

Account/profile is accessed from the top-right avatar/action rather than consuming a permanent fifth tab.

Product screens may have secondary navigation inside the product, but must not replace the global structure with an unrelated tab bar.

Immersive screens such as Reader, Learn session and Create editor may temporarily hide global navigation to protect focus.

## 8. Action hierarchy

Each screen should have one obvious primary action at a time.

Action priority:

1. primary filled action;
2. secondary outlined/surface action;
3. tertiary text/icon action;
4. destructive action, visually separated from the primary flow.

A screen should not show several equally loud violet buttons. If everything looks primary, nothing is primary.

## 9. Surface hierarchy

Iloadi avoids excessive cardification.

Use a card/panel only when it communicates a real grouping, selection surface, preview, state or elevated task.

Preferred hierarchy:

- canvas;
- tonal section;
- list/row;
- elevated panel only when needed;
- modal/bottom sheet for transient focused tasks.

Nested cards are prohibited unless a documented screen blueprint explicitly requires them.

## 10. Copy and engineering separation

Customer-facing UI must never contain implementation terms such as:

- Wave 004;
- backend adapter;
- local repository;
- SDK version;
- architecture boundary;
- development variant.

Those belong in diagnostics or engineering documentation.

## 11. Motion rule

Motion communicates state, hierarchy and continuity. It is not decoration.

Iloadi uses short, smooth transitions with restrained overshoot. Standard motion values are defined in `ILOADI_DESIGN_TOKENS.md`.

No screen may invent its own animation curve/duration without a design-system change.

## 12. Accessibility is part of the visual specification

Every design must include:

- sufficient color contrast;
- accessible touch targets;
- text scaling behaviour;
- content descriptions for non-text controls;
- reduced-motion behaviour;
- focus order;
- state not communicated by color alone.

The quality baseline uses Apple and Android accessibility guidance. Apple recommends a 44×44 pt default control size on iOS, and Android quality guidance requires touch targets of at least 48 dp.

## 13. Design-before-code gate

The sequence for any new UI feature is mandatory:

`feature behaviour -> screen inventory -> blueprint -> component mapping -> states -> accessibility -> design freeze -> implementation -> device QA`

Implementation may not precede design freeze.

## 14. Design freeze does not mean permanent stagnation

The system may improve, but changes must be intentional and versioned.

A token/component/navigation change is a design-system change affecting the product family, not a casual screen-level adjustment.

See `ILOADI_DESIGN_CHANGE_GATE.md`.
