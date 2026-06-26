# Floently Native Progress

## Current milestone

M01 production native foundation.

## Branch

`m01/production-native-foundation`

## Latest verified build

Android Learn debug build succeeded after adding the Professional Finnish native foundation with:

```bash
./scripts/nativectl build android-learn
```

Latest verified commit locally: `8e7eb70`.

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
- YKI native models added for levels, skills, task types, modules, tasks, sessions, progress, and dashboard state.
- YKI repository boundary added with preview seed data, dashboard loading, start-session, save-answer, and locked-module handling.
- YKI native screen shell added with level selection, module cards, progress display, guarded start-session handling, and back navigation.
- Learn dashboard now opens the native YKI shell for YKI practice while other features remain guarded placeholders.
- YKI task/session screen added with prompt display, answer input, save-answer boundary, in-session progress, completion state, and exit handling.
- YKI module start now enters the guarded task/session flow.
- YKI evaluation boundary added with evaluator interface, answer evaluation model, status, release gates, and non-persistent session summary.
- YKI completion screen now displays evaluation count, progress-ready state, persisted state, per-answer status, release gate, preview score, and feedback.
- YKI progress store boundary added with explicit deferred durable-save behavior.
- YKI completion screen now calls the progress store and shows native animated progress capture feedback.
- Professional Finnish native foundation added with domain models, module/session/scenario models, repository boundary, preview scenario data, animated dashboard, animated scenario flow, phrase cards, response input, progress animation, and Learn dashboard wiring.

## Active task

Continue the first real Learn feature foundation without losing YKI, Professional, roleplay, cards, subscriptions, device limits, animations, or interactions.

## Next step

Build the Roleplay native foundation with conversation service boundary, beginner-safe coaching, anti-repetition state, animated native session UI, and Learn dashboard wiring.
