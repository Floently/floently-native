# Bundle-Only Native Build Rule

## Final rule

Floently native rebuild work must use bundles, not loose code patches.

Do not send random code patches.
Do not apply scattered edits directly.
Do not rewrite one old file at a time without a feature bundle.

Every build step must be delivered as a bundle.

## Bundle contents

Each bundle must include:

- manifest
- files
- apply script
- rollback notes
- build/test notes
- known gaps
- target app/product
- affected feature area

## Bundle destination

Bundles may only apply to:

- /root/floently-native

Bundles must not apply directly to:

- /root/floently-finnish

The old Expo/React Native app remains the fallback and reference implementation.

## Required bundle flow

1. Create bundle.
2. Inspect manifest.
3. Apply to isolated native workspace.
4. Run verification.
5. Build/test.
6. Only then continue to next bundle.

## Package location

Generated bundles/zips should be created under:

- /home/vitus/

unless explicitly requested otherwise.
