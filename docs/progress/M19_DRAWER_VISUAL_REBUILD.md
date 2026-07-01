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

## M19-B3 language picker correction

Screenshot source:

- IMG_0490

Correction:

- Tapping the Language card now opens a floating language picker popover above the card.
- The picker follows the original drawer language list pattern: Suomi, Svenska, Русский, Eesti, Українська, العربية, English.
- The Language card no longer hides the selector by routing directly away to Settings.
- Added bottom drawer padding so lower drawer panels can scroll above the Android gesture bar.

## M19-B5 functional drawer correction

Device feedback:

- Language picker opened but was not scrollable.
- Languages were not connected to the app language state.
- Bottom Theme / Session area was partly hidden by the Android gesture bar.

Correction:

- Drawer language picker now lists all enabled Learn languages and scrolls.
- Selecting a language updates the same persisted Learn language state used by Settings.
- Home and signed-in feature drawers both receive selectedLanguage/onLanguageSelected.
- Bottom Theme and Session panels were changed to full-width visible cards with extra bottom padding.
