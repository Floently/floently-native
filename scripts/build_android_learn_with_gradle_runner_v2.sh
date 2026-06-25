#!/usr/bin/env bash
set +e

NATIVE_BASE="/root/floently-native"
ANDROID_DIR="$NATIVE_BASE/apps/android"
TOOLS_DIR="$NATIVE_BASE/tools"
GRADLE_VERSION="8.9"
GRADLE_ZIP="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

mkdir -p "$TOOLS_DIR"

echo "=== runner state ==="
echo "native_base=$NATIVE_BASE"
echo "android_dir=$ANDROID_DIR"
echo "tools_dir=$TOOLS_DIR"
echo "java=$(command -v java)"
java -version 2>&1 | head -5

echo
echo "=== inspect tools dir ==="
find "$TOOLS_DIR" -maxdepth 4 -type f -o -type d | sort | sed -n "1,160p"

GRADLE_EXE="$(find "$TOOLS_DIR" -type f -path "*/bin/gradle" | sort | tail -1)"

if [ -z "$GRADLE_EXE" ]; then
  echo
  echo "=== gradle binary not found; redownload and extract ==="
  python3 - <<PY
import os
import shutil
import urllib.request
import zipfile

url = "$GRADLE_URL"
zip_path = "$GRADLE_ZIP"
tools_dir = "$TOOLS_DIR"

os.makedirs(tools_dir, exist_ok=True)

if os.path.exists(zip_path):
    print("remove stale zip:", zip_path)
    os.remove(zip_path)

for name in os.listdir(tools_dir):
    if name.startswith("gradle-8.9"):
        path = os.path.join(tools_dir, name)
        if os.path.isdir(path):
            print("remove stale dir:", path)
            shutil.rmtree(path)

print("download:", url)
urllib.request.urlretrieve(url, zip_path)

print("extract:", zip_path)
with zipfile.ZipFile(zip_path) as zf:
    zf.extractall(tools_dir)
PY
fi

GRADLE_EXE="$(find "$TOOLS_DIR" -type f -path "*/bin/gradle" | sort | tail -1)"

echo
echo "=== gradle executable candidate ==="
echo "gradle_exe=$GRADLE_EXE"

if [ -z "$GRADLE_EXE" ]; then
  echo "ERROR: no Gradle binary found after extraction"
  find "$TOOLS_DIR" -maxdepth 5 -ls | sed -n "1,220p"
  exit 2
fi

chmod +x "$GRADLE_EXE"

if [ ! -x "$GRADLE_EXE" ]; then
  echo "ERROR: Gradle binary exists but is not executable"
  ls -lh "$GRADLE_EXE"
  exit 2
fi

cd "$ANDROID_DIR" || exit 1

echo
echo "=== gradle version ==="
"$GRADLE_EXE" --version

echo
echo "=== build native Learn debug APK ==="
"$GRADLE_EXE" :FloentlyLearn:app:assembleDebug --no-daemon --stacktrace
exit $?
