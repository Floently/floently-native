# KieliValmis Native Home Blueprint — V1

**Status:** FIRST IMPLEMENTATION BLUEPRINT  
**Effective:** 2026-10-02

## 1. Purpose

The first native Home proves the new visual and information architecture without copying the Expo Home screen.

It demonstrates:

- strong alignment;
- controlled semantic cards;
- section rhythm;
- truthful empty/non-personalized states;
- a native SwiftUI implementation;
- a native Compose implementation.

It does not yet claim real progress or learner personalization.

## 2. Screen structure

```text
safe area
|
|-- top identity row / profile action
|
|-- large title: KieliValmis
|   supporting line
|
|-- Continue hero
|
|-- Pathways
|     |-- YKI preparation
|     |-- Work in Finland
|
|-- Practice by skill
|     |-- Speaking
|     |-- Listening
|     |-- Reading
|     |-- Writing
|
|-- Review / learner state
      |-- truthful neutral state until event/progress API is connected
```

## 3. Layout

Screen horizontal inset:

- standard phone: 20
- large phone: 24
- tablet: 32

Top content spacing after safe area: 16–24.

Screen title:

- 34/40, weight 800.

Title to supporting copy: 8.

Supporting copy to Continue hero: 32.

Section title to content: 12–16.

Section-to-section: 32.

Bottom content inset: at least 32 plus safe area/navigation.

## 4. Continue hero

- full width
- min height 184
- radius 32
- padding 24
- brand-tinted tonal treatment, not saturated violet wallpaper
- eyebrow: "Continue"
- title: "Start a learning session" until a real resumable session exists
- copy: explains that KieliValmis will choose from the learner's available pathways
- one primary action

When a real session is available, this card becomes data-driven.

## 5. Pathway cards

Initial paths:

- YKI preparation
- Work in Finland / Professional Finnish

These are product capabilities, not fabricated personalization.

Each card:

- min height 160
- radius 24
- padding 20
- icon tile 44
- title
- short capability description
- no fake percentage

Everyday Finnish may be introduced as its own pathway when the final information architecture is frozen; the new app is not required to mirror legacy route grouping.

## 6. Skill cards

Initial skill grid:

- Speaking
- Listening
- Reading
- Writing

Each card:

- min height 132
- radius 24
- padding 16–20
- one icon
- title
- one concise description/status

No mastery score until learner-event projections are connected.

## 7. Review state

Before backend learner-event integration, use a neutral card/row:

- title: "Review"
- body: "Your due reviews will appear here after your learning history is connected."

This is intentionally truthful.

When the event/projection architecture lands, this surface may show real due count, weak areas, or Revision Vault entry.

## 8. Interaction states

Every tappable surface must define:

- default;
- pressed;
- disabled if applicable;
- loading where action triggers async work;
- VoiceOver/TalkBack label;
- keyboard/focus semantics where relevant.

## 9. Motion

First implementation:

- native press feedback;
- 240–320 ms section/screen transitions when navigation arrives;
- no decorative continuous animation;
- reduced-motion safe.

## 10. Acceptance

The V1 Home implementation is accepted when:

- both iOS and Android use the same documented dimensions;
- no raw arbitrary geometry is introduced in the screen;
- every card maps to a semantic card family;
- no learner-specific fake data is shown;
- the old Expo app is untouched;
- no store/TestFlight build is required to land the source.
