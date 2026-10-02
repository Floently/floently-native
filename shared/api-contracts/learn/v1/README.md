# KieliValmis Learn Contracts V1

These schemas begin the versioned semantic boundary for the clean native rebuild.

Current V1 definitions:

- `activity-definition.schema.json`
- `learning-session-plan.schema.json`
- `learning-event.schema.json`

Rules:

- mobile/web UI is not the source of learner truth;
- learner events are idempotent;
- session plans are bounded delivery contracts, not copies of the canonical bank;
- backend-owned card/YKI material remains authoritative;
- schemas are language-neutral and may generate Swift/Kotlin/TypeScript/Python models later.

Planned next V1 contracts:

- `EvaluationResultV1`
- `LearnerSnapshotV1`
- `ProgressProjectionV1`
- `RecommendationV1`
- `SpeechTurnV1`
