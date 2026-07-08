# Agent A Everyday + Cards parity update

Scope:
- Everyday Finnish UI
- Cards practice UI
- Cards preview runtime fallback
- Agent A i18n request documentation

Patch:
- Reworked Everyday Finnish away from generic hub structure.
- Matched the old locked Everyday branch shape: Everyday Finnish heading, branch description, two entry cards for Flashcards and Daily roleplay.
- Added selectable A1-A2 / B1-B2 / C1-C2 level rail inside Agent A-owned Everyday screen.
- Preserved routing to Cards and Roleplay without touching navigation enum or shared shell files.
- Brought Cards practice controls closer to old CardPracticeScreen/CardPracticeSession screenshots:
  - smaller Back pill
  - Menu visual pill
  - smaller CardModeTabs
  - Vocabulary / Sentences / Grammar mode tabs
  - tighter recall/header/progress spacing
  - old-source adaptive text sizing for long sentence cards
  - smaller review-bank and end-session controls
- Added a preview grammar deck/cards so the Grammar tab has a working fallback runtime.

Shared i18n:
- Not edited.
- Requested missing keys in docs/progress/agent-a-i18n-requests.md.
