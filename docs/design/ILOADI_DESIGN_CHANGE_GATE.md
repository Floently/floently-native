# Iloadi Design Change Gate

This is a repository governance rule, not a suggestion.

## 1. UI implementation gate

A pull request that adds or materially changes UI must identify:

- the screen blueprint ID;
- the shared components used;
- the token version used;
- the states implemented;
- accessibility considerations;
- device screenshots or visual-test evidence after implementation.

If the UI is not already specified, the design documentation must change **before** or in a design-only PR preceding implementation.

## 2. Forbidden implementation patterns

UI code must not contain:

- raw hex colors except inside the design-token source;
- arbitrary font-family declarations outside typography/font infrastructure;
- arbitrary numeric padding/margin/radius values when a token exists;
- one-off button/card/input/navigation implementations when a shared component exists;
- emoji used as production navigation/action icons;
- screen-local shadows/glows unless explicitly specified;
- multiple visually equivalent primary actions in one decision surface;
- customer-facing engineering/debug copy.

## 3. Design-system change classification

### Patch — UI x.y.Z

Examples:

- correction to documented measurement ambiguity;
- accessibility fix that does not alter product character;
- component-state clarification.

### Minor — UI x.Y.0

Examples:

- new shared component;
- new supported screen pattern;
- new semantic color token;
- new responsive layout rule.

### Major — UI X.0.0

Examples:

- changing the brand font;
- changing signature color;
- changing global navigation architecture;
- changing the base spacing/grid system;
- broad radius/button geometry redesign.

A major change must not happen incidentally inside a feature PR.

## 4. Required design-first workflow

1. Define feature behaviour without UI assumptions.
2. Add/modify screen blueprint.
3. Map every interaction to an existing component or specify a new component.
4. Define normal, loading, empty, disabled, offline, error and success states as applicable.
5. Define accessibility and adaptive behaviour.
6. Freeze the design version.
7. Implement using tokens/components only.
8. Capture iOS and Android screenshots at representative device sizes.
9. Compare implementation against blueprint measurements.
10. Fix deviations before feature completion.

## 5. Visual QA target devices

At minimum for important screens:

- compact/narrow Android phone;
- standard Android phone;
- standard iPhone-equivalent layout;
- large phone when layout risk exists;
- tablet for screens explicitly supporting tablet layouts.

## 6. Measurement tolerance

Implementation should match frozen specifications exactly where values are explicit.

Permitted tolerance:

- 0 for token values and component dimensions;
- platform safe-area/system inset differences are expected;
- text wrapping may vary by platform/font rasterization, but container rules remain fixed;
- adaptive breakpoint rules may alter structure only as documented.

## 7. New-feature rule

A future feature not covered in `ILOADI_SCREEN_BLUEPRINTS.md` is **not permission to improvise**.

It creates a design task first.

The design task must answer:

- where the feature lives in navigation;
- which screen(s) it requires;
- where the primary action is;
- what components it uses;
- exact dimensions and spacing;
- all relevant states;
- adaptive/accessibility rules;
- whether it requires a design-system component or token change.

Only after that is implementation unblocked.

## 8. Review rejection criteria

A UI PR should be rejected when:

- it looks acceptable in isolation but violates global alignment;
- it introduces a new visual pattern without design approval;
- spacing/radius/type values are guessed locally;
- a button is placed wherever convenient instead of according to the screen blueprint;
- it depends on a generic system font when Onest should be loaded;
- it uses inconsistent iconography;
- it introduces inaccessible touch targets/contrast;
- it creates a visual exception because implementation was easier that way.

## 9. Definition of done for UI

A UI feature is done only when:

- implementation matches the frozen design;
- functionality works;
- accessibility works;
- loading/error/offline states work;
- visual QA passes on target devices;
- no raw style escape hatches were introduced;
- deviations are either removed or documented as an approved design-system change.
