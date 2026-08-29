# Iloadi Visual Direction V2 — Quiet Editorial Intelligence

Status: DESIGN FREEZE CANDIDATE. No implementation should treat this as optional styling.

## 1. Product impression

Iloadi must feel calm, intelligent, premium and unmistakably editorial. The product should not look like a dashboard kit, crypto app, gaming UI, generic AI SaaS, or a collection of purple cards.

The signature is **ink + amethyst light + editorial typography + measured negative space**.

The interface should feel more like a beautifully typeset reading object that happens to be intelligent than a control panel that happens to contain text.

## 2. Visual hierarchy

### Canvas

- Base canvas: near-black ink, not pure black.
- Large areas remain quiet; avoid filling the screen with cards.
- Surface elevation is expressed mainly through tone and spacing rather than borders/glows.
- Brand violet is an event, not wallpaper.

### Accent behaviour

Iloadi Violet is reserved for:
- the primary action;
- the current navigation state;
- active reading position;
- meaningful AI insight/focus moments;
- selected/high-value state.

A standard viewport should normally contain only one dominant violet region.

### Signature light

A subtle amethyst light field may appear behind one hero/primary focus region. It must never become a repeated glowing blob behind every card.

## 3. Typography

Primary UI/reading family: Onest.

Typography carries more of the brand than decoration.

Rules:
- screen title is the dominant structural element;
- secondary copy uses generous line-height;
- hero text uses short editorial lines, not marketing paragraphs;
- document titles use restrained size and high contrast;
- body copy remains neutral, never violet;
- emphasis in Reader may use violet selectively for the active phrase/sentence, but never entire paragraphs.

## 4. Shapes

- Fewer containers.
- Panels only when a grouping needs a physical boundary.
- Standard cards use 24 radius only where justified.
- Rows, lists and text-first regions should often remain unboxed.
- Pills are restricted to filters/toggles/chips.
- Primary action is rectangular with softened corners, not a capsule.

## 5. Navigation

Global bottom navigation must be visually quiet and structurally aligned:

Home / Learn / Read / Create

- 4 equal cells.
- icon above or alongside short label depending on platform width.
- active state: subtle violet symbol/indicator, not a large highlighted capsule.
- inactive state: tertiary text/icon.
- bar visually separates from content using tonal elevation, not a thick border.

Reader and full-player are immersive modes and do not show global navigation.

## 6. Motion

Motion should feel controlled and physical, not decorative.

- page entry: content settles rather than flies;
- selection: 100–160 ms tonal/scale feedback;
- sheets: 320 ms emphasized deceleration;
- Reader focus transition: soft crossfade/position emphasis;
- player artwork/waveform motion is subtle and tied to playback state;
- no looping glow animations.

## 7. Home principle

Iloadi Home is not a dashboard of feature cards.

It is an editorial launch surface:
- greeting/context;
- one large question or intent statement;
- three product choices arranged as a composed visual field;
- current/resumable activity appears below as a quiet horizontal object;
- bottom navigation remains anchored and predictable.

## 8. Read principle

Read Home should feel like entering a library/studio, not a document management dashboard.

Priority:
1. continue current reading;
2. add something new;
3. recent library;
4. search/filter.

Avoid four equal action cards. Import is a single deliberate action; Library is content-first.

## 9. Reader principle

Reader is the visual heart of Iloadi.

The screen should become quieter than every other screen.

- near-black ink canvas;
- generous top and side breathing room;
- document title recedes;
- reading text becomes the main visual object;
- current sentence/phrase receives controlled emphasis;
- non-current text remains readable but lower contrast;
- progress becomes a hairline rather than a dashboard widget;
- playback controls float as a composed dock, not a standard toolbar;
- summarization/AI tools stay hidden until requested.

## 10. Player principle

Full Player should feel cinematic and intimate.

- title and author/source are restrained;
- waveform/artwork occupies the visual center;
- progress slider and time labels are aligned on one grid;
- 64 primary play control;
- skip/back/speed/queue/bookmark controls have equal touch geometry;
- speed options live in a sheet, not permanently as multiple buttons;
- background can contain one low-contrast amethyst light field derived from current content artwork or Iloadi Violet.

## 11. Quality bar

A screen fails review if it can plausibly be mistaken for:
- a generic Expo demo;
- a Material sample screen;
- a React Native UI kit;
- a fintech dashboard;
- an AI-generated card collage.

Every high-fidelity screen must pass three questions:
1. Is the hierarchy obvious in under two seconds?
2. Does every visible element earn its space?
3. Would removing the logo still leave a recognisable Iloadi visual language?
