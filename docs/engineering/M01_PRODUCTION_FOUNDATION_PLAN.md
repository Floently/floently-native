# M01 Production Native Foundation Plan

## Purpose

M01 turns the native rebuild from a buildable smoke foundation into a maintainable production foundation.

This milestone must not add product features by shortcut. It must create the architecture that Learn, Read, and Create can safely grow on.

## Platform order

Android is built first on the Linux server because it can be compiled there.

iOS architecture remains part of the same milestone, but iOS compilation requires macOS/Xcode or Xcode Cloud.

## Product order

1. Shared native foundation
2. Learn foundation
3. Read foundation
4. Create shell later

## Android foundation goals

- keep `scripts/nativectl` as the single repo build interface
- remove temporary one-off build scripts
- keep generated Gradle, APK, log, and tool files out of Git
- introduce stable package/module boundaries
- keep authentication outside UI components
- keep backend API access behind typed services
- keep secure session storage behind an interface
- keep subscription/access logic separate from screen code

## iOS foundation goals

- mirror the same boundaries in Swift/SwiftUI
- keep Keychain storage behind a session-store abstraction
- keep API calls in shared service types
- keep product-specific shells separate
- prepare for Xcode project/workspace generation later

## Quality bar

No placeholder-quality final code. Temporary smoke code is allowed only when clearly marked and must be replaced before release.
