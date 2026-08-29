# Iloadi Shared Component Specification — UI 1.0.0

This document defines the shared UI building blocks. Screens are not allowed to recreate these components with local measurements.

## 1. App top bar

### Standard top bar

- content height: 56
- safe area: platform-provided above the 56 content height
- horizontal padding: same as screen container
- left slot: 48×48
- center/title region: flexible
- right slot: 48×48 per action
- max right actions: 2; additional actions go into overflow
- title: `type.title`
- icon: 22–24 visual inside 48 target
- divider: none by default; 1 px `borderSoft` only when content hierarchy needs it

### Product landing top bar

- content height: 64
- left: product wordmark/title
- right: 40×40 avatar inside 48 target
- no back action on product roots

## 2. Global bottom navigation

Permanent destinations:

1. Home
2. Learn
3. Read
4. Create

Geometry:

- visual bar height: 72 + bottom safe area
- top border: 1 `borderSoft`
- item width: equal 25%
- icon box: 24
- icon-to-label gap: 4
- label: `type.micro`, sentence case labels permitted without uppercase
- selected indicator: 32×3, radius pill, `brand`
- active icon/text: `textPrimary` / `brandBright`
- inactive: `textTertiary`
- no large glowing pill behind the selected item

Reader, Learn session and Create editor may hide this bar.

## 3. Primary button

- height: 52
- width: fills parent by default; may fit content only in toolbars/sheets
- horizontal padding: 20
- radius: 16
- font: `type.smallStrong` or equivalent 15–16 / 600
- icon: 20–22
- icon gap: 8
- fill: `brand`
- label: `textOnBrand`

States:

- default: brand fill
- pressed: `brandDeep`, 160 ms
- focused: 2 px brandBright outer focus ring
- loading: spinner + preserved label width; button size cannot jump
- disabled: tonal surface; textDisabled; no bright brand

Rule: one primary button per local decision surface.

## 4. Secondary button

- height: 52
- radius: 16
- border: 1 `border`
- fill: surface1 or transparent depending context
- label: textPrimary
- pressed: surface2
- focus: brand focus ring

## 5. Tertiary/text button

- minimum touch target: 48 high
- visible text may occupy less than 48 but target may not
- padding horizontal: 12
- label: `smallStrong`
- no background by default

## 6. Icon button

- target: 48×48
- visual icon: 22–24
- radius: 14 when surfaced; no radius when transparent
- surfaced fill: surface1
- destructive variant uses danger icon, never brand violet

## 7. Floating import/add action

Allowed only for creation/import actions that remain relevant while browsing a collection.

- size: 56×56
- icon: 24
- radius: 18, not a circle
- fill: brand
- bottom offset: 12 above global nav/safe area
- right offset: screen container padding
- shadow: elevation.brand, restrained
- label-less by default after onboarding familiarity

Do not use multiple floating actions.

## 8. Text field

### Single-line

- control height: 52
- radius: 14
- horizontal padding: 16
- border: 1 border
- background: surface1
- text: body
- placeholder: textTertiary
- label above: smallStrong
- label-to-field gap: 8

States:

- focus: borderFocus + low brand tint
- error: danger border + error copy 12/16 below with 6 gap
- disabled: lower-contrast surface; textDisabled
- filled/read-only: surface2 if needed

### Multiline

- minimum height: 144
- content padding: 16
- line height follows body scale
- grows to screen-specific max before internal scrolling

## 9. Search field

- height: 48
- radius: 14
- leading search icon: 20
- clear action: 48 target / 20 visual
- placeholder: textTertiary
- background: surface1

## 10. Chips and filters

- standard height: 36
- min touch wrapper: 44 iOS / 48 Android where independent action
- horizontal padding: 12
- gap between chips: 8
- radius: pill
- font: caption / 600
- selected: brandTint + brandBright text + subtle border
- unselected: surface1 + textSecondary

Chips are not used as primary buttons.

## 11. Product panel

Used on Iloadi Home for Learn / Read / Create.

- width: full content width on phone
- standard height: 152
- featured/continue panel: 184
- radius: 24
- padding: 20
- icon tile: 44×44, radius 14
- title: h3
- body: small/body, max 2 lines where possible
- status/action anchor: bottom aligned
- only active/selected product uses brand-tonal treatment

