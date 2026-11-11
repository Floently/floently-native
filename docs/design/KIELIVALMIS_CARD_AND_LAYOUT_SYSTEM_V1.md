# KieliValmis Card and Layout System — V1

**Status:** DESIGN CONTRACT  
**Effective:** 2026-10-02  
**Applies to:** new native KieliValmis Learn UI

This document specializes the suite-wide Iloadi/Floently design tokens for KieliValmis learning surfaces.

## 1. Visual goal

KieliValmis should feel:

- native;
- calm;
- premium;
- spacious;
- precisely aligned;
- easy to scan;
- rich without becoming busy.

The visual reference principle is the disciplined geometry seen in strong native applications: clear section hierarchy, controlled cards, consistent dimensions, predictable alignment, restrained color, and meaningful negative space.

No external product is copied.

## 2. Base grid

All logical dimensions are pt on iOS and dp on Android.

Base grid: **4**  
Primary rhythm: **8**

Allowed spacing tokens:

| Token | Value |
|---|---:|
| XS | 4 |
| S | 8 |
| SM | 12 |
| M | 16 |
| ML | 20 |
| L | 24 |
| XL | 32 |
| 2XL | 40 |
| 3XL | 48 |
| 4XL | 64 |

Do not introduce arbitrary values such as 13, 15, 17, 19, 21, or 27 in screen layout without a documented design-system exception.

## 3. Screen containers

Phone horizontal content inset:

- compact/standard phone: **20**
- large phone: **24**
- tablet: **32**

Tablet content max width: **960**.

Focused forms max width: **560**.

Section-to-section vertical spacing: **32**.

Hero-to-first-section spacing: **40**.

Normal card-to-card spacing:

- same semantic group: **12**
- distinct cards in a grid/stack: **16**

## 4. Radius system

Use the suite token system:

| Token | Value | KieliValmis use |
|---|---:|---|
| S | 10 | compact chip |
| M | 14 | field / surfaced icon |
| L | 16 | standard button |
| XL | 24 | standard learning card |
| 2XL | 32 | hero / large focus surface |
| Pill | 999 | chips only |

Do not round every object into a capsule.

## 5. Touch/control geometry

| Component | Size |
|---|---:|
| iOS minimum independent target | 44 |
| Android minimum independent target | 48 |
| standard KieliValmis interactive height | 52 |
| icon button target | 48 x 48 |
| standard icon visual | 24 x 24 |
| compact icon visual | 20 x 20 |
| floating add/action | 56 x 56 |
| search field | 48 high |
| text field | 52 high |
| primary/secondary button | 52 high |

## 6. Card internal geometry

Standard card:

- radius: **24**
- internal padding: **20**
- internal vertical gap: **12**
- border: 1 logical px only where needed
- tonal elevation first; shadow second

Hero/Continue:

- radius: **32**
- padding: **24** on standard/large phones, **20** on compact phone
- minimum height: **184**
- title max: 2 lines
- one primary action
- progress/state indicator only when backed by real data

Compact row/list:

- minimum height: **72**
- leading visual/thumbnail: **40–48**
- leading-to-text gap: **12**
- trailing action target: **48**
- divider aligns to text column, not full screen edge

## 7. Semantic card family

### 7.1 Hero / Continue Card

Purpose: the single strongest next action.

Geometry:

- full content width
- min height: 184
- radius: 32
- padding: 24
- eyebrow/metadata at top
- title 22/28 or 28/34 depending screen
- supporting copy max 2 lines
- action/state anchored toward bottom
- optional progress line: 4 high

Only one Hero/Continue card may dominate a viewport.

### 7.2 Pathway Card

Purpose: Everyday / YKI / Professional pathway entry.

Phone grid:

- two-column when each column remains at least 156 wide;
- otherwise stacked/full-width.

Geometry:

- min height: 160
- radius: 24
- padding: 20
- icon tile: 44 x 44, radius 14
- title: 18/24 or 22/28
- metadata: 12/16 or 14/20
- optional real progress line: 4 high

### 7.3 Skill Card

Purpose: listening, speaking, reading, writing, vocabulary, grammar.

Geometry:

