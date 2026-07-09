# M35 Full Web-to-Native Parity Round

Status: ACTIVE

Android is not production-freeze ready.

This round must complete parity from the public Floently web entry point through the full Learn app experience.

## Source of truth

1. Screenshot lock:
   - docs/ui-lock/screenshots/*.PNG
   - docs/ui-lock/contact_sheet_*.jpg
   - docs/ui-lock/screenshot_manifest.csv

2. Old React Native / Expo app:
   - /home/vitus/m24-yki-backup-search/extracted

3. Web / marketing / landing implementation:
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/app/index.tsx
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/web/LearnLandingPage.tsx
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/features/auth/screens
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/features/onboarding
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/state/PlacementRoute.tsx
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/features/placement

4. Old i18n catalog:
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/index.ts
   - /home/vitus/m24-yki-backup-search/extracted/apps/client/features/i18n/languages.ts

## Global rule

Build success is not parity.

A feature is complete only when:
- UI matches old app / web reference
- function matches old app / web reference
- backend/material source is wired
- all visible copy is translated
- dark/light modes match
- device QA passes

## Round scope

### 1. Public Floently.com / marketing entry

Must be represented in native flow or native equivalent:

- Floently.com landing page
- product choice
- Learn landing page
- marketing sections
- animations from website/web app
- CTAs
- pricing/access route entry
- legal/support links

All public/marketing copy must be translated.

### 2. Auth flow

Must be built and translated:

- Login
- Sign up/register
- Forgot password
- Reset password
- Google sign-in boundary if present
- Access error/loading states
- Logout and account switching

### 3. Placement/onboarding flow

Must be built and translated:

- onboarding welcome
- intent quiz
- plan selection
- practice frequency
- profession selection
- placement test home
- placement questions
- placement result
- recommended path
- route into Learn

### 4. Learn shell

Must be translated and parity checked:

- Home
- Drawer/Menu
- Product navigation
- language selector
- theme selector
- streak/profile header
- access/billing entry
- back/home/menu behavior

### 5. Everyday Finnish

Must be translated and parity checked:

- landing
- A1-A2 / B1-B2 / C1-C2 level selection
- cards entry
- daily roleplay entry
- all cards, chips, buttons, labels, errors

### 6. Cards and overlays

Cards are a hard blocker in this round.

Required:

- Real card bank/backend must be used where available.
- Local fallback may exist only as offline fallback, not as primary production source.
- Decks must match old app / backend bank structure.
- Cards must include:
  - vocabulary mode
  - sentence mode
  - question side
  - answer/meaning side
  - options
  - hint
  - check
  - skip
  - recall
  - review banks
  - end session
  - report card problem
  - card bank overview
  - progress dots
  - audio/speaker button
- Card overlays must be represented:
  - translation overlay
  - explanation overlay
  - grammar overlay
  - example sentence overlay
  - hint overlay
  - report/problem overlay
  - review/difficult-card overlay
  - language-specific overlay
- When the app language changes, card overlays must use the selected language.
- Card problem report UI must translate:
  - wrong answer
  - options do not match question
  - duplicate options
  - bad Finnish
  - not a real Finnish idiom
  - bad grammar explanation
  - bad example sentence
  - audio problem
  - translation/language problem
  - other problem
- Cards must be tested in at least English, Finnish, and one non-Latin language.

### 7. Roleplay / Speaking

Must match old app UI and function.

Required old flow:

- Roleplay starts naturally/automatically when opened.
- AI speaker begins immediately.
- Topic changes dynamically each session.
- Topics come from backend/generated material where available.
- User taps mic to start recording.
- User taps mic again to stop recording and send.
- Start and stop mic sound feedback must happen.
- Mic waveform/ring animates while user speaks.
- User speech is transcribed live/as text.
- AI reply appears as text and speaks automatically.
- No separate listen button in the main flow.
- Conversation continues for exactly 5 user responses.
- Final AI response must conclude the interaction naturally.
- User can download PDF or Word conversation document.
- YKI roleplay route must exist and work.
- Everyday roleplay, YKI roleplay, and professional roleplay must not be confused.

### 8. Professional Finnish

Must match old app/web reference:

- Professional landing
- Everyday Finnish vs My Profession branch
- Profession selection
- Workplace scenarios
- Scenario practice
- Recorded speaking
- Incident practice
- profession-specific cards
- profession-specific roleplay
- interview practice
- phone/speech flow
- report writing
- A1-A2 / B1-B2 / C1-C2 support
- all copy translated

### 9. YKI

Must match old app/web reference:

- YKI Practice
- YKI Mock Exam
- YKI roleplay
- A1-A2 / B1-B2 / C1-C2 selection
- reading
- writing
- listening
- speaking
- recording
- evaluation
- results
- certificate/export
- bank/material source wiring
- all copy translated

### 10. Progress

Must match old app/web reference:

- learning progress
- streaks
- YKI readiness
- workplace communication progress
- vocabulary progress
- saved events
- fallback states
- empty states
- all copy translated

### 11. Settings

Must work and translate:

- language
- dark/light mode
- speaking speed
- hint popups
- clock format
- profile picture / gallery
- use initials
- use logo
- billing and plan
- help/support
- privacy policy
- terms of use
- support/contact
- account deletion
- delete account
- all external links

### 12. Account / billing / access

Must work and translate:

- current plan
- subscription status
- checkout
- restore purchases
- billing plan cards
- individual access
- employer/city programme access
- access type
- selected professions
- Google Play billing boundary
- Stripe/web boundary
- support/demo route
- legal route

### 13. Read and Create visibility

If visible from suite navigation, they must not contain unlocalized placeholder/demo text.

Either:
- complete parity enough for visible shell, or
- hide them from Learn release if not part of Learn Android freeze.

### 14. Dark/light theme

Must match old app/web visual system:

- every screen supports dark and light
- theme toggle updates immediately
- theme persists
- cards and overlays support both themes
- drawer supports both themes
- marketing/auth/placement screens support both themes
- gradients, borders, shadows, buttons, chips, typography, disabled states match

### 15. Full i18n requirement

Every visible word must be translated.

No hardcoded shipped text.

Use old app i18n catalog instead of manual retranslation.

Required:
- import/copy old 21-language catalog
- map all native keys to old keys
- add missing keys only when the native screen has genuinely new text
- support non-Latin scripts
- ensure language switch updates visible UI immediately
- include marketing/auth/placement/settings/account/billing/cards overlays/roleplay/YKI

### 16. Animation parity

Must include old web/app animations where relevant:

- marketing landing animations
- menu/drawer animation
- button press animation
- card flip/slide/progress animation
- mic waveform/ring animation
- loading states
- progress bars
- roleplay audio states
- exam timers/transitions

## QA gate

Final Android freeze requires:

1. Debug build passes.
2. Release build passes.
3. Installed APK passes device QA.
4. Every major flow works.
5. All selected languages translate all screens.
6. Dark/light mode works globally.
7. Cards and overlays work with backend/material source and selected language.
8. Roleplay matches old app function and UI.
9. YKI roleplay/practice/mock work.
10. Screenshot parity is accepted.
11. No reachable placeholder/demo/fake/development text remains.
