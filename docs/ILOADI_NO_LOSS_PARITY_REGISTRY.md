# Iloadi No-Loss Parity Registry

This document is a permanent migration gate. A legacy capability may not disappear simply because the Iloadi implementation is newer.

## Source pins

- Learn source: `Floently/floently-finnish@f2e131e9fee59aa42e0f07ca4f4d0804e627bf23`
- Read + extension source: `Floently/flowreader@41500a7d69bd72800bcc8f0d956af4e030fe7ef1`
- Design authority: `Floently/floently-native@6607ea26cd89995db6c89fb6b91547f1a6f69792`

## Status semantics

- **PORTED** = Iloadi native surface exists and the existing authoritative behavior/backend is connected.
- **PARTIAL** = surface exists and legacy contract is preserved, but one or more behaviors remain.
- **PENDING** = inventoried and protected from deletion, but not yet ported.

A visible button alone never counts as PORTED.

## Learn parity

| Capability | Legacy source | Iloadi target | Wave 006 |
|---|---|---|---|
| Adaptive cards/decks | `features/cards`, `packages/core/api/cards.ts` | `/learn/cards` | PORTED |
| YKI guided practice | `features/yki-practice` | `/learn/yki-practice` | PORTED |
| YKI exam overview/mock/start | `features/yki-exam`, `features/exam` | `/learn/yki-exam` | PARTIAL |
| YKI runtime/review/results/certificate | `app/yki-exam/*` | YKI exam flow | PENDING behavior |
| Speaking lab | `features/speaking` | `/learn/speaking` | PARTIAL |
| Recorded response | `RecordedResponseScreen.tsx` | `/learn/recorded-response` | PARTIAL |
| Roleplay conversation | `RoleplayConversationScreen.tsx`, `api/roleplay.ts` | `/learn/roleplay` | PORTED text-turn behavior; voice loop remains |
| Professional Finnish | `features/professional` | `/learn/professional` | PORTED |
| Workplace incident lab | `WorkplaceIncidentLabScreen.tsx` | `/learn/professional` | PORTED |
| Personal phrase bank | `PersonalPhraseBankScreen.tsx` | `/learn/phrase-bank` | PORTED |
| Revision vault | `RevisionVaultScreen.tsx` | `/learn/revision-vault` | PORTED |
| Confidence tracker | `ConfidenceTrackerScreen.tsx` | `/learn/confidence` | PORTED |
| YKI planner | `YkiPlannerScreen.tsx` | `/learn/planner` | PORTED |
| Progress / learning system | `app/progress`, `api/learning.ts` | `/learn/progress` | PORTED |
| Onboarding intent/profession/frequency/plan | `app/onboarding/*` | `/learn/onboarding` | PARTIAL |
| Auth / Google / password flows | `features/auth` | shared Iloadi account | PARTIAL |
| Learn subscription/store billing | `features/billing` | shared Iloadi access | PARTIAL; live IDs must be preserved |
| Settings | `app/settings` | `/learn/settings` | PARTIAL |
| Organization entry | `app/for-organizations` | `/learn/organizations` | PARTIAL |

## Read parity

Already represented in the Iloadi mobile path: local text/file import, backend document upload, local library, cloud library, reader, progress sync, player, summary boundary, auto-read setting.

Protected remaining behavior includes: full voice picker, language selector/auto-detection, FunctionGuide/FunctionSearch, analytics, account/logout, production entitlement flow, import edge cases, automatic post-upload generation, approved circular progress, and complete accessibility behavior validation.

## Chrome extension parity

The following are protected compatibility behavior and must remain unless an explicit migration is approved:

- Manifest V3
- floating in-page reader
- selected-text reading
- auth bridge
- entitlement policy
- theme support
- fallback to full Read app
- frame tracking
- existing storage keys such as `flowReader.auth.*`
- extension permissions and backend contracts
- existing brand asset lineage

Branding may move to Iloadi Read without renaming technical storage keys or breaking existing sessions.

## Completion gate

Before declaring Learn or Read parity complete:

1. Every row must be PORTED or explicitly approved as a deliberate retirement.
2. Each PORTED row must have a device/API parity test.
3. Existing backend/source-of-truth contracts must not be duplicated without an architecture decision.
4. Design must conform to `docs/design/` rather than copying legacy visuals.
5. Chrome extension impact must be reviewed for every shared Read behavior change.
