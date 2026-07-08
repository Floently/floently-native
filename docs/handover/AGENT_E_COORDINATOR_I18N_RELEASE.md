# Agent E Coordinator

Own:
- learn/app
- learn/i18n
- learn/navigation
- learn/settings
- learn/account
- docs/progress/NATIVE_PROGRESS.md
- release/build docs

Mission:
Coordinate agents, merge, resolve conflicts, complete global i18n/backend parity, run debug/release builds, install APK, and keep branch clean.

Do not start iOS until Android screenshot parity passes.

Main tasks:
- Pull/rebase often.
- Merge A-D work.
- Convert i18n requests into LearnCopy/LearnTranslations.
- Ensure drawer language changes translate shipped screens.
- Run debug and release builds.
- Install APK for phone QA.
- Keep branch clean.

Build:
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn
ANDROID_HOME=/usr/lib/android-sdk ANDROID_SDK_ROOT=/usr/lib/android-sdk ./scripts/nativectl build android-learn-release

Install:
APK=$(find apps/android/FloentlyLearn/app/build/outputs/apk/debug -name "*.apk" | head -n 1)
adb install -r "$APK"
adb shell monkey -p com.floently.app.debug -c android.intent.category.LAUNCHER 1
