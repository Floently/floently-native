# KieliValmis Learn Contract Authority

**Status:** GOVERNING CONTRACT RULE  
**Effective:** 2026-10-03

## Canonical authority

The existing KieliValmis cross-feature contract in:

`Floently/floently-finnish/packages/core/schemas/learning.ts`

is the canonical semantic authority for the following concepts:

- `TaskDescriptor`
- `TaskCapability`
- `TaskResult`
- `LearnerEvent`
- `SkillEvidence`
- `PracticeSessionManifest`

Its contract version is:

`learning.v1`

The next-generation native applications must not create an incompatible parallel learner-event or task contract.

## Native mirror rule

`Floently/floently-native/shared/api-contracts/learn/v1/` contains language-neutral JSON Schema mirrors of the canonical `learning.v1` concepts required by the native line.

Swift and Kotlin models must preserve the canonical wire field names and meanings.

Native implementation types may use platform naming conventions internally, but serialization/deserialization must remain compatible with `learning.v1`.

## Ownership rule

The authenticated backend user id is the learner ownership key.

Clients may construct events containing `learnerId` because the canonical contract requires the field, but the server must verify it against the authenticated user and must never trust a client-selected learner id.

## Compatibility aliases

Earlier clean-rebuild scaffolding used the filenames:

- `activity-definition.schema.json`
- `learning-session-plan.schema.json`

Those filenames are retained temporarily as compatibility aliases, but their contents now mirror:

- `TaskDescriptor`
- `PracticeSessionManifest`

No code may use the earlier incompatible integer `schema_version` / snake-case event wire format.

## Contract change rule

Any future semantic change must be reconciled with the canonical `learning.v1` authority first.

Do not independently evolve Swift, Kotlin, TypeScript, Python, and JSON Schema versions of learner truth.
