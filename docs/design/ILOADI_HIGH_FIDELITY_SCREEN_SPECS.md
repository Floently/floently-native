# Iloadi High-Fidelity Priority Screen Specs

Target reference width: 390 logical pt/dp phone. All dimensions are logical units and adapt through the established breakpoints.

These are implementation acceptance references. UI code must not substitute local geometry.

## A. Iloadi Home

### Structure
- safe-area top inset
- horizontal content inset: 20
- top utility row: 48 high
  - left: contextual greeting, `type.small`
  - right: profile icon button 48×48
- hero begins 28 below utility row
- hero statement max 3 lines, `type.h1` 34/40
- supporting sentence begins 12 below, max width ~330
- product field begins 32 below

### Product field
Not three identical cards.

Composition:
- Learn: full-width quiet surface, 104 high
- Read: full-width signature surface, 128 high, primary product emphasis for current build
- Create: full-width quiet surface, 104 high
- vertical gap: 12
- surface radius: 24
- internal padding: 20
- title: 22/28 700
- description: 14/20
- trailing arrow/action target: 48×48

Only Read may receive a low-opacity amethyst light field in the current product phase. Learn/Create remain ink/plum tonal surfaces.

### Resume object
- 32 gap after products
- section label: `type.title`
- resume row: 88 high
- no full card border; tonal surface + subtle separator
- left thumbnail/icon: 52×68 max
- text column begins 12 after thumbnail
- title 16/22 600, two-line max
- progress hairline: 3 high
- trailing overflow button: 48×48

### Global nav
- 72 visual height + bottom safe inset
- 4 equal cells
- active indicator max 20×3 or equivalent subtle mark
- no pill background around selected tab

## B. Read Home

### Header
- top row: 48 high
- title `Read`, 34/40 800
- search icon button 48×48 at trailing edge
- subtitle optional, 14/20 secondary

### Continue reading
- section starts 28 below header
- document feature surface: 148 high
- thumbnail/art: 72×96
- internal padding 20
- title 18/24 600
- source/metadata 12/16
- progress bar 3 high
- one primary action label; do not add multiple buttons

### Add action
- single `Add to Read` action beneath feature surface
- height 52
- full width
- violet fill only here if no stronger violet element is present
- icon 20, label gap 8

### Recent library
- starts 32 below add action
- content-first rows, each 80–88 high
- no boxed card per row
- separator line or tonal shift only
- three rows visible before global nav on standard phone where content permits

## C. Library

### Header
- 56 high
- title left, search + overflow on right

### Search
- 48 high, radius 14
- 16 gap below header

### Filters
- horizontal chips, 36 high
- All / PDF / EPUB / Text / Web (only types actually supported)
- 8 gap
- selected chip: brandTint + brandBright text; not solid violet unless needed for accessibility

### Document rows
- 88 minimum high
- left thumbnail 44×60
- text starts 12 after thumbnail
- title 16/22 600
- metadata 12/16
- optional progress line 3 high, max width aligned with text column
- trailing overflow 48×48
- separator aligned to text column, not screen edge

### Add
- 56×56 floating add button, bottom-right
- 20 right inset
- 12 above global nav
- used only in Library and nowhere else unless blueprint explicitly permits

## D. Import

Import opens as a bottom sheet over current context, not a new dashboard.

### Sheet
- top radius 32
- handle 36×4
- horizontal inset 20
- internal top 12 after handle
- title 28/34 700
- supporting copy 14/20

### Options
Four maximum primary rows:
1. Choose document
2. Paste text
3. Import from URL
4. Cloud/source connector (only when actually available)

Each row:
- 64 high
- icon well 40×40
- icon visual 20–22
- label 16/22 600
- supporting copy 12/16
- trailing chevron in 48×48 target
- 8 vertical gap or separators, not individual oversized cards

Bottom sheet height follows content, with safe-area bottom padding.

## E. Reader

### Immersive structure
- no global nav
- canvas `color.canvas`
- horizontal text inset 24 on standard phone

### Top reader bar
- 56 high + safe inset
- back target 48×48
- center document title 14/20 600, one line, subdued
- overflow target 48×48

### Progress
- 2 high hairline under top bar
- full available width minus 24/24
- active progress brand at low visual intensity

### Content
- begins 32 after progress
- default reader: Onest 20/32 400
- paragraphs separated by 20–24
- current sentence may receive:
  - primary text color;
  - a subtle left-side amethyst light or selective phrase color;
- nearby text: secondary text, never below readable contrast
- past/future text must not become illegible gray wallpaper

### Reader dock
- floating 76 high visual container
- left/right inset 20
- bottom 12 + safe inset
- radius 24
- tonal surface, minimal border
- controls on a strict horizontal grid:
  - previous/rewind 48
  - play 56 primary circle (64 in expanded/player mode)
  - next/forward 48
  - flexible spacer
  - speed 48
  - more 48
- dock disappears/recedes on scroll/read focus and returns on tap/interaction

AI tools such as summary are not permanent buttons in the reading viewport. They live in the More/Insight sheet.

## F. Full Player

### Top bar
- 56 + safe inset
- collapse/back 48
- centered title 14/20 600
- overflow 48

### Visual focus region
- begins 28 below top bar
- 240–280 high depending on device
- one of:
  - abstract waveform/reading pulse;
  - restrained document cover composition;
  - low-motion amethyst light field
- not a generic square album-art card

### Metadata
- title 22/28 700, center or left aligned consistently with waveform choice
- source/author 14/20 secondary
- max two title lines

### Timeline
- 24 top gap
- slider track 4 high, full content width
- time row 12/16 below, left/right aligned to slider ends

### Primary controls
- 28 gap below timeline
- centered row
- rewind 48
- play/pause 64
- forward 48
- 28–32 horizontal center gaps

### Secondary controls
- 28 below primary row
- equal 48 targets: speed / sleep / bookmark / queue
- icon 22–24
- short labels optional below at 12/16

Speed choices appear in a sheet; do not permanently show 0.75x/1x/1.25x/1.5x/2x chips on Player.

## Screen-family alignment rules

- all screen titles share one left edge;
- all content rows share the same text-column alignment within a screen family;
- all 48×48 icon targets use identical optical icon sizing;
- all primary buttons are 52 high and radius 16;
- no screen invents a new violet shade;
- no screen introduces a font family other than the approved Iloadi type system;
- no implementation may use arbitrary literal spacing outside token values without a documented exception;
- customer-facing screens contain no engineering/build/debug language.
