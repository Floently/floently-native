# Iloadi Screen Blueprints — UI 1.0.0

These are implementation blueprints for the **known product scope**. Unknown future features must go through the same design-first gate before coding.

All dimensions are logical pt/dp. Safe-area insets are added by the platform.

## A. Shared shell

### A01 — Splash

Purpose: immediate branded entry while session state resolves.

Structure:

- full canvas background `color.canvas`;
- Iloadi wordmark centered horizontally;
- wordmark visual width: 116, height determined by final vector ratio;
- vertical position: 46% of usable height;
- optional small loading indicator 32 below wordmark only when startup exceeds ~500 ms;
- no buttons;
- no gradients that obscure the wordmark.

### A02 — Welcome / authentication entry

Phone layout:

- horizontal padding: 20;
- top brand mark: 28 below safe area;
- mark width: 104;
- hero begins: 64 below mark;
- headline: `display`, max 3 lines;
- supporting copy: 16 below headline, max 4 lines;
- flexible spacer;
- auth action stack anchored toward lower content area, 24 above safe-area button zone;
- primary button: 52;
- secondary/provider buttons: 52;
- gap between buttons: 12;
- legal copy: 16 below buttons, caption, max width 340.

### A03 — Sign-in form

- standard top bar with Back;
- form max width: 560 tablet / full phone width;
- title begins 32 below bar;
- fields stack with 16 gap;
- submit button 24 below last field;
- forgot/recovery tertiary action 8 below relevant credential field;
- error summary appears above first field when form-level.

### A04 — Paywall / entitlement

- dismiss/back target: top-left 48;
- product title + benefit statement top section;
- plan cards use selectable rows/panels, not three oversized marketing cards;
- plan row min height 76;
- selected row: 2 px brand border + brandTint;
- CTA fixed or naturally anchored at bottom: 52, 20 side margins;
- restore purchases tertiary action 12 below CTA;
- terms/privacy caption below.

### A05 — Account / profile

- top bar: title `Account`;
- avatar block: 72 avatar, name/title beside or below based width;
- settings grouped into list sections;
- setting row: min 56;
- leading icon 24, title 16, optional value, trailing chevron/control;
- destructive sign-out/delete actions isolated in final section.

### A06 — Offline / service unavailable

- durable status banner at top when partial app remains usable;
- full empty-state treatment only when task cannot proceed;
- one recovery primary action; diagnostics/help tertiary.

## B. Iloadi Home

### B01 — Home

Global bottom nav visible.

Structure:

```text
safe area
┌──────────────────────────────┐
│ Iloadi wordmark      avatar  │ 64
├──────────────────────────────┤
│ Good evening / context       │
│ What do you want to do?      │ h1
│                              │
│ CONTINUE                     │
│ ┌──────────────────────────┐ │
│ │ current activity panel   │ │ 184
│ └──────────────────────────┘ │
│                              │
│ YOUR ILOADI                  │
│ ┌──────────────────────────┐ │
│ │ Learn product panel      │ │ 152
│ └──────────────────────────┘ │
│ 12                           │
│ ┌──────────────────────────┐ │
│ │ Read product panel       │ │ 152
│ └──────────────────────────┘ │
│ 12                           │
│ ┌──────────────────────────┐ │
│ │ Create product panel     │ │ 152
│ └──────────────────────────┘ │
└──────────────────────────────┘
│ Home   Learn   Read  Create  │ 72 + safe
└──────────────────────────────┘
```

Rules:

- horizontal margin: 20;
- top bar: 64;
- hero/context section: 32 top spacing;
- continue section appears only when meaningful recent activity exists;
- product panels are aligned full-width; no mismatched card widths;
- product status is a small label, not a floating badge cluster;
- no diagnostics/backend controls on customer home.

## C. Read

### C01 — Read root

Global nav visible; Read selected.

- product top bar: `Read` left, avatar right;
- hero title: 32 below bar, h1;
- subtitle: 8 below, body secondary;
- continue panel: 32 below hero, 184 high if active document;
- section `Library`: 32 below continue;
- show at most 3 recent document rows;
- `See all` is a tertiary action in section header;
- floating Import action 56×56 anchored 20 right / 12 above nav when mini player absent;
- if mini player present, FAB is not shown on this screen; Import lives as a top/section action to avoid collision.

