# KieliValmis Learn Contract Mirrors

The canonical cross-feature learning contract already exists in:

`Floently/floently-finnish/packages/core/schemas/learning.ts`

and is versioned as:

`learning.v1`

This directory mirrors the canonical concepts needed by the clean native rebuild.

Current mirrors:

- `task-descriptor.schema.json`
- `task-result.schema.json`
- `learning-event.schema.json`
- `skill-evidence.schema.json`
- `practice-session-manifest.schema.json`

Temporary compatibility filenames:

- `activity-definition.schema.json` -> TaskDescriptor
- `learning-session-plan.schema.json` -> PracticeSessionManifest

Rules:

- do not invent a second learner-event wire format;
- server authentication owns learner identity verification;
- backend learner-event persistence is authoritative;
- mobile event outboxes are transport/retry state, not progress authority;
- backend-owned card/YKI content remains canonical;
- native Swift/Kotlin models must serialize compatibly with `learning.v1`.
