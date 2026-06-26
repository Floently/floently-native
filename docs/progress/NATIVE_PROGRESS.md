# Floently Native Progress

## Current milestone

M01 production native foundation.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after wiring the Learn access gate with:

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `a781436`.

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
- Learn controller now checks product access before entering the signed-in Learn shell.
- Learn activity now handles loading, signed-out, auth, access-check, access-blocked, access-error, and signed-in states.

## Active task

Build the typed Learn feature navigation shell, then rebuild Learn features without losing YKI, Professional, roleplay, cards, subscriptions, device limits, animations, or interactions.

## Next step

Add typed Learn feature destinations and a dashboard shell for the rebuilt native Learn product.
