#!/usr/bin/env bash
set +e

BASE="/root/floently-native"

echo "=== native quality preflight ==="

echo
echo "=== forbidden old app path check in app source only ==="
grep -Rni "/root/floently-finnish" "$BASE/apps/android" 2>/dev/null
echo "forbidden_app_source_grep_exit=$?"

echo
echo "=== generated archives/logs inside source tree ==="
find "$BASE/apps" "$BASE/docs" "$BASE/scripts" -type f \( -name "*.tar.gz" -o -name "*.zip" -o -name "*.apk" -o -name "*.aab" -o -name "*.log" \) 2>/dev/null

echo
echo "=== smoke placeholder wording check ==="
grep -RniE "TODO|placeholder|fake|dummy|just enough" "$BASE/apps/android" 2>/dev/null
echo "placeholder_grep_exit=$?"

echo
echo "=== gradle toolchain config ==="
grep -RniE "jvmToolchain|VERSION_17|sourceCompatibility|targetCompatibility" "$BASE/apps/android" 2>/dev/null

echo
echo "=== preflight complete ==="
