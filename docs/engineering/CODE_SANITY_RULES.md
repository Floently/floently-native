# Code Sanity Rules

## Forbidden in production code

- plain-text token/session storage
- untyped backend response handling for important flows
- hardcoded fake user access
- TODO-only functionality
- placeholder screens presented as real product screens
- one giant file per feature
- networking directly inside UI components
- secret values committed to Git
- build artifacts committed to Git
- generated bundle archives committed to Git
- old React Native app mutation during native rebuild

## Required before release

- auth flow with real service wiring
- subscription/access resolution through backend contract
- device-limit behavior
- no lost Learn functionality
- no lost animations/interactions
- accessibility labels where needed
- error/loading/empty states
- offline/retry decisions documented
- release signing separated from debug build
