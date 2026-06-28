# Learn web source-of-truth parity map

## Decision

The completed web Learn build is the authoritative source of truth for the native Learn app.

This applies to **everything**, not only language:

- navigation and route structure
- home dashboard behavior
- settings and account behavior
- billing/access behavior
- YKI practice behavior
- Professional Finnish behavior
- roleplay behavior
- cards and card-bank behavior
- progress and streak behavior
- smart hints and utility drawer behavior
- i18n and RTL behavior
- support, privacy, terms, and account deletion surfaces
- backend contracts and release QA expectations

The native app may improve the experience, but it must not silently remove a completed web capability.

## Completed web reference commit

Use `galapoto/floently-finnish` at commit `f2e131e9fee59aa42e0f07ca4f4d0804e627bf23` as the primary web reference unless a later explicitly verified web release commit is provided.

The final web language enablement is specifically confirmed by commit `376015a84581c58f19efbf471e15379c7e77b496`, which changed the web language system so that all 20 languages are enabled and complete.

## Source-of-truth files by native area

| Native area | Completed web source files | Native target |
|---|---|---|
| Language metadata and i18n | `apps/client/features/i18n/languages.ts`, `apps/client/features/i18n/LanguageSelector.tsx`, `apps/client/scripts/check-i18n-completeness.mjs`, `docs/CG5/*_cg5.txt` | `learn/i18n/*`, `learn/settings/LearnSettingsScreen.kt`, all feature screens |
| App shell and global navigation | `apps/client/state/AppShell.tsx`, `packages/ui/components/UtilityDrawer.tsx` | `learn/app/LearnSignedInShell.kt`, native helper sheet/drawer |
| Home dashboard | `apps/client/state/HomeRoute.tsx`, `packages/ui/screens/HomeScreen.tsx`, `docs/CG5/*_cg5.txt` home keys | `learn/app/LearnHomeScreen.kt` |
| Learning route / cards hub | `apps/client/state/LearningRoute.tsx`, `apps/client/features/cards/components/CardBanksPanel.tsx`, `apps/client/features/cards/screens/CardPracticeScreen.tsx` | `learn/cards/*` |
| YKI | `apps/client/state/YkiPracticeRoute.tsx`, `apps/client/features/yki-exam/screens/YkiExamScreen.tsx` | `learn/yki/*` |
| Professional Finnish | `apps/client/state/ProfessionalRoute.tsx`, professional route components and CG5 professional/home keys | `learn/professional/*` |
| Speaking / roleplay | `apps/client/state/SpeakingRoute.tsx`, `apps/client/features/speaking/components/RoleplayScenarioHeader.tsx`, `apps/client/features/speaking/components/SessionCompletion.tsx` | `learn/roleplay/*` |
| Progress | `apps/client/state/ProgressRoute.tsx`, progress-related CG5 keys | `learn/progress/*` |
| Settings | `apps/client/state/SettingsRoute.tsx`, settings CG5 keys | `learn/settings/*`, `learn/account/*` |
| Billing and access | `apps/client/state/BillingRoute.tsx`, billing CG5 keys, backend access routes | `learn/account/*`, native billing/access surface |
| Smart hints | `packages/ui/components/SmartHintPopup.tsx`, home hint keys | native reusable smart hint component |
| Card overlays | `apps/backend/app/runtime/card_i18n_overlay_runtime.py`, `apps/backend/app/runtime/card_i18n_option_cache_runtime.py`, overlay bank/report files | native card overlay selector and display |
| Store/release/legal | `docs/release/legal/*`, `docs/release/STORE_METADATA_DRAFTS.md`, `docs/release/STORE_REVIEWER_NOTES_DRAFT.md` | native settings support/privacy/account deletion links and Play release docs |

## Native build rule

For each native feature patch, follow this order:

1. Inspect the matching completed web source files.
2. Identify web behavior, copy keys, states, routes, and backend contracts.
3. Build the native equivalent or a better native version.
4. If anything is intentionally postponed, gate it clearly and remove any UI promise.
5. Update this map or `LEARN_150_PARITY_AUDIT.md` when parity status changes.
6. Build debug and release before marking the patch verified.

## Current parity status

### Completed or mostly completed

- Native shell exists.
- Learn home exists and now consumes imported web CG5 copy.
- Language metadata now matches the final web language enablement: all 20 languages are enabled and complete.
- Settings language selector exists and all 20 languages are selectable.
- Final web CG5 copy has been imported into native `LearnCopy.kt` for Learn home/settings-level copy.
- YKI, Professional Finnish, Roleplay, Cards, Progress, and Account foundations exist and compile.

### Still incomplete for 150% web parity

- The selected language is not yet wired through every feature detail screen.
- Native screens still need route-level behavior parity against the web routes.
- Cards need web card-bank and overlay behavior parity.
- Utility drawer/smart hint behavior needs a native equivalent.
- Settings needs full web parity: appearance, audio, pathway/profile, support/privacy/terms/account deletion.
- Billing/access needs native Learn parity.
- Progress needs backend data parity and visual parity.
- Roleplay needs web completion/session parity plus OpenAI/anti-repetition verification.
- YKI and Professional Finnish need web route and backend contract parity.
- Real-device QA and signed release are still pending.

## Augment-build sequence from here

### M17-A4: Propagate selected-language copy into feature screens

Wire `LearnCopy` or feature-specific translated copy into:

- YKI
- Professional Finnish
- Roleplay
- Cards
- Progress
- Account

Goal: changing language in Settings visibly affects more than Home and Settings.

### M17-B: Cards web parity and overlays

Use the completed web card source as the source of truth:

- card banks panel
- card practice flow
- overlay language selector
- card i18n overlay backend contract
- review/session summary
- scheduling/progress behavior

### M17-C: Settings, support, privacy, billing/access parity

Use the completed web Settings and Billing routes as source of truth:

- settings groups
- profile/pathway/access status
- support/contact
- privacy policy
- terms of use
- account deletion
- billing/access management

### M17-D: Utility drawer and smart hint parity

Build native equivalents of:

- `UtilityDrawer`
- `SmartHintPopup`
- helpful hints
- route-aware quick actions

### M17-E: Web route behavior parity

Audit and finish behavior parity for:

- YKI route
- Professional route
- Speaking/Roleplay route
- Progress route
- Home route

### M17-F: Backend/device QA

Verify native against real backend and real device.

### M17-G: Signed release candidate

Configure signing outside Git, build signed AAB, prepare store submission artifacts.

## Release rule

Do not call Learn release-ready until every source-of-truth row is either:

- present in native and verified,
- better in native and verified, or
- explicitly gated/postponed with no user-facing promise.