Product panels are not generic dashboard cards; they are entry points to product spaces.

## 12. Continue panel

Used for current activity.

- min height: 172
- radius: 24
- padding: 20
- metadata top row: caption
- title: h3, max 2 lines
- progress bar: 4 high
- progress gap from copy: 20
- primary action aligned bottom-left
- optional source/status aligned bottom-right

## 13. Document row

Preferred Library pattern instead of oversized cards.

- min height: 84
- horizontal layout
- left source icon/thumbnail: 48×64 or 48×48 depending source
- content gap: 12
- title: title, max 2 lines
- metadata: caption, one line
- trailing menu button: 48 target
- divider from next row: 1 borderSoft aligned with text column
- row horizontal padding: 0 when inside screen list; 16 when inside a surface

Selected multi-select state uses brandTint background and check indicator.

## 14. Empty state

- max content width: 320
- centered vertically in available content region, not whole physical screen
- illustration/icon region: 72 max
- title: h3
- copy: small/body, centered, max 3 lines
- action: one primary 52 button, max width 280
- title-to-copy: 8
- copy-to-action: 24

## 15. Status banner

For offline, processing, sync, failure.

- min height: 48
- radius: 14
- padding: 12 horizontal / 10 vertical
- icon: 20
- copy: small
- optional action: tertiary label on trailing side
- use semantic color as icon/accent, not full saturated background

## 16. Bottom sheet

- top radius: 28
- background: surface2
- drag handle: 36×4; top 10
- internal horizontal padding: 24
- first content baseline: 24 below handle region
- bottom padding: max(24, safe-area inset)
- action stack gap: 12
- long sheets stop at 92% of screen height and scroll internally

## 17. Modal dialog

Phone:

- width: screen width minus 40 minimum
- max width: 420
- radius: 24
- padding: 24
- action placement: vertical stack when labels are long, otherwise horizontal only when both actions remain >= 120 wide

Tablet:

- max width: 480 standard / 560 complex form

## 18. Mini audio player

Read-specific but shared between Read Home, Library and document contexts.

- height: 72
- horizontal margin: screen container
- radius: 18
- thumbnail/source icon: 44
- title: smallStrong, one line
- secondary line: caption
- play/pause target: 48
- progress: 2 px at top edge
- sits 8 above global nav
- never overlaps the floating import action; when player exists, floating action moves above it or disappears per blueprint

## 19. Full player controls

- central play/pause: 64×64
- skip/back: 48×48
- center-to-center spacing: 24
- scrubber track: 4
- scrubber thumb: 16 visual, >= 44/48 touch region
- current/remaining time: caption
- speed/timer/voice controls: 44–48 high chips/buttons

## 20. Progress indicator

### Linear

- 4 high default
- radius pill
- background borderSoft
- fill brand

### Upload/processing

Must include text state in addition to bar:

- Preparing
- Uploading
- Extracting
- Ready to read
- Preparing audio
- Ready to listen
- Failed / Retry

Never show an indefinite spinner when measurable progress/state is available.

## 21. List section header

- height determined by content; minimum 32
- title: title or smallStrong depending hierarchy
- optional trailing action: tertiary 48 target
- section top spacing: 32
- title-to-first-row: 12

## 22. Avatar/account action

- visible avatar: 36×36 or 40×40
- touch target: 48×48
- fallback: initial/monogram in surface2 with brand-tint edge
- always top-right on product roots

## 23. Toast/snackbar

- max width phone: screen width minus 32
- min height: 48
- max 2 lines
- radius: 14
- background: elevated dark surface
- position: 12 above current bottom obstruction (nav/player/keyboard)
- action optional, tertiary
- error toasts do not replace durable inline error messages where user action is required

## 24. Skeletons

Skeletons match final component geometry. They may not introduce placeholder layouts that shift significantly when loaded.

- shimmer optional; reduced motion disables shimmer
- use surface2/surface3 tonal difference
- preserve exact row/card height where known

## 25. Component acceptance rule

A shared component is not ready for implementation until its:

- dimensions;
- padding;
- typography;
- colors;
- icon sizes;
- default/pressed/focused/disabled/loading/error states;
- accessibility label/role;
- motion behaviour;
- responsive behaviour

are specified.
