#!/usr/bin/env bash
set +e

NATIVE_BASE="/root/floently-native"
ANDROID_DIR="$NATIVE_BASE/apps/android"
TOOLS_DIR="$NATIVE_BASE/tools"
GRADLE_VERSION="8.9"
GRADLE_HOME="$TOOLS_DIR/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

mkdir -p "$TOOLS_DIR"

echo "=== build runner info ==="
echo "native_base=$NATIVE_BASE"
echo "android_dir=$ANDROID_DIR"
echo "gradle_home=$GRADLE_HOME"
echo "java=$(command -v java)"
java -version 2>&1 | head -5

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo
  echo "=== downloading gradle $GRADLE_VERSION ==="
  python3 - <<PY
import os
import urllib.request
import zipfile

url = "$GRADLE_URL"
zip_path = "$GRADLE_ZIP"
tools_dir = "$TOOLS_DIR"
gradle_home = "$GRADLE_HOME"

os.makedirs(tools_dir, exist_ok=True)

if not os.path.exists(zip_path):
    print("download:", url)
    urllib.request.urlretrieve(url, zip_path)
else:
    print("using cached zip:", zip_path)

if not os.path.isdir(gradle_home):
    print("extract:", zip_path)
    with zipfile.ZipFile(zip_path) as zf:
        zf.extractall(tools_dir)

print("ready:", gradle_home)
PY
fi

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "ERROR: Gradle runner still missing: $GRADLE_HOME/bin/gradle"
  exit 2
fi

cd "$ANDROID_DIR" || exit 1

echo
echo "=== gradle version ==="
"$GRADLE_HOME/bin/gradle" --version

echo
echo "=== build native Learn debug APK ==="
"$GRADLE_HOME/bin/gradle" :FloentlyLearn:app:assembleDebug --no-daemon --stacktrace
exit $?
