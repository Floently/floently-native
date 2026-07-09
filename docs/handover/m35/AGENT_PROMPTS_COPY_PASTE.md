# M35 Agent Prompts

## Common opening for every agent

You are working on Floently Android M35 full web-to-native parity.

Repo:
cd /home/vitus/floently-native

Branch:
m01/production-native-foundation

Rules:
- Work locally only.
- Do not edit GitHub web UI.
- Pull/rebase before starting.
- Stay inside your ownership.
- If you need shared i18n/theme/navigation changes, write a request doc instead of editing outside scope.
- Build success is not parity.
- Device QA and screenshot parity are the gate.
- Commit with your agent prefix.
- Push to the shared branch.

Start:
cd /home/vitus/floently-native
git pull --rebase origin m01/production-native-foundation
git status --short

Read:
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

## Agent A prompt

You are Agent A.

Read:
docs/handover/m35/AGENT_A_M35_CARDS_OVERLAYS.md
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

Own Cards only. Close card banks, overlays, report overlay, language-specific overlay behavior, card flip/check/skip/review flow, and backend/material source parity.

## Agent B prompt

You are Agent B.

Read:
docs/handover/m35/AGENT_B_M35_ROLEPLAY_SPEAKING.md
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

Own Roleplay/Speaking only. Make old-app roleplay UI/function work: automatic AI start, mic tap start/stop, sound feedback, waveform, transcript, AI auto speech, 5 user turns, conclusive final response, PDF/Word export, and YKI roleplay route.

## Agent C prompt

You are Agent C.

Read:
docs/handover/m35/AGENT_C_M35_PUBLIC_WEB_PROFESSIONAL_UI.md
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

Own public/native web entry screens, Learn landing, onboarding/placement UI, Professional UI, and website/web-app animation equivalents. Do not edit shared i18n/theme directly.

## Agent D prompt

You are Agent D.

Read:
docs/handover/m35/AGENT_D_M35_YKI_PROGRESS_BANKS.md
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

Own YKI and Progress. Close YKI Practice, YKI Mock, YKI roleplay materials, banks, recording/evaluation/results/export, and durable progress parity.

## Agent E prompt

You are Agent E.

Read:
docs/handover/m35/AGENT_E_M35_I18N_THEME_INTEGRATION.md
docs/progress/M35_FULL_WEB_TO_NATIVE_PARITY_ROUND.md

Own full i18n, theme, settings/account/auth/navigation integration, builds, merge conflicts, and final release gate. Your primary job is language parity.
