# Floently Native Progress

## Current milestone

M01 production native foundation.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after showing Learn feature contract requirements with:

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `7a7081e`.

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
- Typed Learn feature destinations added for YKI practice, Professional Finnish, Roleplay, Cards, Progress, and Account.
- Signed-in Learn dashboard renders typed destinations with release guards.
- Guarded native feature placeholder screen added.
- Signed-in shell now supports dashboard to guarded feature screen navigation and back.
- Learn feature contract models added with parity, access, interaction, and release requirements.
- Guarded feature placeholder screens now display their contract requirements.

## Active task

Start the first real Learn feature foundation without losing YKI, Professional, roleplay, cards, subscriptions, device limits, animations, or interactions.

## Next step

Add the YKI native feature foundation: typed YKI module/session models, repository boundary, and guarded screen shell before any backend wiring.
