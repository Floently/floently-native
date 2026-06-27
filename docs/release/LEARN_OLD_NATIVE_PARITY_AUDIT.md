# Learn old-to-native parity audit

## Decision

Floently Learn must become at least 150% of the previous Learn app before public release.

Build success is not enough. Native Learn must preserve the previous product capabilities, recover the small UX systems from the old app, and improve them with a cleaner native implementation.

## Audit basis

Compared against the old `galapoto/floently-finnish` app and the current native `galapoto/floently-native` app.

Old app evidence paths:

- `apps/client/scripts/check-i18n-completeness.mjs`
- `apps/client/features/i18n/languages.ts`
- `apps/client/state/AppShell.tsx`
- `apps/client/config/navigation/AppShell_sidebar_sections.ts`
- `packages/ui/components/UtilityDrawer.tsx`
- `packages/ui/components/SmartHintPopup.tsx`
- `apps/backend/app/runtime/card_i18n_overlay_runtime.py`
- `apps/backend/app/runtime/card_i18n_option_cache_runtime.py`
- `apps/backend/card_bank/new_bank/new_launch_bank/reports/overlay_language_counts.json`
- `apps/backend/card_bank/new_bank/new_launch_bank/overlay_bank/published/...`

Current native evidence paths:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnSignedInShell.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/navigation/LearnFeatureDestination.kt`

## Current native state

Native Learn currently has:

- Learn home
- YKI practice
- Professional Finnish
- Roleplay
- Cards
- Progress
- Account

Recent M16 patches polished these screens and builds pass. But native Learn is not yet a full 150% replacement for the old app.

## P0 augment-build list before Learn release

### 1. Native i18n system

Old app had a full i18n registry and completeness script. Native does not yet prove equivalent i18n.

Required:

- Add native language registry.
- Preserve all old language codes: `fi`, `sv`, `ru`, `et`, `uk`, `ar`, `en`, `so`, `fa`, `zh`, `sq`, `ku`, `vi`, `bn`, `tr`, `tl`, `th`, `ne`, `es`, `ur`.
- Preserve enabled languages: Finnish, Swedish, English.
- Preserve review/in-progress metadata for the other languages.
- Preserve RTL metadata for Arabic, Persian, and Urdu.
- Add native translation helper/provider.
- Translate current native Learn UI at least for Finnish, Swedish, and English before release.
- Add an i18n completeness check for native release.

150% target:

- Language changes apply immediately.
- Missing strings are caught before release.
- RTL metadata is ready even if only enabled later.

### 2. Native language selector and utility drawer

Old app had a UtilityDrawer with profile, email, language selector, theme toggle, clock, session action, avatar/photo fallback, and structured navigation.

Required:

- Add native drawer or premium menu.
- Add language selector.
- Add profile/email area.
- Add sign-out.
- Add access/status surface.
- Add navigation to Learn areas.
- Add theme control if supported.

150% target:

- Cleaner than old drawer, with native gestures and better mobile spacing.

### 3. Card language overlays

Old backend/app had card i18n overlay runtime, option cache runtime, overlay reports, and published overlay JSON files.

Required:

- Wire native Cards to overlay/i18n backend contract.
- Surface helper-language overlays in card content.
- Preserve all old overlay languages.
- Add helper-language badge in card sessions.
- Allow language switch without losing the card session.

### 4. YKI exam and YKI practice parity

Old app had separate YKI practice and YKI exam routes, level-band presets, and links from YKI to speaking practice.

Required:

- Add native YKI hub with practice and exam modes.
- Add level bands: A1-A2, B1-B2, C1-C2.
- Add YKI exam route/screen equivalent.
- Add route from YKI to speaking/roleplay practice.
- Add persisted YKI session restore/clear handling.

150% target:

- Better mobile exam layout and clearer completion feedback than old.

### 5. Speaking and Roleplay parity

Old app supported speaking presets with level band, surface, profession, scenario id, locked profession, entry mode, and context label.

Required:

- Add native speaking/roleplay hub.
- Preserve level-band presets.
- Add profession contexts: general, nurse, doctor, practical nurse.
- Add direct scenario open from Professional Finnish.
- Add workplace/interview entry modes.
- Verify backend dynamic generation and anti-repetition.

150% target:

- Native conversation should feel smoother and less repetitive than old.

### 6. Professional Finnish parity

Old app could open professional roleplay by profession, scenario id, and entry mode. It also used entitlement-aware active context.

Required:

- Add profession/context selection.
- Add workplace and interview modes.
- Add direct handoff from Professional scenario to Roleplay.
- Add entitlement-aware access checks.
- Verify backend content and progress.

### 7. Placement before paywall

Old app ran placement for signed-in users before billing when placement was incomplete.

Required:

- Add native placement assessment or native entry to existing placement backend.
- Persist placement completion.
- Use placement result to personalize Learn home and recommendations.

### 8. Billing, subscriptions, entitlements, and preview paths

Old app had billing route, subscription hydration, entitlement checks, preview paths, internal all-access, Learn/YKI/Professional access, and profession entitlements.

Required:

- Verify live Learn entitlement refresh.
- Add or verify native Learn access/billing screen.
- Preserve Learn, YKI, Professional, and profession access logic.
- Keep Read/Create access separate.

### 9. Settings and Help parity

Old app had SettingsRoute and HelpRoute.

Required:

- Add native Settings.
- Add Help/support.
- Include language, theme, account, access, support, privacy, and app info.

### 10. Navigation persistence and session restore

Old app persisted navigation state, learning sessions, YKI exam sessions, and YKI practice sessions. It handled corrupted, outdated, and offline restore states.

Required:

- Persist current Learn destination.
- Persist active YKI, Cards, and safe Roleplay session state.
- Restore or safely clear outdated state.
- Add offline/error states.

### 11. Progress parity

Old ProgressRoute could navigate to learning, speaking, and YKI.

Required:

- Verify backend progress dashboard.
- Add progress-card navigation to the relevant Learn areas.
- Verify all Learn activities update progress.

### 12. Smart hints and contextual help

Old app had SmartHintPopup.

Required:

- Add native contextual hint component.
- Use it in YKI, Cards, Roleplay, and Professional Finnish where helpful.
- Persist dismissed hints where useful.

### 13. Runtime diagnostics

Old app logged screen transitions and routing decisions.

Required:

- Add lightweight QA diagnostics for screen transitions and recoverable errors.
- Keep user-facing errors simple.

## Recommended build order

### M17-A Native i18n foundation

- Add language registry.
- Add translation helper/provider.
- Add language persistence.
- Add language selector.
- Translate current Learn screens for Finnish, Swedish, and English.
- Preserve metadata for all 20 languages.

### M17-B Card overlay/i18n integration

- Add card helper-language support.
- Wire overlay service/backend contract.
- Add overlay badge and fallback states.

### M17-C Native utility drawer, settings, and help

- Add premium native drawer/menu.
- Add language/theme/account/support/access controls.

### M17-D YKI exam and session persistence parity

- Add YKI hub with exam/practice modes.
- Add level-band support.
- Add persisted session restore.

### M17-E Speaking and Professional parity

- Add profession/context routing.
- Add Professional-to-Roleplay handoff.
- Add workplace/interview modes.

### M17-F Placement and progress guidance

- Add placement before paywall.
- Improve progress into a guidance dashboard.

### M17-G Final Learn parity QA

- Run old-vs-native checklist.
- Run i18n completeness check.
- Run backend QA.
- Run real-device QA.
- Prepare signed release bundle.

## Release decision

Do not submit Learn publicly until the P0 augment-build list is implemented or explicitly signed off as postponed. Because the target is 150% of the old app, the default is to implement, not postpone.