### C02 — Library

- top bar: `Library`, search icon and filter icon max two actions;
- search field expands inline below top bar when activated;
- filter chip row: 12 below search/title area;
- document list starts 20 below filters;
- row height >=84;
- floating Import action 56×56;
- mini player sits above global nav when audio exists;
- when both mini player and add action are needed, add action moves to top bar instead of stacking floating controls.

### C03 — Import source chooser

Presented as bottom sheet from Read root/library.

Sheet content:

- title: `Add to Read`;
- supporting copy: one line/short paragraph;
- source rows, each 64 high:
  1. Files — PDF, EPUB, DOCX, TXT/Markdown;
  2. Paste text;
  3. Web link;
  4. Cloud source when available;
- each row: 40 icon tile + 12 gap + title/subtitle + trailing chevron;
- cancel is system sheet dismissal, not a full violet button.

### C04 — Paste text

- standard top bar: `Paste text`;
- title field 52;
- content textarea fills flexible body; min 220;
- character/word metadata caption under text area;
- primary `Add to Read` button 52 anchored above safe area/keyboard-aware;
- secondary save-to-cloud choice is a switch/option, not another equal CTA.

### C05 — File import / upload progress

- top bar: `Adding document`;
- central file identity: icon/thumbnail 64, filename h3, metadata below;
- progress block 32 below file identity;
- state title + progress bar + explanatory text;
- steps presented vertically with status icons:
  - Uploading
  - Extracting text
  - Preparing first pages
  - Preparing audio when requested
- cancel tertiary action while cancellation is safe;
- retry primary action only on failure;
- first-pages-first rule: as soon as readable content is ready, primary action becomes `Start reading` even if audio continues processing.

### C06 — Reader

Immersive: global bottom nav hidden.

```text
safe area
┌──────────────────────────────┐
│ back   truncated title  more │ 56
│ progress ──────────────────  │ 2–4
├──────────────────────────────┤
│                              │
│   document text              │
│   max width 680              │
│   phone margins 24           │
│                              │
│                              │
│                              │
│ mini player / reading tools  │ 76
└──────────────────────────────┘
safe area
```

Reader measurements:

- toolbar 56;
- progress 2 default / 4 when interacted with;
- phone content horizontal padding: 24;
- tablet reader max width: 680 centered;
- content top padding: 28;
- paragraph gap: 18;
- default reader type: 20/32;
- selection/highlight controls appear contextually, never permanently cluttering canvas;
- single tap toggles chrome only if accessible alternative remains;
- text focus/highlight for audio must not dim the entire document so aggressively that surrounding text becomes unreadable.

### C07 — Reader appearance sheet

Bottom sheet:

- text size segmented/stepper control;
- line spacing option;
- theme: Iloadi dark / soft dark / light if later supported;
- default font remains Onest;
- preferences rows min 56;
- live preview block 96–120 high;
- `Done` only if required by platform interaction; otherwise changes apply immediately.

### C08 — Mini player

Appears above global nav on Read root/library, or at bottom of Reader without global nav.

- height 72;
- content margins 20;
- play target 48;
- progress 2 top edge;
- tapping body opens full player;
- swipe-away is not the primary dismissal pattern; close/stop available through full player/menu.

### C09 — Full audio player

Immersive modal/screen.

- top bar: close/down action 48, `Now listening`, more 48;
- document/source artwork abstraction: 160×200 max on phone or 156 square if no cover;
- title h2 centered/left depending text length;
- progress scrubber 4, 24 below metadata;
- time labels 12 below scrubber;
- controls row 28 below:
  - back 48
  - play 64
  - forward 48
- secondary tools row 24 below: speed, voice, timer/bookmark as available; each >=48 target;
- bottom safe-area padding >=20.

### C10 — Read search in document

- replaces Reader top toolbar with focused search field 48;
- results count and previous/next 48 controls;
- matches highlighted with accessible contrast;
- keyboard-aware bottom inset;
- exiting search restores reader location exactly.

### C11 — Read failure/retry

For failed upload/extraction/audio:

