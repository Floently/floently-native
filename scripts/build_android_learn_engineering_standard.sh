#!/usr/bin/env bash
set +e

export ANDROID_HOME="/root/Android/Sdk"
export ANDROID_SDK_ROOT="/root/Android/Sdk"

NATIVE_BASE="/root/floently-native"
ANDROID_DIR="$NATIVE_BASE/apps/android"
GRADLE_EXE="$(find "$NATIVE_BASE/tools" -type f -path "*/bin/gradle" | sort | tail -1)"

echo "=== quality preflight ==="
"$NATIVE_BASE/scripts/native_quality_preflight.sh"

echo
echo "=== build environment ==="
echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "ANDROID_DIR=$ANDROID_DIR"
echo "GRADLE_EXE=$GRADLE_EXE"
java -version 2>&1 | head -5

if [ -z "$GRADLE_EXE" ]; then
  echo "ERROR: Gradle executable not found"
  exit 2
fi

chmod +x "$GRADLE_EXE"

cd "$ANDROID_DIR" || exit 1

echo
echo "=== clean previous native build outputs ==="
"$GRADLE_EXE" clean --no-daemon

echo
echo "=== build native Learn debug APK ==="
"$GRADLE_EXE" :FloentlyLearn:app:assembleDebug --no-daemon --stacktrace
exit $?
