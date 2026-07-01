# M19-B Drawer Visual Rebuild

Updated: 2026-07-01

## Screenshot source

- IMG_0393: main drawer open
- IMG_0489-IMG_0493: drawer language/session continuation captures

## User feedback

The first device smoke test showed the native drawer was visually very different from the original app.

## Correction in M19-B

The drawer was rebuilt to match the screenshot structure:

- large right-side panel taking most of the screen width
- dark left backdrop showing the underlying page
- blue profile header
- white circular avatar with initial
- close circle inside blue header
- brown/gold daily streak card
- uppercase spaced section labels
- icon-square route rows without full-width card borders
- language card near the bottom
- theme/session panels at the bottom
- Home now uses the same shared drawer as other signed-in screens

## YKI naming

The drawer now visually separates:

- YKI Practice
- YKI Mock Exam

The current native navigation still routes both to the existing YKI practice destination. A dedicated YKI Mock Exam route/screen must be added in the M19-E YKI Mock Exam rebuild.
