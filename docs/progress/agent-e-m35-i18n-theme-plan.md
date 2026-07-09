# Agent E M35 I18N + Theme Implementation Plan

Status: ACTIVE

M35 is not a small string patch. Native still has hardcoded user-facing strings across Learn, auth, settings, account, cards, roleplay, YKI, progress, professional, suite, Read, and Create visible shells.

Old app i18n source:
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/index.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/languages.ts

## Implementation order

1. Extract old app language list and translation keys.
2. Create/import a central native translation catalog from old i18n.
3. Keep typed Kotlin facade only where useful.
4. Replace hardcoded shipped strings screen by screen.
5. Wire language selector so every screen recomposes immediately.
6. Fix dark/light theme state so it updates globally and persists.
7. Wire Settings controls and account/billing/legal links.
8. Run hardcoded text scan until only intentional non-user strings remain.
9. Run debug + release builds.
10. Install and verify language/theme on device.

## Priority screens

1. Floently/public landing native equivalent
2. Learn landing
3. Login/sign-up/forgot/reset
4. Placement/onboarding
5. Learn Home and drawer
6. Everyday Finnish
7. Cards and overlays
8. Roleplay
9. YKI Practice/Mock/Roleplay
10. Professional
11. Progress
12. Settings
13. Account/Billing
14. Legal/support/delete account
15. Visible Read/Create/Suite shells

## Gate

Android cannot freeze until selected languages translate every visible screen and dark/light works globally.
