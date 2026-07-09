# M36 Agent Prompts

Common start for every agent:

cd /home/vitus/floently-native
git pull --rebase origin m01/production-native-foundation
git status --short

Read:
docs/progress/M36_FASTEST_PATH_CROSS_CUTTING_PARITY.md

Rules:
- Work locally only.
- Do not use GitHub web UI.
- Stay in your M36 ownership.
- Pull/rebase before starting and before pushing.
- Build before commit and before push.
- Commit with your agent prefix.
- Build success is not parity.
- Device QA and screenshot parity are the gate.

## Agent A prompt

You are Agent A for M36 Global Localization.

Read:
docs/handover/m36/AGENT_A_GLOBAL_LOCALIZATION.md

Fix the language architecture and hardcoded strings. All 20 languages must work. RTL must work.

## Agent B prompt

You are Agent B for M36 Audio/Speech/Roleplay Runtime.

Read:
docs/handover/m36/AGENT_B_AUDIO_ROLEPLAY_RUNTIME.md

Make Roleplay and YKI Roleplay behave like the old app on device.

## Agent C prompt

You are Agent C for M36 Navigation/Public/Auth/Payment Flow.

Read:
docs/handover/m36/AGENT_C_NAV_PUBLIC_AUTH_PAYMENT.md

Wire the full public journey from Floently landing to auth, placement, account, billing/payment, and Learn.

## Agent D prompt

You are Agent D for M36 Backend/Material Parity.

Read:
docs/handover/m36/AGENT_D_BACKEND_MATERIAL_PARITY.md

Verify and fix backend/material wiring for cards, overlays, roleplay, YKI, progress, account, billing.

## Agent E prompt

You are Agent E for M36 Theme/Design System/Final QA.

Read:
docs/handover/m36/AGENT_E_THEME_DESIGN_FINAL_QA.md

Fix global theme/light-dark/design system/logo parity and run final QA gates.
