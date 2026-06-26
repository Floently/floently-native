# Floently Native Progress

## Current milestone

M01 production native foundation.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build is verified with:

```bash
./scripts/nativectl build android-learn
```

## Completed

- Native repository created and pushed.
- Old Expo repository archived separately.
- Single build interface added: `scripts/nativectl`.
- Temporary one-off build scripts removed.
- Android Learn debug build verified.
- Android auth moved out of screen-local state.
- Secure session storage placed behind a session-store boundary.
- Auth repository boundary added.
- API client path/auth/error handling hardened.
- Learn app container and controller added.
- Learn loading, signed-out, auth-error, busy, and signed-in states added.
- Product access boundary added for Learn, Read, and Create.
- Learn access gate screen added.

## Active task

Wire Learn signed-in flow to product access checks, then rebuild Learn feature shells without losing YKI, Professional, roleplay, cards, subscriptions, device limits, animations, or interactions.

## Next step

Wire Learn app controller to the product access repository.
