# Play Store submission pack

## Current app identity

- App name: Floently
- Package name: `com.floently.app`
- Version name: `0.2.0`
- Version code: `2`
- Products in suite: Floently Learn, Floently Read, Floently Create Studio

## Product separation note

Floently Learn, Floently Read, and Floently Create Studio are presented as separate products inside the native Floently suite. Product access and payments must remain separate unless a bundle is intentionally introduced later.

## Store listing draft

### Short description

Learn Finnish, read smarter, and create content with Floently.

### Full description

Floently is a native language and productivity suite with separate tools for learning Finnish, reading documents, and creating content.

Floently Learn helps users practise Finnish with YKI-style exercises, professional Finnish, roleplay, cards, progress tracking, and account access.

Floently Read helps users import text, URLs, and documents, detect language, generate reading support, and continue reading from a saved library.

Floently Create Studio helps users create hooks, captions, summaries, repurposing packs, and brand-style outputs from direct tools.

Access to Learn, Read, and Create is checked separately. The app is designed as a native Android experience, not a WebView wrapper.

## Screenshot checklist

Capture screenshots after real-device QA from these states:

- Signed-out login screen
- Floently suite product selector
- Learn home
- Learn roleplay session
- Learn cards session
- Learn progress screen
- Read input/generation screen
- Read reader screen
- Create Studio tool selection
- Create Studio result screen
- Access/payment boundary screen

## Data safety preparation

Confirm before submission:

- Account/login data handled by existing backend auth.
- Product access and entitlement status handled by existing backend services.
- Payment checkout is delegated to the configured payment provider.
- User-generated Learn, Read, and Create content should be described honestly based on final backend storage behavior.
- No generated APK/AAB, keystore, signing property file, or local secret is committed to Git.

## Privacy policy placeholders to resolve

- Public privacy policy URL
- Support email
- Data retention summary
- Account deletion process
- Payment provider disclosure
- AI-generation disclosure for Learn roleplay and Create Studio

## Release notes draft

Initial native Floently Android foundation with separated Learn, Read, and Create Studio products, product-specific access checks, native Learn flows, native Read and Create MVP flows, payment boundary, backend service wiring, and release build readiness.
