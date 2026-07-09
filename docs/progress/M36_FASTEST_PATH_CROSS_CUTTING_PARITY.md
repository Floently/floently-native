# M36 Fastest Path: Cross-Cutting Parity

Status: ACTIVE

M35 feature agents largely implemented real work, but Android is still not 100% parity because the remaining blockers are cross-cutting architecture problems.

## Main finding

The remaining work is not one feature. It is shared parity across every screen:

- full i18n
- global theme
- design system
- navigation/web-to-app flow
- auth/account/payment
- hardcoded text cleanup
- route integration
- screenshot parity

## Critical blockers

1. `LearnLanguage` lists 20 languages, but `learnStrings()` only maps FI/SV/EN. All other languages fall back to English.
2. `MainActivity` uses plain `MaterialTheme`.
3. `FloentlyDesign.kt` only has dark palettes.
4. Agent C webentry screens exist but are isolated and not fully routed.
5. Many hardcoded strings remain in Auth, Suite, Read, Create, Roleplay, Cards, YKI, Professional, Progress, and webentry.
6. Payment/account flow is not verified end-to-end.
7. Device QA and screenshot parity are still not complete.

## New fastest-path agent split

### Agent A — Global Localization

Own:
- i18n catalog
- hardcoded text removal
- old translation catalog mapping
- RTL language handling
- Cards/overlays localization
- Auth/Suite/Read/Create/YKI/Roleplay/Profile localization requests

Goal:
Every visible word translates in all 20 languages.

### Agent B — Audio, Speech, Roleplay Runtime

Own:
- microphone start/stop
- sound effects
- waveform/ring animation
- SpeechRecognizer
- TextToSpeech
- AI auto-start/auto-reply
- live transcription
- PDF/Word export
- Roleplay/YKI roleplay runtime

Goal:
Roleplay behaves like the old app on device.

### Agent C — Navigation, Public Flow, Auth, Payment Routing

Own:
- Floently.com native gateway routing
- Learn landing routing
- onboarding/placement route
- auth route
- account route
- billing/payment/checkout route
- CTAs
- external/legal/support route
- webentry integration into shell

Goal:
Every route from Floently.com landing to payment and Learn app is reachable.

### Agent D — Material/Backend Parity

Own:
- backend verification
- cards material
- overlays material
- roleplay topics/materials
- YKI banks
- Progress data
- account/access/billing API status
- fallback guards

Goal:
No fake/fallback content appears as real production content.

### Agent E — Theme, Design System, Final QA

Own:
- global theme provider
- light/dark palettes
- typography
- spacing
- component system
- logo/branding
- animation tokens
- final screenshot parity matrix
- debug/release builds
- device install QA

Goal:
Every screen matches old app/web visually in dark and light mode.

## Final Android freeze gate

Android cannot freeze until:

- all 20 languages work
- RTL works
- dark/light works globally
- public landing/auth/placement/payment routes work
- cards backend + overlays work
- roleplay old flow works
- YKI practice/mock/roleplay works
- account/billing/settings work
- screenshot parity accepted
- debug and release builds pass
- installed APK passes device QA
