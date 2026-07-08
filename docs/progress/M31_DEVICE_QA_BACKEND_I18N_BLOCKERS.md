# M31 Device QA: Backend, I18N, Theme, Functional Parity Blockers

Status: ACTIVE

Android is improved visually, but it is not release-ready and not 100% old-app parity.

## Device QA findings

### Cards

- Everyday Finnish -> Cards opens, design is improved.
- Function is not correct.
- Card flip/question/answer behavior does not match the intended old app behavior.
- Need to verify Cards is connected to the real designed card bank/backend/generated material source.
- No limited fake/local cards should ship if backend/generated card bank exists.

Owner: Agent A

### Roleplay / Speaking

- Roleplay design is improved.
- Roleplay currently shows: request failed with status 404.
- Backend/API endpoint integration must be fixed.
- Roleplay level range is incomplete: currently A1-B2, but app needs A1-A2, B1-B2, C1-C2.
- Topic list appears too limited/hardcoded.
- Roleplay should use backend/generated topics/materials, not only a tiny fixed list.
- Flow should start more automatically when user enters roleplay, while still giving user control.
- Coach and conversation partner should feel integrated, not like confusing separate manual steps.

Owner: Agent B

### YKI Mock Exam

- YKI Mock Exam must support level selection:
  - A1-A2
  - B1-B2
  - C1-C2
- Exam material must come from the correct bank/backend/material source where available.
- No placeholder/fake content should ship.

Owner: Agent D

### Theme

- Dark/light mode toggle is not working.
- Shared theme state must be fixed globally.

Owner: Agent E

### Translation / i18n

- Drawer language switching does not translate most pages.
- Old app already has full translations for the full app in 21 languages.
- Native app must import/copy old translation catalog instead of manually retranslating.
- All shipped pages must use centralized translations instead of hardcoded text.
- This applies to Android now and later iOS.

Owner: Agent E

## Release rule

Android cannot be production-frozen until:

1. Cards use the correct card bank/materials and old behavior.
2. Roleplay backend 404 is fixed.
3. Roleplay supports A1-A2, B1-B2, C1-C2.
4. Roleplay topics/materials are backend/generated, not hardcoded-limited.
5. YKI Mock supports A1-A2, B1-B2, C1-C2.
6. Dark/light theme works globally.
7. 21-language i18n catalog is represented and wired to shipped pages.
8. Debug and release builds pass.
9. Device QA passes.
