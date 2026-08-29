# Iloadi No-Loss Parity Registry

This document is a permanent migration gate. A legacy capability may not disappear simply because the Iloadi implementation is newer.

## Active product scope — Read only

**Until Iloadi Read reaches the Read completion gate, Learn and Create are frozen.**

- Do not rebuild, redesign, refactor, migrate, or otherwise change Learn while Read is active.
- Do not build or expose new Create functionality while Read is active.
- Existing Learn is an authoritative product that will be imported later as a separate migration phase.
- Learn may be inspected only to preserve future compatibility such as identity or entitlement contracts; inspection is not permission to change it.
- Create remains deferred.
- All implementation waves until the Read completion gate must be Read-only unless the user explicitly changes this rule.

The Wave 006 Learn surface work is **not an approved migration direction** and must not be treated as completed Learn parity. The Read-only mobile package restores Learn/Create to their pre-parity placeholders and contains no new Learn package.

## Source pins

- Learn source (reference only while frozen): `Floently/floently-finnish@f2e131e9fee59aa42e0f07ca4f4d0804e627bf23`
- Read + extension source: `Floently/flowreader@41500a7d69bd72800bcc8f0d956af4e030fe7ef1`
- Design authority: `Floently/floently-native@6607ea26cd89995db6c89fb6b91547f1a6f69792`

## Status semantics

- **PORTED** = Iloadi native surface exists and the existing authoritative behavior/backend is connected.
- **PARTIAL** = surface exists and legacy contract is preserved, but one or more behaviors remain.
- **PENDING** = inventoried and protected from deletion, but not yet ported.

A visible button alone never counts as PORTED.

## Learn parity — frozen / deferred

No Learn capability is currently claimed as ported into the Read mobile build. The existing Learn application remains authoritative and untouched. Its migration inventory will be reopened only after Read completion.

## Read parity

### Core mobile surfaces

Protected/active Read work includes:

- local text import
- local text-file import
- PDF/DOCX/EPUB/HTML backend upload
- local library
- cloud library
- reader
- progress persistence/sync
- real voice catalog
- real server-side TTS
- playback controls and speed
- voice selection
- Read account/session
- web-page browser
- signed-in website browsing where the website supports embedded mobile browsing
- live DOM extraction without replacing the website UI
- current-page reading
- selected-text reading
- save extracted web page to library
- reading modes/settings
- accessibility behavior
- summaries and existing Read intelligence functions
- existing web/extension Read feature parity

### Authenticated-page architecture

Mobile Read may use a browser/WebView surface to display a third-party website. This does **not** turn the Iloadi application into a WebView wrapper: Iloadi navigation, library, player, account and product UI remain native; only the user-selected web page is rendered by the browser surface.

The website must remain intact and interactive. Read extracts from the live rendered DOM only after the user invokes Read/Selection, and overlays native Read controls around the browser rather than replacing the website.

Some identity providers intentionally reject embedded user agents. Do not weaken security controls or spoof login flows to bypass those policies. Where an embedded login is provider-blocked, use an approved external/native authentication handoff and return flow.

### Web/desktop architecture

A normal Read website cannot safely inject itself into arbitrary third-party authenticated pages because of browser same-origin rules, CSP and frame restrictions. Authenticated arbitrary-site reading on desktop therefore remains **extension-first**: the user signs into the target page in their normal browser and the Read extension runs on that already-authenticated page.

Do not implement a fake web browser by iframe/proxying third-party signed-in sessions through the Read web app.

### Remaining parity gate

Before Read is declared complete, verify at minimum:

- password/shared Read session and session restoration
- Google/provider login fallback where applicable
- public URL browsing
- normal password form login inside mobile browser
- authenticated page remains usable after Read extraction
- selection reading
- SPA/dynamic page re-extraction
- real `/api/voices/unified` voice catalog
- real `/api/tts/prerender` audio
- player pause/resume/previous/next/speed/voice
- local and cloud library flows
- upload/import edge cases
- account/logout
- entitlement enforcement
- FunctionGuide/FunctionSearch or approved mobile equivalents
- language selection/detection
- accessibility settings
- extension parity and authenticated-page smoke tests
- Read web public/document flows remain operational

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

## Interaction and design gate

Read UI follows the frozen Iloadi design system. Interactive product cards must actually respond to input. Floating cards use consistent elevation/shadow, alignment and spacing; glow is used deliberately for primary/focus surfaces rather than indiscriminately. Static decorative surfaces must not masquerade as buttons.

## Completion gate

Before declaring Read parity complete:

1. Every Read capability must be PORTED or explicitly approved as a deliberate retirement.
2. Each PORTED behavior must have a device/API parity test.
3. Existing backend/source-of-truth contracts must not be duplicated without an architecture decision.
4. Design must conform to `docs/design/` rather than copying legacy visual defects.
5. Chrome extension impact must be reviewed for every shared Read behavior change.
6. Learn and Create remain frozen until this gate is explicitly passed.