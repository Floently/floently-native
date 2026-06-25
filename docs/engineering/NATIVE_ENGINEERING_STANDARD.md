# Floently Native Engineering Standard

## Non-negotiable rule

Floently native apps must be built as long-term maintainable native products, not quick ports.

No final production module may be accepted if it is only "enough to compile".

## Required standard

Native implementation must follow:

- clear module boundaries
- separation of UI, state, domain, networking, storage, and platform services
- secure session/token handling
- stable API contracts
- typed models for backend responses
- accessible UI components
- predictable loading, error, retry, and empty states
- testable business logic
- build reproducibility
- no hidden manual steps
- documented rollback path
- no stale generated files committed
- no old Expo app mutation during native rebuild

## Android direction

Android must use:

- Kotlin
- Jetpack Compose
- clear feature modules over time
- ViewModel/state-holder layer before production flow wiring
- repository/service abstraction for backend APIs
- Android Keystore for sensitive local session material
- Gradle convention or shared build configuration for common standards
- Java/Kotlin toolchain alignment

## iOS direction

iOS must use:

- Swift
- SwiftUI
- Keychain for sensitive session material
- Observable state models or modern Swift concurrency patterns
- typed API clients
- feature-separated views/services/models
- Xcode-ready project structure

## Smoke-build exception

Temporary smoke-build screens are allowed only to validate toolchain and build environment.

Smoke-build code must not be described as production-ready.
Smoke-build code must be replaced by production architecture before release.
