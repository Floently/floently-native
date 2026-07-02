# M20 Section-Lock Visual Rebuild

Updated: 2026-07-02

## Why M20 exists

M19 was a broad visual sweep. It improved some surfaces but did not recreate the old app exactly enough.

The user confirmed these blocking problems:

- Everyday Finnish from the drawer incorrectly opened Cards directly.
- Everyday Finnish must be its own hub with Roleplay, Cards, Interview, and Speech recording.
- Professional Finnish must be its own hub with Roleplay, Cards, Interview, Speech recording, and Report writing.
- Home alignment still does not match the original screenshots.
- Language selection does not translate all visible UI.
- Dark/light mode toggle is not functional.
- Settings interactions are mostly static.
- Professional Finnish does not match the old app flow or layout.
- YKI Practice and YKI Mock Exam do not match the screenshots or logical flow.
- Future work must lock one section before moving to the next.

## New workflow

1. Pick one section.
2. Compare its screenshots in order.
3. Combine scroll-continuation screenshots as one screen.
4. Match the old code logic.
5. Rebuild visual layout and function flow.
6. Install and compare only that section.
7. User confirms accepted before the next section.

## M20-A correction

M20-A repairs the flow foundation:

- Adds Everyday Finnish as a real destination.
- Drawer Everyday Finnish now opens the Everyday hub, not Cards.
- Home Everyday Finnish now opens the Everyday hub.
- Everyday hub contains Roleplay, Cards, Interview, Speech recording.
- Professional Finnish hub now represents Roleplay, Cards, Interview, Speech recording, Report writing.
- Professional hub can route to Roleplay and Cards immediately; incomplete flows are represented but not falsely claimed complete.

## Next section to lock

M20-B should lock Home from IMG_0390-IMG_0392 before moving to YKI, Professional, Settings, or Cards.
