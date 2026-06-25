# Floently Native Workspace

This workspace contains the native rebuild of Floently mobile products.

## Products

- Floently Learn
- Floently Read
- Floently Create

## Native stacks

iOS:

- Swift
- SwiftUI
- UIKit where needed
- AVFoundation / Metal / StoreKit 2 depending on product

Android:

- Kotlin
- Jetpack Compose
- Android Views where needed
- Media3 / WorkManager / Billing depending on product

## Legacy rule

Do not build native work inside `/root/floently-finnish`.

The old Expo app remains fallback until native apps are tested, released, and stable.
