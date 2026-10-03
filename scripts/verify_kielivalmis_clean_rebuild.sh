#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

required_files=(
  "docs/LEARN_NEXT_GENERATION_GOVERNING_RULE.md"
  "docs/LEARN_SESSION_ARCHITECTURE_V1.md"
  "docs/LEARN_CONTRACT_AUTHORITY.md"
  "docs/design/KIELIVALMIS_CARD_AND_LAYOUT_SYSTEM_V1.md"
  "docs/design/KIELIVALMIS_HOME_BLUEPRINT_V1.md"
  "docs/design/KIELIVALMIS_AUTH_BLUEPRINT_V1.md"
  "apps/ios/FloentlyLearn/project.yml"
  "apps/ios/FloentlyLearn/FloentlyLearn/Design/KieliValmisDesignSystem.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/State/LearnAppModel.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/Auth/LearnAuthView.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisHomeView.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisEverydayOverviewView.swift"
  "apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisPathwayOverviewViews.swift"
  "apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnContractsV1.swift"
  "apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnEventSyncService.swift"
  "apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnOverviewService.swift"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/design/KieliValmisDesign.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/state/LearnAppViewModel.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/auth/LearnAuthScreen.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisHomeScreen.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisEverydayOverviewScreen.kt"
  "apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisPathwayOverviewScreens.kt"
  "apps/android/shared/src/main/java/com/floently/shared/learn/LearnContractsV1.kt"
  "apps/android/shared/src/main/java/com/floently/shared/learn/LearnEventSyncService.kt"
  "apps/android/shared/src/main/java/com/floently/shared/learn/LearnOverviewService.kt"
  "shared/api-contracts/learn/v1/activity-definition.schema.json"
  "shared/api-contracts/learn/v1/learning-session-plan.schema.json"
  "shared/api-contracts/learn/v1/task-descriptor.schema.json"
  "shared/api-contracts/learn/v1/practice-session-manifest.schema.json"
  "shared/api-contracts/learn/v1/learning-event.schema.json"
)

for file in "${required_files[@]}"; do
  test -f "$file" || {
    echo "Missing required KieliValmis file: $file" >&2
    exit 1
  }
done

python3 - <<'PY'
import json
from pathlib import Path

for path in sorted(Path("shared/api-contracts/learn/v1").glob("*.json")):
    with path.open("r", encoding="utf-8") as handle:
        json.load(handle)
print("Learn V1 JSON schemas parse successfully.")
PY

# Clean rebuild remains isolated from production app identities.
grep -q "com.vitusidi.floently.learn.native.dev" apps/ios/FloentlyLearn/project.yml
grep -q "com.vitusidi.floently.learn.native.dev" apps/android/FloentlyLearn/app/build.gradle.kts

# Native API traffic must use the actual Learn API authority, not the web frontend host.
grep -q 'https://learn-api.floently.com' apps/ios/FloentlyShared/Sources/FloentlyShared/API/FloentlyAPIClient.swift
grep -q 'https://learn-api.floently.com' apps/android/shared/src/main/java/com/floently/shared/api/FloentlyApiClient.kt

# Password reset must satisfy the current backend contract.
grep -q 'confirmPassword = "confirm_password"' apps/ios/FloentlyShared/Sources/FloentlyShared/Auth/FloentlyAuthModels.swift
grep -q 'put("confirm_password", password)' apps/android/shared/src/main/java/com/floently/shared/auth/FloentlyAuthService.kt

# Real root state owners replace the old placeholder signed-in booleans.
grep -q "LearnAppModel" apps/ios/FloentlyLearn/FloentlyLearn/FloentlyLearnApp.swift
grep -q "LearnAppViewModel" apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/MainActivity.kt

# V1 contract owners exist on both platforms.
grep -q "LearningSessionStateV1" apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnContractsV1.swift
grep -q "LearningEventOutboxV1" apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnContractsV1.swift
grep -q "LearningSessionStateV1" apps/android/shared/src/main/java/com/floently/shared/learn/LearnContractsV1.kt
grep -q "LearningEventOutboxV1" apps/android/shared/src/main/java/com/floently/shared/learn/LearnContractsV1.kt

# Access decoding must include the real feature map used by Everyday Finnish.
grep -q "generalFinnishAccess" apps/ios/FloentlyShared/Sources/FloentlyShared/Billing/FloentlyAccessModels.swift
grep -q "accessibleProfessions" apps/ios/FloentlyShared/Sources/FloentlyShared/Billing/FloentlyAccessModels.swift
grep -q "generalFinnishAccess" apps/android/shared/src/main/java/com/floently/shared/billing/FloentlyAccessModels.kt

# The first real content slices must stay bound to existing backend endpoints.
grep -q "/api/v1/cards/deck" apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnOverviewService.swift
grep -q "/api/v1/cards/deck" apps/android/shared/src/main/java/com/floently/shared/learn/LearnOverviewService.kt
grep -q "/api/v1/yki-practice/overview" apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnOverviewService.swift
grep -q "/api/v1/professional/overview" apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnOverviewService.swift
grep -q "/api/v1/yki-practice/overview" apps/android/shared/src/main/java/com/floently/shared/learn/LearnOverviewService.kt
grep -q "/api/v1/professional/overview" apps/android/shared/src/main/java/com/floently/shared/learn/LearnOverviewService.kt

# Legacy generic card components are not allowed back into the clean Home.
if grep -q "FloentlyCard" apps/ios/FloentlyLearn/FloentlyLearn/Home/KieliValmisHomeView.swift; then
  echo "Legacy generic FloentlyCard found in clean iOS Home." >&2
  exit 1
fi

if grep -q "FloentlyCard" apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/home/KieliValmisHomeScreen.kt; then
  echo "Legacy generic FloentlyCard found in clean Android Home." >&2
  exit 1
fi

# Customer surfaces must not expose implementation-status copy.
if grep -R -nE "Native build foundation|being connected to the existing backend|backend adapter|architecture boundary"   apps/ios/FloentlyLearn/FloentlyLearn   apps/android/FloentlyLearn/app/src/main/java/com/floently/learn; then
  echo "Engineering copy leaked into a customer-facing KieliValmis source." >&2
  exit 1
fi

echo "KieliValmis clean rebuild static verification: PASS"


# Canonical learner-event authority must remain learning.v1 and camelCase.
grep -q '"schemaVersion"' shared/api-contracts/learn/v1/learning-event.schema.json
grep -q '"learning.v1"' shared/api-contracts/learn/v1/learning-event.schema.json
if grep -q '"schema_version"' shared/api-contracts/learn/v1/learning-event.schema.json; then
  echo "Legacy incompatible snake-case learner event contract returned." >&2
  exit 1
fi

grep -q 'schemaVersion: String = "learning.v1"' apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnContractsV1.swift
grep -q 'schemaVersion: String = "learning.v1"' apps/android/shared/src/main/java/com/floently/shared/learn/LearnContractsV1.kt

# Native event sync transport exists but is not automatically invoked during bootstrap.
grep -q '/api/v1/learning/events' apps/ios/FloentlyShared/Sources/FloentlyShared/Learn/LearnEventSyncService.swift
grep -q '/api/v1/learning/events' apps/android/shared/src/main/java/com/floently/shared/learn/LearnEventSyncService.kt