- keep successful artifacts usable;
- error banner/panel explains which stage failed;
- primary action: Retry failed stage;
- secondary: Continue with available content;
- tertiary: Remove document / report problem where applicable.

No forever spinner.

## D. Learn

### D01 — Learn root

Global nav visible; Learn selected.

- top bar: Learn + avatar;
- progress summary panel 152/184;
- `Continue learning` is primary activity;
- path choices displayed as two aligned product/path panels:
  - YKI
  - Professional
- combined access is represented by entitlement/status copy, not a separate visual universe;
- recent progress/next recommended activity below.

### D02 — Learn path overview

- top bar with path name/back;
- progress ring/bar region max 112 high;
- module list uses rows/sections rather than independent flashy cards;
- module row min 72;
- completed state: icon + text, not color alone;
- locked state explains access reason.

### D03 — Card/session screen

Immersive: global nav hidden.

- top bar: exit 48, progress center, options 48;
- prompt/content region begins 24 below bar;
- central learning card/prompt has max width 680 and avoids unnecessary nested containers;
- answer/action region anchored toward lower screen;
- main response controls >=52;
- feedback state uses success/danger semantics plus text/icon;
- next action occupies one primary position consistently.

### D04 — Speaking/roleplay

- immersive session;
- scenario title and context top;
- conversation transcript/body center;
- microphone action: 64 central control, reachable thumb zone;
- clear listening/processing/speaking state text;
- waveform/animation is secondary to state copy;
- cancel/end controls separated from microphone.

### D05 — Learn progress

- top bar `Progress`;
- summary metrics use a small number of aligned stat panels, max 2 columns phone;
- trend/history uses chart or timeline with legible labels;
- achievements secondary; no badge-wall-first design.

## E. Create

### E01 — Create root / initial shell

Global nav visible; Create selected.

- top bar: Create + avatar;
- hero h1 + short positioning copy;
- if product not yet enabled: one strong waitlist/coming-soon state, no fake dashboard controls;
- if project shell enabled: projects section below, floating New project action only when creation is functional.

### E02 — Project dashboard

- top bar `Projects`, search/filter actions;
- project rows/cards use one consistent representation;
- recommended phone pattern: 2-column preview grid only when media thumbnails are meaningful; otherwise rows;
- create action 56 floating or top action depending mini-state conflicts;
- empty state uses one clear `Create project` CTA.

### E03 — Future editor shell

Full editor details are **not frozen** until the Create feature model/timeline requirements are specified. The design gate explicitly forbids implementing the full editor before its own complete blueprint exists.

The shared shell rules already reserve:

- 56 top toolbar;
- 48 min tool targets;
- bottom/side adaptive tool trays;
- safe-area compliance;
- design-token-only typography/color/spacing.

## F. Cross-product system states

### F01 — Loading

- use skeleton matching final geometry when content shape is known;
- spinner only for small indeterminate actions;
- preserve layout to prevent shifting.

### F02 — Empty

- one clear reason;
- one primary recovery/create action;
- optional tertiary help;
- max content width 320.

### F03 — Error

- actionable error copy;
- retry/alternative path;
- preserve local content where possible;
- destructive reset is never the first recommendation.

### F04 — Permission request

- pre-permission education only when the benefit is not obvious;
- one focused illustration/icon + title + body + CTA;
- system permission prompt follows immediately after explicit user action;
- denied state explains Settings path without nagging.

### F05 — Update required / maintenance

- centered constrained state;
- app remains branded but quiet;
- one primary action where actionable;
- support/status tertiary if needed.

## G. Screen-design acceptance checklist

Before any screen is coded, confirm:

- screen ID exists here or in a newer blueprint document;
- top/bottom navigation mode is defined;
- horizontal margins are defined;
- vertical rhythm is defined;
- component names are mapped to `ILOADI_COMPONENT_SPEC.md`;
- primary action is unambiguous;
- loading/empty/error/disabled/offline states are defined;
- keyboard behaviour is defined for input screens;
- safe-area behaviour is defined;
- phone/tablet adaptation is defined where relevant;
- accessibility behaviour is defined;
- reduced motion is defined where animation exists.

If any item is missing, implementation is blocked until design is updated.
