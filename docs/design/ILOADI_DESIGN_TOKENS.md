# Iloadi Design Tokens — UI 1.0.0

These values are design contracts. Implementation must consume them through a shared design-system package/resource layer rather than repeating raw values in screens.

## 1. Color

### Brand and canvas

| Token | Value | Use |
|---|---:|---|
| `color.canvas` | `#07060A` | app background |
| `color.canvasRaised` | `#0B0810` | raised full-screen region |
| `color.surface1` | `#0F0C14` | low elevation surface |
| `color.surface2` | `#15101D` | card/sheet surface |
| `color.surface3` | `#1B1427` | selected/strong surface |
| `color.brand` | `#8B5CFF` | Iloadi Violet; primary action/focus |
| `color.brandBright` | `#B998FF` | active icon, accent text, focus highlight |
| `color.brandDeep` | `#5A34CC` | pressed/tonal brand state |
| `color.brandTint` | `#24183A` | subtle brand-tonal background |

### Text

| Token | Value | Use |
|---|---:|---|
| `color.textPrimary` | `#F7F3FF` | principal text |
| `color.textSecondary` | `#B9B1C4` | explanatory text |
| `color.textTertiary` | `#81788D` | metadata / inactive labels |
| `color.textDisabled` | `#5E5669` | disabled text |
| `color.textOnBrand` | `#FFFFFF` | text/icons on brand fill |

### Lines and state

| Token | Value | Use |
|---|---:|---|
| `color.borderSoft` | `#241E2C` | quiet separators |
| `color.border` | `#30263A` | component borders |
| `color.borderFocus` | `#8B5CFF` | focused input/control |
| `color.success` | `#67D8A8` | success |
| `color.warning` | `#F3C66F` | warning |
| `color.danger` | `#FF718D` | destructive/error |
| `color.info` | `#6CB9FF` | informational |

### Color-use constraints

- Brand violet must not exceed roughly one dominant region per viewport except in onboarding/marketing moments.
- Body copy never uses brand violet.
- Destructive controls never use brand violet.
- Text state cannot rely on opacity alone below accessibility thresholds.
- Gradients are permitted only for hero/primary focus surfaces and never as general card wallpaper.

## 2. Typography

### Font family

`Onest`

Variable font weight range: 100–900. Product-supported weights for UI 1.0 are restricted to 400, 500, 600, 700 and 800 to keep rendering predictable across platforms.

### Type scale

| Token | Size | Line height | Weight | Letter spacing | Use |
|---|---:|---:|---:|---:|---|
| `type.display` | 42 | 46 | 800 | -1.2 | product/onboarding statement |
| `type.h1` | 34 | 40 | 800 | -0.8 | screen title |
| `type.h2` | 28 | 34 | 700 | -0.5 | major section |
| `type.h3` | 22 | 28 | 700 | -0.25 | card/task title |
| `type.title` | 18 | 24 | 600 | -0.1 | row/panel title |
| `type.body` | 16 | 24 | 400 | 0 | primary body |
| `type.bodyStrong` | 16 | 24 | 600 | 0 | emphasized body |
| `type.small` | 14 | 20 | 400 | 0 | secondary copy |
| `type.smallStrong` | 14 | 20 | 600 | 0 | secondary action/label |
| `type.caption` | 12 | 16 | 500 | 0.1 | metadata |
| `type.micro` | 11 | 14 | 600 | 1.0 | uppercase category/eyebrow |
| `type.readerDefault` | 20 | 32 | 400 | 0 | Read document content |
| `type.readerLarge` | 22 | 36 | 400 | 0 | accessibility reading size |
| `type.readerCompact` | 18 | 30 | 400 | 0 | compact reading size |

### Typography constraints

- No body text below 14 pt/dp except metadata/caption.
- No all-caps text beyond short micro labels.
- Screen titles use sentence case.
- Buttons use sentence case, not uppercase.
- Maximum normal body line length on tablet: approximately 68 characters.
- Reader content max width: 680 pt/dp equivalent.

## 3. Spacing

4-point base grid, 8-point primary rhythm.