- min height: 132
- radius: 24
- padding: 16–20
- icon visual: 24
- title: 16/24 strong
- one concise status/action line

Use a 2-column grid on standard phones where readable.

### 7.4 Progress Card

Purpose: explainable progress summary, not decorative percentage.

Geometry:

- min height: 152
- radius: 24
- padding: 20
- primary metric only when real
- secondary evidence beneath
- trailing drill-down affordance optional

Never show fabricated readiness.

### 7.5 Activity Card

Purpose: one concrete learning activity.

Geometry:

- min height: 96
- radius: 24
- padding: 16–20
- leading activity icon: 40–44
- title + metadata
- optional duration/skill
- trailing 48 target

### 7.6 Recommendation Card

Purpose: evidence-backed next-step recommendation.

Geometry:

- min height: 120
- radius: 24
- padding: 20
- reason text is required ("why this")
- action routes to exact target when possible

No generic marketing copy disguised as personalization.

### 7.7 Mission / Scenario Card

Purpose: Professional Finnish mission or roleplay context.

Geometry:

- min height: 144
- radius: 24
- padding: 20
- profession/context label
- title
- skill mix metadata
- one continuation/start action

### 7.8 Review Card

Purpose: due review / Revision Vault / targeted correction.

Geometry:

- min height: 84 for compact form
- radius: 24 when carded; may use list-row form without card
- leading status icon
- title
- truthful due/evidence label

### 7.9 Achievement Card

Purpose: real milestone.

Geometry:

- min height: 120
- radius: 24
- padding: 20
- restrained celebratory visual
- reduced-motion safe

No achievement is emitted without a qualifying learner event.

## 8. Surface hierarchy

Preferred order:

1. canvas;
2. typography/section grouping;
3. row/list;
4. tonal region;
5. semantic card;
6. modal/bottom sheet.

Nested cards are prohibited unless a blueprint explicitly documents a reason.

## 9. Alignment rules

- every screen title uses the same container left edge;
- section headings use the same left edge as cards below them;
- card content follows a consistent 20-point internal left edge;
- repeated trailing actions align to the same 48-point target column;
- two-column grids use equal-width cells and a 12–16 gap;
- labels, progress bars, and buttons align to card content edges;
- arbitrary centering is avoided unless the whole state is intentionally centered.

## 10. Typography

Use the suite typography contract from `ILOADI_DESIGN_TOKENS.md`.

Key KieliValmis hierarchy:

- screen title: 34/40, 800
- section title: 22/28 or 28/34
- card title: 18/24 or 22/28
- body: 16/24
- supporting: 14/20
- metadata: 12/16

Onest remains the target UI font. Platform fallback during early scaffolding is temporary and must not become a product design decision.

## 11. Color

KieliValmis inherits the suite dark canvas and Iloadi Violet token system.

The brand color identifies:

- selected state;
- primary action;
- active focus;
- real progress;
- important learning state.

It is not a general card background.

Large regions remain near-black/plum.

## 12. Motion

Use shared motion tokens:

- press: 100–160 ms;
- standard transition: 240 ms;
- entry: 320 ms;
- meaningful content transition: ~360 ms.

Motion communicates continuity/state.

Reduced Motion removes scale/translation-heavy effects.

## 13. Card-content truth

A beautiful card may not become an excuse for fake data.

If real learner state is unavailable:

- show neutral pathway information;
- show a loading skeleton;
- show "Not synced" / "Unavailable";
- show an empty state;
- omit the metric.

Do not invent percentages, streaks, due counts, proficiency, or readiness to fill layout space.

## 14. Responsive rules

Compact phone (<375):

- stack pathway cards where two columns become cramped;
- hero padding may reduce from 24 to 20;
- never reduce touch targets.

Standard phone (375–429):

- two-column Pathway/Skill grids are allowed where each cell remains readable.

Large phone (430–599):

- 24 horizontal screen inset;
- two-column cards preferred for compact semantic groups.

Tablet (>=600):

- 32 screen inset;
- max content width 960;
- use multi-column layout only when reading order stays obvious.

## 15. Implementation rule

SwiftUI and Compose share this geometry but use native implementation idioms.

They do not share UI source code.

Any card added to production must map to one of the semantic families above or go through a design-system change.
