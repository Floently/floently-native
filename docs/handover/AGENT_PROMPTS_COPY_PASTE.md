# Agent Prompts

## Common for Agents A-D

You are working on Floently native Android parity.

Repo: /home/vitus/floently-native
Branch: m01/production-native-foundation

Do not use GitHub web UI. Work locally and push to GitHub:
git pull --rebase origin m01/production-native-foundation
inspect -> patch owned files only -> debug build -> commit -> pull/rebase -> push

Source of truth:
- docs/ui-lock/screenshots/*.PNG
- docs/ui-lock/contact_sheet_*.jpg
- /home/vitus/m24-yki-backup-search/extracted
- Kotlin source under apps/android/FloentlyLearn/app/src/main/java/com/floently/learn

Build success is not parity. Match old UI, flow, function, translations, and backend behavior.

Before push:
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn

If a conflict touches files outside ownership, stop and report to Agent E.

## Agent A

Own:
- learn/everyday
- learn/cards
- docs/progress/agent-a-*

Mission:
Everyday Finnish + Cards screenshot and old-source parity.

Do not edit shared i18n/navigation/app shell. Put string needs in docs/progress/agent-a-i18n-requests.md.

Commit prefix: A:

## Agent B

Own:
- learn/roleplay
- future learn/speaking
- docs/progress/agent-b-*

Mission:
Roleplay + Speaking screenshot and old-source parity.

Do not edit shared i18n/navigation/app shell. Put string needs in docs/progress/agent-b-i18n-requests.md.

Commit prefix: B:

## Agent C

Own:
- learn/professional
- docs/progress/agent-c-*

Mission:
Professional Finnish + workplace scenarios screenshot and old-source parity, including A1-C2 where intended.

Do not edit shared i18n/navigation/app shell. Put string needs in docs/progress/agent-c-i18n-requests.md.

Commit prefix: C:

## Agent D

Own:
- learn/yki
- learn/progress
- docs/progress/agent-d-*

Mission:
YKI Practice, YKI Mock, and Progress screenshot and old-source parity.

Do not invent placeholder YKI content.
Do not edit shared i18n/navigation/app shell. Put string needs in docs/progress/agent-d-i18n-requests.md.

Commit prefix: D:
