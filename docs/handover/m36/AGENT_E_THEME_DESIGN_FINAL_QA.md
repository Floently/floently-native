# Agent E M36 — Theme, Design System, Final QA

## Mission

Make the app visually match old app/web in dark and light mode, then run final QA.

## Critical known bug

`FloentlyDesign.kt` only has dark palettes and `MainActivity` uses plain `MaterialTheme`.

## Tasks

1. Create real global Floently theme provider.
2. Add dark and light palettes for Learn, Read, Create.
3. Make theme setting update app immediately.
4. Persist theme.
5. Centralize:
   - typography
   - spacing
   - radius
   - borders
   - shadows/elevation
   - button styles
   - card styles
   - chips
   - dialogs/overlays
6. Add consistent Floently logo use.
7. Verify public/auth/placement/Learn/cards/roleplay/YKI/progress/settings/account/billing in dark and light.
8. Build screenshot parity matrix.
9. Run debug build.
10. Run release build.
11. Install APK.
12. Device QA.
13. Push with `E:` prefix.

## Done only when

Dark/light parity and screenshot parity are accepted.
