# M24 YKI Practice Old-Source Parity

Status: ACTIVE

Source of truth:

- `/home/vitus/m24-yki-side-by-side/old/YkiPracticeScreen.tsx`
- `/home/vitus/m24-yki-side-by-side/old/ykiPracticeService.ts`

Native target:

- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/YkiPracticeExactScreen.kt`
- `apps/android/FloentlyLearn/app/src/main/java/com/floently/learn/yki/OldSourceYkiPracticeLanding.kt`

## Verified

- Old YKI Practice source was recovered from the full backup.
- Native runtime already exists and is richer than the old landing.
- Practice landing now delegates to a dedicated old-source Compose component.
- Debug build passed.
- Release build passed.

## Remaining landing parity gaps

Before freezing YKI Practice landing:

1. Match old `PageHeader` treatment or intentionally reuse native top bar if required by app shell.
2. Match old level pill spacing and active colors exactly.
3. Match old focus pill wrapping and active colors exactly.
4. Match old 2-column `TaskCard` grid dimensions.
5. Match old info card background, padding, border, and text scale.
6. Match old metrics row wrapping behavior.
7. Replace placeholder overview/count values with existing native bank-derived values where safe.
8. Ensure landing button labels match old translation keys / visible copy.
9. Keep existing task runtime intact.

## Rule

No product improvements until the landing visually matches the recovered old source as closely as possible.
