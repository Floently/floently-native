# No-Loss Native Parity Rule

## Final rule

The native rebuild must not lose any existing Learn function.

This includes visible and invisible behavior:

- screens
- routes
- buttons
- forms
- navigation
- animations
- transitions
- gestures
- loading states
- error states
- empty states
- disabled states
- success states
- haptics if present
- sounds/audio if present
- keyboard behavior
- scroll behavior
- accessibility labels and flow
- authentication behavior
- subscription/access behavior
- offline or retry behavior
- edge cases
- internal all-access test behavior

## Meaning of parity

Parity does not mean copying React Native source files line by line.

Parity means the native Swift/Kotlin app must preserve the user-visible and product-critical behavior of the existing app.

The old Expo/React Native app is the reference implementation until native Learn is proven better.

## Required migration method

Every feature must go through this process:

1. Inventory old feature.
2. Record old files, screens, routes, stores, API calls, animations, and states.
3. Record backend API dependencies.
4. Build native iOS version.
5. Build native Android version.
6. Test against existing backend.
7. Compare with old app.
8. Mark parity as passed only after review.

## Release gate

A native app cannot replace the old app unless:

- all required parity rows are complete
- no critical function is missing
- no important animation or interaction is lost
- no subscription/auth/card-bank behavior is broken
- old app rollback remains available

## Backend rule

YKI and card bank remain backend-owned.

Native apps must not copy the full card bank or become a second source of truth.

## Bundle rule

All native rebuild work must be delivered as bundles.

No loose code patches.
