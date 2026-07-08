# Floently Android Parity Multi-Agent Master

Goal: Android reaches 100% old-app screenshot, UI, function, i18n, and backend parity before iOS starts.

Source of truth:
- docs/ui-lock/screenshots/*.PNG
- docs/ui-lock/contact_sheet_*.jpg
- docs/ui-lock/screenshot_manifest.csv
- /home/vitus/m24-yki-backup-search/extracted
- apps/android/FloentlyLearn/app/src/main/java/com/floently/learn

Critical rule: build success is not parity.

Fastest method:
Screenshot -> old React Native source -> exact Kotlin Compose implementation -> debug build -> install -> phone compare -> repeat.

Git rule:
All agents work locally and push to the shared GitHub branch.
Do not edit in GitHub web UI.

Branch:
m01/production-native-foundation

Agents:
- A: Everyday Finnish + Cards
- B: Roleplay + Speaking
- C: Professional Finnish + Workplace scenarios
- D: YKI + Progress
- E: Coordinator, i18n, navigation, settings/account, integration, release builds

Shared files belong to Agent E:
- learn/app
- learn/i18n
- learn/navigation
- learn/settings
- learn/account
- docs/progress/NATIVE_PROGRESS.md

Agents A-D must not edit shared files. Translation requests go into docs/progress/agent-x-i18n-requests.md.

Android freeze requires:
- screenshot parity
- debug build pass
- release build pass
- device QA pass
- i18n works
- no reachable placeholders
- backend/runtime parity represented
- branch clean and pushed
