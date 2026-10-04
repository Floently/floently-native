#!/usr/bin/env bash
set -euo pipefail

base_sha="${1:-}"
head_sha="${2:-}"

fail_safe() {
  printf 'android=true\n'
  exit 0
}

if [[ ! "$base_sha" =~ ^[0-9a-fA-F]{40}$ ]] ||
   [[ ! "$head_sha" =~ ^[0-9a-fA-F]{40}$ ]] ||
   [[ "$base_sha" == "0000000000000000000000000000000000000000" ]]; then
  fail_safe
fi

if ! git cat-file -e "${base_sha}^{commit}" 2>/dev/null ||
   ! git cat-file -e "${head_sha}^{commit}" 2>/dev/null; then
  # The workflow uses a full checkout, but keep the detector safe if GitHub
  # changes checkout behavior or a future caller supplies commits that are not
  # present locally. An uncertain scope must never suppress native coverage.
  fail_safe
fi

android=false

while IFS= read -r path; do
  [[ -z "$path" ]] && continue

  case "$path" in
    apps/android/*|shared/api-contracts/read/*|shared/read-core-rust/*|shared/read-core-native-ffi/*|scripts/build_read_native_core_android.sh|scripts/detect_read_ci_scope.sh|.github/workflows/read-mobile-browser-ci.yml)
      android=true
      break
      ;;
  esac
done < <(git diff --name-only "$base_sha" "$head_sha")

printf 'android=%s\n' "$android"
