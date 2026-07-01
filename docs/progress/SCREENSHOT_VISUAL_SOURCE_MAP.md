# Screenshot Visual Source Map

Updated: 2026-07-01

## Purpose

The uploaded original-app screenshots are now the primary visual source of truth for M19 native Android visual parity.

Old source code remains useful for logic, route names, data flow, and component intent. Screenshots are required for visual accuracy.

## Screenshot interpretation rule

When two, three, or four screenshots appear to be from the same page, treat them as one continuous scrollable screen.

The screenshots were taken in order. Rebuilds must logically combine them to understand section order, spacing, card hierarchy, and what comes before and after each scroll position.

## Primary screenshot ranges

| Screenshot range | Visual source |
|---|---|
| IMG_0390-IMG_0392 | Home screen |
| IMG_0393 | Main side drawer |
| IMG_0394-IMG_0396 | Workplace / Professional Finnish entry |
| IMG_0397-IMG_0399 | Cards / vocabulary practice |
| IMG_0400-IMG_0408 | Professional Finnish scenario flow |
| IMG_0409-IMG_0431 | YKI Practice flow |
| IMG_0432-IMG_0479 | YKI Mock Exam flow |
| IMG_0480-IMG_0483 | Settings |
| IMG_0484-IMG_0488 | Subscription / billing |
| IMG_0489-IMG_0493 | Drawer language selector |
| IMG_0494 | Settings/profile continuation |

## YKI naming correction

There is naming confusion in the old app.

Use this distinction in native:

### YKI Practice

Correct names:

- English: YKI Practice
- Finnish: YKI-harjoittelu

Meaning:

- guided practice
- preparation route
- smaller skill tasks
- exam-style practice blocks
- old labels such as "YKI test" belong here

Screenshot source:

- IMG_0409-IMG_0431

### YKI Mock Exam

Correct names:

- English: YKI Mock Exam
- Finnish: YKI-koesimulaatio

Meaning:

- full exam simulation
- formal exam-style flow
- timed sections
- progress dots
- final result overview
- old labels such as "Full YKI exam" belong here

Screenshot source:

- IMG_0432-IMG_0479

## Visual rebuild rule

Every M19 visual patch must name the screenshot range it is matching.

Do not claim exact visual parity until the rebuilt native screen is installed on device and compared against the original screenshots.

## M19 order

1. M19-B: Side drawer rebuild from IMG_0393 and IMG_0489-IMG_0493
2. M19-C: Cards rebuild from IMG_0397-IMG_0399
3. M19-D: YKI Practice rebuild from IMG_0409-IMG_0431
4. M19-E: YKI Mock Exam rebuild from IMG_0432-IMG_0479
5. M19-F: Home rebuild from IMG_0390-IMG_0392
6. M19-G: Professional Finnish rebuild from IMG_0394-IMG_0408
7. M19-H: Settings/subscription/account rebuild from IMG_0480-IMG_0494
8. M19-I: full device screenshot comparison pass

## M19-B implementation note

Drawer visual rebuild source:

- IMG_0393
- IMG_0489-IMG_0493

Implemented in:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnUtilityDrawer.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnHomeScreen.kt`

The rebuild must still be compared on device before claiming exact drawer parity.

## M19-B3 implementation note

Drawer language picker source:

- IMG_0490

Implemented in:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/app/LearnUtilityDrawer.kt`

The language selector is a drawer popover, not a Settings navigation shortcut.

## M19-B5 implementation note

Drawer functional correction:

- Language picker now scrolls and selects enabled Learn languages.
- Selection is wired to persisted Learn language state.
- Theme and Session bottom panels are visible and usable.

This closes drawer functional blocking issues before moving faster to Cards/YKI visual rebuilds.

## M19-C implementation note

Cards visual rebuild source:

- IMG_0397
- IMG_0398
- IMG_0399

Implemented in:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/cards/CardsScreen.kt`

This pass keeps existing Cards practice behavior and changes high-impact visual structure/control styling.

## M19-D/E implementation note

YKI rebuild sources:

- IMG_0409-IMG_0431: YKI Practice
- IMG_0432-IMG_0479: YKI Mock Exam

Implemented in:

- `LearnFeatureDestination.kt`
- `LearnSignedInShell.kt`
- `LearnUtilityDrawer.kt`
- `YkiFeatureScreen.kt`
- `YkiSessionScreen.kt`

Correction:

- YKI Practice and YKI Mock Exam are now separate native destinations and visual modes.

## M19-F/G/H implementation note

Screenshot sources:

- IMG_0390-IMG_0392: Home
- IMG_0394-IMG_0408: Professional Finnish / My Profession
- IMG_0480-IMG_0494: Settings/account/subscription continuation

Implemented in:

- `LearnHomeScreen.kt`
- `ProfessionalFinnishScreen.kt`
- `LearnSettingsScreen.kt`
- `AccountScreen.kt`

No intermediate phone check is done; final device comparison comes after the full M19 sweep.
