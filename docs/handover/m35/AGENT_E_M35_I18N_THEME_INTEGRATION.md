# Agent E M35 — I18N, Theme, Settings, Auth, Integration, Release Gate

## Mission

Own the shared systems that determine whether Android can reach 100% parity.

Primary focus: full 21-language i18n and global theme.

Build success is not enough. Agent E must ensure every visible screen can translate and theme correctly.

## Source of truth

- docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md
- /home/vitus/m34-full-screen-parity-index.txt
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/index.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/languages.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/state/preferencesStore.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/state/uiStore.ts
- /home/vitus/m24-yki-backup-search/extracted/packages/ui/theme/floentlyPalette.ts

## Ownership

You own:

- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/settings
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/account
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/auth
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/navigation
- apps/android/shared/src/main/java/com/floently/shared/design
- integration docs/build docs

## What we discovered

The M34 audit showed the native app still has many hardcoded strings across:
- Professional
- Progress
- Account
- Everyday
- Roleplay
- Settings
- Cards
- Navigation
- Auth
- YKI
- Read/Create/Suite visible shells

The old app already has i18n files. Do not manually retranslate everything. Import/copy/map the old catalog.

## I18N requirements

Every visible word must translate.

Must include:
- Floently.com landing/native equivalent
- Learn landing
- login
- sign up
- forgot/reset password
- onboarding
- placement test
- Learn home
- drawer/menu
- Everyday Finnish
- Cards
- card overlays
- card report overlay
- Roleplay
- YKI roleplay
- YKI Practice
- YKI Mock
- Professional
- Progress
- Settings
- Account
- Billing
- legal/support/account deletion
- Read/Create visible shell if visible

No hardcoded shipped text.

## Theme requirements

Dark/light must:
- update immediately
- persist
- apply to all screens
- match old palette
- support cards/overlays/dialogs/drawer/auth/landing/placement/settings/account/billing/YKI/roleplay
- preserve contrast and disabled states

## Settings/account requirements

Make functional or correctly route:
- language
- theme
- profile picture/gallery
- support
- privacy
- terms
- account deletion
- billing
- plan management
- restore/access state

## Integration duties

- Merge A-D output.
- Resolve conflicts.
- Convert i18n key requests into central i18n.
- Run debug build.
- Run release build.
- Install APK.
- Maintain final parity checklist.
- Do not declare Android freeze until device QA accepts.

## Completion criteria

- Full hardcoded string scan reduced to intentional non-user strings only.
- Language switch translates all screens tested.
- Dark/light toggle works globally.
- Settings/account links work or correctly route.
- Debug build passes.
- Release build passes.
- Device QA passes.
- Commit and push with `E:` prefix.
