# Agent B blocked by shared i18n

Agent B owns Roleplay + Speaking only. The clean Android Learn build is blocked outside Agent B ownership by shared i18n in `LearnCopy.kt`.

## Build command

```bash
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
```

## Failure

The clean build exits with `BUILD_EXIT_CODE=1` and Kotlin compilation errors in:

```text
apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/i18n/LearnCopy.kt
```

The repeated missing constructor parameters are:

- `everydayTitle`
- `everydaySubtitle`
- `everydayHeroEyebrow`
- `everydayHeroTitle`
- `everydayHeroBody`
- `everydayLevelEyebrow`
- `everydayChoosePractice`
- `everydayCardsBody`
- `everydayRoleplayBody`
- `everydayOpenTemplate`

## Scope decision

Agent B must not edit shared i18n files directly. This needs Agent E/coordinator or the shared i18n owner before Agent B can verify/push a clean Android build.

## Local log path

```text
/home/vitus/floently-build-logs/android-learn-clean-agent-b-build.log
```

## Status

Stopped as required because the build failure is outside Agent B ownership.
