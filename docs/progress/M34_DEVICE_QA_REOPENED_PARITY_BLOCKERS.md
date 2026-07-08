# M34 Device QA Reopened Parity Blockers

Status: ACTIVE

M33 build passed, but device QA shows Android is still not old-app parity.

## Agent E blockers

1. Language/i18n is not 100%.
   - Every selected language must translate every visible screen from first app entry to final screens.
   - Use old app translation catalog. Do not manually retranslate.
   - Cards overlays/problem-report UI must also translate.
   - Drawer language must update all pages.

2. Dark/light mode is not fully working.
   - Theme toggle must immediately update shell and screens.
   - Light and dark mode must match old app behavior.

3. Settings/account/billing links must work.
   - Profile picture/gallery flow.
   - External links.
   - Billing checkout/plan.
   - Account deletion/support/privacy/terms.

## Agent B blockers

Roleplay is still not old-app functional/UI parity.

Required:
- Roleplay must start naturally/automatically when opened.
- AI speaker begins the scenario.
- Mic tap starts recording.
- Mic tap again stops recording and sends.
- Start/stop mic sound feedback must happen.
- Live transcript appears for user and AI.
- AI speaks automatically after user sends.
- No separate listen button.
- Exactly 5 user turns, then AI gives a conclusive final response.
- UI must match attached roleplay/mic screenshots.
- YKI roleplay route appears missing; add/fix it even if not obvious in current native app.

## Agent A blockers

Cards improved but still need full language overlay parity:
- Overlay/report UI must translate when language changes.
- Card question/options/buttons must translate correctly.
- Backend card bank overlays must be represented in UI.

## Agent D blockers

YKI roleplay/practice/mock must be audited:
- YKI roleplay appears missing.
- YKI practice/mock banks must be connected.
- A1-A2, B1-B2, C1-C2 must work.
- UI and materials must match old app expectations.

## Release rule

Android cannot freeze until device QA confirms:
- language 100%
- theme 100%
- roleplay UI/function 100%
- cards overlay/backend language behavior
- YKI roleplay/practice/mock parity
