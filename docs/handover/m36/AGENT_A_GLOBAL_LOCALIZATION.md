# Agent A M36 — Global Localization

You no longer own only Cards.

## Mission

Make every visible word in Android translatable using the old app translation catalog.

## Source

Old catalog:
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/index.ts
- /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/languages.ts

Native:
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n
- all screens with hardcoded Text(...)

## Critical known bug

`LearnLanguage` lists 20 languages, but `learnStrings()` only maps FI/SV/EN.

Fix this architecture.

## Tasks

1. Import/map old 20-language catalog.
2. Ensure all 20 languages return real strings, not English fallback.
3. Add RTL handling for Arabic, Persian, Urdu.
4. Replace hardcoded shipped UI strings.
5. Prioritize:
   - Auth
   - webentry
   - Suite
   - Read
   - Create
   - Settings
   - Account
   - Cards + overlays
   - Roleplay
   - YKI
   - Professional
   - Progress
6. Leave non-user internal strings only.
7. Build and push with `A:` prefix.

## Done only when

Changing language updates every reachable screen on device.
