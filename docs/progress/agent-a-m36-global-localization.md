# Agent A M36 Global Localization architecture

M36 source:
- `docs/progress/M36_FASTEST_PATH_CROSS_CUTTING_PARITY.md`
- `docs/handover/m36/AGENT_A_GLOBAL_LOCALIZATION.md`

Patch:
- Replaced the old `learnStrings()` architecture that only selected Finnish, Swedish, and English.
- Generated LearnStrings entries for all 20 enabled `LearnLanguage` values from the existing native `LearnCopy` catalog.
- Added a reusable `LearnLocalizationLayout` provider that switches Compose layout direction using `LearnLanguage.direction`.
- Wired the signed-in Learn shell into the localization layout provider so Arabic, Persian, and Urdu can render RTL layout direction.
- Left the remaining screen-by-screen hardcoded string conversion as the next Agent A pass because the intake shows many feature screens still contain direct `Text("...")` strings.

Verification:
- Android debug build before commit.
- Android debug build before push.
- Device QA still required for all 20 languages and RTL screens.