| Token | Value |
|---|---:|
| `space.1` | 4 |
| `space.2` | 8 |
| `space.3` | 12 |
| `space.4` | 16 |
| `space.5` | 20 |
| `space.6` | 24 |
| `space.8` | 32 |
| `space.10` | 40 |
| `space.12` | 48 |
| `space.16` | 64 |

### Standard relationships

- icon to label: 8
- label to supporting copy: 4–8
- stacked fields: 16
- related controls: 12
- card internal padding: 20
- modal/sheet internal padding: 24
- section-to-section spacing: 32
- hero-to-first section: 40
- screen horizontal padding: 20 standard, 24 large phone, 32 tablet

No random 13, 15, 17, 19, 21, 27 etc. values in UI layout code.

## 4. Corner radius

| Token | Value | Use |
|---|---:|---|
| `radius.s` | 10 | small chips/compact controls |
| `radius.m` | 14 | fields/small buttons |
| `radius.l` | 16 | standard buttons |
| `radius.xl` | 24 | cards/panels |
| `radius.2xl` | 32 | large sheet/hero region |
| `radius.pill` | 999 | pills/chips only |

Avoid the over-rounded "everything is a capsule" look.

## 5. Control heights and touch geometry

| Component | Visual size | Minimum touch target |
|---|---:|---:|
| primary button | 52 high | 52 high |
| secondary button | 52 high | 52 high |
| compact button | 44 visual | 48 target |
| text input | 52 high | 52 high |
| search field | 48 high | 48 high |
| icon button | 48×48 | 48×48 |
| floating add action | 56×56 | 56×56 |
| navigation item | 56 minimum cell | 56 minimum cell |
| checkbox/radio visual | 20–22 | 48 target |
| switch row | 56 minimum | full row target |

## 6. Iconography

- standard icon visual box: 24×24
- compact visual box: 20×20
- icon stroke target: visually ~1.8–2.0
- primary play button icon: 24 inside a 64 control
- do not mix filled, outline and emoji-style glyph languages arbitrarily
- product glyphs must come from the same icon family or custom Iloadi set

## 7. Elevation and shadows

Dark UI uses tonal elevation first, shadow second.

| Token | Specification |
|---|---|
| `elevation.0` | none; canvas |
| `elevation.1` | surface1 + 1px soft border |
| `elevation.2` | surface2 + subtle shadow, 16 blur, 8 y, ~20% black |
| `elevation.3` | surface3 + shadow, 28 blur, 14 y, ~28% black |
| `elevation.brand` | brand-focus glow only; 28 blur, low opacity |

Permanent bright outer glows are prohibited.

## 8. Motion

| Token | Duration | Curve | Use |
|---|---:|---|---|
| `motion.instant` | 100 ms | ease-out | press feedback |
| `motion.fast` | 160 ms | ease-out | icon/control state |
| `motion.standard` | 240 ms | ease-in-out | panel transition |
| `motion.enter` | 320 ms | emphasized decel | screen/sheet entry |
| `motion.exit` | 220 ms | emphasized accel | screen/sheet exit |
| `motion.content` | 360 ms | gentle spring/standard | meaningful content transition |

Reduced-motion mode removes scale/translation-heavy transitions and keeps fades/state changes short.

## 9. Layout breakpoints

Design target sizes are expressed as logical pt/dp, not physical pixels.

- compact phone: `< 375` width
- standard phone: `375–429`
- large phone/fold inner narrow: `430–599`
- tablet: `>= 600`

On tablet:

- global content container max width: 960
- reading content max width: 680
- forms max width: 560
- modal max width: 560 unless editor/content task requires more

## 10. Safe-area rules

- never position critical controls under status bars, Dynamic Island, camera cutouts or gesture navigation areas;
- bottom controls include system safe-area inset in addition to their visual height;
- floating controls sit at least 12 above the safe-area boundary or global nav;
- full-bleed decorative backgrounds may extend into unsafe regions, interactive content may not.

## 11. Accessibility baseline sources

- Apple HIG accessibility: recommended default iOS controls 44×44 pt and attention to spacing/contrast.
- Android core quality: touch targets at least 48 dp and minimum contrast guidance.

These platform requirements are minimums; Iloadi may deliberately exceed them.
