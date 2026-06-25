#!/usr/bin/env bash
set +e

cd /root/floently-native/apps/android || exit 1

echo "pwd=$(pwd)"
echo "java=$(command -v java)"
java -version 2>&1 | head -5

if [ -x "./gradlew" ]; then
  echo "using ./gradlew"
  ./gradlew :FloentlyLearn:app:assembleDebug
  exit $?
fi

if command -v gradle >/dev/null 2>&1; then
  echo "using system gradle"
  gradle :FloentlyLearn:app:assembleDebug
  exit $?
fi

echo "ERROR: no gradle runner found"
exit 2
