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

### Web/desktop authenticated-page architecture

A normal Read page still cannot inject itself into an arbitrary third-party page
that is already authenticated in the person's local browser. Same-origin rules,
CSP, frame restrictions and cookie isolation remain authoritative.

Read therefore has **two explicit desktop authenticated-page modes**. They solve
different session-ownership problems and neither may silently impersonate the
other.

#### 1. Existing browser + Read extension

The extension remains a protected compatibility path. The person signs into the
target site in their normal browser, and the Read extension operates on that
already-authenticated local page under its declared extension permissions.

Use this mode when the intended source is specifically the person's existing
local-browser tab/session or when extension parity requires it. The migration
must not delete the extension, rename its technical storage contracts, or assume
that Browser V2 automatically inherits the person's local cookies.

#### 2. Read Browser V2 + isolated remote Chromium

The next-generation Read web app may also provide its own authenticated Browser
V2 session. Browser V2 is **not** an iframe of the third-party page and is not a
proxy that steals/replays the person's local browser cookies. It allocates an
isolated remote Chromium session behind the authenticated Browser V2 service and
renders that session's page into Read through the owner-authorized display
transport.

For Browser V2:

- the remote Chromium-rendered page remains the primary visible source surface;
- the person authenticates inside that isolated browser when a site requires it;
- DOM extraction is private semantic input and must never replace the page with
  extracted text;
- highlighting, Read From Here and scrolling use revision-fenced source anchors;
- the Read app owns one authenticated control/display channel; public CDP,
  public VNC and credential-bearing display URLs are forbidden;
- Browser V2 session/display tickets are transport credentials and must never be
  exposed as document identity, analytics fields or ordinary URLs;
- one app-owned WebPlaybackSession remains the logical media owner above hidden
  TTS assets;
- navigation/reconnect/page revision changes must fail closed against stale
  extraction, input, highlight and speech work.

The extension and Browser V2 may coexist indefinitely. Choosing Browser V2 does
not retire extension parity, and preserving extension parity does not prohibit
the isolated Browser V2 product surface.

#### Prohibited shortcuts

Do not implement authenticated arbitrary-site reading by:

- embedding third-party signed-in pages in a normal iframe;
- copying local-browser cookies into Read;
- exposing a public CDP/VNC endpoint;
- proxying a user's authenticated local session through an untrusted web relay;
- replacing the rendered page with extracted Reader text and calling that a
  browser.

Those shortcuts remain prohibited even though secure Browser V2 is now an
approved architecture.

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
- extension parity and existing-local-browser authenticated-page smoke tests
- Browser V2 isolated-session/page-preservation/reconnect smoke tests
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