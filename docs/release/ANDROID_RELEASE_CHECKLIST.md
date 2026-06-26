# Android release checklist

## Current release baseline

- App label: Floently.
- Application ID: `com.floently.app`.
- Version: `0.2.0` / code `2`.
- Debug package suffix: `.debug`.
- Release task: `./scripts/nativectl build android-learn-release`.

## Compatibility guardrails

- Learn UI must be preserved as the existing Learn product experience, with only visual/functionality improvements, animations, icons, polish, and usability upgrades.
- Do not rebuild Learn UI from scratch unless explicitly approved.
- Preserve Learn functions and product intent while improving native quality.
- Do not require new environment variables for Learn unless explicitly approved.
- Keep the app plug-and-play with the existing backend/environment assumptions.
- Read and Create can be added as separate products around Learn, but must not be mixed into Learn screens.

## Release gates before store submission

- Configure Play/App signing outside Git.
- Connect real Learn, Read, and Create entitlement services.
- Connect real checkout provider session creation.
- Connect Read import, upload, language detection, generation, reader session, and library services.
- Connect Create direct generation, project history, export, and share services.
- Connect Learn roleplay, dynamic OpenAI generation, anti-repetition, scoring, and progress sync services.
- Run device manual QA for login, suite selector, Learn, Read, Create, checkout boundary, offline/error states, and sign-out.
- Prepare Play Store listing assets, privacy policy, screenshots, data safety answers, and release notes.

## Verification commands

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
```

Build artifacts are intentionally ignored by Git.
