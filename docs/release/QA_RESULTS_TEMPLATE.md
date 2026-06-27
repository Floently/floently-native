# QA results template

Copy this file into a dated local note or issue before each QA pass. Do not put private credentials, signing values, or personal test data in this file.

## Session

- Date:
- Tester:
- Device/emulator:
- Android version:
- Build commit:
- Build type: debug / release / signed release
- Backend environment:

## Build verification

- `./scripts/nativectl signing-status`: PASS / FAIL / NOT CONFIGURED
- `./scripts/nativectl build android-learn`: PASS / FAIL
- `./scripts/nativectl build android-learn-release`: PASS / FAIL
- `./scripts/nativectl build android-learn-bundle`: PASS / FAIL

Notes:

## Authentication

- Fresh launch: PASS / FAIL
- Login: PASS / FAIL
- Invalid credentials error: PASS / FAIL
- Session persistence: PASS / FAIL
- Sign out: PASS / FAIL
- Access failure state: PASS / FAIL

Notes:

## Suite selector

- Product separation: PASS / FAIL
- Learn access check: PASS / FAIL
- Read access check: PASS / FAIL
- Create access check: PASS / FAIL
- Checkout boundary: PASS / FAIL
- Back navigation: PASS / FAIL

Notes:

## Learn

- Home: PASS / FAIL
- YKI: PASS / FAIL
- Professional Finnish: PASS / FAIL
- Roleplay: PASS / FAIL
- Roleplay anti-repetition: PASS / FAIL
- Cards: PASS / FAIL
- Progress: PASS / FAIL
- Account/sign-out: PASS / FAIL
- No Read/Create tabs inside Learn: PASS / FAIL

Notes:

## Read

- Open Read: PASS / FAIL
- Text input: PASS / FAIL
- URL input: PASS / FAIL
- Language detection: PASS / FAIL
- Read automatically: PASS / FAIL
- Generation: PASS / FAIL
- Reader session: PASS / FAIL
- Saved library: PASS / FAIL

Notes:

## Create Studio

- Open Create Studio: PASS / FAIL
- Hooks: PASS / FAIL
- Captions: PASS / FAIL
- Summarize: PASS / FAIL
- Repurpose pack: PASS / FAIL
- Brand brain: PASS / FAIL
- Generation: PASS / FAIL
- Result display: PASS / FAIL
- Projects/history: PASS / FAIL

Notes:

## Payments and entitlements

- Billing dashboard: PASS / FAIL
- Checkout URL creation: PASS / FAIL
- Return/cancel flow: PASS / FAIL
- Entitlement refresh: PASS / FAIL
- Learn/Read/Create separation: PASS / FAIL

Notes:

## Offline and error states

- Offline launch: PASS / FAIL
- Backend unreachable state: PASS / FAIL
- Retry behavior: PASS / FAIL
- Navigation after error: PASS / FAIL
- Recovery after network returns: PASS / FAIL

Notes:

## Store readiness

- Privacy policy URL: PASS / FAIL
- Support email: PASS / FAIL
- Account deletion path: PASS / FAIL
- Data safety answers: PASS / FAIL
- AI/payment disclosures: PASS / FAIL
- Screenshots captured: PASS / FAIL
- Release notes final: PASS / FAIL

Notes:

## Blocking issues

1.
2.
3.

## Decision

Choose one:

- Continue to store preparation.
- Fix blockers first.
- Repeat QA after backend/signing/store updates.
