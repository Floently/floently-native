# Agent B build blocker: shared i18n LearnCopy

Agent B Roleplay/Speaking clean build is blocked by shared i18n.

## Build command

ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn

## Failure

Kotlin compile fails in:

apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n/LearnCopy.kt

The repeated error is:

No value passed for parameter:
- everydayTitle
- everydaySubtitle
- everydayHeroEyebrow
- everydayHeroTitle
- everydayHeroBody
- everydayLevelEyebrow
- everydayChoosePractice
- everydayCardsBody
- everydayRoleplayBody
- everydayOpenTemplate

## Scope note

Agent B must not edit shared i18n files directly. This needs Agent E/coordinator or shared i18n owner.

## Build log

/home/vitus/floently-build-logs/android-learn-clean-agent-b-build.log
