#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "=== Floently Learn Android smoke install ==="
echo "branch=$(git branch --show-current)"
echo "head=$(git rev-parse --short HEAD)"
echo

echo "=== Working tree ==="
git status --short
echo

echo "=== Build debug APK ==="
ANDROID_HOME="${ANDROID_HOME:-/usr/lib/android-sdk}" \
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}" \
./scripts/nativectl build android-learn

echo
echo "=== Find debug APK ==="
APK="$(find apps/android/FloentlyLearn/app/build/outputs/apk/debug -name '*.apk' -type f | sort | tail -n 1 || true)"
if [ -z "$APK" ]; then
  echo "ERROR: debug APK not found."
  exit 1
fi
echo "apk=$APK"

echo
echo "=== ADB device check ==="
if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools or install manually:"
  echo "$APK"
  exit 0
fi

adb devices

DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [ "$DEVICE_COUNT" = "0" ]; then
  echo "No authorized Android device found."
  echo "Enable USB debugging, connect the phone, accept the prompt, then run this script again."
  echo "Manual APK path:"
  echo "$APK"
  exit 0
fi

echo
echo "=== Install debug APK ==="
adb install -r "$APK"

echo
echo "=== Done ==="
echo "Open Floently Learn on the device and use docs/progress/NATIVE_SMOKE_TEST_CHECKLIST.md."
