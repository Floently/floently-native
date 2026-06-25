#!/usr/bin/env bash
set +e

export ANDROID_HOME="/root/Android/Sdk"
export ANDROID_SDK_ROOT="/root/Android/Sdk"

NATIVE_BASE="/root/floently-native"
ANDROID_DIR="$NATIVE_BASE/apps/android"
TOOLS_DIR="$NATIVE_BASE/tools"
GRADLE_EXE="$(find "$TOOLS_DIR" -type f -path "*/bin/gradle" | sort | tail -1)"

echo "=== build environment ==="
echo "ANDROID_HOME=$ANDROID_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "ANDROID_DIR=$ANDROID_DIR"
echo "GRADLE_EXE=$GRADLE_EXE"
echo "java=$(command -v java)"
java -version 2>&1 | head -5

echo
echo "=== local.properties ==="
cat "$ANDROID_DIR/local.properties" 2>/dev/null || true

echo
echo "=== sdk check ==="
ls -ld "$ANDROID_HOME" "$ANDROID_HOME/platforms" "$ANDROID_HOME/build-tools" 2>/dev/null
find "$ANDROID_HOME/platforms" -maxdepth 1 -type d -name "android-*" | sort | tail -10
find "$ANDROID_HOME/build-tools" -maxdepth 1 -type d | sort | tail -10

if [ -z "$GRADLE_EXE" ]; then
  echo "ERROR: Gradle executable not found under $TOOLS_DIR"
  exit 2
fi

chmod +x "$GRADLE_EXE"

cd "$ANDROID_DIR" || exit 1

echo
echo "=== gradle version ==="
"$GRADLE_EXE" --version

echo
echo "=== build native Learn debug APK ==="
"$GRADLE_EXE" :FloentlyLearn:app:assembleDebug --no-daemon --stacktrace
exit $?
