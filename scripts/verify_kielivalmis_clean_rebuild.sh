#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

required_files=(
  "docs/LEARN_NEXT_GENERATION_GOVERNING_RULE.md"
  "docs/design/KIELIVALMIS_CARD_AND_LAYOUT_SYSTEM_V1.md"
  "docs/design/KIELIVALMIS_HOME_BLUEPRINT_V1.md"
  "apps/ios/FloentlyLearn/project.yml"
  "apps/ios/FloentlyLearn/FloentlyLearn/Design/KieliValmisDesignSystem.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisHomeView.swift"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/design/KieliValmisDesign.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisHomeScreen.kt"
  "shared/api-contracts/learn/v1/activity-definition.schema.json"
  "shared/api-contracts/learn/v1/learning-session-plan.schema.json"
  "shared/api-contracts/learn/v1/learning-event.schema.json"
)

for file in "${required_files[@]}"; do
  test -f "$file" || { echo "Missing required KieliValmis file: $file" >&2; exit 1; }
done

python3 - <<'PY'
import json
from pathlib import Path

for path in sorted(Path("shared/api-contracts/learn/v1").glob("*.json")):
    with path.open("r", encoding="utf-8") as handle:
        json.load(handle)
print("Learn V1 JSON schemas parse successfully.")
PY

grep -q "com.vitusidi.floently.learn.native.dev" apps/ios/FloentlyLearn/project.yml
grep -q "com.vitusidi.floently.learn.native.dev" apps/android/FloentlyLearn/app/build.gradle.kts

grep -q "KieliValmisHomeView" apps/ios/FloentlyLearn/FloentlyLearn/FloentlyLearnApp.swift
grep -q "KieliValmisHomeScreen" apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/MainActivity.kt

if grep -q "FloentlyCard" apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisHomeView.swift; then
  echo "Legacy generic FloentlyCard found in clean iOS Home." >&2
  exit 1
fi

if grep -q "FloentlyCard" apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisHomeScreen.kt; then
  echo "Legacy generic FloentlyCard found in clean Android Home." >&2
  exit 1
fi

echo "KieliValmis clean rebuild static verification: PASS"
