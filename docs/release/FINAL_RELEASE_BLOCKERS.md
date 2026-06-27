# Final release blockers

## Verified foundations

These items are no longer treated as blockers:

- Native suite shell with separate Learn, Read, and Create products.
- Product-specific access checks.
- Android debug build.
- Android release build.
- Android release bundle build.
- Optional release signing support without committing signing values.
- Read service wiring with fallback-safe repository.
- Create Studio service wiring with fallback-safe repository.
- Learn roleplay, cards, and progress service wiring with fallback-safe repositories.
- Store submission draft pack.
- Native manual QA checklist.

## True blockers before store submission

### 1. Live backend verification

Service boundaries are wired, but production-like backend behaviour must still be verified.

Check:

- Auth/session persistence.
- Product access for Learn, Read, and Create separately.
- Learn roleplay generation and anti-repetition.
- Learn cards scheduling/review behaviour.
- Learn progress sync and merge behaviour.
- Read upload/import, language detection, generation, reader session, and library.
- Create generation, project history, export, and share.
- Offline/fallback states.

### 2. Payment provider checkout and entitlements

Billing routes and entitlement boundaries are wired, but live provider behaviour must be verified.

Check:

- Checkout URL creation.
- Return/cancel flow.
- Product-specific entitlement refresh.
- Separate Learn, Read, and Create access.
- No accidental bundle access unless intentionally configured.

### 3. Local release signing

Signing support exists, but release signing values must be configured outside Git before Play upload.

Check:

- `apps/android/signing.properties` remains ignored.
- Keystore remains outside Git.
- `./scripts/nativectl signing-status` reports configured only on the signing machine.
- Signed release bundle can be generated locally.

### 4. Privacy, data safety, and store assets

The store pack is drafted, but final values must be filled after real QA.

Check:

- Public privacy policy URL.
- Support email.
- Account deletion path.
- Data retention statement.
- AI generation disclosure for roleplay and Create Studio.
- Payment provider disclosure.
- Play data-safety answers.
- Real-device screenshots.
- Final release notes.

### 5. Manual real-device QA

Build success is not enough for release.

Check the full checklist in `docs/release/NATIVE_MANUAL_QA_CHECKLIST.md` and record pass/fail notes before submission.

## Current release decision

The native app is ready for final QA preparation, but it is not ready for public store submission until all blockers above are closed.
