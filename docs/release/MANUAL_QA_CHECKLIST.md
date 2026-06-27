# Manual QA checklist

## Scope

Run this checklist on a real Android device before store submission. Learn must remain the existing Learn experience with visual/function improvements only. Read and Create must stay separate products.

## Build identity

- App label shows Floently.
- Debug package uses `.debug` suffix.
- Release package uses `com.floently.app`.
- Version shows `0.2.0` / code `2` for this release baseline.

## Authentication

- App opens to the current authentication flow when signed out.
- Existing test account can sign in.
- Session survives app restart.
- Sign out clears the session.
- Failed login displays a clear error.

## Suite selector

- Signed-in user lands on Floently suite selector.
- Learn, Read, and Create are shown as separate products.
- Opening a product checks product access separately.
- Blocked product access shows a useful message and back action.
- Product selector does not mix Read/Create tabs into Learn.

## Learn

- Learn home opens from the suite.
- Back to Floently returns to the suite selector.
- YKI practice opens and preserves the existing flow direction.
- Professional Finnish opens and preserves the existing flow direction.
- Roleplay opens and preserves dynamic-service fallback behavior.
- Cards opens and preserves review/session direction.
- Progress opens and preserves progress dashboard direction.
- Account opens and sign-out remains available.
- Learn UI changes must feel like improvement and polish, not a rebuilt product.

## Read

- Read opens only as the Read product.
- Text input works.
- URL input is accepted.
- Language detection boundary works or falls back safely.
- Generate creates a reader state or falls back safely.
- Read automatically defaults to on.
- Toggle read automatically works.
- Saved library area is visible.

## Create Studio

- Create opens only as the Create Studio product.
- Direct tool cards are visible.
- Selecting hooks, captions, summarize, repurpose, and brand brain works.
- Generate calls the service boundary or falls back safely.
- Latest result appears.
- Projects panel appears.
- Create does not show Read tabs or Learn screens.

## Billing and access

- Billing dashboard loads from existing backend route or fallback state.
- Checkout intent button does not crash.
- Checkout URL is recognized when backend returns one.
- Separate Learn, Read, and Create access notes remain visible.

## Offline and error states

- App does not crash when backend routes are unavailable.
- Fallback messages are understandable.
- Retry/back actions work.
- No blank screen after failed service call.

## Store artifact checks

- Debug build succeeds.
- Release APK build succeeds.
- Release bundle build succeeds.
- Signing status command does not print private values.
- Generated APK/AAB files are not tracked by Git.

## Final gate

Store submission should wait until the checklist passes on a physical Android device and product access/payment behavior is confirmed against the real backend.
