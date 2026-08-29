# Read-only scope lock

Status: **ACTIVE**

The only active product implementation scope is **Iloadi Read**.

## Frozen products

Until the Read completion gate is explicitly passed:

- Learn must not be rebuilt, redesigned, refactored, migrated, or modified.
- Create must not receive new implementation work.
- Existing Learn remains the authoritative source and will be imported in a later dedicated phase.
- Shared-platform compatibility may be inspected, but that inspection must not produce Learn/Create UI or behavior changes.

## Active Read priorities

1. Restore a clean, stable Expo SDK 55 native build.
2. Bring every existing Read web/extension capability into the native Read product or record an approved mobile equivalent.
3. Make real Read account/session, voice catalog, TTS and playback functional.
4. Provide a mobile browser surface for public and signed-in pages while preserving the website UI and interaction.
5. Preserve authenticated desktop-page reading through the Chrome extension.
6. Keep the Read extension synchronized with shared Read behavior and customer-facing branding.
7. Complete parity tests before starting improvement-only work.

## Authenticated page rule

On mobile, the user-selected website may be rendered in a browser/WebView surface while the Iloadi app itself remains native. Read must not replace the target page. Extraction occurs from the live rendered DOM on explicit Read/Selection actions.

On desktop/web, arbitrary authenticated third-party pages are extension-first. Do not try to bypass same-origin, CSP, X-Frame-Options or identity-provider embedded-user-agent restrictions.

## UI interaction rule

Cards that represent actions must be interactive and expose pressed/focus feedback. Read uses aligned floating/elevated cards with consistent spacing and restrained glow for primary/focus states. Decorative cards must remain clearly non-interactive.

## Change gate

Any PR that changes Learn/Create implementation while this lock is ACTIVE must be rejected unless the user explicitly changes the active scope.