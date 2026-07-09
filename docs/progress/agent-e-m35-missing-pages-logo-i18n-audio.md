# Agent E M35 Missing Pages, Logo, I18N, Audio Scope

Status: ACTIVE

Device screenshots and M35 inventory confirm Android is not yet full parity.

## Must be present natively

- Product Gateway
- Learn landing
- Floently learner landing
- Organization landing
- Employer pathway
- Cities pathway
- Learn pathway
- Book demo
- Contact/demo section
- Sign in
- Create account
- Forgot/reset password
- Read entry page
- Create Studio entry page
- Placement/onboarding entry
- Learn shell entry

## Logo parity

Floently logo must be present and consistent on:

- gateway
- landing pages
- auth pages
- drawer/menu
- settings/account
- Read/Create entry pages
- marketing/native web parity screens

## I18N requirement

Old catalog source:

- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/index.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/languages.ts

Inventory confirms old i18n has 20 languages and 16,734 lines of translation source.

All visible text must use the old catalog or mapped native equivalent.

Languages:

- fi
- sv
- ru
- et
- uk
- ar
- en
- so
- fa
- zh
- sq
- ku
- vi
- bn
- tr
- tl
- th
- ne
- es
- ur

RTL must be handled for Arabic, Persian, and Urdu.

## Audio / animation parity

Must include native equivalents for:

- mic start sound
- mic stop sound
- recording waveform/ring
- AI speaking state
- AI thinking/loading state
- roleplay turn transition
- card flip/overlay animation
- landing/marketing motion where applicable

## Known native hardcoded hot spots

From M35 inventory:

- ProfessionalFinnishScreen.kt
- ProfessionalFinnishRepository.kt
- FloentlySuiteShell.kt
- M35WebToNativeParityScreens.kt
- LearnFeatureDestination.kt
- YkiPracticeBank.kt
- YkiMockExamExactScreen.kt
- LearnHomeScreen.kt
- LearnUtilityDrawer.kt
- RoleplayScreen.kt
- LearnProgressScreen.kt
- CardsRepository.kt
- AccountScreen.kt
- SettingsScreen.kt
- ReadShell.kt
- CreateStudioShell.kt

## Agent E implementation order

1. Ensure missing web/native parity pages exist.
2. Ensure logo asset/component is reused consistently.
3. Expand i18n catalog facade to cover public/auth/landing/settings/billing/cards/roleplay/YKI/progress/read/create.
4. Replace hardcoded strings in shared and Agent E-owned screens first.
5. Accept A-D i18n request docs and wire requested keys.
6. Fix theme propagation globally.
7. Verify debug + release builds.
8. Install and device-test language/theme/logo/page presence.
