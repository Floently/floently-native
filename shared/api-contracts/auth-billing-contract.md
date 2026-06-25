# Native Auth/Billing Contract

## Purpose

This contract defines what native Learn, Read, and Create clients need from the existing Floently backend.

The backend remains source of truth.

## Existing backend areas

Existing backend route map shows routers for:

- auth
- subscription
- payment
- devices
- cards
- card audio
- YKI
- voice
- roleplay

Native apps should reuse these where possible instead of creating duplicate backend logic.

## Native auth needs

Native clients need:

- email/password login
- account registration
- Google sign-in token exchange
- current session check
- logout/session invalidation
- device registration if device-limit logic is active

Expected native client storage:

- iOS: Keychain
- Android: EncryptedSharedPreferences or DataStore with encryption strategy

## Native billing/access needs

Native clients need access state for:

- yki_access
- professional_access
- combined_access
- read_access
- create_access later
- internal_all_access for testing

Native app must not decide entitlement permanently on device.

Device may cache entitlement briefly, but backend remains source of truth.

## Payment direction

For mobile release:

- iOS should use StoreKit 2 when subscriptions are native.
- Android should use Google Play Billing.
- RevenueCat can remain an option if already chosen for subscription aggregation.
- Backend must receive purchase/customer status and return normalized access.

## Shared response pattern

Native apps should expect backend responses to use the existing API pattern:

- ok
- data
- error
- meta

## Safety

Do not expose card bank or internal files directly.

Native apps consume APIs only.
