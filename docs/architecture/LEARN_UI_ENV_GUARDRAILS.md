# Learn UI and environment guardrails

## Non-negotiable direction

Floently Learn must not be rebuilt as a different product experience. Work on Learn should preserve the existing product functions, navigation intent, data contracts, and environment assumptions.

## UI rule

Allowed Learn UI work:

- Improve visual quality, spacing, typography, polish, motion, icons, cards, states, and accessibility.
- Improve screens only when the result clearly looks more beautiful and easier to use than the previous version.
- Add small navigation affordances needed by the Floently suite, such as returning to the product selector.
- Keep the Learn product recognizable as Floently Learn.

Not allowed without explicit approval:

- Rebuilding Learn from scratch.
- Replacing working Learn flows with unrelated layouts.
- Removing existing Learn destinations or changing their purpose.
- Mixing Read or Create Studio tabs into Learn.
- Changing Learn into a WebView or wrapper.

## Function rule

Learn functions must be preserved. New native code may add service boundaries and improved UI, but it must not break existing behaviour or product intent.

Preserve these areas:

- Authentication/session flow.
- Product access check semantics.
- YKI practice direction.
- Professional Finnish direction.
- Roleplay direction, including dynamic generation and anti-repetition goal.
- Cards/spaced repetition direction.
- Progress/account/device/subscription direction.

## Environment rule

Do not require new environment variables for Learn unless explicitly approved.

The native app should be plug-and-play against the existing backend assumptions and existing environment setup. Any new backend routes should be adapter-compatible with the current deployment model instead of forcing a new environment-variable migration.

## Suite rule

Floently suite work can add Learn, Read, and Create product separation around Learn, but it must not degrade the Learn experience. Learn access, Read access, and Create access remain separate.
