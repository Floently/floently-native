# M28-D Placeholder Reachability Gate

Status: ACTIVE

Purpose: verify whether `LearnFeaturePlaceholderScreen` is reachable from production navigation before Android production-freeze.

## Gate rule

Android cannot be production-frozen if any shipped Learn route opens a placeholder screen.

## Accepted outcomes

1. `LearnFeaturePlaceholderScreen` has no active production navigation reference and is removed or documented as unreachable dead code.
2. Any active reference is replaced with a production screen before freeze.

